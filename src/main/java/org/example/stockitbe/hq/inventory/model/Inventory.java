package org.example.stockitbe.hq.inventory.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.util.Date;

@Entity
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_sku_location_status", columnNames = {"sku_id", "location_id", "inventory_status"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false, length = 32)
    private InventoryStatus inventoryStatus;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "in_transit_quantity", nullable = false)
    private Integer inTransitQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "status_changed_at", nullable = false)
    private Date statusChangedAt;

    @Column(name = "last_movement_at", nullable = false)
    private Date lastMovementAt;

    @Builder
    private Inventory(Long skuId, Long locationId, InventoryStatus inventoryStatus, Integer quantity,
                      Integer reservedQuantity, Integer inTransitQuantity, Integer availableQuantity,
                      Date statusChangedAt, Date lastMovementAt) {
        this.skuId = skuId;
        this.locationId = locationId;
        this.inventoryStatus = inventoryStatus == null ? InventoryStatus.NORMAL : inventoryStatus;
        this.quantity = quantity == null ? 0 : quantity;
        this.reservedQuantity = reservedQuantity == null ? 0 : reservedQuantity;
        this.inTransitQuantity = inTransitQuantity == null ? 0 : inTransitQuantity;
        this.availableQuantity = availableQuantity == null ? 0 : availableQuantity;
        this.statusChangedAt = statusChangedAt == null ? new Date() : statusChangedAt;
        this.lastMovementAt = lastMovementAt == null ? new Date() : lastMovementAt;
    }

    public void markCircularCandidate(Date changedAt) {
        this.inventoryStatus = InventoryStatus.CIRCULAR_CANDIDATE;
        this.statusChangedAt = changedAt == null ? new Date() : changedAt;
    }

    public void markCircular(Date changedAt) {
        this.inventoryStatus = InventoryStatus.CIRCULAR;
        this.statusChangedAt = changedAt == null ? new Date() : changedAt;
    }

    public void decreaseForConversion(int quantityToMove) {
        int safeMove = Math.max(0, quantityToMove);
        int nextAvailable = Math.max(0, n(this.availableQuantity) - safeMove);
        int nextQuantity = Math.max(0, n(this.quantity) - safeMove);
        this.availableQuantity = nextAvailable;
        this.quantity = nextQuantity;
    }

    public void increaseForConversion(int quantityToMove) {
        int safeMove = Math.max(0, quantityToMove);
        this.availableQuantity = n(this.availableQuantity) + safeMove;
        this.quantity = n(this.quantity) + safeMove;
    }

    public void absorbAsCircularCandidate(Inventory source, Date changedAt) {
        if (source == null) return;
        this.quantity = n(this.quantity) + n(source.getQuantity());
        this.availableQuantity = n(this.availableQuantity) + n(source.getAvailableQuantity());
        this.reservedQuantity = n(this.reservedQuantity) + n(source.getReservedQuantity());
        this.inTransitQuantity = n(this.inTransitQuantity) + n(source.getInTransitQuantity());
        this.statusChangedAt = changedAt == null ? new Date() : changedAt;
        this.lastMovementAt = maxDate(this.lastMovementAt, source.getLastMovementAt());
    }

    public boolean isEmptyStock() {
        return n(this.quantity) <= 0 && n(this.availableQuantity) <= 0;
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private Date maxDate(Date a, Date b) {
        if (a == null) return b == null ? new Date() : b;
        if (b == null) return a;
        return a.after(b) ? a : b;
    }

    /**
     * 발주 SHIPPING 진입 시 가용재고 증가 (이슈 #169 — 발주 ↔ 인벤토리 연결 룰).
     * 도착 전 예약 가용재고로, 실재고(quantity) 는 변하지 않는다.
     */
    public void increaseAvailable(int delta) {
        this.availableQuantity = this.availableQuantity + delta;
        this.lastMovementAt = new Date();
    }

    /**
     * 발주 COMPLETED (입고 확정) 시 가용재고를 실재고로 이동.
     * available_quantity -= delta, quantity += delta.
     */
    public void moveAvailableToPhysical(int delta) {
        this.availableQuantity = this.availableQuantity - delta;
        this.quantity = this.quantity + delta;
        this.lastMovementAt = new Date();
    }

    // 판매 가능 여부를 실재고(quantity) 기준으로 확인 (해당 수량을 팔 수 있는지 검사)
    public boolean canSell(int sellQuantity) {
        return sellQuantity > 0 && this.quantity >= sellQuantity;
    }

    // 판매 반영 - 실재고를 차감하고 이동 시각을 갱신
    public void applySale(int sellQuantity) {
        this.quantity -= sellQuantity;
        // 기존 화면 정합성을 위해 availableQuantity도 함께 감산하되 음수는 방지
        this.availableQuantity = Math.max(0, this.availableQuantity - sellQuantity);
        this.lastMovementAt = new Date();
    }
}
