package org.example.stockitbe.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.notification.NotificationService;
import org.example.stockitbe.notification.NotificationSseService;
import org.example.stockitbe.notification.model.entity.Notification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 원 트랜잭션 커밋 직후 알림 저장 + SSE 발송 (계획서 §4-7)
 * - AFTER_COMMIT: 원 트랜잭션 롤백 시 알림 발생 X
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final NotificationSseService sseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationEvent e) {
        // ① DB 영속화 — 실패하면 SSE 안 보내고 종료
        Notification saved;
        try {
            saved = notificationService.persist(e);
        } catch (Exception ex) {
            log.error("[NotificationEventListener] persist 실패 type={} ref={}-{}",
                    e.getType(), e.getRefType(), e.getRefId(), ex);
            return;
        }
        // ② SSE 푸시 — 실패해도 알림은 DB 에 저장됨
        try {
            sseService.push(saved);
        } catch (Exception ex) {
            log.warn("[NotificationEventListener] SSE 푸시 실패 id={} — DB 저장 완료",
                    saved.getId());
        }
    }

}
