package org.example.stockitbe.hq.warehousetransfer.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "warehouse_transfer_header", uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouse_transfer_no", columnNames = "transfer_no")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseTransferHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_no", nullable = false, length = 50)
    private String transferNo;

    @Column(name = "from_warehouse_id", nullable = false)
    private Long fromWarehouseId;

    @Column(name = "to_warehouse_id", nullable = false)
    private Long toWarehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WarehouseTransferStatus status;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Date requestedAt;

    @Column(name = "reason_summary", length = 500)
    private String reasonSummary;

    @Column(name = "memo_summary", length = 500)
    private String memoSummary;

    @OneToMany(mappedBy = "header", fetch = FetchType.LAZY)
    private List<WarehouseTransferItem> items = new ArrayList<>();

    @Builder
    private WarehouseTransferHeader(String transferNo, Long fromWarehouseId, Long toWarehouseId,
                                    WarehouseTransferStatus status, String requestedBy, Date requestedAt,
                                    String reasonSummary, String memoSummary) {
        this.transferNo = transferNo;
        this.fromWarehouseId = fromWarehouseId;
        this.toWarehouseId = toWarehouseId;
        this.status = status == null ? WarehouseTransferStatus.READY_TO_SHIP : status;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt == null ? new Date() : requestedAt;
        this.reasonSummary = reasonSummary;
        this.memoSummary = memoSummary;
    }

    // 재고이동 실행 직후 출고 준비 상태로 전환한다.
    public void markReadyToShip() {
        this.status = WarehouseTransferStatus.READY_TO_SHIP;
    }

    // 출고 확정 시점에 배송중 상태로 전환한다.
    public void markInTransit() {
        this.status = WarehouseTransferStatus.IN_TRANSIT;
    }

    // 배송 완료 시점에 도착 상태로 전환한다.
    public void markArrived() {
        this.status = WarehouseTransferStatus.ARRIVED;
    }
}
