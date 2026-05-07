package org.example.stockitbe.store.order.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.store.order.model.StoreOrderStatus;

import java.util.Date;

@Entity
@Table(name = "store_order_header")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrderHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "requested_by_member_id", nullable = false, length = 50)
    private String requestedByMemberId;

    @Column(name = "requested_by_name", nullable = false, length = 100)
    private String requestedByName;

    @Column(name = "requested_at", nullable = false)
    private Date requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreOrderStatus status;

    @Column(name = "total_sku_count", nullable = false)
    private Integer totalSkuCount;

    @Column(name = "total_requested_quantity", nullable = false)
    private Integer totalRequestedQuantity;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Builder
    private StoreOrderHeader(String orderNo, Long storeId, Long warehouseId, String requestedByMemberId,
                             String requestedByName, Date requestedAt, StoreOrderStatus status,
                             Integer totalSkuCount, Integer totalRequestedQuantity,
                             String memo, String cancelReason) {
        this.orderNo = orderNo;
        this.storeId = storeId;
        this.warehouseId = warehouseId;
        this.requestedByMemberId = requestedByMemberId;
        this.requestedByName = requestedByName;
        this.requestedAt = requestedAt;
        this.status = status == null ? StoreOrderStatus.REQUESTED : status;
        this.totalSkuCount = totalSkuCount == null ? 0 : totalSkuCount;
        this.totalRequestedQuantity = totalRequestedQuantity == null ? 0 : totalRequestedQuantity;
        this.memo = memo;
        this.cancelReason = cancelReason;
    }

    public void assignOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void updateRequested(Date requestedAt, Integer totalSkuCount, Integer totalRequestedQuantity, String memo) {
        validateRequestedOnly();
        this.requestedAt = requestedAt == null ? this.requestedAt : requestedAt;
        this.totalSkuCount = totalSkuCount;
        this.totalRequestedQuantity = totalRequestedQuantity;
        this.memo = memo;
    }

    public void markCancelled(String cancelReason) {
        validateRequestedOnly();
        this.status = StoreOrderStatus.CANCELLED;
        this.cancelReason = cancelReason;
    }

    public void markApproved() {
        validateRequestedOnly();
        this.status = StoreOrderStatus.APPROVED;
    }

    private void validateRequestedOnly() {
        if (this.status != StoreOrderStatus.REQUESTED) {
            throw BaseException.from(BaseResponseStatus.STORE_ORDER_INVALID_STATUS_TRANSITION);
        }
    }
}
