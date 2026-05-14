package org.example.stockitbe.hq.analytics.salesanalytics.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SalesAnalyticsDto {

    @Getter
    @Builder
    public static class Res {
        private String fromDate;
        private String toDate;
        private SalesPeriod period;
        private KpiSummary kpi;
        private TrendData trend;
        private List<CategorySummary> categorySummary;
        private List<SubCategoryStats> subCategoryStats;
        private Map<String, List<ProductStats>> productDetailsBySubCategory;
    }

    @Getter @Builder
    public static class KpiSummary {
        private BigDecimal totalRevenue;          // 원
        private BigDecimal totalRevenueTrendPct;  // % vs previous (소수 2자리)
        private long totalQuantity;               // 개
        private BigDecimal totalQuantityTrendPct;
        private int activeStoreCount;             // 28
        private int totalStoreCount;              // 132
        private int activeStoreCountDelta;        // +3 (vs previous)
        private String bestCategoryName;          // "아우터"
        private BigDecimal bestCategorySharePct;  // 38.0
    }

    @Getter @Builder
    public static class TrendData {
        private List<TrendPoint> current;
        private List<TrendPoint> previous;
    }

    @Getter @Builder
    public static class TrendPoint {
        private String label;        // "04/14" or "2026-04" (period 따라)
        private BigDecimal revenue;  // 원 (FE 가 M 단위로 변환)
        private long quantity;       // 개 (월별 판매 수량 — 본사 대시보드 차트용)
    }

    @Getter @Builder
    public static class CategorySummary {
        private String mainCategory;
        private long productCount;
        private BigDecimal salesAmount;
        private long quantity;
        private BigDecimal sharePct;
    }

    @Getter @Builder
    public static class SubCategoryStats {
        private String mainCategory;
        private String subCategory;
        private long quantity;
        private BigDecimal salesAmount;
        private BigDecimal sharePct;
    }

    @Getter @Builder
    public static class ProductStats {
        private String productCode;
        private String productName;
        private String mainCategory;
        private String subCategory;
        private long quantity;
        private BigDecimal salesAmount;
    }
}
