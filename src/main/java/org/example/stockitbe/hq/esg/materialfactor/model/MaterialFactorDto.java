package org.example.stockitbe.hq.esg.materialfactor.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 소재 환산 계수 마스터 응답 DTO.
 *  - FE utils/esgScore.js 의 MATERIAL_FACTORS 와 1:1 매칭
 *  - active=true 인 material row 만 노출
 *  - carbon_factor 단위: kgCO₂e / kg fiber (Higg MSI v3 / EU PEF 표준, 동물성 섬유는 cap 적용)
 *
 * 응답 예시:
 *   { "factors": [
 *       { "code": "COTTON",   "label": "면",   "group": "NATURAL_SINGLE", "factor": 6.000 },
 *       { "code": "POLYESTER","label": "폴리에스터","group": "SYNTHETIC","factor": 6.500 },
 *       { "code": "BLEND",    "label": "혼방", "group": "BLEND",          "factor": 5.500 }
 *     ]
 *   }
 */
public class MaterialFactorDto {

    @Getter
    @Builder
    public static class Response {
        // active 소재 마스터의 전체 목록
        private final List<Item> factors;
    }

    @Getter
    @Builder
    public static class Item {
        private final String code;          // material.code (예: COTTON, POLYESTER, BLEND)
        private final String label;         // material.name_ko (한글명)
        private final String group;         // material_group (NATURAL_SINGLE / SYNTHETIC / BLEND)
        private final BigDecimal factor;    // material.carbon_factor (kgCO₂e/kg)
    }
}
