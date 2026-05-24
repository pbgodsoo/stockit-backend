package org.example.stockitbe.store.inbound.repository;

import org.example.stockitbe.store.inbound.model.entity.StoreInboundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoreInboundItemRepository extends JpaRepository<StoreInboundItem, Long> {
    List<StoreInboundItem> findAllByInboundHeaderIdOrderByIdAsc(Long inboundHeaderId);
    List<StoreInboundItem> findAllByInboundHeaderIdIn(Collection<Long> inboundHeaderIds);
}

