package org.example.stockitbe.hq.warehousetransfer.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "warehouse_transfer_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseTransferItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "header_id", nullable = false)
    private WarehouseTransferHeader header;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "from_available_before", nullable = false)
    private Integer fromAvailableBefore;

    @Column(name = "to_available_before", nullable = false)
    private Integer toAvailableBefore;

    @Column(name = "from_available_after")
    private Integer fromAvailableAfter;

    @Column(name = "to_available_after")
    private Integer toAvailableAfter;

    @Builder
    private WarehouseTransferItem(WarehouseTransferHeader header, Long skuId, Integer quantity, String reason,
                                  String memo, Integer fromAvailableBefore, Integer toAvailableBefore,
                                  Integer fromAvailableAfter, Integer toAvailableAfter) {
        this.header = header;
        this.skuId = skuId;
        this.quantity = quantity == null ? 0 : quantity;
        this.reason = reason;
        this.memo = memo;
        this.fromAvailableBefore = fromAvailableBefore == null ? 0 : fromAvailableBefore;
        this.toAvailableBefore = toAvailableBefore == null ? 0 : toAvailableBefore;
        this.fromAvailableAfter = fromAvailableAfter;
        this.toAvailableAfter = toAvailableAfter;
    }
}
