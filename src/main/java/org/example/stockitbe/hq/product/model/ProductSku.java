package org.example.stockitbe.hq.product.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "product_sku", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_sku_code", columnNames = "sku_code"),
        @UniqueConstraint(name = "uk_product_sku_product_color_size", columnNames = {"product_code", "color", "size"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSku extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", nullable = false, length = 32)
    private String skuCode;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "color", nullable = false, length = 32)
    private String color;

    @Column(name = "size", nullable = false, length = 16)
    private String size;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProductStatus status;

    @Builder
    private ProductSku(String skuCode, String productCode, String color, String size,
                       Long unitPrice, ProductStatus status) {
        this.skuCode = skuCode;
        this.productCode = productCode;
        this.color = color;
        this.size = size;
        this.unitPrice = unitPrice;
        this.status = status == null ? ProductStatus.ACTIVE : status;
    }

    public void update(String color, String size, Long unitPrice, ProductStatus status) {
        this.color = color;
        this.size = size;
        this.unitPrice = unitPrice;
        this.status = status;
    }
}
