package org.example.stockitbe.hq.circularsale.repository;

import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleItemMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CircularSaleItemMaterialRepository extends JpaRepository<CircularSaleItemMaterial, Long> {
    List<CircularSaleItemMaterial> findAllBySaleItemIdInOrderBySortOrderAscIdAsc(Collection<Long> saleItemIds);
}

