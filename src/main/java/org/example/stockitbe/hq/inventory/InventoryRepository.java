package org.example.stockitbe.hq.inventory;

import org.example.stockitbe.hq.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findAllBySkuIdIn(Collection<Long> skuIds);

    Optional<Inventory> findBySkuIdAndLocationId(Long skuId, Long locationId);
}
