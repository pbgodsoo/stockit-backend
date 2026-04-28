package org.example.stockitbe.hq.purchaseorder.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.common.model.BaseResponseStatus;

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

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    // vendor.name 시점 복사 — 발주 시점의 거래처명 스냅샷 (warehouseName/memberName 패턴과 동일).
    // statusHistory.changedByName 에 거래처 책임 전환(APPROVED/SHIPPING) 주체로 박힘.
    @Column(name = "vendor_name", nullable = false, length = 128)
    private String vendorName;

    // 창고 BE 미구현 — logical reference (id String + name 시점 복사)
    @Column(name = "warehouse_id", length = 32)
    private String warehouseId;

    @Column(name = "warehouse_name", length = 128)
    private String warehouseName;

    // 회원 BE 미구현, 인증 미정 — logical reference
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

    @Builder
    private PurchaseOrder(String code, Long vendorId, String vendorName,
                          String warehouseId, String warehouseName,
                          String memberId, String memberName, Long totalAmount) {
        this.code = code;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.memberId = memberId;
        this.memberName = memberName;
        this.totalAmount = totalAmount == null ? 0L : totalAmount;
        this.status = PurchaseOrderStatus.PENDING;
    }

    public void markApproved() {
        if (this.status != PurchaseOrderStatus.PENDING) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.APPROVED;
    }

    public void markShipping() {
        if (this.status != PurchaseOrderStatus.APPROVED) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.SHIPPING;
    }

    public void markCompleted() {
        if (this.status != PurchaseOrderStatus.SHIPPING) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.COMPLETED;
    }

    public void markRejected(String reason) {
        if (this.status != PurchaseOrderStatus.PENDING) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        this.status = PurchaseOrderStatus.REJECTED;
        this.cancelReason = reason;
    }

    /**
     * items 교체 + totalAmount 재계산. PENDING 만 허용.
     * Service 가 기존 items 를 별도로 삭제하고 신규 items 를 save 한 뒤 호출.
     */
    public void recalculateTotalAmount(List<PurchaseOrderItem> items) {
        if (this.status != PurchaseOrderStatus.PENDING) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        long sum = items.stream().mapToLong(PurchaseOrderItem::getSubtotal).sum();
        this.totalAmount = sum;
    }

    public void updateLogistics(String warehouseId, String warehouseName) {
        if (this.status != PurchaseOrderStatus.PENDING) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }
        if (warehouseId != null) this.warehouseId = warehouseId;
        if (warehouseName != null) this.warehouseName = warehouseName;
    }
}
