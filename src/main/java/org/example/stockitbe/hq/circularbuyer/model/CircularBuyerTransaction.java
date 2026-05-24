package org.example.stockitbe.hq.circularbuyer.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 순환재고 거래 이력 (1 거래 = 1 row).
 * material_code 는 circular_material_price_policy.material_code 와 매칭 (soft reference).
 *  - JOIN 으로 material_name_ko / material_group / price_per_kg 마스터값 조회.
 *  - unit_price 는 거래 시점 스냅샷 (정책 단가 변동 영향 차단).
 */
@Entity
@Table(name = "circular_buyer_transaction", indexes = {
        @Index(name = "idx_cbt_buyer", columnList = "buyer_id"),
        @Index(name = "idx_cbt_material", columnList = "material_code"),
        @Index(name = "idx_cbt_transacted_at", columnList = "transacted_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CircularBuyerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private CircularBuyer buyer;

    @Column(name = "material_code", nullable = false, length = 32)
    private String materialCode;

    @Column(name = "weight_kg", nullable = false)
    private Integer weightKg;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;          // 거래 시점 단가 스냅샷 (원/kg)

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;           // weight_kg × unit_price (원)

    @Column(name = "transacted_at", nullable = false)
    private LocalDateTime transactedAt;

    // Phase 2: 혼방 거래의 70% 주 소재 코드. 단일 거래는 NULL.
    // ScoreEventsService 가 effective factor 계산 시 materialCode='BLEND' + mainMaterialCode 존재
    // → (material.carbon_factor[mainMaterialCode]) × mainMaterialRatio 로 가중 적용.
    @Column(name = "main_material_code", length = 32)
    private String mainMaterialCode;

    // 주 소재 비율 (예: 0.70). 단일 거래는 NULL.
    @Column(name = "main_material_ratio", precision = 3, scale = 2)
    private BigDecimal mainMaterialRatio;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @Builder
    private CircularBuyerTransaction(CircularBuyer buyer, String materialCode,
                                      Integer weightKg, Integer unitPrice,
                                      Long totalAmount, LocalDateTime transactedAt,
                                      String mainMaterialCode, BigDecimal mainMaterialRatio) {
        this.buyer = buyer;
        this.materialCode = materialCode;
        this.weightKg = weightKg;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.transactedAt = transactedAt;
        this.mainMaterialCode = mainMaterialCode;
        this.mainMaterialRatio = mainMaterialRatio;
        this.createDate = LocalDateTime.now();
    }
}
