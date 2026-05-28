package org.example.stockitbe.hq.esg.scoreevents.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ScoreEventsDto {

    @Schema(description = "ESG 점수 이벤트 응답 — 페이지 슬라이스 + 통계 묶음 + 카본 절감량")
    @Getter
    @Builder
    public static class Response {
        @Schema(description = "조회 연도", example = "2026")
        private final int year;
        @Schema(description = "필터 적용 후 현재 페이지의 이벤트 목록")
        private final List<EventDto> events;

        @Schema(description = "점수 요약 (필터 적용 후 전체 기준)")
        private final Summary summary;
        @Schema(description = "월별 점수 (1~12월 12개, 필터 적용 후)")
        private final List<MonthlyBucket> monthlyBreakdown;
        @Schema(description = "도넛 4종 카테고리 점수 분포")
        private final CategoryBreakdown categoryBreakdown;

        @Schema(description = "카본 절감량 분포 (연도 전체 — 필터/페이지 무관, kg CO₂)")
        private final CarbonReduction carbonReduction;

        @Schema(description = "현재 페이지 번호 (0부터)", example = "0")
        private final int page;
        @Schema(description = "페이지 크기", example = "20")
        private final int size;
        @Schema(description = "필터 적용 후 총 이벤트 건수", example = "47")
        private final long totalElements;
        @Schema(description = "전체 페이지 수", example = "3")
        private final int totalPages;
    }

    @Schema(description = "카본 절감량 분포 (kg CO₂) — ESG 대시보드 KPI/차트 SSOT")
    @Getter
    @Builder
    public static class CarbonReduction {
        @Schema(description = "소재별 절감량 (kg)", example = "{\"POLYESTER\":1820,\"COTTON\":640}")
        private final Map<String, Long> byMaterial;
        @Schema(description = "그룹별 절감량 (kg) — 키: NATURAL_SINGLE/SYNTHETIC/BLEND",
                example = "{\"NATURAL_SINGLE\":640,\"SYNTHETIC\":1820,\"BLEND\":210}")
        private final Map<String, Long> byGroup;
        @Schema(description = "월별 절감량 12개 (1~12월, kg CO₂)",
                example = "[120,210,180,250,300,280,0,0,0,0,0,0]")
        private final List<Long> monthly;
        @Schema(description = "전체 절감량 합계 (kg, byMaterial 합과 동일)", example = "2670")
        private final long total;
    }

    @Schema(description = "점수 요약 KPI (필터 적용 후 전체 기준)")
    @Getter
    @Builder
    public static class Summary {
        @Schema(description = "총 점수 (4종 점수 합)", example = "5400")
        private final long totalScore;
        @Schema(description = "판매 실행 점수 합", example = "2400")
        private final long saleExecutionSum;
        @Schema(description = "탄소 절감 점수 합", example = "1820")
        private final long carbonSum;
        @Schema(description = "신규 거래처 점수 합", example = "600")
        private final long newBuyerSum;
        @Schema(description = "지역 파트너 점수 합", example = "580")
        private final long localPartnerSum;

        @Schema(description = "필터 적용 후 이벤트 수", example = "47")
        private final long totalEventCount;
        @Schema(description = "scoreValid=true 인 이벤트 수 (10kg 이상)", example = "42")
        private final long validEventCount;
        @Schema(description = "이벤트 총 weight 합 (kg)", example = "2670")
        private final long totalKg;
        @Schema(description = "평균 점수 (totalScore / totalEventCount, 없으면 0)", example = "114")
        private final long avgScore;
    }

    @Schema(description = "월별 점수 바 — 월 + 합계")
    @Getter
    @Builder
    public static class MonthlyBucket {
        @Schema(description = "월 (1~12)", example = "5")
        private final int month;
        @Schema(description = "해당 월 점수 합", example = "850")
        private final long score;
    }

    @Schema(description = "도넛 차트용 4종 점수 누적 합계")
    @Getter
    @Builder
    public static class CategoryBreakdown {
        @Schema(description = "판매 실행 점수", example = "2400")
        private final long saleExecution;
        @Schema(description = "탄소 절감 점수", example = "1820")
        private final long carbon;
        @Schema(description = "신규 거래처 점수", example = "600")
        private final long newBuyer;
        @Schema(description = "지역 파트너 점수", example = "580")
        private final long localPartner;
    }

    @Schema(description = "ESG 점수 이벤트 1건 (FE EsgTreeScoreView event 객체와 1:1 매칭)")
    @Getter
    @Builder
    public static class EventDto {
        @Schema(description = "이벤트 PK (sale id)", example = "12")
        private final Long id;
        @Schema(description = "이벤트 일자", example = "2026-05-15")
        private final String date;
        @Schema(description = "이벤트 유형 (현재 sale 고정)", example = "sale")
        private final String type;
        @Schema(description = "거래처(circular_buyer) 이름", example = "그린리사이클")
        private final String buyer;
        @Schema(description = "소재 코드", example = "POLYESTER")
        private final String material;
        @Schema(description = "거래 weight (kg)", example = "120")
        private final Integer weightKg;
        @Schema(description = "신규 거래처 거래 여부", example = "true")
        private final boolean isNewBuyer;
        @Schema(description = "지역 파트너 여부 (partner_type=local_small/social_enterprise)", example = "false")
        private final boolean isLocalPartner;

        @Schema(description = "혼방 거래 주 소재 코드 (단일 소재 거래는 null)", example = "COTTON", nullable = true)
        private final String mainMaterialCode;
        @Schema(description = "주 소재 비율 (예: 0.70)", example = "0.70", nullable = true)
        private final BigDecimal mainMaterialRatio;

        @Schema(description = "판매 실행 점수 (scoreValid=true 면 100, 아니면 0)", example = "100")
        private final int saleExecution;
        @Schema(description = "탄소 점수 (weight × effectiveFactor × 0.1)", example = "78")
        private final int carbon;
        @Schema(description = "신규 거래처 점수 (신규 + scoreValid 면 150)", example = "150")
        private final int newBuyer;
        @Schema(description = "지역 파트너 점수 (지역 + scoreValid 면 150)", example = "0")
        private final int localPartner;
        @Schema(description = "이벤트 총 점수 = 위 4종 합계", example = "328")
        private final int total;
        @Schema(description = "점수 유효 여부 (weight >= 10kg)", example = "true")
        private final boolean scoreValid;
    }
}
