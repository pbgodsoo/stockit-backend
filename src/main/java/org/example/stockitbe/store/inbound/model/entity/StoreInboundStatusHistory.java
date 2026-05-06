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
@Table(name = "store_inbound_status_history",
        indexes = {
                @Index(name = "idx_store_inbound_history_header_changed", columnList = "inbound_header_id,changed_at,id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreInboundStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_header_id", nullable = false)
    private Long inboundHeaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreInboundStatus status;

    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    @Column(name = "changed_by_member_id", length = 50)
    private String changedByMemberId;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    @Column(name = "reason", length = 500)
    private String reason;

    @Builder
    private StoreInboundStatusHistory(Long inboundHeaderId, StoreInboundStatus status, Date changedAt,
                                      String changedByMemberId, String changedByName, String reason) {
        this.inboundHeaderId = inboundHeaderId;
        this.status = status;
        this.changedAt = changedAt == null ? new Date() : changedAt;
        this.changedByMemberId = changedByMemberId;
        this.changedByName = changedByName;
        this.reason = reason;
    }
}

