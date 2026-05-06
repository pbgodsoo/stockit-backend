package org.example.stockitbe.warehouse.outbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.warehouse.outbound.model.OutboundDestinationType;
import org.example.stockitbe.warehouse.outbound.model.OutboundSourceType;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;

import java.util.Date;

@Entity
@Table(name = "wh_outbound_header",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wh_outbound_no", columnNames = "outbound_no"),
                @UniqueConstraint(name = "uk_wh_outbound_source_ref", columnNames = {"source_type", "source_ref_no"})
        },
        indexes = {
                @Index(name = "idx_wh_outbound_warehouse_status", columnList = "warehouse_id,status"),
                @Index(name = "idx_wh_outbound_destination", columnList = "destination_type,destination_id"),
                @Index(name = "idx_wh_outbound_requested_at", columnList = "requested_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhOutboundHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbound_no", nullable = false, length = 50)
    private String outboundNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private OutboundSourceType sourceType;

    @Column(name = "source_ref_no", nullable = false, length = 50)
    private String sourceRefNo;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false, length = 30)
    private OutboundDestinationType destinationType;

    @Column(name = "destination_id", nullable = false)
    private Long destinationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboundStatus status;

    @Column(name = "total_requested_quantity", nullable = false)
    private Integer totalRequestedQuantity;

    @Column(name = "requested_at", nullable = false)
    private Date requestedAt;

    @Column(name = "confirmed_at")
    private Date confirmedAt;

    @Column(name = "departed_at")
    private Date departedAt;

    @Column(name = "arrived_at")
    private Date arrivedAt;

    @Column(name = "requested_by_member_id", length = 50)
    private String requestedByMemberId;

    @Column(name = "requested_by_name", length = 100)
    private String requestedByName;

    @Column(name = "memo", length = 500)
    private String memo;

    @Builder
    private WhOutboundHeader(String outboundNo, OutboundSourceType sourceType, String sourceRefNo, Long sourceRefId,
                             Long warehouseId, OutboundDestinationType destinationType, Long destinationId,
                             OutboundStatus status, Integer totalRequestedQuantity,
                             Date requestedAt, Date confirmedAt, Date departedAt, Date arrivedAt,
                             String requestedByMemberId, String requestedByName, String memo) {
        this.outboundNo = outboundNo;
        this.sourceType = sourceType;
        this.sourceRefNo = sourceRefNo;
        this.sourceRefId = sourceRefId;
        this.warehouseId = warehouseId;
        this.destinationType = destinationType;
        this.destinationId = destinationId;
        this.status = status == null ? OutboundStatus.READY_TO_SHIP : status;
        this.totalRequestedQuantity = totalRequestedQuantity == null ? 0 : totalRequestedQuantity;
        this.requestedAt = requestedAt == null ? new Date() : requestedAt;
        this.confirmedAt = confirmedAt;
        this.departedAt = departedAt;
        this.arrivedAt = arrivedAt;
        this.requestedByMemberId = requestedByMemberId;
        this.requestedByName = requestedByName;
        this.memo = memo;
    }

    public void markInTransit(Date now) {
        this.status = OutboundStatus.IN_TRANSIT;
        this.confirmedAt = now == null ? new Date() : now;
        this.departedAt = now == null ? new Date() : now;
    }

    public void markArrived(Date now) {
        this.status = OutboundStatus.ARRIVED;
        this.arrivedAt = now == null ? new Date() : now;
    }
}
