package org.example.stockitbe.notification.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.stockitbe.notification.model.entity.Notification;

import java.util.Date;
import java.util.List;

public class NotificationDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class NotificationRes {
        private Long id;
        private String type;
        private String severity;
        private String title;
        private String message;
        private String refType;
        private String refId;
        private boolean read;
        private Date readAt;
        private Date createdAt;

        public static NotificationRes from(Notification e) {
            return NotificationRes.builder()
                    .id(e.getId())
                    .type(e.getType().name())
                    .severity(e.getSeverity().name())
                    .title(e.getTitle())
                    .message(e.getMessage())
                    .refType(e.getRefType())
                    .refId(e.getRefId())
                    .read(e.isRead())
                    .readAt(e.getReadAt())
                    .createdAt(e.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class NotificationListRes {
        private List<NotificationRes> items;
        private long totalElements;
        private int totalPages;
        private int page;
        private int size;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class UnreadCountRes {
        private long count;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SsePayload {
        private Long id;
        private String type;
        private String severity;
        private String title;
        private String message;
        private Date createdAt;

        public static SsePayload from(Notification e) {
            return SsePayload.builder()
                    .id(e.getId())
                    .type(e.getType().name())
                    .severity(e.getSeverity().name())
                    .title(e.getTitle())
                    .message(e.getMessage())
                    .createdAt(e.getCreatedAt())
                    .build();
        }
    }
}