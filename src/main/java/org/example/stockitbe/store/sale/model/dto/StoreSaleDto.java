package org.example.stockitbe.store.sale.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.store.sale.model.entity.StoreSaleHeader;
import org.example.stockitbe.store.sale.model.entity.StoreSaleItem;
import org.example.stockitbe.store.sale.model.StoreSaleStatus;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class StoreSaleDto {

    @Schema(description = "판매 생성 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaleReq {
        @Valid
        @NotEmpty
        @Schema(description = "판매 SKU 라인 목록")
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

    @Schema(description = "판매 생성 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleRes {
        @Schema(description = "판매번호", example = "SAL-20240101-001")
        private String saleNo;
        @Schema(description = "매장 코드", example = "STORE-001")
        private String storeCode;
        @Schema(description = "판매 일시")
        private Date soldAt;
        @Schema(description = "총 판매 수량", example = "5")
        private Integer totalQuantity;
        @Schema(description = "총 판매 금액 (원)", example = "75000")
        private Long totalAmount;
        @Schema(description = "판매 SKU 라인 목록")
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

    @Schema(description = "판매 목록 조회 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleListRes {
        @Schema(description = "판매번호", example = "SAL-20240101-001")
        private String saleNo;
        @Schema(description = "매장 코드", example = "STORE-001")
        private String storeCode;
        @Schema(description = "판매 일시")
        private Date soldAt;
        @Schema(description = "총 판매 수량", example = "5")
        private Integer totalQuantity;
        @Schema(description = "총 판매 금액 (원)", example = "75000")
        private Long totalAmount;
        @Schema(description = "판매 헤드라인 (첫 상품명 외 n건)", example = "반팔 티셔츠 외 2건")
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

    @Schema(description = "판매 상세 조회 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleDetailRes {
        @Schema(description = "판매번호", example = "SAL-20240101-001")
        private String saleNo;
        @Schema(description = "매장 코드", example = "STORE-001")
        private String storeCode;
        @Schema(description = "판매 일시")
        private Date soldAt;
        @Schema(description = "총 판매 수량", example = "5")
        private Integer totalQuantity;
        @Schema(description = "총 판매 금액 (원)", example = "75000")
        private Long totalAmount;
        @Schema(description = "판매 상태")
        private StoreSaleStatus status;
        @Schema(description = "판매 SKU 라인 목록")
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

    @Schema(description = "판매 SKU 라인 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaleLineReq {
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        @NotBlank
        private String skuCode;
        @Schema(description = "판매 수량", example = "2")
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

    @Schema(description = "판매 SKU 라인 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class SaleLineRes {
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        private String skuCode;
        @Schema(description = "상품 코드", example = "ITEM-001")
        private String productCode;
        @Schema(description = "상품명", example = "반팔 티셔츠")
        private String productName;
        @Schema(description = "대분류", example = "상의")
        private String mainCategory;
        @Schema(description = "소분류", example = "티셔츠")
        private String subCategory;
        @Schema(description = "색상", example = "레드")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "판매 수량", example = "2")
        private Integer quantity;
        @Schema(description = "단가 (원)", example = "15000")
        private Long unitPrice;
        @Schema(description = "라인 금액 (원)", example = "30000")
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
