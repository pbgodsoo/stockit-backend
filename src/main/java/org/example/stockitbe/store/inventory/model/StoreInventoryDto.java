package org.example.stockitbe.store.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public class StoreInventoryDto {

    // 매장 재고 품목 페이지 응답 DTO
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

    // 매장 재고 품목 목록 응답 DTO
    // 품목(상품코드) 단위 집계 결과를 반환한다. updatedAt 은 응답에서 제외 (운영 추적용 DB 컬럼은 유지).
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

        // 품목 집계 필드를 응답 DTO로 변환한다.
        public static ItemRes from(String itemCode,
                                   String parentCategory,
                                   String childCategory,
                                   String itemName,
                                   int actualStock,
                                   int availableStock,
                                   int safetyStock,
                                   String status) {
            return ItemRes.builder()
                    .itemCode(itemCode)
                    .parentCategory(parentCategory)
                    .childCategory(childCategory)
                    .itemName(itemName)
                    .actualStock(actualStock)
                    .availableStock(availableStock)
                    .safetyStock(safetyStock)
                    .status(status)
                    .build();
        }
    }

    // 매장 재고 SKU 목록 응답 DTO (옛 /{itemCode}/skus 라우트 호환용 — FE 라우트 폐기 후 cleanup 예정)
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

    // 매장 재고 SKU 단위 페이지 응답 DTO (모드 토글 SKU 모드 — 마스터 무관 모든 SKU 한 표).
    // updatedAt 응답 X (전사/창고재고 SKU 와 동일 정책).
    @Getter
    @Builder
    public static class SkuRowRes {
        private String skuCode;
        private String itemCode;
        private String itemName;
        private String parentCategory;
        private String childCategory;
        private String color;
        private String size;
        private Long unitPrice;
        private Integer actualStock;
        private Integer availableStock;
        private Integer safetyStock;
        private Integer inboundExpectedQuantity;
        private String status;

        public static SkuRowRes from(String skuCode,
                                     String itemCode,
                                     String itemName,
                                     String parentCategory,
                                     String childCategory,
                                     String color,
                                     String size,
                                     long unitPrice,
                                     int actualStock,
                                     int availableStock,
                                     int safetyStock,
                                     int inboundExpectedQuantity,
                                     String status) {
            return SkuRowRes.builder()
                    .skuCode(skuCode)
                    .itemCode(itemCode)
                    .itemName(itemName)
                    .parentCategory(parentCategory)
                    .childCategory(childCategory)
                    .color(color)
                    .size(size)
                    .unitPrice(unitPrice)
                    .actualStock(actualStock)
                    .availableStock(availableStock)
                    .safetyStock(safetyStock)
                    .inboundExpectedQuantity(inboundExpectedQuantity)
                    .status(status)
                    .build();
        }
    }

    // SKU 페이지 응답 DTO + 페이지 메타.
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuPageRes {
        private List<SkuRowRes> items;
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;

        public static SkuPageRes from(Page<SkuRowRes> page) {
            return SkuPageRes.builder()
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

    // SKU 칩 필터용 facets 응답 DTO — 같은 거점/카테고리/검색 필터 조건 안에서 가능한 색상/사이즈 distinct.
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuFacetsRes {
        private List<String> colors;
        private List<String> sizes;
    }
}
