package org.example.stockitbe.hq.esg.circularrevenue.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class CircularRevenueDto {

    /** GET 응답 — 순환재고 월별 판매 수익 (12개월 시계열 + 통계) */
    @Getter
    @Builder
    public static class Response {
        private final int year;
        private final List<MonthlyPoint> monthly;  // 길이 12 (1월~12월)
        private final long totalRevenue;           // 12개월 수익 합계
        private final long totalCount;             // 12개월 거래 건수 합계
        private final int monthsWithData;          // 거래 있는 월 수 (avgMonthly 분모)
        private final long avgMonthly;             // 월 평균 (거래 있는 월 기준)
    }

    /** 월별 단일 포인트 */
    @Getter
    @Builder
    public static class MonthlyPoint {
        private final int month;       // 1 ~ 12
        private final long revenue;    // 해당 월 총 수익 (원)
        private final long count;      // 해당 월 거래 건수
    }
}
