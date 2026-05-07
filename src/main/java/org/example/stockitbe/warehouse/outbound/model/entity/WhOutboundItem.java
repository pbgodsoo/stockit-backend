package org.example.stockitbe.warehouse.outbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "wh_outbound_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wh_outbound_item_header_sku", columnNames = {"outbound_header_id", "sku_id"})
        },
        indexes = {
                @Index(name = "idx_wh_outbound_item_header", columnList = "outbound_header_id"),
                @Index(name = "idx_wh_outbound_item_sku", columnList = "sku_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhOutboundItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbound_header_id", nullable = false)
    private Long outboundHeaderId;

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

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "memo", length = 500)
    private String memo;

    @Builder
    private WhOutboundItem(Long outboundHeaderId, Long sourceLineRefId, Long skuId, String skuCode, String productCode,
                           String productName, String mainCategory, String subCategory, String color, String size,
                           Long unitPrice, Integer requestedQuantity, String memo) {
        this.outboundHeaderId = outboundHeaderId;
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
        this.requestedQuantity = requestedQuantity == null ? 0 : requestedQuantity;
        this.memo = memo;
    }
}
