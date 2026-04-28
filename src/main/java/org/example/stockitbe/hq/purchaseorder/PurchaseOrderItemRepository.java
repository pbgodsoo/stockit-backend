package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findAllByPurchaseOrderId(Long purchaseOrderId);

    List<PurchaseOrderItem> findAllByPurchaseOrderIdIn(Collection<Long> purchaseOrderIds);

    void deleteAllByPurchaseOrderId(Long purchaseOrderId);
}
