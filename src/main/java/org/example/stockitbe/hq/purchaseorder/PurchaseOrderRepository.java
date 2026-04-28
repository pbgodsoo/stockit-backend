package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>,
                                                  JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findByCode(String code);

    long countByCodeStartingWith(String prefix);
}
