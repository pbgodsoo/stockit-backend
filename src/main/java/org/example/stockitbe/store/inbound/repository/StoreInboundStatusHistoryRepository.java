package org.example.stockitbe.store.inbound.repository;

import org.example.stockitbe.store.inbound.model.entity.StoreInboundStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreInboundStatusHistoryRepository extends JpaRepository<StoreInboundStatusHistory, Long> {
    List<StoreInboundStatusHistory> findAllByInboundHeaderIdOrderByChangedAtAscIdAsc(Long inboundHeaderId);
    boolean existsByInboundHeaderIdAndStatus(Long inboundHeaderId, org.example.stockitbe.store.inbound.model.StoreInboundStatus status);
}

