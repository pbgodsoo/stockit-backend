package org.example.stockitbe.hq.product.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;
import java.util.List;

public class ProductDto {

    @Schema(description = "제품 마스터 등록/수정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductMasterUpsertReq {
        @Schema(description = "제품명", example = "폴리에스터 자켓", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String name;
        @Schema(description = "하위 카테고리 코드", example = "CAT-L2-OUT-JK", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String categoryCode;
        @Schema(description = "기준 가격", example = "59000", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(0) private Long basePrice;
        @Schema(description = "리드타임(일)", example = "7", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(0) private Integer leadTimeDays;
        @Schema(description = "창고 안전재고", example = "100", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(0) private Integer warehouseSafetyStock;
        @Schema(description = "매장 안전재고", example = "20", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(0) private Integer storeSafetyStock;
        @Schema(description = "메인 공급처 코드", example = "VEN-00001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String mainVendorCode;
        @Schema(description = "소재 타입. 허용값: NATURAL_SINGLE, SYNTHETIC, BLEND", example = "SYNTHETIC", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull private ProductMaterialType materialType;
        @Schema(description = "소재 구성 목록. 비율 합계는 100이어야 한다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull private List<ProductMaterialCompositionReq> materialCompositions;
        @Schema(description = "제품 상태. 허용값: ACTIVE, INACTIVE, SUSPENDED", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
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
                    .status(status)
                    .build();
        }
    }

    @Schema(description = "제품 소재 구성 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductMaterialCompositionReq {
        @Schema(description = "소재 코드", example = "POLYESTER", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String materialCode;
        @Schema(description = "소재 구성 비율", example = "100", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(0) private Integer ratio;
    }

    @Schema(description = "제품 SKU 등록/수정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuUpsertReq {
        @Schema(description = "색상 코드. 허용값: BLK, WHT, NVY, GRY", example = "BLK", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String color;
        @Schema(description = "사이즈 코드. 허용값: XS, S, M, L, XL", example = "M", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String size;
        @Schema(description = "SKU 단가", example = "59000", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(0) private Long unitPrice;
        @Schema(description = "SKU 상태. 허용값: ACTIVE, INACTIVE, SUSPENDED", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "제품 SKU 벌크 등록 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuBulkCreateReq {
        @Schema(description = "색상 코드 목록. 허용값: BLK, WHT, NVY, GRY", example = "[\"BLK\", \"WHT\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull private List<String> colors;
        @Schema(description = "사이즈 코드 목록. 허용값: XS, S, M, L, XL", example = "[\"M\", \"L\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull private List<String> sizes;
        @Schema(description = "SKU 단가", example = "59000", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(0) private Long unitPrice;
        @Schema(description = "SKU 상태. 허용값: ACTIVE, INACTIVE, SUSPENDED", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull private ProductStatus status;
    }

    @Schema(description = "제품 SKU 전체 가격 수정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuPriceBulkUpdateReq {
        @Schema(description = "변경할 SKU 단가", example = "62000", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(0) private Long unitPrice;
    }

    @Schema(description = "제품 SKU 전체 상태 수정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSkuStatusBulkUpdateReq {
        @Schema(description = "변경할 SKU 상태. 허용값: ACTIVE, INACTIVE, SUSPENDED", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull private ProductStatus status;
    }

    @Schema(description = "제품 마스터 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductMasterRes {
        @Schema(description = "제품 코드", example = "PRD-OUT-JK-001")
        private String code;
        @Schema(description = "제품명", example = "폴리에스터 자켓")
        private String name;
        @Schema(description = "카테고리 코드", example = "CAT-L2-OUT-JK")
        private String categoryCode;
        @Schema(description = "기준 가격", example = "59000")
        private Long basePrice;
        @Schema(description = "리드타임(일)", example = "7")
        private Integer leadTimeDays;
        @Schema(description = "창고 안전재고", example = "100")
        private Integer warehouseSafetyStock;
        @Schema(description = "매장 안전재고", example = "20")
        private Integer storeSafetyStock;
        @Schema(description = "메인 공급처 코드", example = "VEN-00001")
        private String mainVendorCode;
        @Schema(description = "소재 타입", example = "SYNTHETIC")
        private ProductMaterialType materialType;
        @Schema(description = "소재 구성 목록")
        private List<ProductMaterialCompositionRes> materialCompositions;
        @Schema(description = "제품 상태", example = "ACTIVE")
        private ProductStatus status;
        @Schema(description = "하위 SKU 수", example = "4")
        private long skuCount;
        @Schema(description = "수정 일시")
        private Date updatedAt;

        public static ProductMasterRes from(ProductMaster m,
                                            long skuCount,
                                            ProductMaterialType materialType,
                                            java.util.Map<String, String> materialNameMap) {
            List<ProductMaterialCompositionRes> compositions = m.getMaterialCompositions() == null
                    ? List.of()
                    : m.getMaterialCompositions().stream()
                    .map(c -> {
                        String code = c.getMaterial() == null ? "" : c.getMaterial().getCode();
                        return ProductMaterialCompositionRes.builder()
                                .materialCode(code)
                                .materialNameKo(materialNameMap.getOrDefault(code, code))
                                .ratio(c.getRatio())
                                .build();
                    })
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
                    .materialType(materialType)
                    .materialCompositions(compositions)
                    .status(m.getStatus())
                    .skuCount(skuCount)
                    .updatedAt(m.getUpdatedAt())
                    .build();
        }
    }

    @Schema(description = "제품 소재 구성 응답")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductMaterialCompositionRes {
        @Schema(description = "소재 코드", example = "POLYESTER")
        private String materialCode;
        @Schema(description = "소재 한글명", example = "폴리에스터")
        private String materialNameKo;
        @Schema(description = "소재 구성 비율", example = "100")
        private Integer ratio;
    }

    @Schema(description = "제품 SKU 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuRes {
        @Schema(description = "SKU 코드", example = "PRD-OUT-JK-001-BLK-M")
        private String skuCode;
        @Schema(description = "제품 코드", example = "PRD-OUT-JK-001")
        private String productCode;
        @Schema(description = "색상 코드", example = "BLK")
        private String color;
        @Schema(description = "사이즈 코드", example = "M")
        private String size;
        @Schema(description = "표시 옵션", example = "BLK/M")
        private String displayOption;
        @Schema(description = "SKU 단가", example = "59000")
        private Long unitPrice;
        @Schema(description = "SKU 상태", example = "ACTIVE")
        private ProductStatus status;
        @Schema(description = "수정 일시")
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

    @Schema(description = "제품 SKU 벌크 등록 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuBulkCreateRes {
        @Schema(description = "제품 코드", example = "PRD-OUT-JK-001")
        private String productCode;
        @Schema(description = "요청 조합 수", example = "4")
        private int requestedCount;
        @Schema(description = "생성된 SKU 수", example = "3")
        private int createdCount;
        @Schema(description = "중복 등으로 건너뛴 SKU 수", example = "1")
        private int skippedCount;
    }

    @Schema(description = "제품 SKU 전체 가격 수정 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuPriceBulkUpdateRes {
        @Schema(description = "제품 코드", example = "PRD-OUT-JK-001")
        private String productCode;
        @Schema(description = "수정된 SKU 수", example = "4")
        private long updatedCount;
        @Schema(description = "변경된 단가", example = "62000")
        private Long unitPrice;
    }

    @Schema(description = "제품 SKU 전체 상태 수정 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductSkuStatusBulkUpdateRes {
        @Schema(description = "제품 코드", example = "PRD-OUT-JK-001")
        private String productCode;
        @Schema(description = "수정된 SKU 수", example = "4")
        private long updatedCount;
        @Schema(description = "변경된 SKU 상태", example = "ACTIVE")
        private ProductStatus status;
    }
}
