package org.example.stockitbe.hq.analytics.turnoveranalytics.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public class TurnoverAnalyticsDto {

    @Schema(description = "재고 회전율 분석 응답")
    @Getter
    @Builder
    public static class Res {
        @Schema(description = "조회 시작일", example = "2026-04-01")
        private String fromDate;
        @Schema(description = "조회 종료일", example = "2026-05-27")
        private String toDate;
        @Schema(description = "집계 주기", example = "MONTHLY")
        private TurnoverPeriod period;
        @Schema(description = "조회 범위 (전체/매장/창고)", example = "ALL")
        private TurnoverScope scope;
        @Schema(description = "특정 위치 코드 (전체 조회 시 null)", example = "WH-GW-0001", nullable = true)
        private String locationCode;
        @Schema(description = "위치별 회전율 통계 (매장·창고별)")
        private List<LocationStats> locationStats;
        @Schema(description = "재고 건강도 (신호등 카운트 + Top SKU)")
        private InventoryHealth inventoryHealth;
    }

    @Schema(description = "위치별 회전율 통계")
    @Getter
    @Builder
    public static class LocationStats {
        @Schema(description = "위치 코드", example = "WH-GW-0001")
        private String code;
        @Schema(description = "위치 이름", example = "강원 강릉 동해안 물류허브")
        private String name;
        @Schema(description = "위치 유형 한글", example = "창고", allowableValues = {"매장","창고"})
        private String type;
        @Schema(description = "위치 평균 재고 (수량 합)", example = "12500")
        private long avgInventory;
        @Schema(description = "기간 내 판매 수량 합", example = "4200")
        private long sales;
        @Schema(description = "회전율 (sales / avgInventory)", example = "4.2")
        private BigDecimal turnover;
        @Schema(description = "재고 잔여일 (365/turnover, cap=999)", example = "86.9")
        private BigDecimal daysOnHand;
        @Schema(description = "신호등 상태 라벨", example = "정상", allowableValues = {"정상","주의","경고","위험"})
        private String status;
    }

    @Schema(description = "재고 건강도 종합")
    @Getter
    @Builder
    public static class InventoryHealth {
        @Schema(description = "전체 inventory row 수", example = "236280")
        private long totalSku;
        @Schema(description = "정상 (turnover ≥ 4)", example = "180500")
        private long healthy;
        @Schema(description = "주의 (2 ≤ turnover < 4)", example = "42000")
        private long caution;
        @Schema(description = "경고 (1 ≤ turnover < 2)", example = "11200")
        private long warning;
        @Schema(description = "위험 (turnover < 1)", example = "2580")
        private long danger;
        @Schema(description = "전체 재고 가치 (백만원 단위)", example = "1820.5")
        private BigDecimal totalValue;
        @Schema(description = "위험 단계 묶인 금액 (백만원 단위)", example = "62.3")
        private BigDecimal lockedValue;
        @Schema(description = "정상 단계 Top 50 SKU (회전율 내림차순)")
        private List<SkuItem> healthySkus;
        @Schema(description = "주의 단계 Top 50 SKU")
        private List<SkuItem> cautionSkus;
        @Schema(description = "경고 단계 Top 50 SKU (회전율 오름차순)")
        private List<SkuItem> warningSkus;
        @Schema(description = "위험 단계 Top 50 SKU")
        private List<SkuItem> dangerSkus;
    }

    @Schema(description = "SKU 회전율 행 (재고 건강도 모달용)")
    @Getter
    @Builder
    public static class SkuItem {
        @Schema(description = "SKU 코드", example = "PRD-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "상품명", example = "코튼 에센셜 크루 반팔")
        private String productName;
        @Schema(description = "카테고리 한글", example = "상의 > 반팔")
        private String category;
        @Schema(description = "재고 위치 이름", example = "강원 강릉 동해안 물류허브")
        private String location;
        @Schema(description = "회전율", example = "5.2")
        private BigDecimal turnover;
        @Schema(description = "재고 잔여일", example = "70")
        private long daysOnHand;
        @Schema(description = "재고 수량", example = "125")
        private long units;
        @Schema(description = "재고 가치 (KRW)", example = "2637500")
        private BigDecimal value;
    }
}
