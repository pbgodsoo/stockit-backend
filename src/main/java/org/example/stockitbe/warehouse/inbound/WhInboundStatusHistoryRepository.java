package org.example.stockitbe.warehouse.inbound;

import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhInboundStatusHistoryRepository extends JpaRepository<WhInboundStatusHistory, Long> {

    List<WhInboundStatusHistory> findAllByInboundHeaderIdOrderByChangedAtAsc(Long inboundHeaderId);
}
