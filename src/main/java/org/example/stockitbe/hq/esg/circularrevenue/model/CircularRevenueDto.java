package org.example.stockitbe.hq.esg.circularrevenue.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class CircularRevenueDto {

    @Schema(description = "순환재고 월별 판매 수익 응답 (12개월 시계열 + 통계)")
    @Getter
    @Builder
    public static class Response {
        @Schema(description = "조회 연도", example = "2026")
        private final int year;
        @Schema(description = "월별 포인트 (길이 12, 1월~12월 순)")
        private final List<MonthlyPoint> monthly;
        @Schema(description = "12개월 수익 합계 (KRW)", example = "5400000")
        private final long totalRevenue;
        @Schema(description = "12개월 거래 건수 합계", example = "23")
        private final long totalCount;
        @Schema(description = "거래 있는 월 수 (avgMonthly 분모)", example = "8")
        private final int monthsWithData;
        @Schema(description = "월 평균 수익 (거래 있는 월 기준)", example = "675000")
        private final long avgMonthly;
    }

    @Schema(description = "월별 단일 포인트")
    @Getter
    @Builder
    public static class MonthlyPoint {
        @Schema(description = "월 (1~12)", example = "5")
        private final int month;
        @Schema(description = "해당 월 총 수익 (KRW)", example = "1200000")
        private final long revenue;
        @Schema(description = "해당 월 거래 건수", example = "4")
        private final long count;
    }
}
