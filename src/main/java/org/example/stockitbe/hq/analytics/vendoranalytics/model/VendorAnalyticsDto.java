package org.example.stockitbe.hq.analytics.vendoranalytics.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class VendorAnalyticsDto {

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Res {
        private String fromDate;
        private String toDate;
        private VendorPeriod period;
        private KpiSummary kpi;
        private List<VendorStats> vendors;             // 거래처 상세
        private List<MaterialStats> circularMaterials; // 순환재고 소재 상세
    }

    /** KPI 4카드. */
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class KpiSummary {
        private Integer activeVendorCount;       // 활성 거래처 수
        private Integer activeMaterialCount;     // 거래된 소재 종류 수
        private String  topVendorName;           // 제일 많이 거래한 거래처
        private BigDecimal topVendorAmount;      // 그 거래처 매출 (원)
        private String  topMaterialName;         // 제일 많이 팔린 소재 (한글명)
        private BigDecimal topMaterialAmount;    // 그 소재 매출 (원)
        private BigDecimal totalSalesAmount;     // 총 판매 금액 (원)
    }

    /** 거래처 상세 행. */
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class VendorStats {
        private String name;                // company_name
        private String material;            // material_name_ko (그 거래처가 가장 많이 거래한 소재)
        private Integer unitPrice;          // 원/kg (소재 정책 단가 그대로)
        private Long    orderWeight;        // 총 weight_kg
        private BigDecimal orderValue;      // 총 total_amount (원)
    }

    /** 순환재고 소재 상세 행. */
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class MaterialStats {
        private String materialCode;        // POLYESTER, COTTON, ...
        private String name;                // material_name_ko
        private String materialType;        // '천연 단일' | '합성' | '혼방'  (material_group → 변환)
        private Long   units;               // 총 weight_kg
        private BigDecimal sales;           // 총 total_amount (원)
        private Boolean eco;                // materialType == '천연 단일' 이면 true (간단 규칙)
    }
}
