package org.example.stockitbe.hq.esg.materialfactor.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 소재 환산 계수 마스터 응답 DTO.
 *  - FE utils/esgScore.js 의 MATERIAL_FACTORS 와 1:1 매칭
 *  - active=true 인 material row 만 노출
 *  - carbon_factor 단위: kgCO₂e / kg fiber (Higg MSI v3 / EU PEF 표준, 동물성 섬유는 cap 적용)
 */
public class MaterialFactorDto {

    @Schema(description = "소재 환산 계수 응답 — FE esgScore.js MATERIAL_FACTORS 와 1:1 매칭")
    @Getter
    @Builder
    public static class Response {
        @Schema(description = "활성 소재 마스터 전체 목록")
        private final List<Item> factors;
    }

    @Schema(description = "소재 환산 계수 1건")
    @Getter
    @Builder
    public static class Item {
        @Schema(description = "소재 코드", example = "POLYESTER")
        private final String code;
        @Schema(description = "소재 한글명", example = "폴리에스터")
        private final String label;
        @Schema(description = "소재 그룹 (NATURAL_SINGLE/SYNTHETIC/BLEND)", example = "SYNTHETIC")
        private final String group;
        @Schema(description = "탄소배출계수 (kgCO₂e/kg)", example = "6.500")
        private final BigDecimal factor;
    }
}
