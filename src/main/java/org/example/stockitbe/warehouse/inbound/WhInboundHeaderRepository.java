package org.example.stockitbe.warehouse.inbound;

import org.example.stockitbe.warehouse.inbound.model.InboundStatus;
import org.example.stockitbe.warehouse.inbound.model.InboundType;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WhInboundHeaderRepository extends JpaRepository<WhInboundHeader, Long>,
        JpaSpecificationExecutor<WhInboundHeader> {

    Optional<WhInboundHeader> findByInboundCode(String inboundCode);

    // mirror 멱등성 — sourceRefNo + inboundType 으로 중복 INSERT 방지.
    Optional<WhInboundHeader> findBySourceRefNoAndInboundType(String sourceRefNo, InboundType inboundType);

    // 입고번호 시퀀스 다음값용. prefix 예: "IB-20260507-".
    Optional<WhInboundHeader> findTopByInboundCodeStartingWithOrderByInboundCodeDesc(String prefix);

    List<WhInboundHeader> findAllByWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId, Collection<InboundStatus> statuses);
}
