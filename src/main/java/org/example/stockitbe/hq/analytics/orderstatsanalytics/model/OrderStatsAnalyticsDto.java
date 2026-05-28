package org.example.stockitbe.hq.analytics.orderstatsanalytics.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public class OrderStatsAnalyticsDto {

    @Schema(description = "발주 통계 분석 응답")
    @Getter
    @Builder
    public static class Res {
        @Schema(description = "조회 시작일", example = "2026-04-01")
        private String fromDate;
        @Schema(description = "조회 종료일", example = "2026-05-27")
        private String toDate;
        @Schema(description = "집계 주기", example = "MONTHLY")
        private OrderStatsPeriod period;
        @Schema(description = "발주 KPI 요약")
        private KpiSummary kpi;
        @Schema(description = "창고별 발주 통계")
        private List<WarehouseStats> warehouseOrders;
        @Schema(description = "품목 그룹(L2) 발주 주기")
        private List<ItemCycle> orderCycleData;
        @Schema(description = "상품별 발주 주기")
        private List<ProductCycle> productOrderData;
        @Schema(description = "월별 발주 추이")
        private List<MonthlyTrendPoint> monthlyTrend;
    }


    @Schema(description = "발주 KPI 요약")
    @Getter
    @Builder
    public static class KpiSummary {
        @Schema(description = "발주 이력 있는 품목 수 (L2 기준)", example = "32")
        private int managedItemCount;
        @Schema(description = "전체 품목 평균 발주 주기 (일)", example = "9")
        private int avgCycleDays;
        @Schema(description = "최단 발주 주기 (일)", example = "3")
        private int shortestCycleDays;
        @Schema(description = "최단 주기 품목명", example = "반팔")
        private String shortestCycleItem;
        @Schema(description = "최장 발주 주기 (일)", example = "21")
        private int longestCycleDays;
        @Schema(description = "최장 주기 품목명", example = "후디")
        private String longestCycleItem;
        @Schema(description = "누적 발주 건수 (라인 합)", example = "417")
        private long totalOrders;
    }

    @Schema(description = "창고별 발주 통계 한 행")
    @Getter
    @Builder
    public static class WarehouseStats {
        @Schema(description = "창고 코드", example = "WH-GW-0001")
        private String warehouseCode;
        @Schema(description = "창고 이름", example = "강원 강릉 동해안 물류허브")
        private String warehouseName;
        @Schema(description = "창고별 발주 건수", example = "47")
        private long orders;
        @Schema(description = "창고별 발주 품목 수", example = "320")
        private long items;
        @Schema(description = "창고별 발주 총액 (KRW)", example = "62000000")
        private BigDecimal totalValue;
        @Schema(description = "전체 발주액 대비 점유율 (%)", example = "17.7")
        private BigDecimal sharePct;
    }

    @Schema(description = "품목 그룹(L2) 발주 주기")
    @Getter
    @Builder
    public static class ItemCycle {
        @Schema(description = "품목 그룹명", example = "반팔")
        private String item;
        @Schema(description = "상위 카테고리", example = "상의")
        private String category;
        @Schema(description = "평균 발주 주기 (일)", example = "7")
        private int avgCycle;
        @Schema(description = "평균 발주 수량", example = "120")
        private long avgQty;
        @Schema(description = "누적 발주 건수", example = "32")
        private long totalOrders;
        @Schema(description = "마지막 발주일", example = "2026-05-20")
        private String lastOrderedAt;
    }

    @Schema(description = "상품별 발주 주기")
    @Getter
    @Builder
    public static class ProductCycle {
        @Schema(description = "상품명", example = "코튼 에센셜 크루 반팔")
        private String item;
        @Schema(description = "상품 코드", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "상품 유형 (L2)", example = "반팔")
        private String productType;
        @Schema(description = "상위 카테고리", example = "상의")
        private String category;
        @Schema(description = "평균 발주 주기 (일)", example = "5")
        private int avgCycle;
        @Schema(description = "평균 발주 수량", example = "200")
        private long avgQty;
        @Schema(description = "누적 발주 건수", example = "18")
        private long totalOrders;
        @Schema(description = "마지막 발주일", example = "2026-05-22")
        private String lastOrderedAt;
    }

    @Schema(description = "월별 발주 추이 1포인트")
    @Getter
    @Builder
    public static class MonthlyTrendPoint {
        @Schema(description = "연-월", example = "2026-04")
        private String month;
        @Schema(description = "해당 월 발주 건수", example = "120")
        private long orders;
        @Schema(description = "해당 월 발주 품목 수", example = "850")
        private long items;
    }
}
