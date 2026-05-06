package org.example.stockitbe.hq.product.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "product_material_composition", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_material_composition", columnNames = {"product_id", "material_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMaterialComposition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductMaster productMaster;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "ratio", nullable = false)
    private Integer ratio;

    @Column(name = "composition_order", nullable = false)
    private Integer compositionOrder;

    public ProductMaterialComposition(Material material, Integer ratio, Integer compositionOrder) {
        this.material = material;
        this.ratio = ratio;
        this.compositionOrder = compositionOrder;
    }

    void assignProductMaster(ProductMaster productMaster) {
        this.productMaster = productMaster;
    }
}
