package org.example.stockitbe.hq.purchaseorder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 새 발주 페이지 카탈로그 응답 DTO.
 *
 * 카탈로그 한 행 = 한 SKU 표시를 위해 BE 가 마스터 + SKU 묶음 + optionFacets 를 한 번에 내려준다.
 * vendor_product 도메인 오염 회피를 위해 발주 도메인 안에 신설.
 *
 * stock 관련 필드(available/safety)는 인벤토리 도메인 합류 후 SkuRes 에 nullable 추가 예정 — 본 사이클은 제외.
 */
public class PurchaseOrderCatalogDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CatalogRes {
        private List<MasterRes> masters;
        private List<FacetRes> optionFacets;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class MasterRes {
        private String vendorCode;
        private String vendorName;
        private String vendorProductCode;   // VendorProduct.code (한 마스터의 식별자)
        private String productCode;          // ProductMaster.code
        private String productName;
        private Long contractUnitPrice;      // VendorProduct.unitPrice (계약 기본 단가, SKU 단가 fallback)
        private Long minSkuUnitPrice;        // 그룹 안 SKU 단가 최소
        private Long maxSkuUnitPrice;        // 그룹 안 SKU 단가 최대 — 범위 표시 (₩6,800~7,200)
        private List<SkuRes> skus;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SkuRes {
        private String skuCode;
        private String optionName;           // 슬래시 합성 ("색상/사이즈")
        private String optionValue;          // 슬래시 합성 ("화이트/L")
        private Long unitPrice;              // SKU 단가 (sku.unit_price 우선)
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class FacetRes {
        private String name;                 // 단일 axis name. 슬래시 합성 axis 는 분해해서 각각 facet
        private List<String> values;         // 자연 정렬
    }
}
