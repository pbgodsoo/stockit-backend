package org.example.stockitbe.store.inventory.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

public class StoreInventoryDto {

    @Schema(description = "매장 재고 품목 페이지 응답 DTO")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemPageRes {
        @Schema(description = "품목 목록")
        private List<ItemRes> items;
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private Integer page;
        @Schema(description = "페이지 크기", example = "20")
        private Integer size;
        @Schema(description = "총 요소 수", example = "100")
        private Long totalElements;
        @Schema(description = "총 페이지 수", example = "5")
        private Integer totalPages;
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부", example = "false")
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

    @Schema(description = "매장 재고 품목 응답 DTO (품목 단위 집계)")
    @Getter
    @Builder
    public static class ItemRes {
        @Schema(description = "상품 코드", example = "ITEM-001")
        private String itemCode;
        @Schema(description = "대분류 카테고리", example = "상의")
        private String parentCategory;
        @Schema(description = "소분류 카테고리", example = "티셔츠")
        private String childCategory;
        @Schema(description = "상품명", example = "반팔 티셔츠")
        private String itemName;
        @Schema(description = "실재고 수량", example = "30")
        private Integer actualStock;
        @Schema(description = "가용 재고 수량", example = "20")
        private Integer availableStock;
        @Schema(description = "안전 재고 수량", example = "10")
        private Integer safetyStock;
        @Schema(description = "재고 상태 (정상 / 부족 / 품절)", example = "정상")
        private String status;

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

    @Schema(description = "매장 재고 SKU 행 응답 DTO (SKU 모드 — 모든 SKU 한 표)")
    @Getter
    @Builder
    public static class SkuRowRes {
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        private String skuCode;
        @Schema(description = "상품 코드", example = "ITEM-001")
        private String itemCode;
        @Schema(description = "상품명", example = "반팔 티셔츠")
        private String itemName;
        @Schema(description = "대분류 카테고리", example = "상의")
        private String parentCategory;
        @Schema(description = "소분류 카테고리", example = "티셔츠")
        private String childCategory;
        @Schema(description = "색상", example = "레드")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "단가 (원)", example = "15000")
        private Long unitPrice;
        @Schema(description = "실재고 수량", example = "10")
        private Integer actualStock;
        @Schema(description = "가용 재고 수량", example = "8")
        private Integer availableStock;
        @Schema(description = "안전 재고 수량", example = "5")
        private Integer safetyStock;
        @Schema(description = "입고 예정 수량", example = "20")
        private Integer inboundExpectedQuantity;
        @Schema(description = "재고 상태 (정상 / 부족 / 품절)", example = "정상")
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

    @Schema(description = "매장 재고 SKU 페이지 응답 DTO")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuPageRes {
        @Schema(description = "SKU 목록")
        private List<SkuRowRes> items;
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private Integer page;
        @Schema(description = "페이지 크기", example = "20")
        private Integer size;
        @Schema(description = "총 요소 수", example = "100")
        private Long totalElements;
        @Schema(description = "총 페이지 수", example = "5")
        private Integer totalPages;
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부", example = "false")
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

    @Schema(description = "SKU 칩 필터용 Facets 응답 DTO")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuFacetsRes {
        @Schema(description = "현재 조건 내 가능한 색상 목록", example = "[\"레드\", \"블랙\", \"화이트\"]")
        private List<String> colors;
        @Schema(description = "현재 조건 내 가능한 사이즈 목록", example = "[\"S\", \"M\", \"L\", \"XL\"]")
        private List<String> sizes;
    }
}
