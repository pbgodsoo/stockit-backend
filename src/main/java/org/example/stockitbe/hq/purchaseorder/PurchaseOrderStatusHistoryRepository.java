package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderStatusHistoryRepository extends JpaRepository<PurchaseOrderStatusHistory, Long> {

    List<PurchaseOrderStatusHistory> findAllByPurchaseOrderIdOrderByChangedAtAsc(Long purchaseOrderId);
}
