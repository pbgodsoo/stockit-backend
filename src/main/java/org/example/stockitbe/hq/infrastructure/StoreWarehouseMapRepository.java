package org.example.stockitbe.hq.infrastructure;

import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.StoreWarehouseMap;
import org.example.stockitbe.hq.infrastructure.model.StoreWarehouseRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreWarehouseMapRepository extends JpaRepository<StoreWarehouseMap, Long> {
    List<StoreWarehouseMap> findByStoreIn(List<Infrastructure> stores);
    List<StoreWarehouseMap> findByWarehouseIn(List<Infrastructure> warehouses);
    Optional<StoreWarehouseMap> findByStoreAndRole(Infrastructure store, StoreWarehouseRole role);
    Optional<StoreWarehouseMap> findByStoreAndWarehouse(Infrastructure store, Infrastructure warehouse);
    long countByWarehouse(Infrastructure warehouse);
}
