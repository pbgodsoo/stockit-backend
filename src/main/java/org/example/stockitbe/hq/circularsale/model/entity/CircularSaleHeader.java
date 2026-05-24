package org.example.stockitbe.hq.circularsale.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;
import org.example.stockitbe.hq.circularsale.model.CircularSaleStatus;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "circular_sale_header",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_circular_sale_header_sale_no", columnNames = "sale_no")
        },
        indexes = {
                @Index(name = "idx_circular_sale_header_status_sold_at", columnList = "status,sold_at"),
                @Index(name = "idx_circular_sale_header_buyer_sold_at", columnList = "buyer_id,sold_at"),
                @Index(name = "idx_circular_sale_header_warehouse_sold_at", columnList = "warehouse_id,sold_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CircularSaleHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_no", nullable = false, length = 40)
    private String saleNo;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CircularSaleStatus status;

    @Column(name = "sold_at", nullable = false)
    private Date soldAt;

    @Column(name = "sold_by_member_id", nullable = false, length = 50)
    private String soldByMemberId;

    @Column(name = "sold_by_name", nullable = false, length = 100)
    private String soldByName;

    @Column(name = "outbound_header_id")
    private Long outboundHeaderId;

    @Column(name = "material_type", nullable = false, length = 50)
    private String materialType;

    @Column(name = "total_sku_count", nullable = false)
    private Integer totalSkuCount;

    @Column(name = "total_requested_weight_kg", nullable = false, precision = 14, scale = 3, columnDefinition = "DECIMAL(14,3)")
    private BigDecimal totalRequestedWeightKg;

    @Column(name = "total_actual_weight_kg", nullable = false, precision = 14, scale = 3, columnDefinition = "DECIMAL(14,3)")
    private BigDecimal totalActualWeightKg;

    @Column(name = "total_sold_quantity", nullable = false)
    private Integer totalSoldQuantity;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "completed_at")
    private Date completedAt;

    @Builder
    private CircularSaleHeader(String saleNo, Long buyerId, Long warehouseId, CircularSaleStatus status, Date soldAt,
                               String soldByMemberId, String soldByName, Long outboundHeaderId, String materialType,
                               Integer totalSkuCount, BigDecimal totalRequestedWeightKg, BigDecimal totalActualWeightKg,
                               Integer totalSoldQuantity, Long totalAmount, String memo, Date completedAt) {
        this.saleNo = saleNo;
        this.buyerId = buyerId;
        this.warehouseId = warehouseId;
        this.status = status == null ? CircularSaleStatus.READY_TO_SHIP : status;
        this.soldAt = soldAt == null ? new Date() : soldAt;
        this.soldByMemberId = soldByMemberId;
        this.soldByName = soldByName;
        this.outboundHeaderId = outboundHeaderId;
        this.materialType = materialType;
        this.totalSkuCount = totalSkuCount == null ? 0 : totalSkuCount;
        this.totalRequestedWeightKg = totalRequestedWeightKg == null ? BigDecimal.ZERO : totalRequestedWeightKg;
        this.totalActualWeightKg = totalActualWeightKg == null ? BigDecimal.ZERO : totalActualWeightKg;
        this.totalSoldQuantity = totalSoldQuantity == null ? 0 : totalSoldQuantity;
        this.totalAmount = totalAmount == null ? 0L : totalAmount;
        this.memo = memo;
        this.completedAt = completedAt;
    }

    public void assignSaleNo(String saleNo) {
        this.saleNo = saleNo;
    }

    public void linkOutboundHeader(Long outboundHeaderId) {
        this.outboundHeaderId = outboundHeaderId;
    }

    public void markInTransit() {
        this.status = CircularSaleStatus.IN_TRANSIT;
    }

    public void markArrived(Date now) {
        this.status = CircularSaleStatus.ARRIVED;
        this.completedAt = now == null ? new Date() : now;
    }
}

