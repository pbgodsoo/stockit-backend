package org.example.stockitbe.hq.analytics.dashboardanalytics.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class DashboardAnalyticsDto {

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Res {
        private String fromDate;
        private String toDate;
        private DashboardPeriod period;
        private KpiSummary kpi;
        private List<TrendPoint> trendCurrent;   // 일자별 매출 추이
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;       // "04/14" 또는 "2026-04"
        private BigDecimal revenue; // 원
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class KpiSummary {
        // ━━ KPI 5카드 ━━
        private BigDecimal totalRevenue;
        private BigDecimal totalRevenueTrendPct;
        private BigDecimal lockedValue;
        private Long dangerSkuCount;
        private Long totalSkuCount;
        private Integer activeVendorCount;
        private Integer activeMaterialCount;
        private BigDecimal circularSalesAmount;

        // 카드 1 (Sales 점프): 매출 1위 품목 + 베스트 카테고리
        private String topProductName;
        private BigDecimal topProductSales;
        private Long topProductUnits;
        private String bestCategoryName;
        private BigDecimal bestCategoryAmount;
        private BigDecimal bestCategorySharePct;
        private Long totalSalesQty;



        // 카드 2 (Turnover 점프): 재고 건강도
        private Long healthyCount;
        private Long cautionCount;
        private Long warningCount;

        // 카드 3 (OrderStats 점프): 발주 주기 + TOP 창고
        private String shortestOrderItem;
        private Integer shortestOrderCycle;
        private String longestOrderItem;
        private Integer longestOrderCycle;
        private String topWarehouseName;
        private Long topWarehouseOrderCount;
        private Long topWarehouseItemCount;
        private BigDecimal topWarehouseAmount;

        // 카드 4 (Vendor 점프): TOP 거래처
        private String topVendorName;
        private BigDecimal topVendorAmount;

        // 카드 5 (Vendor 점프): TOP 소재 — 매출(원) 기준 → 판매량(kg) 기준 전환으로
        //                                topMaterialAmount(BigDecimal) 폐기, topMaterialWeight 만 유지.
        private String topMaterialName;
        private Long topMaterialWeight;
        private String topMaterialType;
    }
}
