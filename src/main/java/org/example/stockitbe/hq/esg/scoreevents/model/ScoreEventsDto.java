package org.example.stockitbe.hq.esg.scoreevents.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

public class ScoreEventsDto {

    /**
     * GET 응답 — 친환경 나무 키우기 점수 페이지 풀세트.
     *  - events: 페이지 슬라이스 (필터 적용 후 페이지에 해당하는 이벤트만)
     *  - summary / monthlyBreakdown / categoryBreakdown: 페이지와 무관하게 "필터 적용 후 전체" 기준 통계
     *  - page/size/totalElements/totalPages: 서버 페이징 메타
     */
    @Getter
    @Builder
    public static class Response {
        private final int year;
        private final List<EventDto> events;

        // ── Phase 3 (A''-1) — BE 가 직접 계산한 통계 묶음 ──
        private final Summary summary;
        private final List<MonthlyBucket> monthlyBreakdown;     // 1~12월 12개 (필터 후 기준)
        private final CategoryBreakdown categoryBreakdown;      // 도넛 차트 4종 합계

        // ── Phase 3 (B) — ESG 대시보드 카본 절감량 분포 묶음 ──
        //   필터/페이지와 무관하게 "연도 전체" 기준. 단위: kg CO₂. 점수 carbon 과 동일 산식.
        private final CarbonReduction carbonReduction;

        // ── 서버 페이징 메타 ──
        private final int page;            // 0-based
        private final int size;
        private final long totalElements;  // 필터 적용 후 총 건수
        private final int totalPages;
    }

    /**
     * 카본 절감량 분포 (kg CO₂) — ESG 대시보드 KPI/차트 SSOT.
     *  - 산식: 거래별 weight × effectiveFactor (BLEND 포함 자기 factor 사용)
     *  - 점수 carbon 과 동일 값 — 실제 감축 환산 단위
     *  - 카테고리 필터/페이지와 무관하게 연도 전체 이벤트 합산
     *  - byGroup 키 어휘: FE 와 통일 ("NATURAL_SINGLE" / "SYNTHETIC" / "BLEND")
     *    BE 어휘 'NATURAL' 은 응답 직전 'NATURAL_SINGLE' 로 정규화
     */
    @Getter
    @Builder
    public static class CarbonReduction {
        private final Map<String, Long> byMaterial;   // { COTTON: kg, WOOL: kg, ... }
        private final Map<String, Long> byGroup;      // { NATURAL_SINGLE: kg, SYNTHETIC: kg, BLEND: kg }
        private final List<Long> monthly;             // 12개 (1~12월). kg CO₂.
        private final long total;                     // byMaterial 합계와 동일
    }

    /**
     * 점수 요약 KPI (필터 적용 후 전체 기준).
     *  - 화면 상단 카드 + 평균 점수 표시에 사용
     */
    @Getter
    @Builder
    public static class Summary {
        private final long totalScore;          // 4종 점수의 합 (모든 이벤트)
        private final long saleExecutionSum;
        private final long carbonSum;
        private final long newBuyerSum;
        private final long localPartnerSum;
        private final long donationExecutionSum;

        private final long totalEventCount;     // 필터 적용 후 이벤트 수
        private final long validEventCount;     // scoreValid=true 인 이벤트 수
        private final long totalKg;             // 합산 weight (어뷰징 방지 무게 미달 포함)
        private final long avgScore;            // totalScore / totalEventCount (없으면 0)
    }

    /** 12개월 막대그래프용 — month=1~12, score=해당 월 합계 (필터 적용 후) */
    @Getter
    @Builder
    public static class MonthlyBucket {
        private final int month;
        private final long score;
    }

    /** 도넛 차트용 — 4종 점수의 (필터 적용 후) 누적 합계 */
    @Getter
    @Builder
    public static class CategoryBreakdown {
        private final long saleExecution;
        private final long carbon;
        private final long newBuyer;
        private final long localPartner;
        private final long donationExecution;
    }

    /**
     * 거래 이벤트 1건 (FE EsgTreeScoreView 의 event 객체 형태와 1:1 매칭).
     *  - Phase 2: isLocalPartner 가 circular_buyer.partner_type 기반으로 매핑 (이전: 항상 false)
     *  - Phase 2: 거래별 점수 4종을 BE 가 직접 계산해서 응답 (FE 의 esgScore.js 산식 함수 폐기 준비)
     */
    @Getter
    @Builder
    public static class EventDto {
        private final Long id;
        private final String date;          // "yyyy-MM-dd"
        private final String type;          // "sale" 고정 (donation 미지원)
        private final String buyer;         // circular_buyer.company_name
        private final String material;      // material_code (e.g. "POLYESTER", "BLEND")
        private final Integer weightKg;
        private final boolean isNewBuyer;   // 본 거래가 해당 buyer 의 최초 거래인지
        private final boolean isLocalPartner;  // Phase 2: partner_type=local_small/social_enterprise → true
        private final String saleType;         // "SALE" | "DONATION"

        // Phase 2 — BE 가 계산한 거래별 점수 5종
        private final int saleExecution;       // 100 (scoreValid 시), 0 (미달 시)
        private final int carbon;              // weight × effectiveFactor
        private final int newBuyer;            // 150 (신규 거래처 & scoreValid)
        private final int localPartner;        // 150 (지역 파트너 & scoreValid)
        private final int donationExecution;   // 기부 실행 점수 (판매면 0)
        private final int total;               // 위 점수 합계
        private final boolean scoreValid;      // weight >= MIN_WEIGHT_KG(10kg)
    }
}
