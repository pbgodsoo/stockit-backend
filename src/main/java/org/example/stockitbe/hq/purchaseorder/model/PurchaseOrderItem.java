package org.example.stockitbe.hq.purchaseorder.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.hq.vendor.model.VendorProduct;

@Entity
@Table(name = "purchase_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 부모-자식 컴포지션 — 라이프사이클 동일, cascade 자동화 대상.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    // 외부 도메인 — JPA 정석 매핑.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_product_id", nullable = false)
    private VendorProduct vendorProduct;

    // 마스터 제품 자연 키 + 시점 박제 — ProductMaster.productCode (String 자연 키, 매핑 안 박음).
    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 256)
    private String productName;

    // SKU 자연 키 + 옵션 시점 박제 — ProductSku.skuCode (String 자연 키, 매핑 안 박음).
    @Column(name = "sku_code", nullable = false, length = 32)
    private String skuCode;

    @Column(name = "color", nullable = false, length = 32)
    private String color;

    @Column(name = "size", nullable = false, length = 16)
    private String size;

    // 발주 시점 단가 박제 — sku.unitPrice 우선 (옵션별 차등 가능)
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false)
    private Long subtotal;

    @Builder
    private PurchaseOrderItem(PurchaseOrder purchaseOrder, VendorProduct vendorProduct,
                              String productCode, String productName,
                              String skuCode, String color, String size,
                              Long unitPrice, Integer quantity) {
        this.purchaseOrder = purchaseOrder;
        this.vendorProduct = vendorProduct;
        this.productCode = productCode;
        this.productName = productName;
        this.skuCode = skuCode;
        this.color = color;
        this.size = size;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice * quantity;
    }

    void linkToParent(PurchaseOrder parent) {
        this.purchaseOrder = parent;
    }
}
