package org.example.stockitbe.store.order.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "store_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_header_id", nullable = false)
    private Long orderHeaderId;

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

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Builder
    private StoreOrderItem(Long orderHeaderId, Long skuId, String skuCode,
                           String productCode, String productName,
                           String mainCategory, String subCategory,
                           String color, String size, Long unitPrice, Integer requestedQuantity) {
        this.orderHeaderId = orderHeaderId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.productCode = productCode;
        this.productName = productName;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.color = color;
        this.size = size;
        this.unitPrice = unitPrice;
        this.requestedQuantity = requestedQuantity;
    }
}
