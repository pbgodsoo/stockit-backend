package org.example.stockitbe.hq.circularbuyer.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

public class CircularBuyerDto {

    @Schema(description = "순환 거래처 등록 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @Schema(description = "거래처명", example = "그린리사이클", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String companyName;
        @Schema(description = "산업군", example = "재활용 섬유", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String industryGroup;
        @Schema(description = "취급 제품 유형 목록. productTypes alias로도 입력 가능", example = "[\"원사\", \"부직포\"]")
        @JsonAlias("productTypes")
        private List<String> factoryProduct;
        @Schema(description = "거래처 설명", example = "합성섬유 재활용 및 원료화 전문 업체")
        private String description;
        @Schema(description = "주요 적합 소재. 허용값: natural-single, synthetic, blended", example = "synthetic", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String primaryMaterialFit;
        @Schema(description = "담당자명", example = "김담당", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String managerName;
        @Schema(description = "담당자 연락처", example = "010-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String phone;
        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String address;
        @Schema(description = "거래처 유형. 허용값: local_small, social_enterprise, general", example = "general", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "순환 거래처 수정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        @Schema(description = "거래처명", example = "그린리사이클")
        private String companyName;
        @Schema(description = "산업군", example = "재활용 섬유")
        private String industryGroup;
        @Schema(description = "취급 제품 유형 목록. productTypes alias로도 입력 가능", example = "[\"원사\", \"부직포\"]")
        @JsonAlias("productTypes")
        private List<String> factoryProduct;
        @Schema(description = "거래처 설명", example = "합성섬유 재활용 및 원료화 전문 업체")
        private String description;
        @Schema(description = "주요 적합 소재. 허용값: natural-single, synthetic, blended", example = "synthetic")
        private String primaryMaterialFit;
        @Schema(description = "담당자명", example = "김담당")
        private String managerName;
        @Schema(description = "담당자 연락처", example = "010-1234-5678")
        private String phone;
        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
        private String address;
        @Schema(description = "거래처 유형. 허용값: local_small, social_enterprise, general", example = "general")
        private String partnerType;
    }

    @Schema(description = "순환 거래처 목록 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        @Schema(description = "순환 거래처 코드", example = "RCV-00001")
        private String code;
        @Schema(description = "거래처명", example = "그린리사이클")
        private String companyName;
        @Schema(description = "산업군", example = "재활용 섬유")
        private String industryGroup;
        @Schema(description = "취급 제품 유형 목록", example = "[\"원사\", \"부직포\"]")
        private List<String> factoryProduct;
        // 거래처 카드/우측 디테일에서 표시 — ListRes 단일 호출로 description/address 노출
        // (목록은 30건 수준이라 TEXT 컬럼 포함해도 페이로드 영향 미미).
        @Schema(description = "거래처 설명", example = "합성섬유 재활용 및 원료화 전문 업체")
        private String description;
        @Schema(description = "주요 적합 소재", example = "synthetic")
        private String primaryMaterialFit;
        @Schema(description = "담당자명", example = "김담당")
        private String managerName;
        @Schema(description = "담당자 연락처", example = "010-1234-5678")
        private String phone;
        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
        private String address;
        @Schema(description = "거래처 유형", example = "general")
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

    @Schema(description = "순환 거래처 페이지 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class PageRes {
        @Schema(description = "거래처 목록")
        private List<ListRes> content;
        @Schema(description = "현재 페이지 번호", example = "0")
        private int page;
        @Schema(description = "페이지 크기", example = "20")
        private int size;
        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPages;
        @Schema(description = "전체 거래처 수", example = "100")
        private long totalElements;
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부", example = "false")
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
    @Schema(description = "순환 거래처 추천 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendReq {
        @Schema(description = "추천 기준 소재 적합도. 허용값: natural-single, synthetic, blended", example = "synthetic", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String materialFit;
        @Schema(description = "상품명", example = "폴리에스터 자켓 외 3건")
        private String productName;
        @Schema(description = "순환재고 또는 상품 설명", example = "폴리에스터 100% 합성 섬유 잔재고")
        private String description;
        @Schema(description = "수량 힌트", example = "약 120.0kg / 300벌")
        private String quantityHint;
        @Schema(description = "상품 코드. 전달 시 상품 소재 구성을 추천 입력에 보강한다.", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "창고 코드. 전달 시 거래처와의 거리 산정에 사용한다.", example = "WH-001")
        private String warehouseCode;
    }

    /**
     * 추천 1건 — 화면 카드용 핵심 필드만.
     * 점수(코사인 유사도)는 시스템 내부 정보이므로 응답에 노출하지 않는다 (사용자 결정 2026-04-30).
     */
    @Schema(description = "순환 거래처 추천 항목")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class RecommendItem {
        @Schema(description = "순환 거래처 코드", example = "RCV-00001")
        private String code;
        @Schema(description = "거래처명", example = "그린리사이클")
        private String companyName;
        @Schema(description = "주요 적합 소재", example = "synthetic")
        private String primaryMaterialFit;
        @Schema(description = "산업군", example = "재활용 섬유")
        private String industryGroup;
        @Schema(description = "거래처 유형", example = "general")
        private String partnerType;
        @Schema(description = "취급 제품 유형 목록", example = "[\"원사\", \"부직포\"]")
        private List<String> factoryProduct;
        @Schema(description = "담당자명", example = "김담당")
        private String managerName;
        @Schema(description = "담당자 연락처", example = "010-1234-5678")
        private String phone;
        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
        private String address;
        @Schema(description = "창고와 거래처 간 거리(km)", example = "12.5")
        private Double distanceKm;
        @Schema(description = "거래처 추천 사유", example = "합성섬유 처리 이력이 많은 거래처입니다.")
        private String companyRationale;
        @Schema(description = "소재 적합도 사유", example = "폴리에스터 계열 소재 처리에 적합합니다.")
        private String materialRationale;
        @Schema(description = "거리 기반 추천 사유", example = "창고에서 가까운 위치에 있습니다.")
        private String distanceRationale;
        @Schema(description = "종합 추천 사유", example = "소재 적합도와 접근성이 모두 양호합니다.")
        private String rationale;
    }

    @Schema(description = "순환 거래처 추천 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class RecommendRes {
        @Schema(description = "추천 거래처 목록")
        private List<RecommendItem> recommendations;
    }

    @Schema(description = "순환 거래처 상세 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        @Schema(description = "순환 거래처 코드", example = "RCV-00001")
        private String code;
        @Schema(description = "거래처명", example = "그린리사이클")
        private String companyName;
        @Schema(description = "산업군", example = "재활용 섬유")
        private String industryGroup;
        @Schema(description = "취급 제품 유형 목록", example = "[\"원사\", \"부직포\"]")
        private List<String> factoryProduct;
        @Schema(description = "거래처 설명", example = "합성섬유 재활용 및 원료화 전문 업체")
        private String description;
        @Schema(description = "주요 적합 소재", example = "synthetic")
        private String primaryMaterialFit;
        @Schema(description = "담당자명", example = "김담당")
        private String managerName;
        @Schema(description = "담당자 연락처", example = "010-1234-5678")
        private String phone;
        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
        private String address;
        @Schema(description = "거래처 유형", example = "general")
        private String partnerType;
        @Schema(description = "등록 일시")
        private Date createdAt;
        @Schema(description = "수정 일시")
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
