package org.example.stockitbe.hq.product;

import org.example.stockitbe.hq.product.model.ProductMaster;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductMasterRepository extends JpaRepository<ProductMaster, Long> {
    @EntityGraph(attributePaths = {"materialCompositions", "materialCompositions.material"})
    Optional<ProductMaster> findByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndCodeNot(String name, String code);
    @EntityGraph(attributePaths = {"materialCompositions", "materialCompositions.material"})
    List<ProductMaster> findByNameContainingIgnoreCaseAndCategoryCodeContainingIgnoreCaseOrderByIdDesc(String keyword, String categoryCode);
    List<ProductMaster> findAllByOrderByIdDesc();
    @EntityGraph(attributePaths = {"materialCompositions", "materialCompositions.material"})
    List<ProductMaster> findAllByCodeIn(Collection<String> codes);
}
