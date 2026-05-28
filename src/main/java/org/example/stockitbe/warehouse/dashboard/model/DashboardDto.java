package org.example.stockitbe.warehouse.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;

import java.util.Map;

public class DashboardDto {

    @Schema(description = "창고 입고 진행률 응답 — KPI 카드 + PO 상태 분포")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class InboundProgressRes {
        @Schema(description = "입고 진행률 KPI 카드 (예정/완료/총건/진행률/평균 처리시간)")
        private Kpi kpi;
        @Schema(description = "PO 상태별 건수 분포 (READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED 등)",
                example = "{\"READY_TO_SHIP\":4,\"IN_TRANSIT\":2,\"ARRIVED\":3,\"COMPLETED\":12}")
        private Map<PurchaseOrderStatus, Long> statusBreakdown;
    }

    @Schema(description = "입고 진행률 KPI")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class Kpi {
        @Schema(description = "예정 입고 건수 (READY_TO_SHIP + IN_TRANSIT + ARRIVED)", example = "9")
        private long scheduledCount;
        @Schema(description = "완료된 입고 건수 (COMPLETED)", example = "12")
        private long completedCount;
        @Schema(description = "총 입고 건수 (scheduled + completed)", example = "21")
        private long totalCount;
        @Schema(description = "입고 진행률 (0.0 ~ 1.0)", example = "0.57")
        private double progressRate;
        @Schema(description = "평균 처리 시간 (시간 단위, null 가능)", example = "26.4", nullable = true)
        private Double avgProcessingHours;
    }
}
