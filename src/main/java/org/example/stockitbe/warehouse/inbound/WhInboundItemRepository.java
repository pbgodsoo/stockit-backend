package org.example.stockitbe.warehouse.inbound;

import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WhInboundItemRepository extends JpaRepository<WhInboundItem, Long> {

    // N+1 회피용 batch 조회.
    List<WhInboundItem> findAllByInboundHeaderIdInOrderByIdAsc(Collection<Long> inboundHeaderIds);

    List<WhInboundItem> findAllByInboundHeaderIdOrderByIdAsc(Long inboundHeaderId);
}
