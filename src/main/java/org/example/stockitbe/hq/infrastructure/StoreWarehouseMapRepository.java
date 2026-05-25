package org.example.stockitbe.hq.infrastructure;

import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.StoreWarehouseMap;
import org.example.stockitbe.hq.infrastructure.model.StoreWarehouseRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StoreWarehouseMapRepository extends JpaRepository<StoreWarehouseMap, Long> {
    List<StoreWarehouseMap> findByStoreIn(List<Infrastructure> stores);
    List<StoreWarehouseMap> findByWarehouseIn(List<Infrastructure> warehouses);
    Optional<StoreWarehouseMap> findByStoreAndRole(Infrastructure store, StoreWarehouseRole role);
    Optional<StoreWarehouseMap> findByStoreAndWarehouse(Infrastructure store, Infrastructure warehouse);
    long countByWarehouse(Infrastructure warehouse);

    @Query("""
            select m
            from StoreWarehouseMap m
            join fetch m.warehouse
            where m.store.id in :storeIds
              and m.role = :role
            """)
    List<StoreWarehouseMap> findByStoreIdsAndRoleWithWarehouse(Collection<Long> storeIds, StoreWarehouseRole role);
}
