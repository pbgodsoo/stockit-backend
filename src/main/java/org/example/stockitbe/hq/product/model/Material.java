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

    // ADR-021 추천 임베딩 input 풍부화 — 소재별 자연어 설명. 시드 + 추천 시점 product → composition → material join 으로 활용.
    @Column(name = "description", length = 500)
    private String description;

    @Builder
    private Material(String code, String nameKo, String materialGroup,
                     BigDecimal carbonFactor, Boolean active, String description) {
        this.code = code;
        this.nameKo = nameKo;
        this.materialGroup = materialGroup;
        this.carbonFactor = carbonFactor;
        this.active = active == null ? Boolean.TRUE : active;
        this.description = description;
    }
}
