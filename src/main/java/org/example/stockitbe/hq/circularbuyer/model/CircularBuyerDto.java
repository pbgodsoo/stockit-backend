package org.example.stockitbe.hq.circularbuyer.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

public class CircularBuyerDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @NotBlank
        private String code;
        @NotBlank
        private String companyName;
        @NotBlank
        private String industryGroup;
        private List<String> productTypes;
        private String productNote;
        private String description;
        @NotBlank
        private String primaryMaterialFit;
        @NotBlank
        private String managerName;
        @NotBlank
        private String phone;

        public CircularBuyer toEntity() {
            return CircularBuyer.builder()
                    .code(this.code)
                    .companyName(this.companyName)
                    .industryGroup(this.industryGroup)
                    .productTypes(this.productTypes)
                    .productNote(this.productNote)
                    .description(this.description)
                    .primaryMaterialFit(this.primaryMaterialFit)
                    .managerName(this.managerName)
                    .phone(this.phone)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private String companyName;
        private String industryGroup;
        private List<String> productTypes;
        private String productNote;
        private String description;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String code;
        private String companyName;
        private String industryGroup;
        private List<String> productTypes;
        // 거래처 카드/우측 디테일에서 표시 — ListRes 단일 호출로 description/productNote 노출
        // (목록은 30건 수준이라 TEXT 컬럼 포함해도 페이로드 영향 미미).
        private String productNote;
        private String description;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;

        public static ListRes from(CircularBuyer v) {
            return ListRes.builder()
                    .code(v.getCode())
                    .companyName(v.getCompanyName())
                    .industryGroup(v.getIndustryGroup())
                    .productTypes(v.getProductTypes())
                    .productNote(v.getProductNote())
                    .description(v.getDescription())
                    .primaryMaterialFit(v.getPrimaryMaterialFit())
                    .managerName(v.getManagerName())
                    .phone(v.getPhone())
                    .build();
        }
    }

    /**
     * ADR-021 추천 입력 — 옵션 B (사용자 결정 2026-04-30):
     *   materialFit (필수, 1층 SQL 룰 키) + productName + description + quantityHint.
     * 추천 호출 시점 = 판매 등록 페이지 Step 1 → Step 2 [다음] 클릭 1회.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendReq {
        @NotBlank
        private String materialFit;
        private String productName;
        private String description;
        private String quantityHint;
    }

    /**
     * 추천 1건 — 화면 카드용 핵심 필드만.
     * 점수(코사인 유사도)는 시스템 내부 정보이므로 응답에 노출하지 않는다 (사용자 결정 2026-04-30).
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class RecommendItem {
        private String code;
        private String companyName;
        private String primaryMaterialFit;
        private String industryGroup;
        private String rationale;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class RecommendRes {
        private List<RecommendItem> recommendations;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        private String code;
        private String companyName;
        private String industryGroup;
        private List<String> productTypes;
        private String productNote;
        private String description;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;
        private Date createdAt;
        private Date updatedAt;

        public static DetailRes from(CircularBuyer v) {
            return DetailRes.builder()
                    .code(v.getCode())
                    .companyName(v.getCompanyName())
                    .industryGroup(v.getIndustryGroup())
                    .productTypes(v.getProductTypes())
                    .productNote(v.getProductNote())
                    .description(v.getDescription())
                    .primaryMaterialFit(v.getPrimaryMaterialFit())
                    .managerName(v.getManagerName())
                    .phone(v.getPhone())
                    .createdAt(v.getCreatedAt())
                    .updatedAt(v.getUpdatedAt())
                    .build();
        }
    }
}
