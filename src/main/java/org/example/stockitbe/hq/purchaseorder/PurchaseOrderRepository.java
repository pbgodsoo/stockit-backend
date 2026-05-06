package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>,
                                                  JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findByCode(String code);

    long countByCodeStartingWith(String prefix);

    // SYS-001 배치용
    List<PurchaseOrder> findAllByStatus(PurchaseOrderStatus status);

    List<PurchaseOrder> findAllByStatusAndUpdatedAtBefore(PurchaseOrderStatus status, Date cutoff);
}
