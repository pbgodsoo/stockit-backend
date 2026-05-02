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
        @NotNull @Min(0) private Integer warehouseSafetyStock;
        @NotNull @Min(0) private Integer storeSafetyStock;
        @NotBlank private String mainVendorCode;
        @NotNull private ProductMaterialType materialType;
        @NotNull private List<ProductMaterialCompositionReq> materialCompositions;
        @NotNull private ProductStatus status;

        public ProductMaster toEntity(String code) {
            return ProductMaster.builder()
                    .code(code)
                    .name(name.trim())
                    .categoryCode(categoryCode.trim())
                    .basePrice(basePrice)
                    .leadTimeDays(leadTimeDays)
                    .warehouseSafetyStock(warehouseSafetyStock)
                    .storeSafetyStock(storeSafetyStock)
                    .mainVendorCode(mainVendorCode.trim())
                    .materialSpec(toMaterialSpec())
                    .status(status)
                    .build();
        }

        public ProductMaterialSpec toMaterialSpec() {
            return ProductMaterialSpec.builder()
                    .materialType(materialType)
                    .compositions(materialCompositions.stream()
                            .map(ProductMaterialCompositionReq::toMaterialComposition)
                            .toList())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductMaterialCompositionReq {
        @NotBlank private String materialCode;
        @NotNull @Min(0) private Integer ratio;

        public ProductMaterialComposition toMaterialComposition() {
            return ProductMaterialComposition.builder()
                    .materialCode(materialCode.trim().toUpperCase())
                    .ratio(ratio)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuUpsertReq {
        @NotBlank private String color;
        @NotBlank private String size;
        @NotNull @Min(0) private Long unitPrice;
        @NotNull private ProductStatus status;

        public ProductSku toEntity(String skuCode, String productCode) {
            return ProductSku.builder()
                    .skuCode(skuCode)
                    .productCode(productCode)
                    .color(color.trim())
                    .size(size.trim())
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
        @NotNull private List<String> colors;
        @NotNull private List<String> sizes;
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
        private Integer warehouseSafetyStock;
        private Integer storeSafetyStock;
        private String mainVendorCode;
        private ProductMaterialType materialType;
        private List<ProductMaterialCompositionRes> materialCompositions;
        private ProductStatus status;
        private long skuCount;
        private Date updatedAt;

        public static ProductMasterRes from(ProductMaster m, long skuCount, java.util.Map<String, String> materialNameMap) {
            ProductMaterialSpec materialSpec = m.getMaterialSpec();
            List<ProductMaterialCompositionRes> compositions = materialSpec == null || materialSpec.getCompositions() == null
                    ? List.of()
                    : materialSpec.getCompositions().stream()
                    .map(c -> ProductMaterialCompositionRes.builder()
                            .materialCode(c.getMaterialCode())
                            .materialNameKo(materialNameMap.getOrDefault(c.getMaterialCode(), c.getMaterialCode()))
                            .ratio(c.getRatio())
                            .build())
                    .toList();
            return ProductMasterRes.builder()
                    .code(m.getCode())
                    .name(m.getName())
                    .categoryCode(m.getCategoryCode())
                    .basePrice(m.getBasePrice())
                    .leadTimeDays(m.getLeadTimeDays())
                    .warehouseSafetyStock(m.getWarehouseSafetyStock())
                    .storeSafetyStock(m.getStoreSafetyStock())
                    .mainVendorCode(m.getMainVendorCode())
                    .materialType(materialSpec == null ? null : materialSpec.getMaterialType())
                    .materialCompositions(compositions)
                    .status(m.getStatus())
                    .skuCount(skuCount)
                    .updatedAt(m.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductMaterialCompositionRes {
        private String materialCode;
        private String materialNameKo;
        private Integer ratio;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuRes {
        private String skuCode;
        private String productCode;
        private String color;
        private String size;
        private String displayOption;
        private Long unitPrice;
        private ProductStatus status;
        private Date updatedAt;

        public static ProductSkuRes from(ProductSku sku) {
            return ProductSkuRes.builder()
                    .skuCode(sku.getSkuCode())
                    .productCode(sku.getProductCode())
                    .color(sku.getColor())
                    .size(sku.getSize())
                    .displayOption(sku.getColor() + "/" + sku.getSize())
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
