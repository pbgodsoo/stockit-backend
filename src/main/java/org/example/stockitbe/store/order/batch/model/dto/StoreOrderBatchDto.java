package org.example.stockitbe.store.order.batch.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.store.order.batch.model.enums.StoreOrderBatchScope;
import org.example.stockitbe.store.order.batch.model.enums.StoreOrderBatchTriggerType;

import java.time.LocalDateTime;
import java.util.List;

public class StoreOrderBatchDto {

    @Schema(description = "발주 배치 승인 수동 실행 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RunReq {
        @Schema(description = "배치 범위 (ALL / STORE)", example = "ALL")
        @NotNull
        private StoreOrderBatchScope mode;
        @Schema(description = "매장 코드 (mode=STORE일 때 필수)", example = "ST-SL-0001")
        private String storeCode;

        // Reader SQL의 BETWEEN 절에 직접 전달되는 처리 대상 기간.
        // 호출자(Controller/Scheduler)가 범위를 결정해 전달하므로 BatchConfig 내부에서 날짜를 계산하지 않는다.
        @Schema(description = "처리 대상 시작 일시 (포함)", example = "2026-05-01T00:00:00")
        private LocalDateTime fromDateTime;
        @Schema(description = "처리 대상 종료 일시 (포함)", example = "2024-01-31T23:59:59")
        private LocalDateTime toDateTime;

        // Bean Validation으로는 두 필드 조합 검증이 불가능하므로 @AssertTrue로 보완.
        // isXxx() 네이밍 규칙을 따라야 Bean Validation이 getter로 인식한다.
        @AssertTrue(message = "mode=STORE requires storeCode")
        @Schema(hidden = true)
        public boolean isStoreCodeValidForMode() {
            if (mode != StoreOrderBatchScope.STORE) return true;
            return storeCode != null && !storeCode.trim().isEmpty();
        }
    }

    @Schema(description = "발주 배치 승인 실행 결과 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class RunRes {
        @Schema(description = "Job Execution ID", example = "42")
        private String runId;
        @Schema(description = "실행 유형")
        private StoreOrderBatchTriggerType triggerType;
        @Schema(description = "처리 범위")
        private StoreOrderBatchScope scope;
        @Schema(description = "매장 코드 (STORE 범위일 때)", example = "ST-SL-0001")
        private String storeCode;
        @Schema(description = "처리 요청 건수", example = "15")
        private Integer requestedCount;
        @Schema(description = "성공 건수", example = "14")
        private Integer successCount;
        @Schema(description = "실패 건수", example = "1")
        private Integer failCount;
        @Schema(description = "건별 처리 결과 목록")
        private List<ItemRes> results;
    }

    @Schema(description = "배치 건별 처리 결과 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ItemRes {
        @Schema(description = "발주번호", example = "SOR-20260510-00001")
        private String orderNo;
        @Schema(description = "처리 결과 (SUCCESS / FAIL)", example = "SUCCESS")
        private String result;
        @Schema(description = "결과 코드", example = "1000")
        private Integer code;
        @Schema(description = "결과 메시지", example = "승인 완료")
        private String message;
    }

    @Schema(description = "승인 대기 발주 존재 매장 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class PendingStoreRes {
        @Schema(description = "매장 코드", example = "ST-SL-0001")
        private String storeCode;
        @Schema(description = "매장명", example = "강남점")
        private String storeName;
        @Schema(description = "지역", example = "서울")
        private String region;
        @Schema(description = "승인 대기 건수", example = "3")
        private Integer requestedCount;
    }
}
