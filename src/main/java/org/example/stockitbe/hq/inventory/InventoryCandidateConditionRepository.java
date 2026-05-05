package org.example.stockitbe.hq.inventory;

import org.example.stockitbe.hq.inventory.model.InventoryCandidateCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InventoryCandidateConditionRepository extends JpaRepository<InventoryCandidateCondition, Long> {
    List<InventoryCandidateCondition> findAllByInventoryIdIn(Collection<Long> inventoryIds);
    void deleteByInventoryIdIn(Collection<Long> inventoryIds);
}
