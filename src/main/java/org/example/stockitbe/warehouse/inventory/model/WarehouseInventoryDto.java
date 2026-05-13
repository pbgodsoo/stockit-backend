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

        public static ItemRes from(String itemCode,
                                   String parentCategory,
                                   String childCategory,
                                   String itemName,
                                   int actualStock,
                                   int availableStock,
                                   int safetyStock,
                                   String status,
                                   Date updatedAt) {
            return ItemRes.builder()
                    .itemCode(itemCode)
                    .parentCategory(parentCategory)
                    .childCategory(childCategory)
                    .itemName(itemName)
                    .actualStock(actualStock)
                    .availableStock(availableStock)
                    .safetyStock(safetyStock)
                    .status(status)
                    .updatedAt(updatedAt)
                    .build();
        }
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

        public static SkuRes from(String skuCode,
                                  String color,
                                  String size,
                                  int actualStock,
                                  int availableStock,
                                  int safetyStock,
                                  String status,
                                  Date updatedAt) {
            return SkuRes.builder()
                    .skuCode(skuCode)
                    .color(color)
                    .size(size)
                    .actualStock(actualStock)
                    .availableStock(availableStock)
                    .safetyStock(safetyStock)
                    .status(status)
                    .updatedAt(updatedAt)
                    .build();
        }
    }
}
