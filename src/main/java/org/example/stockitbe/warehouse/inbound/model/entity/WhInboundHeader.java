package org.example.stockitbe.warehouse.inbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.warehouse.inbound.model.InboundStatus;
import org.example.stockitbe.warehouse.inbound.model.InboundType;

import java.util.Date;

@Entity
@Table(name = "wh_inbound_header", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wh_inbound_code", columnNames = "inbound_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhInboundHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_code", nullable = false, length = 50)
    private String inboundCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "inbound_type", nullable = false, length = 30)
    private InboundType inboundType;

    // PO-... 또는 STF-... 등 source 도메인의 비즈니스 코드 시점 복사 (결합 차단 4패턴 #1).
    @Column(name = "source_ref_no", nullable = false, length = 50)
    private String sourceRefNo;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_name", nullable = false, length = 128)
    private String warehouseName;

    // 공급처명(발주) 또는 출발창고명(이동) — inboundType 별 의미 다름, 시점 복사.
    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InboundStatus status;

    @Column(name = "total_quantity", nullable = false)
    private Long totalQuantity;

    // 발주 입고만 채움. 이동 입고는 사내 이동이라 null.
    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "arrived_at")
    private Date arrivedAt;

    @Column(name = "completed_at")
    private Date completedAt;

    @Column(name = "confirmed_by_member_id", length = 50)
    private String confirmedByMemberId;

    @Column(name = "confirmed_by_name", length = 128)
    private String confirmedByName;

    @Column(name = "memo", length = 500)
    private String memo;

    @Builder
    private WhInboundHeader(String inboundCode, InboundType inboundType, String sourceRefNo, Long sourceRefId,
                            Long warehouseId, String warehouseName, String sourceName,
                            InboundStatus status, Long totalQuantity, Long totalAmount, String memo,
                            Date arrivedAt, Date completedAt) {
        this.inboundCode = inboundCode;
        this.inboundType = inboundType;
        this.sourceRefNo = sourceRefNo;
        this.sourceRefId = sourceRefId;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.sourceName = sourceName;
        this.status = status == null ? InboundStatus.READY_TO_SHIP : status;
        this.totalQuantity = totalQuantity == null ? 0L : totalQuantity;
        this.totalAmount = totalAmount;
        this.memo = memo;
        // arrivedAt/completedAt 은 backfill 전용 — 일반 흐름엔 markArrived/markCompleted 도메인 메소드만 사용.
        this.arrivedAt = arrivedAt;
        this.completedAt = completedAt;
    }

    /** READY_TO_SHIP → IN_TRANSIT. PO mirror — 인벤토리 가용재고+ 시점은 Service 책임. */
    public void markInTransit() {
        if (this.status != InboundStatus.READY_TO_SHIP) {
            throw BaseException.from(BaseResponseStatus.INVALID_INBOUND_STATUS_TRANSITION);
        }
        this.status = InboundStatus.IN_TRANSIT;
    }

    /** IN_TRANSIT → ARRIVED. PO mirror — 도착 시점 박힘. */
    public void markArrived(Date now) {
        if (this.status != InboundStatus.IN_TRANSIT) {
            throw BaseException.from(BaseResponseStatus.INVALID_INBOUND_STATUS_TRANSITION);
        }
        this.status = InboundStatus.ARRIVED;
        this.arrivedAt = now == null ? new Date() : now;
    }

    /** ARRIVED → COMPLETED. 창고 [입고 확정] 매뉴얼. inbound 가 PO 도 mirror 갱신. */
    public void markCompleted(Date now, String byMemberId, String byName) {
        if (this.status != InboundStatus.ARRIVED) {
            throw BaseException.from(BaseResponseStatus.INVALID_INBOUND_STATUS_TRANSITION);
        }
        this.status = InboundStatus.COMPLETED;
        this.completedAt = now == null ? new Date() : now;
        this.confirmedByMemberId = byMemberId;
        this.confirmedByName = byName;
    }
}
