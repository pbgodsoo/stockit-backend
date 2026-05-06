package org.example.stockitbe.store.order;

import org.example.stockitbe.store.order.model.entity.StoreOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreOrderStatusHistoryRepository extends JpaRepository<StoreOrderStatusHistory, Long> {
    List<StoreOrderStatusHistory> findAllByOrderHeaderIdOrderByChangedAtAscIdAsc(Long orderHeaderId);
}

