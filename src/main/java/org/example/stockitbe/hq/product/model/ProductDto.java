package org.example.stockitbe.hq.product.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;
import java.util.List;

public class ProductDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductMasterUpsertReq {
        @NotBlank private String name;
        @NotBlank private String categoryCode;
        @NotNull @Min(0) private Long basePrice;
        @NotNull @Min(0) private Integer leadTimeDays;
        @NotBlank private String mainVendorCode;
        @NotNull private ProductStatus status;

        public ProductMaster toEntity(String code) {
            return ProductMaster.builder()
                    .code(code)
                    .name(name.trim())
                    .categoryCode(categoryCode.trim())
                    .basePrice(basePrice)
                    .leadTimeDays(leadTimeDays)
                    .mainVendorCode(mainVendorCode.trim())
                    .status(status)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuUpsertReq {
        @NotBlank private String optionName;
        @NotBlank private String optionValue;
        @NotNull @Min(0) private Long unitPrice;
        @NotNull private ProductStatus status;

        public ProductSku toEntity(String skuCode, String productCode) {
            return ProductSku.builder()
                    .skuCode(skuCode)
                    .productCode(productCode)
                    .optionName(optionName.trim())
                    .optionValue(optionValue.trim())
                    .unitPrice(unitPrice)
                    .status(status)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuBulkCreateReq {
        @NotBlank private String optionName;
        @NotNull private List<String> optionValues;
        @NotNull @Min(0) private Long unitPrice;
        @NotNull private ProductStatus status;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuPriceBulkUpdateReq {
        @NotNull @Min(0) private Long unitPrice;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuStatusBulkUpdateReq {
        @NotNull private ProductStatus status;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductMasterRes {
        private String code;
        private String name;
        private String categoryCode;
        private Long basePrice;
        private Integer leadTimeDays;
        private String mainVendorCode;
        private ProductStatus status;
        private long skuCount;
        private Date updatedAt;

        public static ProductMasterRes from(ProductMaster m, long skuCount) {
            return ProductMasterRes.builder()
                    .code(m.getCode())
                    .name(m.getName())
                    .categoryCode(m.getCategoryCode())
                    .basePrice(m.getBasePrice())
                    .leadTimeDays(m.getLeadTimeDays())
                    .mainVendorCode(m.getMainVendorCode())
                    .status(m.getStatus())
                    .skuCount(skuCount)
                    .updatedAt(m.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuRes {
        private String skuCode;
        private String productCode;
        private String optionName;
        private String optionValue;
        private Long unitPrice;
        private ProductStatus status;
        private Date updatedAt;

        public static ProductSkuRes from(ProductSku sku) {
            return ProductSkuRes.builder()
                    .skuCode(sku.getSkuCode())
                    .productCode(sku.getProductCode())
                    .optionName(sku.getOptionName())
                    .optionValue(sku.getOptionValue())
                    .unitPrice(sku.getUnitPrice())
                    .status(sku.getStatus())
                    .updatedAt(sku.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuBulkCreateRes {
        private String productCode;
        private int requestedCount;
        private int createdCount;
        private int skippedCount;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuPriceBulkUpdateRes {
        private String productCode;
        private long updatedCount;
        private Long unitPrice;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuStatusBulkUpdateRes {
        private String productCode;
        private long updatedCount;
        private ProductStatus status;
    }
}
