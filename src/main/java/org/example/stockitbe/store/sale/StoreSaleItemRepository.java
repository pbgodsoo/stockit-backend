package org.example.stockitbe.store.sale;

import org.example.stockitbe.store.sale.model.entity.StoreSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoreSaleItemRepository extends JpaRepository<StoreSaleItem, Long> {
    List<StoreSaleItem> findAllBySaleHeaderIdOrderByIdAsc(Long saleHeaderId);
    List<StoreSaleItem> findAllBySaleHeaderIdIn(Collection<Long> saleHeaderIds);
}

