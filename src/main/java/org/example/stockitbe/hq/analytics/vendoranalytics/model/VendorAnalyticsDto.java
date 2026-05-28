package org.example.stockitbe.hq.analytics.vendoranalytics.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class VendorAnalyticsDto {

    @Schema(description = "거래처 분석 응답 — KPI + 거래처 상세 + 순환재고 소재 상세")
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Res {
        @Schema(description = "조회 시작일", example = "2026-04-01")
        private String fromDate;
        @Schema(description = "조회 종료일", example = "2026-05-27")
        private String toDate;
        @Schema(description = "집계 주기", example = "MONTHLY")
        private VendorPeriod period;
        @Schema(description = "KPI 4카드 요약")
        private KpiSummary kpi;
        @Schema(description = "거래처 상세 행 목록")
        private List<VendorStats> vendors;
        @Schema(description = "순환재고 소재 상세 행 목록")
        private List<MaterialStats> circularMaterials;
    }

    @Schema(description = "거래처 분석 KPI 4카드 요약")
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class KpiSummary {
        @Schema(description = "활성 거래처 수", example = "8")
        private Integer activeVendorCount;
        @Schema(description = "거래된 소재 종류 수", example = "10")
        private Integer activeMaterialCount;
        @Schema(description = "TOP 거래처 이름", example = "(주)테크서플라이")
        private String  topVendorName;
        @Schema(description = "TOP 거래처 매출 (KRW)", example = "92000000")
        private BigDecimal topVendorAmount;
        @Schema(description = "TOP 소재 한글명 (판매량 1위)", example = "폴리에스터")
        private String  topMaterialName;
        @Schema(description = "TOP 소재 판매량 (kg)", example = "1850")
        private Long    topMaterialWeight;
        @Schema(description = "총 판매 금액 (KRW)", example = "350000000")
        private BigDecimal totalSalesAmount;
    }

    @Schema(description = "거래처 한 행")
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class VendorStats {
        @Schema(description = "거래처 이름", example = "(주)테크서플라이")
        private String name;
        @Schema(description = "가장 많이 거래한 소재 한글명", example = "폴리에스터")
        private String material;
        @Schema(description = "소재 단가 (원/kg)", example = "1200")
        private Integer unitPrice;
        @Schema(description = "총 거래 중량 (kg)", example = "320")
        private Long    orderWeight;
        @Schema(description = "총 거래 금액 (KRW)", example = "62000000")
        private BigDecimal orderValue;
    }

    @Schema(description = "순환재고 소재 한 행")
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class MaterialStats {
        @Schema(description = "소재 코드", example = "POLYESTER")
        private String materialCode;
        @Schema(description = "소재 한글명", example = "폴리에스터")
        private String name;
        @Schema(description = "소재 유형 한글", example = "합성", allowableValues = {"천연 단일","합성","혼방"})
        private String materialType;
        @Schema(description = "총 판매 중량 (kg)", example = "1850")
        private Long   units;
        @Schema(description = "총 판매 매출 (KRW)", example = "2220000")
        private BigDecimal sales;
        @Schema(description = "에코 소재 여부 (천연 단일이면 true)", example = "false")
        private Boolean eco;
    }
}
