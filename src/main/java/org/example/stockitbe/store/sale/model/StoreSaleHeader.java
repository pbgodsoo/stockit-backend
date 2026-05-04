package org.example.stockitbe.store.sale.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.util.Date;

@Entity
@Table(name = "store_sale_header", indexes = {
        @Index(name = "idx_store_sale_header_store_soldat", columnList = "store_id,sold_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_store_sale_header_sale_no", columnNames = "sale_no")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreSaleHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_no", nullable = false, length = 50)
    private String saleNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "sold_at", nullable = false)
    private Date soldAt;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StoreSaleStatus status;

    @Builder
    private StoreSaleHeader(String saleNo, Long storeId, Date soldAt, Integer totalQuantity, Long totalAmount,
                            StoreSaleStatus status) {
        this.saleNo = saleNo;
        this.storeId = storeId;
        this.soldAt = soldAt;
        this.totalQuantity = totalQuantity == null ? 0 : totalQuantity;
        this.totalAmount = totalAmount == null ? 0L : totalAmount;
        this.status = status == null ? StoreSaleStatus.COMPLETED : status;
    }

    // 헤더 저장 후 PK 기반 판매번호를 주입할 때 사용한다.
    public void assignSaleNo(String saleNo) {
        this.saleNo = saleNo;
    }
}
