package org.example.stockitbe.hq.inventory.model;

import lombok.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CircularCandidateConvertRes {
        private Integer requestedCount;
        private Integer convertedCount;
        private Integer skippedCount;
        private List<CircularCandidateConvertItemRes> items;
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
    }
}
