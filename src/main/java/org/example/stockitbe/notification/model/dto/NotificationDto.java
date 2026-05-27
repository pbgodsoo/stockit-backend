package org.example.stockitbe.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "알림 단건 응답")
    public static class NotificationRes {
        // 응답 필드 최소화 (2026-05-23) — FE 가 실제로 사용하는 7개만 응답.
        // 제거된 필드: refType, refId, readAt (FE 미사용 — 통신량 약 25%↓ + 페이로드 의도 명확화)
        // 제거 정책 근거: docs/plan/notification/응답 필드 최소화.md (예정) — DB 컬럼/Entity 필드는 유지.
        @Schema(description = "알림 ID", example = "1")
        private Long id;
        @Schema(description = "알림 종류", example = "USER_SIGNUP_PENDING",
                allowableValues = {"INVENTORY_SHORTAGE", "INVENTORY_OUT_OF_STOCK", "USER_SIGNUP_PENDING", "CIRCULAR_CANDIDATE"})
        private String type;
        @Schema(description = "중요도", example = "INFO",
                allowableValues = {"INFO", "WARNING", "CRITICAL"})
        private String severity;
        @Schema(description = "제목", example = "신규 회원가입 신청")
        private String title;
        @Schema(description = "본문", example = "테스트유저(test@stockit.com) 회원가입 승인 대기 중입니다.")
        private String message;
        @Schema(description = "본인 읽음 여부", example = "false")
        private boolean read;
        @Schema(description = "발생 시각")
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
    @Schema(description = "알림 목록 응답")
    public static class NotificationListRes {
        @Schema(description = "알림 항목 리스트")
        private List<NotificationRes> items;
        @Schema(description = "전체 건수", example = "27")
        private long totalElements;
        @Schema(description = "전체 페이지 수", example = "2")
        private int totalPages;
        @Schema(description = "현재 페이지 (0-based)", example = "0")
        private int page;
        @Schema(description = "페이지 크기", example = "20")
        private int size;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "미읽음 카운트 응답")
    public static class UnreadCountRes {
        @Schema(description = "미읽음 알림 개수", example = "4")
        private long count;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "SSE 푸시 페이로드")
    public static class SsePayload {
        @Schema(description = "알림 ID", example = "1")
        private Long id;
        @Schema(description = "알림 종류", example = "USER_SIGNUP_PENDING")
        private String type;
        @Schema(description = "중요도", example = "INFO")
        private String severity;
        @Schema(description = "제목", example = "신규 회원가입 신청")
        private String title;
        @Schema(description = "본문", example = "테스트유저(test@stockit.com) 회원가입 승인 대기 중입니다.")
        private String message;
        @Schema(description = "발생 시각")
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
