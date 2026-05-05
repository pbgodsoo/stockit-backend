package org.example.stockitbe.hq.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "circular_material_price_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CircularMaterialPricePolicy extends BaseEntity {

    @Id
    @Column(name = "material_code", nullable = false, length = 32)
    private String materialCode;

    @Column(name = "material_name_ko", nullable = false, length = 32)
    private String materialNameKo;

    @Column(name = "material_group", nullable = false, length = 32)
    private String materialGroup;

    @Column(name = "price_per_kg", nullable = false)
    private Integer pricePerKg;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public void updatePrice(Integer nextPricePerKg) {
        this.pricePerKg = nextPricePerKg == null ? 0 : Math.max(0, nextPricePerKg);
    }
}
