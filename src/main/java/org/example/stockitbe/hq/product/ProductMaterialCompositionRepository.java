package org.example.stockitbe.hq.product;

import org.example.stockitbe.hq.product.model.ProductMaterialComposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductMaterialCompositionRepository extends JpaRepository<ProductMaterialComposition, Long> {
    List<ProductMaterialComposition> findByProductMasterIdIn(Collection<Long> productIds);
}
