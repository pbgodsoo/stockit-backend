package org.example.stockitbe.warehouse.inventory.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;

public class WarehouseInventoryDto {

    @Getter
    @Builder
    public static class ItemRes {
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
    public static class SkuRes {
        private String skuCode;
        private String color;
        private String size;
        private Integer actualStock;
        private Integer availableStock;
        private Integer safetyStock;
        private String status;
        private Date updatedAt;
    }
}
