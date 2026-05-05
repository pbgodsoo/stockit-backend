package org.example.stockitbe.hq.inventory;

import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findAllBySkuIdIn(Collection<Long> skuIds);
    List<Inventory> findAllByInventoryStatus(InventoryStatus inventoryStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockBySkuIdAndLocationIdAndInventoryStatus(Long skuId, Long locationId, InventoryStatus inventoryStatus);
}
