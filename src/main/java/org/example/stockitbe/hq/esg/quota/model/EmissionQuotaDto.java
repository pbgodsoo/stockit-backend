package org.example.stockitbe.hq.esg.quota.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

public class EmissionQuotaDto {

    /** PUT body */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private BigDecimal yearlyAllocation;            // 정부 할당량
        private BigDecimal[] monthlyEmissions;          // 길이 12 (1월~12월). null 원소 = 미입력
        private Integer    warnThresholdPct;            // 경고 임계 %
    }

    /** GET / PUT 응답 */
    @Getter
    @Builder
    public static class Response {
        private final Integer      fiscalYear;
        private final BigDecimal   yearlyAllocation;
        // ─── 입력값 (12개월) ───
        private final BigDecimal[] monthlyEmissions;    // 길이 12, 미입력은 null
        // ─── 자동 계산 필드 ───
        private final BigDecimal[] quarterlyEmissions;  // 길이 4 (Q1=m1+m2+m3, Q2=m4+m5+m6, ...)
        private final BigDecimal   ytdEmissions;        // 12개월 합계 (자동)
        private final BigDecimal   remaining;           // = allocation - ytdEmissions
        private final BigDecimal   utilizationPct;      // = ytd / allocation × 100
        private final boolean      warning;             // utilizationPct ≥ warnThresholdPct
        // ─── 메타 ───
        private final Integer      warnThresholdPct;
        private final String       updatedBy;
        private final Date         updatedAt;
    }
}
