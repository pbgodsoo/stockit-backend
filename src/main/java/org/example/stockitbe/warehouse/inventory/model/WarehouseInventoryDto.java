package org.example.stockitbe.warehouse.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public class WarehouseInventoryDto {

    // 창고 재고 품목 페이지 응답 DTO
    // 품목(상품코드) 단위 집계 결과 + 페이지 메타.
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemPageRes {
        private List<ItemRes> items;
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;

        // 페이지 결과를 응답 DTO로 변환한다.
        public static ItemPageRes from(Page<ItemRes> page) {
            return ItemPageRes.builder()
                    .items(page.getContent())
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .build();
        }
    }

    // 창고 재고 품목 목록 응답 DTO
    // 품목(상품코드) 단위 집계 결과를 반환한다.
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

        // 품목 집계 필드를 응답 DTO로 변환한다.
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

    // 창고 재고 SKU 목록 응답 DTO
    // 선택 품목 내 SKU 단위 집계 결과를 반환한다.
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

        // SKU 집계 필드를 응답 DTO로 변환한다.
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
