package org.example.stockitbe.hq.product;

import org.example.stockitbe.hq.product.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByCode(String code);
    List<Material> findAllByCodeIn(Collection<String> codes);
    List<Material> findAllByActiveTrueOrderByCodeAsc();
}
