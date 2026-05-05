package org.example.stockitbe.hq.inventory;

import jakarta.persistence.LockModeType;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findAllBySkuIdIn(Collection<Long> skuIds);

    Optional<Inventory> findBySkuIdAndLocationId(Long skuId, Long locationId);

    // 매장 판매 처리 시 동일 SKU 동시 차감을 제어하기 위한 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.skuId = :skuId and i.locationId = :locationId")
    Optional<Inventory> findBySkuIdAndLocationIdForUpdate(@Param("skuId") Long skuId, @Param("locationId") Long locationId);
}
