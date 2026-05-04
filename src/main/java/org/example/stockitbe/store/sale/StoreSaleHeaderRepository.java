package org.example.stockitbe.store.sale;

import org.example.stockitbe.store.sale.model.StoreSaleHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreSaleHeaderRepository extends JpaRepository<StoreSaleHeader, Long> {
    Optional<StoreSaleHeader> findBySaleNo(String saleNo);
    List<StoreSaleHeader> findAllByOrderBySoldAtDescIdDesc();
}

