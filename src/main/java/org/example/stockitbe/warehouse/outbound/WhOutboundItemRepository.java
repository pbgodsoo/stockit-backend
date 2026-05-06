package org.example.stockitbe.warehouse.outbound;

import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WhOutboundItemRepository extends JpaRepository<WhOutboundItem, Long> {
    List<WhOutboundItem> findAllByOutboundHeaderIdOrderByIdAsc(Long outboundHeaderId);
    List<WhOutboundItem> findAllByOutboundHeaderIdIn(Collection<Long> outboundHeaderIds);
}

