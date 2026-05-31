package org.example.stockitbe.hq.circularbuyer.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
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

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "sale_type", nullable = false, length = 20)
    private String saleType;   // "SALE" | "DONATION"

    @Column(name = "donee_name", length = 200)
    private String doneeName;

    @Builder
    private CircularBuyerTransaction(CircularBuyer buyer, String materialCode,
                                      Integer weightKg, Integer unitPrice,
                                      Long totalAmount, LocalDateTime transactedAt,
                                      String saleType, String doneeName) {
        this.buyer = buyer;
        this.materialCode = materialCode;
        this.weightKg = weightKg;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.transactedAt = transactedAt;
        this.saleType = saleType;
        this.doneeName = doneeName;
        this.createDate = LocalDateTime.now();
    }
}
