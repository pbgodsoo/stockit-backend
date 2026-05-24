package org.example.stockitbe.warehouse.inbound;

import org.example.stockitbe.warehouse.inbound.model.InboundType;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface WhInboundHeaderRepository extends JpaRepository<WhInboundHeader, Long>,
        JpaSpecificationExecutor<WhInboundHeader> {

    @EntityGraph(attributePaths = "warehouse")
    Optional<WhInboundHeader> findByInboundCode(String inboundCode);

    Optional<WhInboundHeader> findBySourceRefNoAndInboundType(String sourceRefNo, InboundType inboundType);

    Optional<WhInboundHeader> findTopByInboundCodeStartingWithOrderByInboundCodeDesc(String prefix);

    // items 는 OneToMany 라 카타시안 회피로 @EntityGraph 미포함 — Service 가 batch 별도 fetch.
    @EntityGraph(attributePaths = "warehouse")
    List<WhInboundHeader> findAllByWarehouseIdAndInboundTypeOrderByCreatedAtDesc(
            Long warehouseId, InboundType inboundType);

    @EntityGraph(attributePaths = "warehouse")
    List<WhInboundHeader> findAllByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
}
