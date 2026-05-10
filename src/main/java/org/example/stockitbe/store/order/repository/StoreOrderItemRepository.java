package org.example.stockitbe.store.order.repository;

import org.example.stockitbe.store.order.model.entity.StoreOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoreOrderItemRepository extends JpaRepository<StoreOrderItem, Long> {
    List<StoreOrderItem> findAllByOrderHeaderIdOrderByIdAsc(Long orderHeaderId);
    List<StoreOrderItem> findAllByOrderHeaderIdIn(Collection<Long> orderHeaderIds);
    void deleteAllByOrderHeaderId(Long orderHeaderId);
}

