package org.example.stockitbe.warehouse.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;

import java.util.Map;

public class DashboardDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class InboundProgressRes {
        private Kpi kpi;
        private Map<PurchaseOrderStatus, Long> statusBreakdown;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Kpi {
        private long scheduledCount;
        private long completedCount;
        private long totalCount;
        private double progressRate;
        private Double avgProcessingHours;
    }
}
