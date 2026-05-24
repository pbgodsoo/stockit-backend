package org.example.stockitbe.hq.inventory;

import org.example.stockitbe.hq.inventory.model.InventoryCandidateCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

// 순환재고 후보 판정 조건 매핑 Repository
public interface InventoryCandidateConditionRepository extends JpaRepository<InventoryCandidateCondition, Long> {
    // 순환재고 후보 inventoryId 목록 기준 조건 코드를 조회한다.
    List<InventoryCandidateCondition> findAllByInventoryIdIn(Collection<Long> inventoryIds);

    // 순환재고 후보 inventoryId 목록 기준 조건 코드를 일괄 삭제한다.
    void deleteByInventoryIdIn(Collection<Long> inventoryIds);
}
