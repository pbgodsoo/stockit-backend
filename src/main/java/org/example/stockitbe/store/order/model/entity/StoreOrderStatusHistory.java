package org.example.stockitbe.store.order.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.store.order.model.StoreOrderHistoryType;

import java.util.Date;

@Entity
@Table(name = "store_order_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrderStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_header_id", nullable = false)
    private Long orderHeaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_type", nullable = false, length = 20)
    private StoreOrderHistoryType historyType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    @Column(name = "changed_by_member_id", length = 50)
    private String changedByMemberId;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    @Column(name = "reason", length = 500)
    private String reason;

    @Builder
    private StoreOrderStatusHistory(Long orderHeaderId, StoreOrderHistoryType historyType, String status,
                                    Date changedAt, String changedByMemberId, String changedByName, String reason) {
        this.orderHeaderId = orderHeaderId;
        this.historyType = historyType;
        this.status = status;
        this.changedAt = changedAt == null ? new Date() : changedAt;
        this.changedByMemberId = changedByMemberId;
        this.changedByName = changedByName;
        this.reason = reason;
    }
}
