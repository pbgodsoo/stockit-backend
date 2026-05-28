package org.example.stockitbe.store.order.batch.model.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.store.order.batch.model.enums.StoreOrderBatchScope;
import org.example.stockitbe.store.order.batch.model.enums.StoreOrderBatchTriggerType;

import java.time.LocalDateTime;
import java.util.List;

public class StoreOrderBatchDto {

    // 매장 발주 수동 배치 처리 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RunReq {
        private StoreOrderBatchScope mode;
        private String storeCode;

        // Reader SQL의 BETWEEN 절에 직접 전달되는 처리 대상 기간.
        // 호출자(Controller/Scheduler)가 범위를 결정해 전달하므로 BatchConfig 내부에서 날짜를 계산하지 않는다.
        private LocalDateTime fromDateTime;

        private LocalDateTime toDateTime;

        // Bean Validation으로는 두 필드 조합 검증이 불가능하므로 @AssertTrue로 보완.
        // isXxx() 네이밍 규칙을 따라야 Bean Validation이 getter로 인식한다.
        @AssertTrue(message = "mode=STORE requires storeCode")
        public boolean isStoreCodeValidForMode() {
            if (mode != StoreOrderBatchScope.STORE) return true;
            return storeCode != null && !storeCode.trim().isEmpty();
        }
    }

    // 매장 발주 수동 배치 처리 응답 DTO
    @Getter
    @AllArgsConstructor
    @Builder
    public static class RunRes {
        private String runId;
        private StoreOrderBatchTriggerType triggerType;
        private StoreOrderBatchScope scope;
        private String storeCode;
        private Integer requestedCount;
        private Integer successCount;
        private Integer failCount;
        private List<ItemRes> results;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ItemRes {
        private String orderNo;
        private String result;
        private Integer code;
        private String message;
    }

    // 승인 대기 발주건이 있는 매장 조회 응답 DTO
    @Getter
    @AllArgsConstructor
    @Builder
    public static class PendingStoreRes {
        private String storeCode;
        private String storeName;
        private String region;
        private Integer requestedCount;
    }
}
