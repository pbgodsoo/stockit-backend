package org.example.stockitbe.hq.esg.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class CarbonPriceDto {

    // ─────────── 외부 공공데이터 API 응답 매핑 ───────────

    @Getter @NoArgsConstructor
    public static class ApiResponse {
        private ResponseBody response;
    }

    @Getter @NoArgsConstructor
    public static class ResponseBody {
        private Header header;
        private Body body;
    }

    @Getter @NoArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter @NoArgsConstructor
    public static class Body {
        private Integer numOfRows;
        private Integer pageNo;
        private Integer totalCount;
        private Items items;
    }

    @Getter @NoArgsConstructor
    public static class Items {
        @JsonProperty("item")
        private List<Item> item;
    }

    @Getter @NoArgsConstructor
    public static class Item {
        private String basDt;     // 기준일자 YYYYMMDD
        private String srtnCd;    // 단축코드
        private String isinCd;    // ISIN 코드
        private String itmsNm;    // 종목명 (KAU25 등)
        private String clpr;      // 종가 (원/톤)
        private String vs;        // 전일대비
        private String fltRt;     // 등락률
        private String mkp;       // 시가
        private String hipr;      // 고가
        private String lopr;      // 저가
        private String trqu;      // 거래량
        private String trPrc;     // 거래대금
    }

    // ─────────── BE → FE 응답 ───────────

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class Snapshot {
        private int pricePerTon;    // 원/톤
        private String symbol;      // KAU25
        private String basDt;       // YYYYMMDD
        private String fltRt;       // 등락률
        private boolean fallback;   // 폴백값 여부
    }
}
