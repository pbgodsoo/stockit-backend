package org.example.stockitbe.hq.purchaseorder.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 새 발주 페이지 카탈로그 응답 DTO.
 *
 * SKU 평탄 row (Page<SkuRowRes>) + 색상/사이즈 facet 별도 (FacetsRes).
 * 진입점: ProductSku — page 단위가 SKU 라 진입점도 SKU.
 * vendor_product 는 JOIN 으로 결합. product_master 와 ProductSku 는 자연 키 결합이라 JPA 매핑 X — 네이티브 SQL.
 */
public class PurchaseOrderCatalogDto {

    @Schema(description = "발주 카탈로그 SKU 한 행 — vendor_product × product_sku × inventory 평탄화")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SkuRowRes {
        @Schema(description = "거래처 코드", example = "VND-001")
        private String vendorCode;
        @Schema(description = "거래처 이름", example = "(주)테크서플라이")
        private String vendorName;
        @Schema(description = "거래처 상품 코드", example = "VP-TOP-SS-001-V00")
        private String vendorProductCode;
        @Schema(description = "본사 상품 코드", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "상품명", example = "코튼 에센셜 크루 반팔")
        private String productName;
        @Schema(description = "SKU 코드", example = "PRD-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "색상 코드 (3자리)", example = "BLK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "화면 표기용 옵션 문자열", example = "BLK/M")
        private String displayOption;
        @Schema(description = "SKU 표준 단가 (KRW)", example = "21100")
        private Long unitPrice;
        @Schema(description = "거래처 계약 단가 (KRW). 표준 단가와 다를 수 있음", example = "21100")
        private Long contractUnitPrice;
        @Schema(description = "선택 창고의 가용재고 (warehouseId 미지정 시 전 창고 합계)", example = "342")
        private Long availableQty;

        public static SkuRowRes from(SkuRowProjection p) {
            return SkuRowRes.builder()
                    .vendorCode(p.getVendorCode())
                    .vendorName(p.getVendorName())
                    .vendorProductCode(p.getVendorProductCode())
                    .productCode(p.getProductCode())
                    .productName(p.getProductName())
                    .skuCode(p.getSkuCode())
                    .color(p.getColor())
                    .size(p.getSize())
                    .displayOption(p.getDisplayOption())
                    .unitPrice(p.getUnitPrice())
                    .contractUnitPrice(p.getContractUnitPrice())
                    .availableQty(p.getAvailableQty() == null ? 0L : p.getAvailableQty())
                    .build();
        }
    }

    @Schema(description = "카탈로그 facet 응답 — 현재 조건 안에서 칩 필터에 노출할 색상·사이즈 distinct")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class FacetsRes {
        @Schema(description = "노출 가능한 색상 코드 목록", example = "[\"BLK\",\"WHT\",\"NVY\",\"GRY\"]")
        private List<String> colors;
        @Schema(description = "노출 가능한 사이즈 목록", example = "[\"S\",\"M\",\"L\",\"XL\"]")
        private List<String> sizes;
    }
}
