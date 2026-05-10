package org.example.stockitbe.hq.analytics.orderstatsanalytics.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public class OrderStatsAnalyticsDto {

    @Getter
    @Builder
    public static class Res {
        private String fromDate;
        private String toDate;
        private OrderStatsPeriod period;
        private KpiSummary kpi;
        private List<WarehouseStats> warehouseOrders;
        private List<ItemCycle> orderCycleData;
        private List<ProductCycle> productOrderData;
        private List<MonthlyTrendPoint> monthlyTrend;
    }


    @Getter
    @Builder
    public static class KpiSummary {
        private int managedItemCount;       // 발주 이력 있는 품목 수 (L2)
        private int avgCycleDays;           // 품목 평균 주기 (전체 평균)
        private int shortestCycleDays;
        private String shortestCycleItem;   // "반팔"
        private int longestCycleDays;
        private String longestCycleItem;
        private long totalOrders;           // 누적 발주 건수 (라인 합)
    }

    @Getter
    @Builder
    public static class WarehouseStats {
        private String warehouseCode;
        private String warehouseName;
        private long orders;
        private long items;
        private BigDecimal totalValue;      // 원
        private BigDecimal sharePct;        // %
    }

    @Getter
    @Builder
    public static class ItemCycle {
        private String item;                // "반팔"
        private String category;            // "상의"
        private int avgCycle;
        private long avgQty;
        private long totalOrders;
        private String lastOrderedAt;       // yyyy-MM-dd
    }

    @Getter
    @Builder
    public static class ProductCycle {
        private String item;                // 상품명 (PRD-TOP-SS-001 → "코튼 에센셜 크루 반팔")
        private String productCode;
        private String productType;         // "반팔"
        private String category;            // "상의"
        private int avgCycle;
        private long avgQty;
        private long totalOrders;
        private String lastOrderedAt;
    }

    @Getter
    @Builder
    public static class MonthlyTrendPoint {
        private String month;               // "2026-04"
        private long orders;
        private long items;
    }
}
