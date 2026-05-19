package org.example.stockitbe.hq.circularsale.repository;

import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CircularSaleStatusHistoryRepository extends JpaRepository<CircularSaleStatusHistory, Long> {
    List<CircularSaleStatusHistory> findAllBySaleHeaderIdOrderByChangedAtAscIdAsc(Long saleHeaderId);
}

