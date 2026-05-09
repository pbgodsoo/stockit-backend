package org.example.stockitbe.hq.product.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "material", uniqueConstraints = {
        @UniqueConstraint(name = "uk_material_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Material extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name_ko", nullable = false, length = 32)
    private String nameKo;

    @Column(name = "material_group", nullable = false, length = 32)
    private String materialGroup;

    @Column(name = "carbon_factor", nullable = false, precision = 5, scale = 3)
    private BigDecimal carbonFactor;   // tCO₂/kg

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Builder
    private Material(String code, String nameKo, String materialGroup, Boolean active) {
        this.code = code;
        this.nameKo = nameKo;
        this.materialGroup = materialGroup;
        this.carbonFactor = carbonFactor;
        this.active = active == null ? Boolean.TRUE : active;
    }
}
