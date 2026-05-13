package org.example.stockitbe.hq.inventory;

import jakarta.persistence.LockModeType;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Date;

// 재고 엔티티 조회/락 제어용 Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // 위치 기준 재고 목록 조회
    List<Inventory> findAllByLocationId(Long locationId);
    // SKU 목록 기준 재고 조회
    List<Inventory> findAllBySkuIdIn(Collection<Long> skuIds);
    // 상태 기준 재고 조회
    List<Inventory> findAllByInventoryStatus(InventoryStatus inventoryStatus);

    // 단건 재고를 비관적 락으로 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockById(Long id);

    // (sku, location, status) 단건 재고를 비관적 락으로 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockBySkuIdAndLocationIdAndInventoryStatus(Long skuId, Long locationId, InventoryStatus inventoryStatus);

    // (sku, location) 기준 단건/목록 조회
    Optional<Inventory> findBySkuIdAndLocationId(Long skuId, Long locationId);
    List<Inventory> findAllBySkuIdAndLocationId(Long skuId, Long locationId);

    // status 분리 저장 구조에서 특정 상태 행만 정확히 조회할 때 사용한다.
    Optional<Inventory> findBySkuIdAndLocationIdAndInventoryStatus(Long skuId, Long locationId, InventoryStatus inventoryStatus);

    // 매장 판매 시 동시 차감을 제어하기 위한 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.skuId = :skuId and i.locationId = :locationId")
    Optional<Inventory> findBySkuIdAndLocationIdForUpdate(@Param("skuId") Long skuId, @Param("locationId") Long locationId);


    // 통계용
    /**
     * 위치별 회전율 산출용 — 매장은 store_sale_header.store_id 직접 매칭,
     * 창고는 store_warehouse_map 으로 매핑된 매장들의 sale 합산.
     *
     * 반환 컬럼 (Object[] 인덱스):
     *   [0] code (String)              — infrastructure.code
     *   [1] name (String)              — infrastructure.name
     *   [2] location_type (String)     — "STORE" | "WAREHOUSE"
     *   [3] avg_inventory (Number)     — 위치별 inventory.quantity SUM (현재 재고)
     *   [4] sales_qty (Number)         — 기간 내 판매 수량 SUM
     *
     * @param scope        "ALL" | "STORE" | "WAREHOUSE"
     * @param locationCode null 이면 scope 그룹 적용, 값 있으면 그 위치만
     */
    @Query(value = """
    SELECT inf.code AS code,
           inf.name AS name,
           inf.location_type AS location_type,
           IFNULL(SUM(i.quantity), 0) AS avg_inventory,
           IFNULL(
             CASE
               WHEN inf.location_type = 'STORE' THEN (
                 SELECT IFNULL(SUM(ssi.quantity), 0)
                 FROM store_sale_header ssh
                 JOIN store_sale_item ssi ON ssi.sale_header_id = ssh.id
                 WHERE ssh.store_id = inf.id
                   AND ssh.sold_at >= :fromDt AND ssh.sold_at < :toDt
                   AND ssh.status = 'COMPLETED'
               )
               WHEN inf.location_type = 'WAREHOUSE' THEN (
                 SELECT IFNULL(SUM(ssi.quantity), 0)
                 FROM store_warehouse_map swm
                 JOIN store_sale_header ssh ON ssh.store_id = swm.store_id
                 JOIN store_sale_item ssi ON ssi.sale_header_id = ssh.id
                 WHERE swm.warehouse_id = inf.id
                   AND ssh.sold_at >= :fromDt AND ssh.sold_at < :toDt
                   AND ssh.status = 'COMPLETED'
               )
             END
           , 0) AS sales_qty
    FROM infrastructure inf
    LEFT JOIN inventory i ON i.location_id = inf.id
    WHERE inf.status = 'ACTIVE'
      AND ( :scope = 'ALL'
         OR (:scope = 'STORE' AND inf.location_type = 'STORE')
         OR (:scope = 'WAREHOUSE' AND inf.location_type = 'WAREHOUSE') )
      AND (:locationCode IS NULL OR inf.code = :locationCode)
    GROUP BY inf.id, inf.code, inf.name, inf.location_type
    ORDER BY sales_qty DESC
    """, nativeQuery = true)
    List<Object[]> aggregateLocationTurnover(@Param("fromDt") Date fromDt,
                                             @Param("toDt") Date toDt,
                                             @Param("scope") String scope,
                                             @Param("locationCode") String locationCode);

    @Query(value = """
    SELECT ps.sku_code AS sku_code,
           pm.name AS product_name,
           pm.category_code AS category_code,
           inf.code AS location_code,
           inf.name AS location_name,
           inf.location_type AS location_type,
           i.quantity AS units,
           ps.unit_price AS unit_price,
           IFNULL((
             SELECT IFNULL(SUM(ssi.quantity), 0)
             FROM store_sale_item ssi
             JOIN store_sale_header ssh ON ssh.id = ssi.sale_header_id
             WHERE ssi.sku_id = i.sku_id
               AND ssh.sold_at >= :fromDt AND ssh.sold_at < :toDt
               AND ssh.status = 'COMPLETED'
               AND (
                 (inf.location_type = 'STORE' AND ssh.store_id = inf.id)
                 OR (inf.location_type = 'WAREHOUSE' AND ssh.store_id IN (
                   SELECT swm.store_id FROM store_warehouse_map swm
                   WHERE swm.warehouse_id = inf.id
                 ))
               )
           ), 0) AS sales_qty
    FROM inventory i
    JOIN product_sku ps ON ps.id = i.sku_id
    JOIN product_master pm ON pm.code = ps.product_code
    JOIN infrastructure inf ON inf.id = i.location_id
    WHERE inf.status = 'ACTIVE'
      AND ( :scope = 'ALL'
         OR (:scope = 'STORE' AND inf.location_type = 'STORE')
         OR (:scope = 'WAREHOUSE' AND inf.location_type = 'WAREHOUSE') )
      AND (:locationCode IS NULL OR inf.code = :locationCode)
    """, nativeQuery = true)
    List<Object[]> skuTurnoverList(@Param("fromDt") Date fromDt,
                                   @Param("toDt") Date toDt,
                                   @Param("scope") String scope,
                                   @Param("locationCode") String locationCode);
}
