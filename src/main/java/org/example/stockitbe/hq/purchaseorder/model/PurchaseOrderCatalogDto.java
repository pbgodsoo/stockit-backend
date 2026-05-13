package org.example.stockitbe.hq.purchaseorder.model;

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

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SkuRowRes {
        private String vendorCode;
        private String vendorName;
        private String vendorProductCode;
        private String productCode;
        private String productName;
        private String skuCode;
        private String color;
        private String size;
        private String displayOption;
        private Long unitPrice;
        private Long contractUnitPrice;
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

    @Getter
    @AllArgsConstructor
    @Builder
    public static class FacetsRes {
        private List<String> colors;
        private List<String> sizes;
    }
}
