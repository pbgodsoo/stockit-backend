package org.example.stockitbe.hq.esg.carbonprice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class CarbonPriceDto {

    // ─────────── 외부 공공데이터 API 응답 매핑 ───────────

    @Schema(hidden = true)
    @Getter @NoArgsConstructor
    public static class ApiResponse {
        private ResponseBody response;
    }

    @Schema(hidden = true)
    @Getter @NoArgsConstructor
    public static class ResponseBody {
        private Header header;
        private Body body;
    }

    @Schema(hidden = true)
    @Getter @NoArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Schema(hidden = true)
    @Getter @NoArgsConstructor
    public static class Body {
        private Integer numOfRows;
        private Integer pageNo;
        private Integer totalCount;
        private Items items;
    }

    @Schema(hidden = true)
    @Getter @NoArgsConstructor
    public static class Items {
        @JsonProperty("item")
        private List<Item> item;
    }

    @Schema(hidden = true)
    @Getter @NoArgsConstructor
    public static class Item {
        private String basDt;
        private String srtnCd;
        private String isinCd;
        private String itmsNm;
        private String clpr;
        private String vs;
        private String fltRt;
        private String mkp;
        private String hipr;
        private String lopr;
        private String trqu;
        private String trPrc;
    }

    // ─────────── BE → FE 응답 ───────────

    @Schema(description = "탄소배출권 가격 스냅샷 — 외부 공공데이터 KAU25 종가 캐시")
    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class Snapshot {
        @Schema(description = "톤당 가격 (KRW)", example = "13420")
        private int pricePerTon;
        @Schema(description = "심볼 (KAU25)", example = "KAU25")
        private String symbol;
        @Schema(description = "기준일자 (YYYYMMDD)", example = "20260527")
        private String basDt;
        @Schema(description = "전일 대비 등락률 (%)", example = "1.2")
        private String fltRt;
        @Schema(description = "외부 API 실패로 폴백값을 반환했는지 여부", example = "false")
        private boolean fallback;
    }
}
