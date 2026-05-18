package org.example.stockitbe.notification;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.notification.model.dto.NotificationDto;
import org.example.stockitbe.notification.model.entity.Notification;
import org.springframework.scheduling.annotation.Scheduled;
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

    public void push(Notification n) {
        NotificationDto.SsePayload payload = NotificationDto.SsePayload.from(n);

        if (n.getTargetUserId() != null) {
            EmitterEntry entry = emitters.get(n.getTargetUserId());
            if (entry != null) sendOne(n.getTargetUserId(), entry, payload);
            return;
        }
        if (n.getTargetRole() == null) return;
        for (Map.Entry<Long, EmitterEntry> kv : emitters.entrySet()) {
            EmitterEntry e = kv.getValue();
            if (!n.getTargetRole().equals(e.role)) continue;
            if (n.getTargetLocationCode() != null
                    && !n.getTargetLocationCode().equals(e.locationCode)) continue;
            sendOne(kv.getKey(), e, payload);
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
