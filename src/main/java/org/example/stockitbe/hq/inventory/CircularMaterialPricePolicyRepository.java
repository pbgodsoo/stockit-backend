package org.example.stockitbe.hq.inventory;

import org.example.stockitbe.hq.inventory.model.CircularMaterialPricePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CircularMaterialPricePolicyRepository extends JpaRepository<CircularMaterialPricePolicy, String> {
    List<CircularMaterialPricePolicy> findAllByActiveTrueOrderByMaterialCodeAsc();
}
