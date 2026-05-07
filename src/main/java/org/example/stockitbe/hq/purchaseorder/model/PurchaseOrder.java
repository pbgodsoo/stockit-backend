package org.example.stockitbe.hq.purchaseorder.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.vendor.model.Vendor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order", uniqueConstraints = {
        @UniqueConstraint(name = "uk_purchase_order_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    // 외부 도메인 — JPA 정석 매핑. N+1 은 조회 시점 @EntityGraph / join fetch 로 통제.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    // vendor.name 시점 박제 스냅샷 — 거래처 개명에 영향 안 받게 발주 시점 라벨 보존 (ERP 정석).
    @Column(name = "vendor_name", nullable = false, length = 128)
    private String vendorName;

    // vendor.contactName 시점 박제 — 공급처 담당자명. statusHistory.changedByName 의
    // 거래처 책임 단계(APPROVED/READY_TO_SHIP/IN_TRANSIT/ARRIVED) 주체로 박힘.
    @Column(name = "vendor_contact_name", nullable = false, length = 64)
    private String vendorContactName;

    // 외부 도메인 — Infrastructure 가 창고 마스터 (LocationType=WAREHOUSE).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Infrastructure warehouse;

    // warehouse.name 시점 박제 스냅샷.
    @Column(name = "warehouse_name", nullable = false, length = 128)
    private String warehouseName;

    // 회원 logical reference — User.employeeCode 자연 키 String 스냅샷.
    // Long PK 매핑 전환은 데이터 마이그레이션 동반 별 사이클 (이슈 #205 범위 외).
    @Column(name = "member_id", length = 32)
    private String memberId;

    @Column(name = "member_name", length = 64)
    private String memberName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PurchaseOrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "cancel_reason", length = 256)
    private String cancelReason;

    // 부모-자식 컴포지션 — cascade 자동화 대상.
    // items: 라이프사이클 동일, REQUESTED 시 교체 빈번 → orphanRemoval 필수.
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    // statusHistory: append-only — cascade=PERSIST 만으로 충분 (수정/삭제 X).
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.PERSIST)
    @OrderBy("changedAt ASC")
    private List<PurchaseOrderStatusHistory> statusHistory = new ArrayList<>();

    @Builder
    private PurchaseOrder(String code, Vendor vendor, String vendorName, String vendorContactName,
                          Infrastructure warehouse, String warehouseName,
                          String memberId, String memberName, Long totalAmount) {
        this.code = code;
        this.vendor = vendor;
        this.vendorName = vendorName;
        this.vendorContactName = vendorContactName;
        this.warehouse = warehouse;
        this.warehouseName = warehouseName;
        this.memberId = memberId;
        this.memberName = memberName;
        this.totalAmount = totalAmount == null ? 0L : totalAmount;
        this.status = PurchaseOrderStatus.REQUESTED;
    }

    /** REQUESTED → APPROVED. SYS-001 배치 자동 전환 (거래처 책임). */
    public void markApproved() {
        if (this.status != PurchaseOrderStatus.REQUESTED) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.APPROVED;
    }

    /** APPROVED → READY_TO_SHIP. SYS-001 배치 자동 전환 (거래처 배송 준비). */
    public void markReadyToShip() {
        if (this.status != PurchaseOrderStatus.APPROVED) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.READY_TO_SHIP;
    }

    /** READY_TO_SHIP → IN_TRANSIT. SYS-001 배치 자동 전환 — 인벤토리 가용재고 + 시점. */
    public void markInTransit() {
        if (this.status != PurchaseOrderStatus.READY_TO_SHIP) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.IN_TRANSIT;
    }

    /** IN_TRANSIT → ARRIVED. SYS-001 배치 자동 전환 (창고 도착). */
    public void markArrived() {
        if (this.status != PurchaseOrderStatus.IN_TRANSIT) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.ARRIVED;
    }

    /** ARRIVED → COMPLETED. 창고 [입고 확정] 매뉴얼 (검수 미구현, ADR-015). */
    public void markCompleted() {
        if (this.status != PurchaseOrderStatus.ARRIVED) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.COMPLETED;
    }

    /** REQUESTED → CANCELLED. 승인 대기 단계에서만 본사 [취소] 가능. */
    public void markCancelled(String reason) {
        if (this.status != PurchaseOrderStatus.REQUESTED) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.CANCELLED;
        this.cancelReason = reason;
    }

    /**
     * items 교체 + totalAmount 재계산. REQUESTED 만 허용.
     * orphanRemoval=true 가 기존 자식 row 자동 DELETE, cascade=ALL 이 신규 자식 row 자동 INSERT.
     */
    public void replaceItems(List<PurchaseOrderItem> newItems) {
        if (this.status != PurchaseOrderStatus.REQUESTED) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.items.clear();
        newItems.forEach(it -> {
            it.linkToParent(this);
            this.items.add(it);
        });
        this.totalAmount = this.items.stream().mapToLong(PurchaseOrderItem::getSubtotal).sum();
    }

    /**
     * 진행 이력 한 행 추가 (append-only). cascade=PERSIST 가 자동 INSERT.
     * changedByName 분기 책임은 Service — 인증/배치 호출 컨텍스트에 따라 다르므로.
     */
    public void appendHistory(String changedByName, String note) {
        PurchaseOrderStatusHistory entry = PurchaseOrderStatusHistory.builder()
                .purchaseOrder(this)
                .status(this.status)
                .changedByName(changedByName)
                .note(note)
                .build();
        this.statusHistory.add(entry);
    }

    public void updateLogistics(Infrastructure warehouse, String warehouseName) {
        if (this.status != PurchaseOrderStatus.REQUESTED) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        if (warehouse != null) this.warehouse = warehouse;
        if (warehouseName != null) this.warehouseName = warehouseName;
    }
}
