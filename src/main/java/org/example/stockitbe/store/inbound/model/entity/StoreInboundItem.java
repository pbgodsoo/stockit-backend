package org.example.stockitbe.store.inbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "store_inbound_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_inbound_item_header_sku", columnNames = {"inbound_header_id", "sku_id"})
        },
        indexes = {
                @Index(name = "idx_store_inbound_item_header", columnList = "inbound_header_id"),
                @Index(name = "idx_store_inbound_item_sku", columnList = "sku_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreInboundItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_header_id", nullable = false)
    private Long inboundHeaderId;

    @Column(name = "outbound_item_id")
    private Long outboundItemId;

    @Column(name = "source_line_ref_id")
    private Long sourceLineRefId;

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

    @Column(name = "unit_price")
    private Long unitPrice;

    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    @Column(name = "memo", length = 500)
    private String memo;

    @Builder
    private StoreInboundItem(Long inboundHeaderId, Long outboundItemId, Long sourceLineRefId, Long skuId,
                             String skuCode, String productCode, String productName, String mainCategory,
                             String subCategory, String color, String size, Long unitPrice,
                             Integer expectedQuantity, String memo) {
        this.inboundHeaderId = inboundHeaderId;
        this.outboundItemId = outboundItemId;
        this.sourceLineRefId = sourceLineRefId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.productCode = productCode;
        this.productName = productName;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.color = color;
        this.size = size;
        this.unitPrice = unitPrice;
        this.expectedQuantity = expectedQuantity == null ? 0 : expectedQuantity;
        this.memo = memo;
    }
}

