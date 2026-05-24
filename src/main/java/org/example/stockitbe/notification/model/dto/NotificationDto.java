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
        // 응답 필드 최소화 (2026-05-23) — FE 가 실제로 사용하는 7개만 응답.
        // 제거된 필드: refType, refId, readAt (FE 미사용 — 통신량 약 25%↓ + 페이로드 의도 명확화)
        // 제거 정책 근거: docs/plan/notification/응답 필드 최소화.md (예정) — DB 컬럼/Entity 필드는 유지.
        private Long id;
        private String type;
        private String severity;
        private String title;
        private String message;
        private boolean read;
        private Date createdAt;

        // 본인 기준 읽음 상태와 함께 변환.
        // myReadAt 가 null 이면 본인이 아직 읽지 않은 알림 → read=false.
        // (개별 읽음 추적: notification_read 매핑 테이블에서 LEFT JOIN 으로 가져온 본인 read_at)
        // 시그니처 (Notification, Date) 는 유지 — Service.list 의 V2 호출부 호환성 보장.
        public static NotificationRes from(Notification e, Date myReadAt) {
            return NotificationRes.builder()
                    .id(e.getId())
                    .type(e.getType().name())
                    .severity(e.getSeverity().name())
                    .title(e.getTitle())
                    .message(e.getMessage())
                    .read(myReadAt != null)
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