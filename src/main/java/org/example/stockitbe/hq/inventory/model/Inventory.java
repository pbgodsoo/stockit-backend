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

    public boolean isEmptyStock() {
        return n(this.quantity) <= 0 && n(this.availableQuantity) <= 0;
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }
}
