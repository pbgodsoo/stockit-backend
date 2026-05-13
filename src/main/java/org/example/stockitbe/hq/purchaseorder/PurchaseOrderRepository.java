package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>,
        JpaSpecificationExecutor<PurchaseOrder> {

    @EntityGraph(attributePaths = {"vendor", "warehouse"})
    Optional<PurchaseOrder> findByCode(String code);

    @Override
    @EntityGraph(attributePaths = {"vendor", "warehouse"})
    List<PurchaseOrder> findAll(Specification<PurchaseOrder> spec);

    @Override
    @EntityGraph(attributePaths = {"vendor", "warehouse"})
    Page<PurchaseOrder> findAll(Specification<PurchaseOrder> spec, Pageable pageable);

    long countByCodeStartingWith(String prefix);

    List<PurchaseOrder> findAllByStatus(PurchaseOrderStatus status);

    List<PurchaseOrder> findAllByStatusAndUpdatedAtBefore(PurchaseOrderStatus status, Date cutoff);

    // 통계 부분 정보 뽑아오기용 — 카테고리 필터(codePrefix) 추가
    @Query(value = """
    SELECT w.code, w.name,
           COUNT(DISTINCT po.id) AS orders_count,
           COUNT(DISTINCT poi.product_code) AS items_count,
           IFNULL(SUM(poi.subtotal), 0) AS total_value
    FROM purchase_order po
    JOIN purchase_order_item poi ON poi.purchase_order_id = po.id
    JOIN infrastructure w ON w.id = po.warehouse_id
    WHERE po.status <> 'CANCELLED'
      AND po.create_date >= :fromDt AND po.create_date < :toDt
      AND (:codePrefix IS NULL OR poi.product_code LIKE CONCAT(:codePrefix, '%'))
    GROUP BY w.code, w.name
    ORDER BY orders_count DESC
    """, nativeQuery = true)
    List<Object[]> aggregateByWarehouse(@Param("fromDt") Date fromDt,
                                        @Param("toDt") Date toDt,
                                        @Param("codePrefix") String codePrefix);

    @Query(value = """
    SELECT DATE_FORMAT(po.create_date, '%Y-%m') AS ym,
           COUNT(DISTINCT po.id) AS orders_count,
           COUNT(DISTINCT poi.product_code) AS items_count
    FROM purchase_order po
    JOIN purchase_order_item poi ON poi.purchase_order_id = po.id
    WHERE po.status <> 'CANCELLED'
      AND po.create_date >= :fromDt AND po.create_date < :toDt
      AND (:codePrefix IS NULL OR poi.product_code LIKE CONCAT(:codePrefix, '%'))
    GROUP BY ym
    ORDER BY ym
    """, nativeQuery = true)
    List<Object[]> monthlyTrend(@Param("fromDt") Date fromDt,
                                @Param("toDt") Date toDt,
                                @Param("codePrefix") String codePrefix);
}
