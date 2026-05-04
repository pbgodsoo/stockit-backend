package org.example.stockitbe.store.sale.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class StoreSaleDto {

    // 판매 요청 DTO
    // 매장 코드와 판매 SKU 라인 목록을 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaleReq {
        @NotBlank
        private String storeCode;
        @Valid
        @NotEmpty
        private List<SaleLineReq> items;


        public StoreSaleHeader toEntity(SaleHeaderContext context) {
            return StoreSaleHeader.builder()
                    .saleNo(context.getTemporarySaleNo())
                    .storeId(context.getStoreId())
                    .soldAt(context.getSoldAt())
                    .totalQuantity(context.getTotalQuantity())
                    .totalAmount(context.getTotalAmount())
                    .status(StoreSaleStatus.COMPLETED)
                    .build();
        }
    }

    // 판매 응답 DTO
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleRes {
        private String saleNo;
        private String storeCode;
        private Date soldAt;
        private Integer totalQuantity;
        private Long totalAmount;
        private List<SaleLineRes> items;

        public static SaleRes from(StoreSaleHeader header, String storeCode, List<SaleLineRes> items) {
            return SaleRes.builder()
                    .saleNo(header.getSaleNo())
                    .storeCode(storeCode)
                    .soldAt(header.getSoldAt())
                    .totalQuantity(header.getTotalQuantity())
                    .totalAmount(header.getTotalAmount())
                    .items(items)
                    .build();
        }
    }

    // 판매 목록 응답 DTO
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleListRes {
        private String saleNo;
        private String storeCode;
        private Date soldAt;
        private Integer totalQuantity;
        private Long totalAmount;
        private String headline;

        public static SaleListRes from(StoreSaleHeader header, String storeCode, String headline) {
            return SaleListRes.builder()
                    .saleNo(header.getSaleNo())
                    .storeCode(storeCode)
                    .soldAt(header.getSoldAt())
                    .totalQuantity(header.getTotalQuantity())
                    .totalAmount(header.getTotalAmount())
                    .headline(headline)
                    .build();
        }
    }

    // 판매 상세 응답 DTO
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleDetailRes {
        private String saleNo;
        private String storeCode;
        private Date soldAt;
        private Integer totalQuantity;
        private Long totalAmount;
        private StoreSaleStatus status;
        private List<SaleLineRes> items;

        public static SaleDetailRes from(StoreSaleHeader header, String storeCode, List<SaleLineRes> items) {
            return SaleDetailRes.builder()
                    .saleNo(header.getSaleNo())
                    .storeCode(storeCode)
                    .soldAt(header.getSoldAt())
                    .totalQuantity(header.getTotalQuantity())
                    .totalAmount(header.getTotalAmount())
                    .status(header.getStatus())
                    .items(items)
                    .build();
        }
    }

    // 판매 SKU 요청 DTO
    // SKU 코드와 판매 수량을 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaleLineReq {
        @NotBlank
        private String skuCode;
        @Min(1)
        private Integer quantity;

        public StoreSaleItem toEntity(SaleLineContext context) {
            int safeQuantity = Objects.requireNonNull(this.quantity);
            long lineAmount = context.getUnitPrice() * safeQuantity;
            return StoreSaleItem.builder()
                    .saleHeaderId(context.getSaleHeaderId())
                    .skuId(context.getSkuId())
                    .skuCode(this.skuCode)
                    .productCode(context.getProductCode())
                    .productName(context.getProductName())
                    .mainCategory(context.getMainCategory())
                    .subCategory(context.getSubCategory())
                    .color(context.getColor())
                    .size(context.getSize())
                    .quantity(safeQuantity)
                    .unitPrice(context.getUnitPrice())
                    .lineAmount(lineAmount)
                    .build();
        }
    }

    // 판매 SKU 응답 DTO
    // SKU/상품/옵션/금액을 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleLineRes {
        private String skuCode;
        private String productCode;
        private String productName;
        private String mainCategory;
        private String subCategory;
        private String color;
        private String size;
        private Integer quantity;
        private Long unitPrice;
        private Long lineAmount;

        public static SaleLineRes from(StoreSaleItem item) {
            return SaleLineRes.builder()
                    .skuCode(item.getSkuCode())
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .mainCategory(item.getMainCategory())
                    .subCategory(item.getSubCategory())
                    .color(item.getColor())
                    .size(item.getSize())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineAmount(item.getLineAmount())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleHeaderContext {
        private Long storeId;
        private Date soldAt;
        private Integer totalQuantity;
        private Long totalAmount;
        private String temporarySaleNo;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleLineContext {
        private Long saleHeaderId;
        private Long skuId;
        private String productCode;
        private String productName;
        private String mainCategory;
        private String subCategory;
        private String color;
        private String size;
        private Long unitPrice;
    }
}
