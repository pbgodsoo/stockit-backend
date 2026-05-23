package org.example.stockitbe.notification.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

// 알림 개별 읽음 추적 (B3 broadcast 읽음 문제 해결)
//   - 알림 본문(Notification) 은 권한군 broadcast 1행
//   - 읽음 상태는 (notification_id, user_id) 사용자별 매핑
//   - 한 admin 이 읽어도 다른 admin 의 미읽음 상태는 유지됨
@Entity
@Table(name = "notification_read", indexes = {
        @Index(name = "idx_notification_read_user", columnList = "user_id,notification_id")
})
@IdClass(NotificationReadId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRead {

    @Id
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private Date readAt;

    @Builder
    private NotificationRead(Long notificationId, Long userId, Date readAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.readAt = readAt;
    }
}
