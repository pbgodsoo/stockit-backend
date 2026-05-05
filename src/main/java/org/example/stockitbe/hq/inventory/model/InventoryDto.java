package org.example.stockitbe.hq.inventory.model;

import lombok.*;

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
    public static class CircularCandidateRefreshRes {
        private Integer scannedCount;
        private Integer convertedCount;
    }
}
