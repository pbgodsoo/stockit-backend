package org.example.stockitbe.hq.infrastructure.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "store_warehouse_map", uniqueConstraints = {
        @UniqueConstraint(name = "uk_store_warehouse_map_store_role", columnNames = {"store_id", "role"}),
        @UniqueConstraint(name = "uk_store_warehouse_map_store_warehouse", columnNames = {"store_id", "warehouse_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreWarehouseMap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Infrastructure store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Infrastructure warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private StoreWarehouseRole role;

    public StoreWarehouseMap(Infrastructure store, Infrastructure warehouse, StoreWarehouseRole role) {
        this.store = store;
        this.warehouse = warehouse;
        this.role = role;
    }

    public void changeWarehouse(Infrastructure warehouse) {
        this.warehouse = warehouse;
    }
}
