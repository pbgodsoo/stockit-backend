package org.example.stockitbe.notification.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.notification.model.entity.NotificationSeverity;
import org.example.stockitbe.notification.model.entity.NotificationType;

import java.util.Date;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_role_loc_unread",
                columnList = "target_role,target_location_code,is_read,create_date"),
        @Index(name = "idx_notification_user_unread",
                columnList = "target_user_id,is_read,create_date"),
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

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Date readAt;

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
        this.read = false;
    }

    public void markAsRead(Date now) {
        this.read = true;
        this.readAt = now;
    }
}