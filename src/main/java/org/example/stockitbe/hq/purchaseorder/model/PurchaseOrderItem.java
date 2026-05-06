package org.example.stockitbe.hq.purchaseorder.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "vendor_product_id", nullable = false)
    private Long vendorProductId;

    // 마스터 제품 logical reference (시점 복사)
    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 256)
    private String productName;

    // SKU 식별자 + 옵션 스냅샷 (결합 차단 4패턴 #1 — String ID + 시점 복사, @ManyToOne ProductSku 미사용)
    @Column(name = "sku_code", nullable = false, length = 32)
    private String skuCode;

    @Column(name = "color", nullable = false, length = 32)
    private String color;

    @Column(name = "size", nullable = false, length = 16)
    private String size;

    // 발주 시점 단가 복사 — sku.unitPrice 우선 (옵션별 차등 가능)
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false)
    private Long subtotal;

    @Builder
    private PurchaseOrderItem(Long purchaseOrderId, Long vendorProductId,
                              String productCode, String productName,
                              String skuCode, String color, String size,
                              Long unitPrice, Integer quantity) {
        this.purchaseOrderId = purchaseOrderId;
        this.vendorProductId = vendorProductId;
        this.productCode = productCode;
        this.productName = productName;
        this.skuCode = skuCode;
        this.color = color;
        this.size = size;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice * quantity;
    }

    public void linkToPurchaseOrder(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }
}
