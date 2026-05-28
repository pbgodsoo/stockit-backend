package org.example.stockitbe.warehouse.inventory.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public class WarehouseInventoryDto {

    // 창고 재고 품목 페이지 응답 DTO
    // 품목(상품코드) 단위 집계 결과 + 페이지 메타.
    @Schema(description = "창고 재고 품목 단위 페이지 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemPageRes {
        @Schema(description = "현재 페이지 품목 목록")
        private List<ItemRes> items;
        @Schema(description = "현재 페이지 번호 (0부터)", example = "0")
        private Integer page;
        @Schema(description = "페이지 크기", example = "20")
        private Integer size;
        @Schema(description = "전체 품목 수", example = "150")
        private Long totalElements;
        @Schema(description = "전체 페이지 수", example = "8")
        private Integer totalPages;
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부", example = "false")
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
    // 품목(상품코드) 단위 집계 결과를 반환한다. updatedAt 은 응답에서 제외 (운영 추적용 DB 컬럼은 유지).
    @Schema(description = "창고 재고 품목 한 행 — 본인 창고 기준 품목 단위 집계")
    @Getter
    @Builder
    public static class ItemRes {
        @Schema(description = "품목 코드", example = "PRD-TOP-SS-001")
        private String itemCode;
        @Schema(description = "상위 카테고리 한글명", example = "상의")
        private String parentCategory;
        @Schema(description = "하위 카테고리 한글명", example = "반팔")
        private String childCategory;
        @Schema(description = "품목명", example = "코튼 에센셜 크루 반팔")
        private String itemName;
        @Schema(description = "실재고 합계 (창고 내 해당 품목 모든 SKU 합)", example = "850")
        private Integer actualStock;
        @Schema(description = "가용재고 합계", example = "800")
        private Integer availableStock;
        @Schema(description = "안전재고 임계값", example = "100")
        private Integer safetyStock;
        @Schema(description = "재고 상태 한국어 라벨", example = "정상", allowableValues = {"정상","부족","품절"})
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

    // 창고 재고 SKU 목록 응답 DTO (옛 /{itemCode}/skus 라우트 호환용 — FE 라우트 폐기 후 cleanup 예정)
    // 선택 품목 내 SKU 단위 집계 결과를 반환한다.
    @Schema(description = "선택 품목 산하 SKU 단위 행 (옛 라우트 — cleanup 예정)")
    @Getter
    @Builder
    public static class SkuRes {
        @Schema(description = "SKU 코드", example = "PRD-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "색상", example = "BLK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "실재고", example = "125")
        private Integer actualStock;
        @Schema(description = "가용재고", example = "125")
        private Integer availableStock;
        @Schema(description = "안전재고", example = "20")
        private Integer safetyStock;
        @Schema(description = "재고 상태 한국어 라벨", example = "정상", allowableValues = {"정상","부족","품절"})
        private String status;
        @Schema(description = "마지막 변동 시각", example = "2026-05-25T13:21:20.000+09:00")
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

    // 창고 재고 SKU 단위 페이지 응답 DTO (모드 토글 SKU 모드 — 마스터 무관 모든 SKU 한 표).
    // updatedAt 응답 X (전사재고 SKU 와 동일 정책).
    @Schema(description = "창고 재고 SKU 한 행 — 마스터 무관 SKU 평탄 row")
    @Getter
    @Builder
    public static class SkuRowRes {
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
        @Schema(description = "색상", example = "BLK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "실재고", example = "125")
        private Integer actualStock;
        @Schema(description = "가용재고", example = "125")
        private Integer availableStock;
        @Schema(description = "안전재고", example = "20")
        private Integer safetyStock;
        @Schema(description = "재고 상태 한국어 라벨", example = "정상", allowableValues = {"정상","부족","품절"})
        private String status;

        public static SkuRowRes from(String skuCode,
                                     String itemCode,
                                     String itemName,
                                     String parentCategory,
                                     String childCategory,
                                     String color,
                                     String size,
                                     int actualStock,
                                     int availableStock,
                                     int safetyStock,
                                     String status) {
            return SkuRowRes.builder()
                    .skuCode(skuCode)
                    .itemCode(itemCode)
                    .itemName(itemName)
                    .parentCategory(parentCategory)
                    .childCategory(childCategory)
                    .color(color)
                    .size(size)
                    .actualStock(actualStock)
                    .availableStock(availableStock)
                    .safetyStock(safetyStock)
                    .status(status)
                    .build();
        }
    }

    // SKU 페이지 응답 DTO + 페이지 메타.
    @Schema(description = "창고 재고 SKU 페이지 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuPageRes {
        @Schema(description = "현재 페이지 SKU 목록")
        private List<SkuRowRes> items;
        @Schema(description = "현재 페이지 번호 (0부터)", example = "0")
        private Integer page;
        @Schema(description = "페이지 크기", example = "20")
        private Integer size;
        @Schema(description = "전체 SKU 수", example = "1350")
        private Long totalElements;
        @Schema(description = "전체 페이지 수", example = "68")
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

    // SKU 칩 필터용 facets 응답 DTO — 같은 거점/카테고리/검색 필터 조건 안에서 가능한 색상/사이즈 distinct.
    @Schema(description = "창고 재고 SKU 칩 필터 옵션 (색상·사이즈 distinct)")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuFacetsRes {
        @Schema(description = "노출 가능 색상 코드 목록", example = "[\"BLK\",\"WHT\",\"NVY\",\"GRY\"]")
        private List<String> colors;
        @Schema(description = "노출 가능 사이즈 목록", example = "[\"S\",\"M\",\"L\",\"XL\"]")
        private List<String> sizes;
    }
}
