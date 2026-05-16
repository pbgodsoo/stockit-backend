package org.example.stockitbe.hq.circularbuyer.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
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
        private String companyName;
        @NotBlank
        private String industryGroup;
        @JsonAlias("productTypes")
        private List<String> factoryProduct;
        private String description;
        @NotBlank
        private String primaryMaterialFit;
        @NotBlank
        private String managerName;
        @NotBlank
        private String phone;
        @NotBlank
        private String address;
        @NotBlank
        private String partnerType;

        public CircularBuyer toEntity(String code) {
            return CircularBuyer.builder()
                    .code(code)
                    .companyName(this.companyName)
                    .industryGroup(this.industryGroup)
                    .factoryProduct(this.factoryProduct)
                    .description(this.description)
                    .primaryMaterialFit(this.primaryMaterialFit)
                    .managerName(this.managerName)
                    .phone(this.phone)
                    .address(this.address)
                    .partnerType(this.partnerType)
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
        @JsonAlias("productTypes")
        private List<String> factoryProduct;
        private String description;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;
        private String address;
        private String partnerType;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String code;
        private String companyName;
        private String industryGroup;
        private List<String> factoryProduct;
        // 거래처 카드/우측 디테일에서 표시 — ListRes 단일 호출로 description/address 노출
        // (목록은 30건 수준이라 TEXT 컬럼 포함해도 페이로드 영향 미미).
        private String description;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;
        private String address;
        private String partnerType;

        // FE 구버전 호환 alias (2단계 전환 중 임시 유지).
        @JsonGetter("productTypes")
        public List<String> legacyProductTypes() {
            return factoryProduct;
        }

        public static ListRes from(CircularBuyer v) {
            return ListRes.builder()
                    .code(v.getCode())
                    .companyName(v.getCompanyName())
                    .industryGroup(v.getIndustryGroup())
                    .factoryProduct(v.getFactoryProduct())
                    .description(v.getDescription())
                    .primaryMaterialFit(v.getPrimaryMaterialFit())
                    .managerName(v.getManagerName())
                    .phone(v.getPhone())
                    .address(v.getAddress())
                    .partnerType(v.getPartnerType())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class PageRes {
        private List<ListRes> content;
        private int page;
        private int size;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * ADR-021 추천 입력 — 옵션 B (사용자 결정 2026-04-30):
     *   materialFit (필수, 1층 SQL 룰 키) + productName + description + quantityHint.
     * 추천 호출 시점 = 판매 등록 페이지 Step 1 → Step 2 [다음] 클릭 1회.
     *
     * productCode (이슈 #218, 임베딩 풍부화) — 선택 입력. 박히면 BE 가 ProductMaster → composition →
     * Material join 으로 소재 자연어를 자동 합쳐 임베딩 input 풍부화. 미박힘 시 자유 텍스트 fallback.
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
        private String productCode;
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
        private String partnerType;
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
        private List<String> factoryProduct;
        private String description;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;
        private String address;
        private String partnerType;
        private Date createdAt;
        private Date updatedAt;

        // FE 구버전 호환 alias (2단계 전환 중 임시 유지).
        @JsonGetter("productTypes")
        public List<String> legacyProductTypes() {
            return factoryProduct;
        }

        public static DetailRes from(CircularBuyer v) {
            return DetailRes.builder()
                    .code(v.getCode())
                    .companyName(v.getCompanyName())
                    .industryGroup(v.getIndustryGroup())
                    .factoryProduct(v.getFactoryProduct())
                    .description(v.getDescription())
                    .primaryMaterialFit(v.getPrimaryMaterialFit())
                    .managerName(v.getManagerName())
                    .phone(v.getPhone())
                    .address(v.getAddress())
                    .partnerType(v.getPartnerType())
                    .createdAt(v.getCreatedAt())
                    .updatedAt(v.getUpdatedAt())
                    .build();
        }
    }
}
