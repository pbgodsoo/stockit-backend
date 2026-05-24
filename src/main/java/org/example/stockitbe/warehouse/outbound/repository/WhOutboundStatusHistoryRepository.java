package org.example.stockitbe.warehouse.outbound.repository;

import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhOutboundStatusHistoryRepository extends JpaRepository<WhOutboundStatusHistory, Long> {
    List<WhOutboundStatusHistory> findAllByOutboundHeaderIdOrderByChangedAtAscIdAsc(Long outboundHeaderId);
}

