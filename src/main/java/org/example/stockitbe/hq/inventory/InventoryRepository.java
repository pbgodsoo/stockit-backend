package org.example.stockitbe.hq.inventory;

import jakarta.persistence.LockModeType;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findAllByLocationId(Long locationId);
    List<Inventory> findAllBySkuIdIn(Collection<Long> skuIds);
    List<Inventory> findAllByInventoryStatus(InventoryStatus inventoryStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockBySkuIdAndLocationIdAndInventoryStatus(Long skuId, Long locationId, InventoryStatus inventoryStatus);

    Optional<Inventory> findBySkuIdAndLocationId(Long skuId, Long locationId);
    List<Inventory> findAllBySkuIdAndLocationId(Long skuId, Long locationId);

    // 같은 (sku, location) 에 status 별로 최대 3행 (NORMAL/CIRCULAR_CANDIDATE/CIRCULAR) 가 있을 수 있어
    // 발주 hook 처럼 NORMAL 만 다뤄야 하는 경우 status 까지 좁혀 단일 결과를 보장.
    Optional<Inventory> findBySkuIdAndLocationIdAndInventoryStatus(Long skuId, Long locationId, InventoryStatus inventoryStatus);

    // 매장 판매 처리 시 동일 SKU 동시 차감을 제어하기 위한 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.skuId = :skuId and i.locationId = :locationId")
    Optional<Inventory> findBySkuIdAndLocationIdForUpdate(@Param("skuId") Long skuId, @Param("locationId") Long locationId);
}
