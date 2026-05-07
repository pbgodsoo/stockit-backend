package org.example.stockitbe.warehouse.inbound;

import org.example.stockitbe.warehouse.inbound.model.InboundType;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface WhInboundHeaderRepository extends JpaRepository<WhInboundHeader, Long>,
        JpaSpecificationExecutor<WhInboundHeader> {

    Optional<WhInboundHeader> findByInboundCode(String inboundCode);

    Optional<WhInboundHeader> findBySourceRefNoAndInboundType(String sourceRefNo, InboundType inboundType);

    Optional<WhInboundHeader> findTopByInboundCodeStartingWithOrderByInboundCodeDesc(String prefix);

    List<WhInboundHeader> findAllByWarehouseIdAndInboundTypeOrderByCreatedAtDesc(
            Long warehouseId, InboundType inboundType);

    List<WhInboundHeader> findAllByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
}
