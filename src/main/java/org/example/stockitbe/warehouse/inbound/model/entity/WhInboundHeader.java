package org.example.stockitbe.warehouse.inbound.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.warehouse.inbound.model.InboundType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 창고 입고 헤더 (ERP 표준 — Goods Receipt Note 패턴).
 *
 * inbound 자체는 status 컬럼을 가지지 않는다. 진행 단계 (READY_TO_SHIP/IN_TRANSIT/ARRIVED) 의
 * 진실 원천은 source 도메인 (PURCHASE_ORDER 면 PurchaseOrder, WAREHOUSE_TRANSFER 면 WhOutbound) —
 * Service.findAll/findByCode 가 LEFT JOIN 해서 응답 status 필드 채움.
 *
 * inbound 의 책임은 "도착 후 자산 확정 시점·확정자 기록" 만. completedAt + confirmedBy* 가 그 책임.
 */
@Entity
@Table(name = "wh_inbound_header", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wh_inbound_code", columnNames = "inbound_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhInboundHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_code", nullable = false, length = 50)
    private String inboundCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "inbound_type", nullable = false, length = 30)
    private InboundType inboundType;

    @Column(name = "source_ref_no", nullable = false, length = 50)
    private String sourceRefNo;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    // 외부 도메인 — Infrastructure 가 창고 마스터 (LocationType=WAREHOUSE).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Infrastructure warehouse;

    // warehouse.name 시점 박제 스냅샷 — 창고명 변경에 영향 안 받게 입고 시점 라벨 보존.
    @Column(name = "warehouse_name", nullable = false, length = 128)
    private String warehouseName;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "total_quantity", nullable = false)
    private Long totalQuantity;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "completed_at")
    private Date completedAt;

    @Column(name = "confirmed_by_member_id", length = 50)
    private String confirmedByMemberId;

    @Column(name = "confirmed_by_name", length = 128)
    private String confirmedByName;

    @Column(name = "memo", length = 500)
    private String memo;

    // 부모-자식 컴포지션 — items 라이프사이클 동일, cascade 자동화 대상.
    @OneToMany(mappedBy = "inboundHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WhInboundItem> items = new ArrayList<>();

    @Builder
    private WhInboundHeader(String inboundCode, InboundType inboundType, String sourceRefNo, Long sourceRefId,
                            Infrastructure warehouse, String warehouseName, String sourceName,
                            Long totalQuantity, Long totalAmount, String memo,
                            Date completedAt, String confirmedByMemberId, String confirmedByName) {
        this.inboundCode = inboundCode;
        this.inboundType = inboundType;
        this.sourceRefNo = sourceRefNo;
        this.sourceRefId = sourceRefId;
        this.warehouse = warehouse;
        this.warehouseName = warehouseName;
        this.sourceName = sourceName;
        this.totalQuantity = totalQuantity == null ? 0L : totalQuantity;
        this.totalAmount = totalAmount;
        this.memo = memo;
        // backfill 전용 인자 — 일반 생성 시 null
        this.completedAt = completedAt;
        this.confirmedByMemberId = confirmedByMemberId;
        this.confirmedByName = confirmedByName;
    }

    /**
     * items 일괄 등록 (시점 복사). cascade=ALL 이 자동 INSERT.
     */
    public void replaceItems(List<WhInboundItem> newItems) {
        this.items.clear();
        newItems.forEach(it -> {
            it.linkToParent(this);
            this.items.add(it);
        });
    }

    /**
     * 입고 확정 — 창고 [입고 확정] 매뉴얼 트리거.
     * completedAt!=null 이면 이미 확정된 상태 → 중복 차단.
     */
    public void markConfirmed(Date now, String byMemberId, String byName) {
        if (this.completedAt != null) {
            throw BaseException.from(BaseResponseStatus.INBOUND_NOT_CONFIRMABLE);
        }
        this.completedAt = now == null ? new Date() : now;
        this.confirmedByMemberId = byMemberId;
        this.confirmedByName = byName;
    }
}
