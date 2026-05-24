package org.example.stockitbe.warehouse.outbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;

import java.util.Date;

@Entity
@Table(name = "wh_outbound_status_history",
        indexes = {
                @Index(name = "idx_wh_outbound_history_header_changed", columnList = "outbound_header_id,changed_at,id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhOutboundStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbound_header_id", nullable = false)
    private Long outboundHeaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboundStatus status;

    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    @Column(name = "changed_by_member_id", length = 50)
    private String changedByMemberId;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    @Column(name = "reason", length = 500)
    private String reason;

    @Builder
    private WhOutboundStatusHistory(Long outboundHeaderId, OutboundStatus status, Date changedAt,
                                    String changedByMemberId, String changedByName, String reason) {
        this.outboundHeaderId = outboundHeaderId;
        this.status = status;
        this.changedAt = changedAt == null ? new Date() : changedAt;
        this.changedByMemberId = changedByMemberId;
        this.changedByName = changedByName;
        this.reason = reason;
    }
}

