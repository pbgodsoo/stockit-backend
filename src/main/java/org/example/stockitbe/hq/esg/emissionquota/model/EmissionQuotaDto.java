package org.example.stockitbe.hq.esg.emissionquota.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

public class EmissionQuotaDto {

    @Schema(description = "탄소 배출권 할당량/월별 배출량 수정 요청 (PUT body)")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        @Schema(description = "연간 정부 할당량 (tCO₂e)", example = "10000")
        private BigDecimal yearlyAllocation;
        @Schema(description = "월별 배출량 배열 (길이 12, 1월~12월 순서). null 원소 = 미입력",
                example = "[820,850,860,830,840,null,null,null,null,null,null,null]")
        private BigDecimal[] monthlyEmissions;
        @Schema(description = "경고 임계 % — utilizationPct 가 이 값 이상이면 warning=true", example = "80")
        private Integer    warnThresholdPct;
    }

    @Schema(description = "탄소 배출권 응답 — 할당량 + 월별/분기별/누적/잔여 + 경고 플래그")
    @Getter
    @Builder
    public static class Response {
        @Schema(description = "회계연도", example = "2026")
        private final Integer      fiscalYear;
        @Schema(description = "연간 정부 할당량 (tCO₂e)", example = "10000")
        private final BigDecimal   yearlyAllocation;
        @Schema(description = "월별 입력값 (길이 12, 미입력은 null)",
                example = "[820,850,860,830,840,null,null,null,null,null,null,null]")
        private final BigDecimal[] monthlyEmissions;
        @Schema(description = "분기별 합계 (길이 4) — Q1=m1+m2+m3 ...", example = "[2530,1670,0,0]")
        private final BigDecimal[] quarterlyEmissions;
        @Schema(description = "YTD 누적 배출량 (12개월 합계, 자동)", example = "4200")
        private final BigDecimal   ytdEmissions;
        @Schema(description = "남은 할당량 = allocation - ytdEmissions", example = "5800")
        private final BigDecimal   remaining;
        @Schema(description = "사용률 (%) = ytd / allocation × 100", example = "42.0")
        private final BigDecimal   utilizationPct;
        @Schema(description = "경고 여부 (utilizationPct ≥ warnThresholdPct)", example = "false")
        private final boolean      warning;
        @Schema(description = "경고 임계 (%)", example = "80")
        private final Integer      warnThresholdPct;
        @Schema(description = "마지막 수정자", example = "이선엽")
        private final String       updatedBy;
        @Schema(description = "마지막 수정 시각", example = "2026-05-27T09:15:00.000+09:00")
        private final Date         updatedAt;
    }
}
