package org.example.stockitbe.hq.inventory;

import org.example.stockitbe.hq.inventory.model.CircularMaterialPricePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 순환재고 소재 단가 정책 Repository
public interface CircularMaterialPricePolicyRepository extends JpaRepository<CircularMaterialPricePolicy, String> {
    // 활성 상태 소재 단가 정책을 소재 코드 순으로 조회한다.
    List<CircularMaterialPricePolicy> findAllByActiveTrueOrderByMaterialCodeAsc();
}
