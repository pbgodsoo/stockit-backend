package org.example.stockitbe.hq.inventory.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.util.Date;

@Entity
@Table(name = "inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_sku_location_status", columnNames = {"sku_id", "location_id", "inventory_status"})
    },
    // (location_id, inventory_status) 복합 인덱스 — 창고 재고 조회 핵심 필터.
    // 단일 (location_id) 보다 status IN (...) 까지 cover 가능.
    // 기존 uk 의 prefix 가 sku_id 라 location 단독 검색은 자동 활용 안 됨.
    indexes = {
        @Index(name = "idx_inventory_location_status", columnList = "location_id, inventory_status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 재고 엔티티
// SKU + 위치 + 상태 조합별 수량 스냅샷과 전이 상태를 관리한다.
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

    // NORMAL -> CIRCULAR_CANDIDATE 상태 전이
    public void markCircularCandidate(Date changedAt) {
        this.inventoryStatus = InventoryStatus.CIRCULAR_CANDIDATE;
        this.statusChangedAt = changedAt == null ? new Date() : changedAt;
    }

    // CIRCULAR 상태 전이
    public void markCircular(Date changedAt) {
        this.inventoryStatus = InventoryStatus.CIRCULAR;
        this.statusChangedAt = changedAt == null ? new Date() : changedAt;
    }

    // 후보 재고를 순환재고로 전환할 때 원본 재고를 차감한다.
    public void decreaseForConversion(int quantityToMove) {
        int safeMove = Math.max(0, quantityToMove);
        int nextAvailable = Math.max(0, n(this.availableQuantity) - safeMove);
        int nextQuantity = Math.max(0, n(this.quantity) - safeMove);
        this.availableQuantity = nextAvailable;
        this.quantity = nextQuantity;
    }

    // 순환재고 전환 수량을 대상 재고에 가산한다.
    public void increaseForConversion(int quantityToMove) {
        int safeMove = Math.max(0, quantityToMove);
        this.availableQuantity = n(this.availableQuantity) + safeMove;
        this.quantity = n(this.quantity) + safeMove;
    }

    // 동일 SKU/위치 후보 재고를 병합한다.
    public void absorbAsCircularCandidate(Inventory source, Date changedAt) {
        if (source == null) return;
        this.quantity = n(this.quantity) + n(source.getQuantity());
        this.availableQuantity = n(this.availableQuantity) + n(source.getAvailableQuantity());
        this.reservedQuantity = n(this.reservedQuantity) + n(source.getReservedQuantity());
        this.inTransitQuantity = n(this.inTransitQuantity) + n(source.getInTransitQuantity());
        this.statusChangedAt = changedAt == null ? new Date() : changedAt;
        this.lastMovementAt = maxDate(this.lastMovementAt, source.getLastMovementAt());
    }

    // 실재고와 가용재고가 모두 0 이하인지 확인한다.
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
     * 본사 발주 SHIPPING 진입 시 가용재고 증가 (이슈 #169 — 발주 ↔ 인벤토리 연결 룰).
     * 도착 전 예약 가용재고로, 실재고(quantity) 는 변하지 않는다.
     */
    public void increaseAvailable(int delta) {
        this.availableQuantity = this.availableQuantity + delta;
        this.lastMovementAt = new Date();
    }

    /**
     * 본사 발주 COMPLETED (입고 확정) 시 실재고 인식 (ADR-024 정정 2026-05-18, 이슈 #303).
     * 가용재고는 SHIPPING 단계 increaseAvailable 로 이미 +delta 됐으므로, 입고 확정에선
     * 실재고(quantity) 만 +delta. 가용재고를 또 차감하면 "가용재고 0 으로 떨어짐" 버그 재발.
     * 단계별 의미:
     *   - SHIPPING : available += delta (예약 가용, 매장 발주가 잡을 수 있음)
     *   - COMPLETED: quantity  += delta (실재고 인식, available 은 그대로)
     */
    public void moveAvailableToPhysical(int delta) {
        this.quantity = this.quantity + delta;
        this.lastMovementAt = new Date();
    }

    // --------------- 매장 판매 관련 ----------------
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

    // ---------------- 매장 발주 관련 -----------------
    // 발주 승인 시 출고지 창고 재고를 예약 처리
    // 요청 수량만큼 예약하려고 시도하고, 실제 예약된 수량을 반환
    public int reserveUpTo(int requestedQuantity) {
        int safeRequested = Math.max(0, requestedQuantity);
        int reservable = Math.max(0, n(this.availableQuantity));
        int reserved = Math.min(safeRequested, reservable);
        this.availableQuantity = n(this.availableQuantity) - reserved;
        this.reservedQuantity = n(this.reservedQuantity) + reserved;
        this.lastMovementAt = new Date();
        return reserved;
    }

    // ------------- 물류 창고 출고 관련 ------------------
    // 출고리스트에서 reserved로 잡힌 아이템을 출고 확정 시 InTransit 재고로 반영
    public int moveReservedToInTransit(int requestedQuantity) {
        int safeRequested = Math.max(0, requestedQuantity);
        int movable = Math.max(0, n(this.reservedQuantity));
        int moved = Math.min(safeRequested, movable);
        this.reservedQuantity = n(this.reservedQuantity) - moved;
        this.inTransitQuantity = n(this.inTransitQuantity) + moved;
        this.lastMovementAt = new Date();
        return moved;
    }

    // ------------- 매장 입고 확정 관련 ------------------
    // 매장 입고 확정 시점에 실재고/가용재고를 동시에 증가시킨다.
    public void increaseOnHandAndAvailable(int receivedQuantity) {
        int safeReceived = Math.max(0, receivedQuantity);
        this.quantity = n(this.quantity) + safeReceived;
        this.availableQuantity = n(this.availableQuantity) + safeReceived;
        this.lastMovementAt = new Date();
    }

    // ------------- 창고간 이동 입고 확정 관련 ------------------
    // 수신 [입고 확정] 시점에 송신 창고 측 inTransit 재고를 차감 (음수 방어).
    public void reduceInTransit(int delta) {
        int safeDelta = Math.max(0, delta);
        this.inTransitQuantity = Math.max(0, n(this.inTransitQuantity) - safeDelta);
        this.lastMovementAt = new Date();
    }
}
