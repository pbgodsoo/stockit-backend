package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    // Spring Data JPA 가 nested property `purchaseOrder.id` 로 자동 traversal.
    // 자식 row 삭제는 PurchaseOrder.replaceItems 의 orphanRemoval=true 가 자동 처리 (deleteAll* 직접 호출 폐기).
    List<PurchaseOrderItem> findAllByPurchaseOrderId(Long purchaseOrderId);

    List<PurchaseOrderItem> findAllByPurchaseOrderIdIn(Collection<Long> purchaseOrderIds);
}
