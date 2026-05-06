package org.example.stockitbe.warehouse.outbound;

import org.example.stockitbe.warehouse.outbound.model.OutboundSourceType;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WhOutboundHeaderRepository extends JpaRepository<WhOutboundHeader, Long> {
    Optional<WhOutboundHeader> findByOutboundNo(String outboundNo);
    Optional<WhOutboundHeader> findBySourceTypeAndSourceRefNo(OutboundSourceType sourceType, String sourceRefNo);
    List<WhOutboundHeader> findAllByWarehouseIdOrderByRequestedAtDescIdDesc(Long warehouseId);
}

