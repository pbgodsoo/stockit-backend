package org.example.stockitbe.notification;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.notification.model.dto.NotificationDto;
import org.example.stockitbe.notification.model.entity.Notification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SSE 알림 fan-out 서비스 (v2 — emitter 생명주기/동시성 안정화).
 *
 * 운영 장애 패턴 ("RecycleRequiredException + heartbeat 실패 + taskScheduler queue 누적") 의
 * 근본 원인 4가지를 모두 정리:
 *  1. sessionId 기반 다중 emitter — 사용자가 다중 탭/새로고침/EventSource 재연결 해도
 *     이전 emitter 가 leak 없이 공존 (이전: Map<Long, EmitterEntry> 라 덮어쓰면서 leak).
 *  2. compare-and-remove — onCompletion/onTimeout/onError 콜백에서 (sessionId, entry) 매칭으로 제거.
 *     같은 userId 의 옛 emitter 콜백이 새 emitter 를 silent 하게 삭제하는 race condition 차단.
 *  3. per-emitter ReentrantLock — heartbeat × push 가 같은 emitter 에 동시 send() 시
 *     출력 스트림 interleaved write 로 깨지지 않도록 직렬화.
 *  4. heartbeat 도 notificationExecutor 로 비동기 fan-out — 단일 @Scheduled 스레드 점유 해소.
 *     1500명 × 50ms = 75초 누적되어 taskScheduler queue 가 쌓이던 문제 제거.
 */
@Service
@Slf4j
public class NotificationSseService {

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L; // 30분

    // key = sessionId (UUID). 같은 userId 에 대해 여러 emitter (다중 탭 등) 공존 허용.
    private final Map<String, EmitterEntry> emitters = new ConcurrentHashMap<>();

    // SSE push 비동기 fan-out 전용 스레드 풀 (AsyncConfig#notificationExecutor)
    // 1500+ HQ admin fan-out 시 사용자별 emitter.send 를 별도 스레드에서 병렬 실행.
    private final ThreadPoolTaskExecutor notificationExecutor;

    public NotificationSseService(ThreadPoolTaskExecutor notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    @PreDestroy
    void shutdown() {
        // graceful — 각 emitter 에 lock 잡고 complete() 호출 (동시 send 중인 emitter 와 충돌 방지)
        for (EmitterEntry entry : emitters.values()) {
            entry.lock.lock();
            try {
                try { entry.emitter.complete(); } catch (Exception ignored) {}
            } finally {
                entry.lock.unlock();
            }
        }
        emitters.clear();
    }

    public SseEmitter subscribe(Long userId, String role, String locationCode) {
        // 매 구독마다 고유 sessionId — 같은 userId 가 새 탭으로 다시 구독해도 별도 entry 로 보관.
        final String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        EmitterEntry entry = new EmitterEntry(emitter, userId, role, locationCode);
        emitters.put(sessionId, entry);

        // compare-and-remove — emitters.remove(K, V) 가 false 면 이미 다른 콜백이 정리한 상태이거나
        // 같은 sessionId 에 새 entry 가 들어간 상태라 안전하게 통과.
        // onError/onTimeout 모두 complete() 명시 호출 — 안 하면 Tomcat async request 가 정상 종료 안 돼
        // 'RecycleRequiredException' (Encountered a non-recycled request) 발화 가능.
        emitter.onCompletion(() -> emitters.remove(sessionId, entry));
        emitter.onTimeout(() -> {
            emitters.remove(sessionId, entry);
            try { emitter.complete(); } catch (Exception ignored) {}
        });
        emitter.onError((e) -> {
            emitters.remove(sessionId, entry);
            try { emitter.complete(); } catch (Exception ignored) {}
        });

        // 초기 connect 이벤트 — lock 보호 (이론상 첫 send 는 단일 스레드지만 일관성 위해)
        entry.lock.lock();
        try {
            emitter.send(SseEmitter.event().name("connect").data("ok"));
        } catch (IOException e) {
            emitters.remove(sessionId, entry);
        } finally {
            entry.lock.unlock();
        }
        return emitter;
    }

    /**
     * 알림 fan-out 디스패치.
     *  - 도메인 응답 스레드는 emitter 목록 순회 + executor.execute(submit) 만 수행 → 즉시 반환.
     *  - 실제 emitter.send 는 notificationExecutor 풀에서 사용자별 병렬 실행.
     */
    public void push(Notification n) {
        NotificationDto.SsePayload payload = NotificationDto.SsePayload.from(n);

        if (n.getTargetUserId() != null) {
            // 직접 수신 — 해당 userId 의 모든 emitter (다중 탭) 에 비동기 send
            final Long targetUid = n.getTargetUserId();
            for (Map.Entry<String, EmitterEntry> kv : emitters.entrySet()) {
                EmitterEntry e = kv.getValue();
                if (!targetUid.equals(e.userId)) continue;
                final String sid = kv.getKey();
                notificationExecutor.execute(() -> sendOne(sid, e, payload));
            }
            return;
        }
        if (n.getTargetRole() == null) return;

        // broadcast — 권한군의 모든 emitter 에 비동기 fan-out
        for (Map.Entry<String, EmitterEntry> kv : emitters.entrySet()) {
            EmitterEntry e = kv.getValue();
            if (!n.getTargetRole().equals(e.role)) continue;
            if (n.getTargetLocationCode() != null
                    && !n.getTargetLocationCode().equals(e.locationCode)) continue;
            // 사용자별 send 를 별도 스레드 풀로 위임 (병렬 실행)
            final String sid = kv.getKey();
            notificationExecutor.execute(() -> sendOne(sid, e, payload));
        }
    }

    /** 단일 emitter 에 알림 이벤트 send — per-emitter lock 으로 직렬화. */
    private void sendOne(String sessionId, EmitterEntry entry, NotificationDto.SsePayload payload) {
        entry.lock.lock();
        try {
            entry.emitter.send(SseEmitter.event().name("notification").data(payload));
        } catch (Exception ex) {
            log.warn("[NotificationSseService] push 실패 sessionId={} userId={} — emitter 정리",
                    sessionId, entry.userId);
            // compare-and-remove — 같은 sessionId 에 다른 entry 가 들어간 경우 건드리지 않음
            emitters.remove(sessionId, entry);
            try { entry.emitter.complete(); } catch (Exception ignored) {}
        } finally {
            entry.lock.unlock();
        }
    }

    /**
     * heartbeat — 모든 emitter 에 30초마다 ping comment 전송.
     *  v1 의 단일 스레드 직렬 send 를 폐기하고 notificationExecutor 로 비동기 fan-out.
     *  → 1500명 × 50ms 직렬(75초) 누적 문제 해결 + push 와 동일 패턴 일관성.
     */
    @Scheduled(fixedRate = 30_000L)
    void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        for (Map.Entry<String, EmitterEntry> kv : emitters.entrySet()) {
            final String sid = kv.getKey();
            final EmitterEntry entry = kv.getValue();
            notificationExecutor.execute(() -> sendHeartbeatOne(sid, entry));
        }
    }

    /** 단일 emitter 에 heartbeat ping — per-emitter lock 으로 push 와의 동시 send 직렬화. */
    private void sendHeartbeatOne(String sessionId, EmitterEntry entry) {
        entry.lock.lock();
        try {
            entry.emitter.send(SseEmitter.event().comment("ping"));
        } catch (Exception ex) {
            log.warn("[NotificationSseService] heartbeat 실패 sessionId={} userId={} — emitter 정리",
                    sessionId, entry.userId);
            emitters.remove(sessionId, entry);
            try { entry.emitter.complete(); } catch (Exception ignored) {}
        } finally {
            entry.lock.unlock();
        }
    }

    /**
     * Emitter 1개의 메타 정보 + 동시 send 직렬화용 lock.
     *  - userId : 사용자별 fan-out 필터링용 (key 가 sessionId 이므로 별도 보관 필요)
     *  - role / locationCode : broadcast 권한군 매칭용
     *  - lock : heartbeat × push 동시 send 시 SseEmitter 출력 스트림 직렬화 보장
     */
    private static class EmitterEntry {
        final SseEmitter emitter;
        final Long userId;
        final String role;
        final String locationCode;
        final ReentrantLock lock = new ReentrantLock();

        EmitterEntry(SseEmitter emitter, Long userId, String role, String locationCode) {
            this.emitter = emitter;
            this.userId = userId;
            this.role = role;
            this.locationCode = locationCode;
        }
    }
}
