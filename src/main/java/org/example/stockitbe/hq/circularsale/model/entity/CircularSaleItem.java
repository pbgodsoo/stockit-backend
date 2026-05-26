package org.example.stockitbe.hq.circularsale.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "circular_sale_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_circular_sale_item_header_inventory", columnNames = {"sale_header_id", "inventory_id"})
        },
        indexes = {
                @Index(name = "idx_circular_sale_item_header", columnList = "sale_header_id"),
                @Index(name = "idx_circular_sale_item_sku", columnList = "sku_id"),
                @Index(name = "idx_circular_sale_item_inventory", columnList = "inventory_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CircularSaleItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_header_id", nullable = false)
    private Long saleHeaderId;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "sku_code", nullable = false, length = 50)
    private String skuCode;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "main_category", length = 100)
    private String mainCategory;

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "size", length = 50)
    private String size;

    @Column(name = "material_type", nullable = false, length = 50)
    private String materialType;

    @Column(name = "requested_weight_kg", nullable = false, precision = 14, scale = 3, columnDefinition = "DECIMAL(14,3)")
    private BigDecimal requestedWeightKg;

    @Column(name = "actual_weight_kg", nullable = false, precision = 14, scale = 3, columnDefinition = "DECIMAL(14,3)")
    private BigDecimal actualWeightKg;

    @Column(name = "estimated_quantity", nullable = false, precision = 14, scale = 3, columnDefinition = "DECIMAL(14,3)")
    private BigDecimal estimatedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity;

    @Column(name = "stock_quantity_snapshot", nullable = false)
    private Integer stockQuantitySnapshot;

    @Column(name = "stock_weight_kg_snapshot", nullable = false, precision = 14, scale = 3, columnDefinition = "DECIMAL(14,3)")
    private BigDecimal stockWeightKgSnapshot;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "line_amount", nullable = false)
    private Long lineAmount;

    @Column(name = "memo", length = 500)
    private String memo;

    @Builder
    private CircularSaleItem(Long saleHeaderId, Long inventoryId, Long skuId, String skuCode, String productCode,
                             String productName, String mainCategory, String subCategory, String color, String size,
                             String materialType, BigDecimal requestedWeightKg, BigDecimal actualWeightKg,
                             BigDecimal estimatedQuantity, Integer soldQuantity, Integer stockQuantitySnapshot,
                             BigDecimal stockWeightKgSnapshot, Long unitPrice, Long lineAmount,
                             String memo) {
        this.saleHeaderId = saleHeaderId;
        this.inventoryId = inventoryId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.productCode = productCode;
        this.productName = productName;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.color = color;
        this.size = size;
        this.materialType = materialType;
        this.requestedWeightKg = requestedWeightKg == null ? BigDecimal.ZERO : requestedWeightKg;
        this.actualWeightKg = actualWeightKg == null ? BigDecimal.ZERO : actualWeightKg;
        this.estimatedQuantity = estimatedQuantity == null ? BigDecimal.ZERO : estimatedQuantity;
        this.soldQuantity = soldQuantity == null ? 0 : soldQuantity;
        this.stockQuantitySnapshot = stockQuantitySnapshot == null ? 0 : stockQuantitySnapshot;
        this.stockWeightKgSnapshot = stockWeightKgSnapshot == null ? BigDecimal.ZERO : stockWeightKgSnapshot;
        this.unitPrice = unitPrice == null ? 0L : unitPrice;
        this.lineAmount = lineAmount == null ? 0L : lineAmount;
        this.memo = memo;
    }
}

