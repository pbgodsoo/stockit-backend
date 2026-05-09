package org.example.stockitbe.warehouse.outbound;

import org.example.stockitbe.warehouse.outbound.model.OutboundSourceType;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WhOutboundHeaderRepository extends JpaRepository<WhOutboundHeader, Long> {
    Optional<WhOutboundHeader> findByOutboundNo(String outboundNo);
    Optional<WhOutboundHeader> findBySourceTypeAndSourceRefNoAndSourceRefSeq(
            OutboundSourceType sourceType, String sourceRefNo, Integer sourceRefSeq
    );
    List<WhOutboundHeader> findAllBySourceTypeAndSourceRefNoOrderBySourceRefSeqAsc(
            OutboundSourceType sourceType, String sourceRefNo
    );

    @Query("""
            select coalesce(max(h.sourceRefSeq), 0)
            from WhOutboundHeader h
            where h.sourceType = :sourceType and h.sourceRefNo = :sourceRefNo
            """)
    Integer findMaxSourceRefSeq(
            @Param("sourceType") OutboundSourceType sourceType,
            @Param("sourceRefNo") String sourceRefNo
    );

    List<WhOutboundHeader> findAllByWarehouseIdOrderByRequestedAtDescIdDesc(Long warehouseId);
}

