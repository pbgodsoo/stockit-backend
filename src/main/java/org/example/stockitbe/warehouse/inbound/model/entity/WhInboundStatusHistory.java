package org.example.stockitbe.warehouse.inbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.warehouse.inbound.model.InboundStatus;

import java.util.Date;

@Entity
@Table(name = "wh_inbound_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhInboundStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // header FK Long ID (PurchaseOrderStatusHistory 패턴)
    @Column(name = "inbound_header_id", nullable = false)
    private Long inboundHeaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InboundStatus status;

    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    @Column(name = "changed_by_member_id", length = 50)
    private String changedByMemberId;

    // PR #190 라벨 형식: "이름 (소속)" 매뉴얼 단계 / "담당자명 (회사명)" 거래처 단계
    @Column(name = "changed_by_name", length = 200)
    private String changedByName;

    @Column(name = "note", length = 500)
    private String note;

    @Builder
    private WhInboundStatusHistory(Long inboundHeaderId, InboundStatus status,
                                   String changedByMemberId, String changedByName, String note) {
        this.inboundHeaderId = inboundHeaderId;
        this.status = status;
        this.changedAt = new Date();
        this.changedByMemberId = changedByMemberId;
        this.changedByName = changedByName;
        this.note = note;
    }
}
