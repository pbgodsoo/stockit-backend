package org.example.stockitbe.hq.product;

import org.example.stockitbe.hq.product.model.ProductMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductMasterRepository extends JpaRepository<ProductMaster, Long> {
    Optional<ProductMaster> findByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndCodeNot(String name, String code);
    List<ProductMaster> findByNameContainingIgnoreCaseAndCategoryCodeContainingIgnoreCaseOrderByIdDesc(String keyword, String categoryCode);
    List<ProductMaster> findAllByOrderByIdDesc();
}
