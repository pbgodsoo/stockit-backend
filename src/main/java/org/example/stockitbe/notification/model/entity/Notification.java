package org.example.stockitbe.notification.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

// 알림 본문 (broadcast 패턴 — 권한군에 1행 INSERT).
// 읽음 상태는 NotificationRead 매핑 테이블에서 사용자별로 추적 (B3 해결).
//   - 본 엔티티에서 is_read / read_at 필드는 폐기됨 (2026-05-23)
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_role_loc",
                columnList = "target_role,target_location_code,create_date"),
        @Index(name = "idx_notification_user",
                columnList = "target_user_id,create_date"),
        @Index(name = "idx_notification_ref",
                columnList = "ref_type,ref_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private NotificationSeverity severity;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "target_role", length = 20)
    private String targetRole;

    @Column(name = "target_location_code", length = 20)
    private String targetLocationCode;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "ref_type", length = 40)
    private String refType;

    @Column(name = "ref_id", length = 100)
    private String refId;

    @Builder
    private Notification(NotificationType type, NotificationSeverity severity,
                         String title, String message,
                         String targetRole, String targetLocationCode, Long targetUserId,
                         String refType, String refId) {
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.targetRole = targetRole;
        this.targetLocationCode = targetLocationCode;
        this.targetUserId = targetUserId;
        this.refType = refType;
        this.refId = refId;
    }
}
