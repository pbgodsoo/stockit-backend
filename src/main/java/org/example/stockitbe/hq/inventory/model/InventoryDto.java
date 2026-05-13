package org.example.stockitbe.hq.inventory.model;

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
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWideRes {
        private String itemCode;
        private String parentCategory;
        private String childCategory;
        private String itemName;
        private Integer actualStock;
        private Integer availableStock;
        private Integer safetyStock;
        private String status;
        private Date updatedAt;
    }

    // 전사 재고 SKU 상세 응답 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWideSkuDetailRes {
        private String skuCode;
        private String color;
        private String size;
        private Long unitPrice;
        private Integer actualStock;
        private Integer availableStock;
        private Integer safetyStock;
        private String status;
        private Date updatedAt;
    }

    // 위치 필터 옵션 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class LocationOptionRes {
        private Long id;
        private String code;
        private String name;
    }

    // 전사 재고 페이지 응답 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWidePageRes {
        private List<CompanyWideRes> items;
        private List<LocationOptionRes> locationOptions;
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
        private Boolean hasNext;
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
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateRes {
        private Long inventoryId;
        private String skuCode;
        private String itemCode;
        private String parentCategory;
        private String childCategory;
        private String itemName;
        private String warehouseCode;
        private String warehouseName;
        private String color;
        private String size;
        private Integer actualStock;
        private Integer availableStock;
        private Integer convertibleStock;
        private Date updatedAt;
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
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidatePageRes {
        private List<CircularCandidateRes> content;
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
        private Boolean hasNext;
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
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateRefreshRes {
        private Integer scannedCount;
        private Integer convertedCount;
    }

    // 후보 전환 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircularCandidateConvertItemReq {
        @NotNull
        private Long inventoryId;

        @NotNull
        @Min(1)
        private Integer convertQuantity;
    }

    // 후보 전환 결과 행 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateConvertItemRes {
        private Long inventoryId;
        private Integer requested;
        private Integer converted;
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
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateConvertRes {
        private Integer requestedCount;
        private Integer convertedCount;
        private Integer skippedCount;
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
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularInventoryRes {
        private Long inventoryId;
        private String skuCode;
        private String itemCode;
        private String itemName;
        private String warehouseCode;
        private String warehouseName;
        private String parentCategory;
        private String childCategory;
        private String color;
        private String size;
        private Integer availableQuantity;
        private String materialType;
        private List<MaterialCompositionRes> materialCompositions;
        private Integer materialKgPrice;
        private Double unitWeightKg;
        private Double totalWeightKg;
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
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularInventoryPageRes {
        private List<CircularInventoryRes> content;
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
        private Boolean hasNext;
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
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MaterialCompositionRes {
        private String materialCode;
        private String materialNameKo;
        private Integer ratio;
    }

    // 순환재고 소재 단가 정책 응답 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularMaterialPriceRes {
        private String materialCode;
        private String materialNameKo;
        private String materialGroup;
        private Integer pricePerKg;
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
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircularMaterialPriceUpdateReq {
        @NotNull
        @Min(0)
        private Integer pricePerKg;
    }

    // 전사 재고 불균형 SKU 응답 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ImbalancedSkuRes {
        private String skuCode;
        private String itemCode;
        private String itemName;
        private String color;
        private String size;
        private String category;
        private Integer totalOnHand;
        private Integer totalAvailable;
        private Integer shortageWarehouseCount;
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
    }
}
