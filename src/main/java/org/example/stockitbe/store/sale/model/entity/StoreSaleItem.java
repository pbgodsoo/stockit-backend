package org.example.stockitbe.store.sale.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "store_sale_item", indexes = {
        @Index(name = "idx_store_sale_item_sale_header_id", columnList = "sale_header_id"),
        @Index(name = "idx_store_sale_item_sku_id", columnList = "sku_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreSaleItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_header_id", nullable = false)
    private Long saleHeaderId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "sku_code", nullable = false, length = 32)
    private String skuCode;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "main_category", nullable = false, length = 64)
    private String mainCategory;

    @Column(name = "sub_category", nullable = false, length = 64)
    private String subCategory;

    @Column(name = "color", nullable = false, length = 32)
    private String color;

    @Column(name = "size", nullable = false, length = 16)
    private String size;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "line_amount", nullable = false)
    private Long lineAmount;

    @Builder
    private StoreSaleItem(Long saleHeaderId, Long skuId, String skuCode, String productCode, String productName,
                          String mainCategory, String subCategory, String color, String size,
                          Integer quantity, Long unitPrice, Long lineAmount) {
        this.saleHeaderId = saleHeaderId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.productCode = productCode;
        this.productName = productName;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.color = color;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineAmount = lineAmount;
    }
}

