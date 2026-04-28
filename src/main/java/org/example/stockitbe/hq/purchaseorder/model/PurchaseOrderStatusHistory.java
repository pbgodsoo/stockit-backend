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

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PurchaseOrderStatus status;

    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    // 인증 미정 — placeholder, 추후 실제 사용자명
    @Column(name = "changed_by_name", length = 64)
    private String changedByName;

    @Column(name = "note", length = 256)
    private String note;

    @Builder
    private PurchaseOrderStatusHistory(Long purchaseOrderId, PurchaseOrderStatus status,
                                        String changedByName, String note) {
        this.purchaseOrderId = purchaseOrderId;
        this.status = status;
        this.changedAt = new Date();
        this.changedByName = changedByName;
        this.note = note;
    }
}
