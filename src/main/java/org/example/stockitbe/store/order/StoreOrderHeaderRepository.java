package org.example.stockitbe.store.order;

import org.example.stockitbe.store.order.model.StoreOrderStatus;
import org.example.stockitbe.store.order.model.entity.StoreOrderHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface StoreOrderHeaderRepository extends JpaRepository<StoreOrderHeader, Long> {
    Optional<StoreOrderHeader> findByOrderNo(String orderNo);
    List<StoreOrderHeader> findAllByOrderByRequestedAtDescIdDesc();
    List<StoreOrderHeader> findAllByStatusAndRequestedAtBetweenOrderByRequestedAtDescIdDesc(
            StoreOrderStatus status, Date from, Date to
    );
}

