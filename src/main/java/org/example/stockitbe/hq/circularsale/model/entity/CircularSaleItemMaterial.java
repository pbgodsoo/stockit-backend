package org.example.stockitbe.hq.circularsale.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "circular_sale_item_material",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_circular_sale_item_material_unique", columnNames = {"sale_item_id", "material_code"})
        },
        indexes = {
                @Index(name = "idx_circular_sale_item_material_item", columnList = "sale_item_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CircularSaleItemMaterial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_item_id", nullable = false)
    private Long saleItemId;

    @Column(name = "material_code", nullable = false, length = 32)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "ratio", nullable = false)
    private Integer ratio;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    private CircularSaleItemMaterial(Long saleItemId, String materialCode, String materialName, Integer ratio, Integer sortOrder) {
        this.saleItemId = saleItemId;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.ratio = ratio == null ? 0 : ratio;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }
}

