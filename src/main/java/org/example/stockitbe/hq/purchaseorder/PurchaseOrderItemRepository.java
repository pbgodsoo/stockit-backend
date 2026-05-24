package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    // Spring Data JPA 가 nested property `purchaseOrder.id` 로 자동 traversal.
    // 자식 row 삭제는 PurchaseOrder.replaceItems 의 orphanRemoval=true 가 자동 처리 (deleteAll* 직접 호출 폐기).
    List<PurchaseOrderItem> findAllByPurchaseOrderId(Long purchaseOrderId);

    List<PurchaseOrderItem> findAllByPurchaseOrderIdIn(Collection<Long> purchaseOrderIds);

    // 통계용
    @Query(value = """
    SELECT SUBSTRING(poi.product_code, 1, 10) AS l2_prefix,
           poi.product_code,
           poi.product_name,
           DATE(po.create_date) AS order_date,
           poi.quantity,
           po.id AS po_id
    FROM purchase_order_item poi
    JOIN purchase_order po ON po.id = poi.purchase_order_id
    WHERE po.status <> 'CANCELLED'
      AND po.create_date >= :fromDt AND po.create_date < :toDt
    ORDER BY l2_prefix, poi.product_code, order_date
    """, nativeQuery = true)
    List<Object[]> findItemOrderHistory(@Param("fromDt") Date fromDt, @Param("toDt") Date toDt);
}
