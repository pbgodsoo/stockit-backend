package org.example.stockitbe.hq.inventory.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

// 본사 재고/순환재고 API 응답·요청 DTO 모음
public class InventoryDto {

    // 전사 재고(품목 단위) 응답 DTO
    @Schema(description = "전사 재고 한 행 — 품목 단위 집계 (거점 그룹별 1개 row)")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWideRes {
        @Schema(description = "품목 코드", example = "PRD-TOP-SS-001")
        private String itemCode;
        @Schema(description = "상위 카테고리 한글명", example = "상의")
        private String parentCategory;
        @Schema(description = "하위 카테고리 한글명", example = "반팔")
        private String childCategory;
        @Schema(description = "품목명", example = "코튼 에센셜 크루 반팔")
        private String itemName;
        @Schema(description = "실재고 합계 (해당 거점 그룹 안 모든 SKU 합)", example = "1250")
        private Integer actualStock;
        @Schema(description = "가용재고 합계", example = "1180")
        private Integer availableStock;
        @Schema(description = "안전재고 임계값", example = "100")
        private Integer safetyStock;
        @Schema(description = "재고 상태 한국어 라벨", example = "정상", allowableValues = {"정상","부족","품절"})
        private String status;
        @Schema(description = "마지막 변동 시각", example = "2026-05-25T13:21:20.000+09:00")
        private Date updatedAt;
    }

    // 전사 재고 SKU 상세 응답 DTO
    @Schema(description = "전사 재고 SKU 상세 — 특정 품목 하위 SKU 한 행")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWideSkuDetailRes {
        @Schema(description = "SKU 코드", example = "PRD-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "색상 (3자리 코드)", example = "BLK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "SKU 단가 (KRW)", example = "21100")
        private Long unitPrice;
        @Schema(description = "실재고 수량", example = "125")
        private Integer actualStock;
        @Schema(description = "가용재고 수량", example = "125")
        private Integer availableStock;
        @Schema(description = "안전재고 임계값", example = "20")
        private Integer safetyStock;
        @Schema(description = "재고 상태 한국어 라벨", example = "정상", allowableValues = {"정상","부족","품절"})
        private String status;
        @Schema(description = "마지막 변동 시각", example = "2026-05-25T13:21:20.000+09:00")
        private Date updatedAt;
    }

    // 위치 필터 옵션 DTO
    // region: 한글 지역명 (예: "서울"/"경기"). FE 거점 트리 지역 그룹화용.
    @Schema(description = "거점 필터 옵션 1건 — FE 거점 트리에서 지역으로 그룹화")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class LocationOptionRes {
        @Schema(description = "거점 PK (infrastructure.id)", example = "21")
        private Long id;
        @Schema(description = "거점 코드", example = "WH-GW-0001")
        private String code;
        @Schema(description = "거점 이름", example = "강원 강릉 동해안 물류허브")
        private String name;
        @Schema(description = "지역 한글명 (FE 그룹화 키)", example = "강원")
        private String region;
    }

    // 전사 재고 SKU 단위 응답 DTO (모드 토글 SKU 모드용 — 마스터 무관 페이징)
    // updatedAt 응답 X (FE 컬럼 제거 결정).
    @Schema(description = "전사 재고 SKU 한 행 — 마스터 무관 SKU 모드 평탄 row")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWideSkuRowRes {
        @Schema(description = "SKU 코드", example = "PRD-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "품목 코드", example = "PRD-TOP-SS-001")
        private String itemCode;
        @Schema(description = "품목명", example = "코튼 에센셜 크루 반팔")
        private String itemName;
        @Schema(description = "상위 카테고리", example = "상의")
        private String parentCategory;
        @Schema(description = "하위 카테고리", example = "반팔")
        private String childCategory;
        @Schema(description = "색상 코드", example = "BLK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "실재고 수량", example = "125")
        private Integer actualStock;
        @Schema(description = "가용재고 수량", example = "125")
        private Integer availableStock;
        @Schema(description = "안전재고 임계값", example = "20")
        private Integer safetyStock;
        @Schema(description = "재고 상태 한국어 라벨 (SQL CASE)", example = "정상", allowableValues = {"정상","부족","품절"})
        private String status;     // "정상"/"부족"/"품절" — SQL CASE 라벨
    }

    // 전사 재고 SKU facets 응답 DTO (칩 필터용 — 거점/카테고리/검색 조건 안의 distinct 색상/사이즈)
    @Schema(description = "전사 재고 SKU 화면의 색상·사이즈 칩 필터 옵션")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWideSkuFacetsRes {
        @Schema(description = "노출 가능 색상 코드 목록", example = "[\"BLK\",\"WHT\",\"NVY\",\"GRY\"]")
        private List<String> colors;
        @Schema(description = "노출 가능 사이즈 목록", example = "[\"S\",\"M\",\"L\",\"XL\"]")
        private List<String> sizes;
    }

    // 전사 재고 SKU 페이지 응답 DTO
    @Schema(description = "전사 재고 SKU 페이지 응답 — 페이지 메타 + 거점 옵션 패키지")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWideSkuPageRes {
        @Schema(description = "현재 페이지 행 목록")
        private List<CompanyWideSkuRowRes> items;
        @Schema(description = "FE 거점 필터에 노출할 거점 옵션 (보통 전체 STORE+WAREHOUSE 또는 권한 안 거점)")
        private List<LocationOptionRes> locationOptions;
        @Schema(description = "현재 페이지 번호 (0부터)", example = "0")
        private Integer page;
        @Schema(description = "페이지 크기", example = "20")
        private Integer size;
        @Schema(description = "전체 row 수", example = "236280")
        private Long totalElements;
        @Schema(description = "전체 페이지 수", example = "11814")
        private Integer totalPages;
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private Boolean hasPrevious;

        public static CompanyWideSkuPageRes from(Page<CompanyWideSkuRowRes> page,
                                                 List<LocationOptionRes> locationOptions) {
            return CompanyWideSkuPageRes.builder()
                    .items(page.getContent())
                    .locationOptions(locationOptions)
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .build();
        }
    }

    // 전사 재고 페이지 응답 DTO
    @Schema(description = "전사 재고 품목 단위 페이지 응답 — 페이지 메타 + 거점 옵션 패키지")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWidePageRes {
        @Schema(description = "현재 페이지 품목 행 목록")
        private List<CompanyWideRes> items;
        @Schema(description = "FE 거점 필터 옵션 (보통 STORE+WAREHOUSE 전체)")
        private List<LocationOptionRes> locationOptions;
        @Schema(description = "현재 페이지 번호 (0부터)", example = "0")
        private Integer page;
        @Schema(description = "페이지 크기", example = "20")
        private Integer size;
        @Schema(description = "전체 row 수", example = "150")
        private Long totalElements;
        @Schema(description = "전체 페이지 수", example = "8")
        private Integer totalPages;
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private Boolean hasPrevious;

        // 페이지 결과 + 위치 옵션 목록을 응답 DTO로 변환한다.
        public static CompanyWidePageRes from(Page<CompanyWideRes> page,
                                              List<LocationOptionRes> locationOptions) {
            return CompanyWidePageRes.builder()
                    .items(page.getContent())
                    .locationOptions(locationOptions)
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .build();
        }
    }

    // 순환재고 후보 목록 행 DTO
    @Schema(description = "순환재고 후보 목록 행")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateRes {
        @Schema(description = "재고 ID", example = "1001")
        private Long inventoryId;
        @Schema(description = "SKU 코드", example = "SKU-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "품목 코드", example = "PRD-TOP-SS-001")
        private String itemCode;
        @Schema(description = "상위 카테고리", example = "상의")
        private String parentCategory;
        @Schema(description = "하위 카테고리", example = "자켓")
        private String childCategory;
        @Schema(description = "품목명", example = "폴리에스터 자켓")
        private String itemName;
        @Schema(description = "창고 코드", example = "WH-001")
        private String warehouseCode;
        @Schema(description = "창고명", example = "서울 물류창고")
        private String warehouseName;
        @Schema(description = "색상", example = "BLACK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "실재고 수량", example = "120")
        private Integer actualStock;
        @Schema(description = "가용재고 수량", example = "100")
        private Integer availableStock;
        @Schema(description = "순환재고 전환 가능 수량", example = "80")
        private Integer convertibleStock;
        @Schema(description = "재고 수정 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date updatedAt;
        @Schema(description = "충족한 순환재고 후보 조건 코드 목록", example = "[1,2]")
        private List<Integer> matchedConditionCodes;

        // 후보 재고 집계 필드를 응답 DTO로 변환한다.
        public static CircularCandidateRes from(Long inventoryId,
                                              ProductSku sku,
                                              ProductMaster master,
                                              String parentCategory,
                                              String childCategory,
                                              String warehouseCode,
                                              String warehouseName,
                                              int actualStock,
                                              int availableStock,
                                              int convertibleStock,
                                              Date updatedAt,
                                              List<Integer> matchedConditionCodes) {
            return CircularCandidateRes.builder()
                    .inventoryId(inventoryId)
                    .skuCode(sku.getSkuCode())
                    .itemCode(master.getCode())
                    .parentCategory(parentCategory)
                    .childCategory(childCategory)
                    .itemName(master.getName())
                    .warehouseCode(warehouseCode)
                    .warehouseName(warehouseName)
                    .color(sku.getColor())
                    .size(sku.getSize())
                    .actualStock(actualStock)
                    .availableStock(availableStock)
                    .convertibleStock(convertibleStock)
                    .updatedAt(updatedAt)
                    .matchedConditionCodes(matchedConditionCodes)
                    .build();
        }
    }

    // 순환재고 후보 페이지 응답 DTO
    @Schema(description = "순환재고 후보 페이지 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidatePageRes {
        @Schema(description = "순환재고 후보 목록")
        private List<CircularCandidateRes> content;
        @Schema(description = "현재 페이지 번호", example = "0")
        private Integer page;
        @Schema(description = "페이지 크기", example = "20")
        private Integer size;
        @Schema(description = "전체 후보 수", example = "125")
        private Long totalElements;
        @Schema(description = "전체 페이지 수", example = "7")
        private Integer totalPages;
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private Boolean hasPrevious;

        // 페이지 계산 결과를 후보 페이지 응답으로 변환한다.
        public static CircularCandidatePageRes from(List<CircularCandidateRes> content,
                                                  int page,
                                                  int size,
                                                  long totalElements,
                                                  int totalPages,
                                                  boolean hasNext,
                                                  boolean hasPrevious) {
            return CircularCandidatePageRes.builder()
                    .content(content)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(hasNext)
                    .hasPrevious(hasPrevious)
                    .build();
        }
    }

    // 후보 리프레시 결과 DTO
    @Schema(description = "순환재고 후보 리프레시 결과")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateRefreshRes {
        @Schema(description = "스캔한 재고 수", example = "500")
        private Integer scannedCount;
        @Schema(description = "후보 상태로 갱신된 재고 수", example = "42")
        private Integer convertedCount;
    }

    // 후보 전환 요청 DTO
    @Schema(description = "순환재고 후보 전환 요청 항목")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircularCandidateConvertItemReq {
        @Schema(description = "전환할 후보 재고 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Long inventoryId;

        @Schema(description = "순환재고로 전환할 수량", example = "10", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1)
        private Integer convertQuantity;
    }

    // 후보 전환 결과 행 DTO
    @Schema(description = "순환재고 후보 전환 결과 항목")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateConvertItemRes {
        @Schema(description = "재고 ID", example = "1001")
        private Long inventoryId;
        @Schema(description = "요청 수량", example = "10")
        private Integer requested;
        @Schema(description = "전환 완료 수량", example = "10")
        private Integer converted;
        @Schema(description = "처리 결과 또는 실패 사유", example = "SUCCESS")
        private String reason;

        // 전환 처리 결과를 응답 DTO로 변환한다.
        public static CircularCandidateConvertItemRes from(Long inventoryId, int requested, int converted, String reason) {
            return CircularCandidateConvertItemRes.builder()
                    .inventoryId(inventoryId)
                    .requested(requested)
                    .converted(converted)
                    .reason(reason)
                    .build();
        }
    }

    // 후보 전환 요약 DTO
    @Schema(description = "순환재고 후보 전환 요약 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateConvertRes {
        @Schema(description = "요청 항목 수", example = "3")
        private Integer requestedCount;
        @Schema(description = "전환 성공 항목 수", example = "2")
        private Integer convertedCount;
        @Schema(description = "전환 제외 항목 수", example = "1")
        private Integer skippedCount;
        @Schema(description = "항목별 전환 결과")
        private List<CircularCandidateConvertItemRes> items;

        // 전환 처리 집계값을 요약 DTO로 변환한다.
        public static CircularCandidateConvertRes from(int requestedCount,
                                                     int convertedCount,
                                                     List<CircularCandidateConvertItemRes> items) {
            return CircularCandidateConvertRes.builder()
                    .requestedCount(requestedCount)
                    .convertedCount(convertedCount)
                    .skippedCount(requestedCount - convertedCount)
                    .items(items)
                    .build();
        }
    }

    // 순환재고 목록 행 DTO
    @Schema(description = "순환재고 목록 행")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularInventoryRes {
        @Schema(description = "재고 ID", example = "2001")
        private Long inventoryId;
        @Schema(description = "SKU 코드", example = "SKU-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "품목 코드", example = "PRD-TOP-SS-001")
        private String itemCode;
        @Schema(description = "품목명", example = "폴리에스터 자켓")
        private String itemName;
        @Schema(description = "창고 코드", example = "WH-001")
        private String warehouseCode;
        @Schema(description = "창고명", example = "서울 물류창고")
        private String warehouseName;
        @Schema(description = "상위 카테고리", example = "상의")
        private String parentCategory;
        @Schema(description = "하위 카테고리", example = "자켓")
        private String childCategory;
        @Schema(description = "색상", example = "BLACK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "가용 수량", example = "80")
        private Integer availableQuantity;
        @Schema(description = "소재 그룹", example = "synthetic")
        private String materialType;
        @Schema(description = "소재 구성 비율 목록")
        private List<MaterialCompositionRes> materialCompositions;
        @Schema(description = "kg당 소재 단가", example = "1200")
        private Integer materialKgPrice;
        @Schema(description = "단위 중량(kg)", example = "0.4")
        private Double unitWeightKg;
        @Schema(description = "총 중량(kg)", example = "32.0")
        private Double totalWeightKg;
        @Schema(description = "산정된 순환재고 판매가", example = "38400")
        private Long circularSalePrice;

        // 순환재고 집계 필드를 응답 DTO로 변환한다.
        public static CircularInventoryRes from(Long inventoryId,
                                              ProductSku sku,
                                              ProductMaster master,
                                              String warehouseCode,
                                              String warehouseName,
                                              String parentCategory,
                                              String childCategory,
                                              int availableQuantity,
                                              String materialType,
                                              List<MaterialCompositionRes> materialCompositions,
                                              int materialKgPrice,
                                              double unitWeightKg,
                                              double totalWeightKg,
                                              long circularSalePrice) {
            return CircularInventoryRes.builder()
                    .inventoryId(inventoryId)
                    .skuCode(sku.getSkuCode())
                    .itemCode(master.getCode())
                    .itemName(master.getName())
                    .warehouseCode(warehouseCode)
                    .warehouseName(warehouseName)
                    .parentCategory(parentCategory)
                    .childCategory(childCategory)
                    .color(sku.getColor())
                    .size(sku.getSize())
                    .availableQuantity(availableQuantity)
                    .materialType(materialType)
                    .materialCompositions(materialCompositions)
                    .materialKgPrice(materialKgPrice)
                    .unitWeightKg(unitWeightKg)
                    .totalWeightKg(totalWeightKg)
                    .circularSalePrice(circularSalePrice)
                    .build();
        }
    }

    // 순환재고 페이지 응답 DTO
    @Schema(description = "순환재고 페이지 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularInventoryPageRes {
        @Schema(description = "순환재고 목록")
        private List<CircularInventoryRes> content;
        @Schema(description = "현재 페이지 번호", example = "0")
        private Integer page;
        @Schema(description = "페이지 크기", example = "20")
        private Integer size;
        @Schema(description = "전체 순환재고 수", example = "85")
        private Long totalElements;
        @Schema(description = "전체 페이지 수", example = "5")
        private Integer totalPages;
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private Boolean hasPrevious;

        // 페이지 계산 결과를 순환재고 페이지 응답으로 변환한다.
        public static CircularInventoryPageRes from(List<CircularInventoryRes> content,
                                                  int page,
                                                  int size,
                                                  long totalElements,
                                                  int totalPages,
                                                  boolean hasNext,
                                                  boolean hasPrevious) {
            return CircularInventoryPageRes.builder()
                    .content(content)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(hasNext)
                    .hasPrevious(hasPrevious)
                    .build();
        }
    }

    // 소재 구성 비율 응답 DTO
    @Schema(description = "소재 구성 비율")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MaterialCompositionRes {
        @Schema(description = "소재 코드", example = "POLYESTER")
        private String materialCode;
        @Schema(description = "소재 한글명", example = "폴리에스터")
        private String materialNameKo;
        @Schema(description = "구성 비율", example = "100")
        private Integer ratio;
    }

    // 순환재고 소재 단가 정책 응답 DTO
    @Schema(description = "순환재고 소재 단가 정책")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularMaterialPriceRes {
        @Schema(description = "소재 코드", example = "POLYESTER")
        private String materialCode;
        @Schema(description = "소재 한글명", example = "폴리에스터")
        private String materialNameKo;
        @Schema(description = "소재 그룹", example = "synthetic")
        private String materialGroup;
        @Schema(description = "kg당 단가", example = "1200")
        private Integer pricePerKg;
        @Schema(description = "활성 여부", example = "true")
        private Boolean active;

        // 소재 단가 정책 엔티티를 응답 DTO로 변환한다.
        public static CircularMaterialPriceRes from(CircularMaterialPricePolicy policy) {
            return CircularMaterialPriceRes.builder()
                    .materialCode(policy.getMaterialCode())
                    .materialNameKo(policy.getMaterialNameKo())
                    .materialGroup(policy.getMaterialGroup())
                    .pricePerKg(policy.getPricePerKg())
                    .active(policy.getActive())
                    .build();
        }
    }

    // 순환재고 소재 단가 수정 요청 DTO
    @Schema(description = "순환재고 소재 단가 수정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircularMaterialPriceUpdateReq {
        @Schema(description = "변경할 kg당 단가", example = "1300", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(0)
        private Integer pricePerKg;
    }

    // 전사 재고 불균형 SKU 응답 DTO
    @Schema(description = "창고별 재고 불균형 SKU 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ImbalancedSkuRes {
        @Schema(description = "SKU 코드", example = "SKU-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "품목 코드", example = "PRD-TOP-SS-001")
        private String itemCode;
        @Schema(description = "품목명", example = "폴리에스터 자켓")
        private String itemName;
        @Schema(description = "색상", example = "BLACK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "카테고리", example = "상의 > 자켓")
        private String category;
        @Schema(description = "전체 창고 실재고 합계", example = "120")
        private Integer totalOnHand;
        @Schema(description = "전체 창고 가용재고 합계", example = "100")
        private Integer totalAvailable;
        @Schema(description = "부족 상태 창고 수", example = "2")
        private Integer shortageWarehouseCount;
        @Schema(description = "전체 부족 수량", example = "30")
        private Integer totalShortageQty;

        // 불균형 SKU 집계 필드를 응답 DTO로 변환한다.
        public static ImbalancedSkuRes from(ProductSku sku,
                                          ProductMaster master,
                                          String categoryLabel,
                                          int totalOnHand,
                                          int totalAvailable,
                                          int shortageWarehouseCount,
                                          int totalShortageQty) {
            return ImbalancedSkuRes.builder()
                    .skuCode(sku.getSkuCode())
                    .itemCode(master.getCode())
                    .itemName(master.getName())
                    .color(sku.getColor())
                    .size(sku.getSize())
                    .category(categoryLabel)
                    .totalOnHand(totalOnHand)
                    .totalAvailable(totalAvailable)
                    .shortageWarehouseCount(shortageWarehouseCount)
                    .totalShortageQty(totalShortageQty)
                    .build();
        }

        // 네이티브 SQL projection row를 응답 DTO로 변환한다.
        public static ImbalancedSkuRes from(ImbalancedSkuRow row) {
            return ImbalancedSkuRes.builder()
                    .skuCode(row.getSkuCode())
                    .itemCode(row.getItemCode())
                    .itemName(row.getItemName())
                    .color(row.getColor())
                    .size(row.getSize())
                    .category(row.getCategory())
                    .totalOnHand(row.getTotalOnHand())
                    .totalAvailable(row.getTotalAvailable())
                    .shortageWarehouseCount(row.getShortageWarehouseCount())
                    .totalShortageQty(row.getTotalShortageQty())
                    .build();
        }
    }
}
