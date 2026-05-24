package org.example.stockitbe.store.inbound.repository;

import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreInboundHeaderRepository extends JpaRepository<StoreInboundHeader, Long> {
    Optional<StoreInboundHeader> findByInboundNo(String inboundNo);
    Optional<StoreInboundHeader> findByOutboundNo(String outboundNo);
    List<StoreInboundHeader> findAllBySourceRefNo(String sourceRefNo);
    List<StoreInboundHeader> findAllByStoreIdOrderByRequestedAtDescIdDesc(Long storeId);
    List<StoreInboundHeader> findAllByStoreIdAndStatusOrderByRequestedAtDescIdDesc(Long storeId, StoreInboundStatus status);
}

