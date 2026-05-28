package org.example.stockitbe.hq.analytics.dashboardanalytics.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class DashboardAnalyticsDto {

    @Schema(description = "본사 대시보드 분석 응답 — KPI 요약 + 매출 추이")
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Res {
        @Schema(description = "조회 시작일", example = "2026-04-01")
        private String fromDate;
        @Schema(description = "조회 종료일", example = "2026-05-27")
        private String toDate;
        @Schema(description = "집계 주기 (DAILY/WEEKLY/MONTHLY)", example = "DAILY")
        private DashboardPeriod period;
        @Schema(description = "5카드 KPI 요약")
        private KpiSummary kpi;
        @Schema(description = "기간 내 일자별/주별/월별 매출 추이")
        private List<TrendPoint> trendCurrent;
    }

    @Schema(description = "매출 추이 1포인트")
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class TrendPoint {
        @Schema(description = "x축 라벨 — 일별이면 \"04/14\", 월별이면 \"2026-04\"", example = "04/14")
        private String label;
        @Schema(description = "해당 시점 매출 (KRW)", example = "12500000")
        private BigDecimal revenue;
    }

    @Schema(description = "본사 대시보드 5카드 KPI 묶음")
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class KpiSummary {
        @Schema(description = "총 매출 (KRW)", example = "350000000")
        private BigDecimal totalRevenue;
        @Schema(description = "총 매출 전기 대비 증감률 (%)", example = "12.5")
        private BigDecimal totalRevenueTrendPct;
        @Schema(description = "재고에 잠긴 금액 (KRW)", example = "180000000")
        private BigDecimal lockedValue;
        @Schema(description = "위험 SKU 수 (안전재고 미달)", example = "42")
        private Long dangerSkuCount;
        @Schema(description = "전체 SKU 수", example = "1350")
        private Long totalSkuCount;
        @Schema(description = "활성 거래처 수", example = "8")
        private Integer activeVendorCount;
        @Schema(description = "활성 소재 수", example = "10")
        private Integer activeMaterialCount;
        @Schema(description = "순환재고 판매 누적 금액 (KRW)", example = "5400000")
        private BigDecimal circularSalesAmount;

        @Schema(description = "매출 1위 품목명", example = "코튼 에센셜 크루 반팔")
        private String topProductName;
        @Schema(description = "매출 1위 품목 매출 (KRW)", example = "15300000")
        private BigDecimal topProductSales;
        @Schema(description = "매출 1위 품목 판매 수량", example = "725")
        private Long topProductUnits;
        @Schema(description = "베스트 카테고리명", example = "상의 > 반팔")
        private String bestCategoryName;
        @Schema(description = "베스트 카테고리 매출 (KRW)", example = "82000000")
        private BigDecimal bestCategoryAmount;
        @Schema(description = "베스트 카테고리 매출 점유율 (%)", example = "23.4")
        private BigDecimal bestCategorySharePct;
        @Schema(description = "총 판매 수량", example = "12500")
        private Long totalSalesQty;

        @Schema(description = "정상 재고 SKU 수", example = "1200")
        private Long healthyCount;
        @Schema(description = "주의 재고 SKU 수", example = "108")
        private Long cautionCount;
        @Schema(description = "경고 재고 SKU 수", example = "42")
        private Long warningCount;

        @Schema(description = "발주 주기 가장 짧은 품목", example = "코튼 에센셜 크루 반팔")
        private String shortestOrderItem;
        @Schema(description = "최단 발주 주기 (일)", example = "3")
        private Integer shortestOrderCycle;
        @Schema(description = "발주 주기 가장 긴 품목", example = "오버핏 그래픽 후디")
        private String longestOrderItem;
        @Schema(description = "최장 발주 주기 (일)", example = "21")
        private Integer longestOrderCycle;
        @Schema(description = "TOP 창고 이름", example = "강원 강릉 동해안 물류허브")
        private String topWarehouseName;
        @Schema(description = "TOP 창고 발주 건수", example = "47")
        private Long topWarehouseOrderCount;
        @Schema(description = "TOP 창고 품목 수", example = "320")
        private Long topWarehouseItemCount;
        @Schema(description = "TOP 창고 발주 금액 (KRW)", example = "62000000")
        private BigDecimal topWarehouseAmount;

        @Schema(description = "TOP 거래처 이름", example = "(주)테크서플라이")
        private String topVendorName;
        @Schema(description = "TOP 거래처 발주 금액 (KRW)", example = "92000000")
        private BigDecimal topVendorAmount;

        @Schema(description = "TOP 소재 이름", example = "폴리에스터")
        private String topMaterialName;
        @Schema(description = "TOP 소재 판매 중량 (kg)", example = "1850")
        private Long topMaterialWeight;
        @Schema(description = "TOP 소재 유형 (synthetic/natural-single/blended)", example = "synthetic")
        private String topMaterialType;
    }
}
