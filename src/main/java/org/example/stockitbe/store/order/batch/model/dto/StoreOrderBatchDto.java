package org.example.stockitbe.store.order.batch.model.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.store.order.batch.model.enums.StoreOrderBatchScope;
import org.example.stockitbe.store.order.batch.model.enums.StoreOrderBatchTriggerType;

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
