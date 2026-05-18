package org.example.stockitbe.notification.event;

import lombok.Builder;
import lombok.Getter;
import org.example.stockitbe.notification.model.entity.NotificationSeverity;
import org.example.stockitbe.notification.model.entity.NotificationType;
import org.example.stockitbe.user.model.entity.UserRole;

@Getter
@Builder
public class NotificationEvent {
    private NotificationType type;
    private NotificationSeverity severity;
    private String title;
    private String message;
    private UserRole targetRole;          // null 가능
    private String targetLocationCode;    // null 가능
    private Long targetUserId;            // null 가능
    private String refType;
    private String refId;
}