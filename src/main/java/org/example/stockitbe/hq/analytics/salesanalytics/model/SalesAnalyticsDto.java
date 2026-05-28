package org.example.stockitbe.hq.analytics.salesanalytics.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SalesAnalyticsDto {

    @Schema(description = "매출 분석 응답 — KPI + 추이 + 카테고리 통계")
    @Getter
    @Builder
    public static class Res {
        @Schema(description = "조회 시작일", example = "2026-04-01")
        private String fromDate;
        @Schema(description = "조회 종료일", example = "2026-05-27")
        private String toDate;
        @Schema(description = "집계 주기", example = "MONTHLY")
        private SalesPeriod period;
        @Schema(description = "매출 KPI 요약")
        private KpiSummary kpi;
        @Schema(description = "기간 매출 추이 (현재 vs 전기간)")
        private TrendData trend;
        @Schema(description = "대분류 카테고리 매출 요약")
        private List<CategorySummary> categorySummary;
        @Schema(description = "소분류 카테고리 매출 통계")
        private List<SubCategoryStats> subCategoryStats;
        @Schema(description = "소분류별 상품 상세 매출 (key=subCategory)")
        private Map<String, List<ProductStats>> productDetailsBySubCategory;
    }

    @Schema(description = "매출 KPI 요약")
    @Getter @Builder
    public static class KpiSummary {
        @Schema(description = "총 매출 (KRW)", example = "350000000")
        private BigDecimal totalRevenue;
        @Schema(description = "총 매출 전기 대비 증감률 (%)", example = "12.5")
        private BigDecimal totalRevenueTrendPct;
        @Schema(description = "총 판매 수량 (개)", example = "12500")
        private long totalQuantity;
        @Schema(description = "총 판매 수량 전기 대비 증감률 (%)", example = "8.3")
        private BigDecimal totalQuantityTrendPct;
        @Schema(description = "활성 매장 수", example = "28")
        private int activeStoreCount;
        @Schema(description = "전체 매장 수", example = "132")
        private int totalStoreCount;
        @Schema(description = "활성 매장 수 변동 (vs 전기)", example = "3")
        private int activeStoreCountDelta;
        @Schema(description = "베스트 카테고리명", example = "아우터")
        private String bestCategoryName;
        @Schema(description = "베스트 카테고리 매출 점유율 (%)", example = "38.0")
        private BigDecimal bestCategorySharePct;
    }

    @Schema(description = "매출 추이 — 현재 vs 전기간 비교")
    @Getter @Builder
    public static class TrendData {
        @Schema(description = "현재 기간 추이 포인트")
        private List<TrendPoint> current;
        @Schema(description = "전기간 추이 포인트")
        private List<TrendPoint> previous;
    }

    @Schema(description = "매출 추이 1포인트")
    @Getter @Builder
    public static class TrendPoint {
        @Schema(description = "x축 라벨 (period 따라 \"04/14\" 또는 \"2026-04\")", example = "04/14")
        private String label;
        @Schema(description = "해당 시점 매출 (KRW)", example = "12500000")
        private BigDecimal revenue;
        @Schema(description = "해당 시점 판매 수량", example = "320")
        private long quantity;
    }

    @Schema(description = "대분류 카테고리 매출 요약")
    @Getter @Builder
    public static class CategorySummary {
        @Schema(description = "대분류 이름", example = "상의")
        private String mainCategory;
        @Schema(description = "상품 수", example = "120")
        private long productCount;
        @Schema(description = "매출액 (KRW)", example = "82000000")
        private BigDecimal salesAmount;
        @Schema(description = "판매 수량", example = "3200")
        private long quantity;
        @Schema(description = "전체 매출 대비 점유율 (%)", example = "23.4")
        private BigDecimal sharePct;
    }

    @Schema(description = "소분류 카테고리 매출 통계")
    @Getter @Builder
    public static class SubCategoryStats {
        @Schema(description = "대분류", example = "상의")
        private String mainCategory;
        @Schema(description = "소분류", example = "반팔")
        private String subCategory;
        @Schema(description = "판매 수량", example = "1850")
        private long quantity;
        @Schema(description = "매출 (KRW)", example = "42000000")
        private BigDecimal salesAmount;
        @Schema(description = "전체 매출 대비 점유율 (%)", example = "12.0")
        private BigDecimal sharePct;
    }

    @Schema(description = "상품별 매출 통계")
    @Getter @Builder
    public static class ProductStats {
        @Schema(description = "상품 코드", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "상품명", example = "코튼 에센셜 크루 반팔")
        private String productName;
        @Schema(description = "대분류", example = "상의")
        private String mainCategory;
        @Schema(description = "소분류", example = "반팔")
        private String subCategory;
        @Schema(description = "판매 수량", example = "725")
        private long quantity;
        @Schema(description = "매출 (KRW)", example = "15300000")
        private BigDecimal salesAmount;
    }
}
