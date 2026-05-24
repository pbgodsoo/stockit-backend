package org.example.stockitbe.hq.circularsale.repository;

import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CircularSaleItemRepository extends JpaRepository<CircularSaleItem, Long> {
    List<CircularSaleItem> findAllBySaleHeaderIdOrderByIdAsc(Long saleHeaderId);
    List<CircularSaleItem> findAllBySaleHeaderIdIn(Collection<Long> saleHeaderIds);
}

