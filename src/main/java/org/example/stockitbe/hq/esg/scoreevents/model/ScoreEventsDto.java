package org.example.stockitbe.hq.esg.scoreevents.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class ScoreEventsDto {

    /** GET 응답 — 연간 sale 거래 이벤트 리스트 */
    @Getter
    @Builder
    public static class Response {
        private final int year;
        private final List<EventDto> events;
    }

    /**
     * 거래 이벤트 1건 (FE EsgTreeScoreView 의 event 객체 형태와 1:1 매칭).
     *  - donationType / method 는 sale 만 다루므로 미포함
     *  - isLocalPartner 는 현재 항상 false (partner_type 컬럼 없음)
     */
    @Getter
    @Builder
    public static class EventDto {
        private final Long id;
        private final String date;          // "yyyy-MM-dd"
        private final String type;          // "sale" 고정 (donation 미지원)
        private final String buyer;         // circular_buyer.company_name
        private final String material;      // material_code (e.g. "POLYESTER")
        private final Integer weightKg;
        private final boolean isNewBuyer;   // 본 거래가 해당 buyer 의 최초 거래인지
        private final boolean isLocalPartner;  // 현재 false 고정
    }
}
