package org.example.stockitbe.hq.purchaseorder.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "purchase_order_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 부모-자식 컴포지션 — @ManyToOne 매핑 (append-only, cascade=PERSIST 만으로 충분).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PurchaseOrderStatus status;

    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    @Column(name = "changed_by_name", length = 64)
    private String changedByName;

    @Column(name = "note", length = 256)
    private String note;

    @Builder
    private PurchaseOrderStatusHistory(PurchaseOrder purchaseOrder, PurchaseOrderStatus status,
                                        String changedByName, String note) {
        this.purchaseOrder = purchaseOrder;
        this.status = status;
        this.changedAt = new Date();
        this.changedByName = changedByName;
        this.note = note;
    }

    void linkToParent(PurchaseOrder parent) {
        this.purchaseOrder = parent;
    }
}
