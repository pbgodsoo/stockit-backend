package org.example.stockitbe.store.inbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;

import java.util.Date;

@Entity
@Table(name = "store_inbound_header",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_inbound_no", columnNames = "inbound_no"),
                @UniqueConstraint(name = "uk_store_inbound_source_ref", columnNames = "source_ref_no")
        },
        indexes = {
                @Index(name = "idx_store_inbound_store_status", columnList = "store_id,status"),
                @Index(name = "idx_store_inbound_outbound_no", columnList = "outbound_no"),
                @Index(name = "idx_store_inbound_expected_arrival", columnList = "expected_arrival_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreInboundHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_no", nullable = false, length = 50)
    private String inboundNo;

    @Column(name = "source_ref_no", nullable = false, length = 50)
    private String sourceRefNo;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(name = "outbound_no", nullable = false, length = 50)
    private String outboundNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "from_warehouse_id", nullable = false)
    private Long fromWarehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreInboundStatus status;

    @Column(name = "total_sku_count", nullable = false)
    private Integer totalSkuCount;

    @Column(name = "total_expected_quantity", nullable = false)
    private Integer totalExpectedQuantity;

    @Column(name = "expected_arrival_at", nullable = false)
    private Date expectedArrivalAt;

    @Column(name = "requested_at", nullable = false)
    private Date requestedAt;

    @Column(name = "received_at")
    private Date receivedAt;

    @Column(name = "requested_by_member_id", length = 50)
    private String requestedByMemberId;

    @Column(name = "requested_by_name", length = 100)
    private String requestedByName;

    @Column(name = "received_by_member_id", length = 50)
    private String receivedByMemberId;

    @Column(name = "received_by_name", length = 100)
    private String receivedByName;

    @Column(name = "memo", length = 500)
    private String memo;

    @Builder
    private StoreInboundHeader(String inboundNo, String sourceRefNo, Long sourceRefId, String outboundNo,
                               Long storeId, Long fromWarehouseId, StoreInboundStatus status,
                               Integer totalSkuCount, Integer totalExpectedQuantity, Date expectedArrivalAt,
                               Date requestedAt, Date receivedAt, String requestedByMemberId, String requestedByName,
                               String receivedByMemberId, String receivedByName, String memo) {
        this.inboundNo = inboundNo;
        this.sourceRefNo = sourceRefNo;
        this.sourceRefId = sourceRefId;
        this.outboundNo = outboundNo;
        this.storeId = storeId;
        this.fromWarehouseId = fromWarehouseId;
        this.status = status == null ? StoreInboundStatus.PENDING_RECEIPT : status;
        this.totalSkuCount = totalSkuCount == null ? 0 : totalSkuCount;
        this.totalExpectedQuantity = totalExpectedQuantity == null ? 0 : totalExpectedQuantity;
        this.expectedArrivalAt = expectedArrivalAt == null ? new Date() : expectedArrivalAt;
        this.requestedAt = requestedAt == null ? new Date() : requestedAt;
        this.receivedAt = receivedAt;
        this.requestedByMemberId = requestedByMemberId;
        this.requestedByName = requestedByName;
        this.receivedByMemberId = receivedByMemberId;
        this.receivedByName = receivedByName;
        this.memo = memo;
    }

    public void markReceived(Date receivedAt, String receivedByMemberId, String receivedByName) {
        this.status = StoreInboundStatus.RECEIVED;
        this.receivedAt = receivedAt == null ? new Date() : receivedAt;
        this.receivedByMemberId = receivedByMemberId;
        this.receivedByName = receivedByName;
    }
}

