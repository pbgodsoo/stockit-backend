package org.example.stockitbe.warehouse.inbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "wh_inbound_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhInboundItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // header FK Long ID (결합 차단 4패턴 #1 — @ManyToOne 안 박음, PurchaseOrderItem 패턴 일관)
    @Column(name = "inbound_header_id", nullable = false)
    private Long inboundHeaderId;

    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 256)
    private String productName;

    @Column(name = "sku_code", nullable = false, length = 32)
    private String skuCode;

    @Column(name = "color", length = 32)
    private String color;

    @Column(name = "size", length = 16)
    private String size;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // 발주 입고만 채움. 이동 입고는 null.
    @Column(name = "unit_price")
    private Long unitPrice;

    @Column(name = "subtotal")
    private Long subtotal;

    @Builder
    private WhInboundItem(Long inboundHeaderId, String productCode, String productName,
                          String skuCode, String color, String size,
                          Integer quantity, Long unitPrice, Long subtotal) {
        this.inboundHeaderId = inboundHeaderId;
        this.productCode = productCode;
        this.productName = productName;
        this.skuCode = skuCode;
        this.color = color;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public void linkToHeader(Long inboundHeaderId) {
        this.inboundHeaderId = inboundHeaderId;
    }
}
