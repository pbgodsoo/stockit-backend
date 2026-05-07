package org.example.stockitbe.store.order;

import org.example.stockitbe.store.order.model.StoreOrderStatus;
import org.example.stockitbe.store.order.model.entity.StoreOrderHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface StoreOrderHeaderRepository extends JpaRepository<StoreOrderHeader, Long> {
    Optional<StoreOrderHeader> findByOrderNo(String orderNo);
    List<StoreOrderHeader> findAllByOrderByRequestedAtDescIdDesc();
    List<StoreOrderHeader> findAllByStatusOrderByRequestedAtAscIdAsc(StoreOrderStatus status);
    List<StoreOrderHeader> findAllByStatusAndStoreIdOrderByRequestedAtAscIdAsc(StoreOrderStatus status, Long storeId);
    List<StoreOrderHeader> findAllByStatusAndRequestedAtBetweenOrderByRequestedAtDescIdDesc(
            StoreOrderStatus status, Date from, Date to
    );

    @Query("""
            select h.storeId as storeId, count(h.id) as requestedCount
            from StoreOrderHeader h
            where h.status = :status
            group by h.storeId
            """)
    List<PendingStoreProjection> countPendingByStore(StoreOrderStatus status);

    interface PendingStoreProjection {
        Long getStoreId();
        Long getRequestedCount();
    }
}

