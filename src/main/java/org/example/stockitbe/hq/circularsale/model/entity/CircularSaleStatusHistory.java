package org.example.stockitbe.hq.circularsale.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.hq.circularsale.model.CircularSaleStatus;

import java.util.Date;

@Entity
@Table(name = "circular_sale_status_history",
        indexes = {
                @Index(name = "idx_circular_sale_history_header_changed", columnList = "sale_header_id,changed_at,id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CircularSaleStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_header_id", nullable = false)
    private Long saleHeaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private CircularSaleStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CircularSaleStatus status;

    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    @Column(name = "changed_by_member_id", length = 50)
    private String changedByMemberId;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    @Column(name = "reason", length = 500)
    private String reason;

    @Builder
    private CircularSaleStatusHistory(Long saleHeaderId, CircularSaleStatus fromStatus, CircularSaleStatus status,
                                      Date changedAt, String changedByMemberId, String changedByName, String reason) {
        this.saleHeaderId = saleHeaderId;
        this.fromStatus = fromStatus;
        this.status = status;
        this.changedAt = changedAt == null ? new Date() : changedAt;
        this.changedByMemberId = changedByMemberId;
        this.changedByName = changedByName;
        this.reason = reason;
    }
}

