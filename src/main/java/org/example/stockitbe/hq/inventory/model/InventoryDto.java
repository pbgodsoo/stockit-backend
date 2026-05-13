package org.example.stockitbe.hq.inventory.model;

import lombok.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;

import java.util.Date;
import java.util.List;

public class InventoryDto {

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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LocationOptionRes {
        private Long id;
        private String code;
        private String name;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanyWidePageRes {
        private List<CompanyWideRes> items;
        private List<LocationOptionRes> locationOptions;
    }

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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateRefreshRes {
        private Integer scannedCount;
        private Integer convertedCount;
    }

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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateConvertItemRes {
        private Long inventoryId;
        private Integer requested;
        private Integer converted;
        private String reason;

        public static CircularCandidateConvertItemRes from(Long inventoryId, int requested, int converted, String reason) {
            return CircularCandidateConvertItemRes.builder()
                    .inventoryId(inventoryId)
                    .requested(requested)
                    .converted(converted)
                    .reason(reason)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateConvertRes {
        private Integer requestedCount;
        private Integer convertedCount;
        private Integer skippedCount;
        private List<CircularCandidateConvertItemRes> items;

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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MaterialCompositionRes {
        private String materialCode;
        private String materialNameKo;
        private Integer ratio;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularMaterialPriceRes {
        private String materialCode;
        private String materialNameKo;
        private String materialGroup;
        private Integer pricePerKg;
        private Boolean active;

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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircularMaterialPriceUpdateReq {
        @NotNull
        @Min(0)
        private Integer pricePerKg;
    }

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
