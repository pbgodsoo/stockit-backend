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
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationSseService {

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L; // 30분

    private final Map<Long, EmitterEntry> emitters = new ConcurrentHashMap<>();

    // SSE push 비동기 fan-out 전용 스레드 풀 (AsyncConfig#notificationExecutor)
    // 1500+ HQ admin fan-out 시 사용자별 emitter.send 를 별도 스레드에서 병렬 실행.
    private final ThreadPoolTaskExecutor notificationExecutor;

    public NotificationSseService(ThreadPoolTaskExecutor notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    @PreDestroy
    void shutdown() {
        emitters.values().forEach(e -> { try { e.emitter.complete(); } catch (Exception ignored) {} });
        emitters.clear();
    }

    public SseEmitter subscribe(Long userId, String role, String locationCode) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        EmitterEntry entry = new EmitterEntry(emitter, role, locationCode);
        emitters.put(userId, entry);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> { emitters.remove(userId); emitter.complete(); });
        emitter.onError((e) -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("ok"));
        } catch (IOException e) {
            emitters.remove(userId);
        }
        return emitter;
    }

    // 알림 fan-out 디스패치.
    // 도메인 응답 스레드(EventListener) 는 emitter 목록 순회 + executor.execute(submit) 만 수행 → 즉시 반환.
    // 실제 emitter.send 는 notificationExecutor 풀에서 사용자별 병렬 실행.
    //   - 1500명 fan-out 시 동기 직렬: 50ms × 1500 = 75초 (도메인 응답 묶임)
    //   - 비동기 병렬 (32 스레드): 도메인 응답 즉시 반환 + send 실제 시간 ~2.3초
    public void push(Notification n) {
        NotificationDto.SsePayload payload = NotificationDto.SsePayload.from(n);

        if (n.getTargetUserId() != null) {
            // 직접 수신 — 해당 사용자 emitter 만 비동기 send
            EmitterEntry entry = emitters.get(n.getTargetUserId());
            if (entry != null) {
                final Long uid = n.getTargetUserId();
                notificationExecutor.execute(() -> sendOne(uid, entry, payload));
            }
            return;
        }
        if (n.getTargetRole() == null) return;

        // broadcast — 권한군의 모든 emitter 에 비동기 fan-out
        for (Map.Entry<Long, EmitterEntry> kv : emitters.entrySet()) {
            EmitterEntry e = kv.getValue();
            if (!n.getTargetRole().equals(e.role)) continue;
            if (n.getTargetLocationCode() != null
                    && !n.getTargetLocationCode().equals(e.locationCode)) continue;
            // 사용자별 send 를 별도 스레드 풀로 위임 (병렬 실행)
            final Long uid = kv.getKey();
            notificationExecutor.execute(() -> sendOne(uid, e, payload));
        }
    }

    private void sendOne(Long userId, EmitterEntry entry, NotificationDto.SsePayload payload) {
        try {
            entry.emitter.send(SseEmitter.event().name("notification").data(payload));
        } catch (Exception ex) {
            log.warn("[NotificationSseService] push 실패 userId={} — emitter 정리", userId);
            emitters.remove(userId);
            try { entry.emitter.complete(); } catch (Exception ignored) {}
        }
    }

    @Scheduled(fixedRate = 30_000L)
    void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        for (Map.Entry<Long, EmitterEntry> kv : emitters.entrySet()) {
            try {
                kv.getValue().emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception ex) {
                // 클라이언트 끊김 — emitter 정리 + complete 호출 (다음 cycle 부터 깨끗)
                log.warn("[NotificationSseService] heartbeat 실패 userId={} — emitter 정리",
                        kv.getKey());
                emitters.remove(kv.getKey());
                try { kv.getValue().emitter.complete(); } catch (Exception ignored) {}
            }
        }
    }

    private static class EmitterEntry {
        final SseEmitter emitter;
        final String role;
        final String locationCode;
        EmitterEntry(SseEmitter emitter, String role, String locationCode) {
            this.emitter = emitter;
            this.role = role;
            this.locationCode = locationCode;
        }
    }
}
