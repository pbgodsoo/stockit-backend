package org.example.stockitbe.hq.inventory;

import jakarta.persistence.LockModeType;
import org.example.stockitbe.hq.inventory.model.CompanyWideAggregateRow;
import org.example.stockitbe.hq.inventory.model.CircularInventoryCompositionRow;
import org.example.stockitbe.hq.inventory.model.CircularInventoryPageRow;
import org.example.stockitbe.hq.inventory.model.CompanyWideSkuRow;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.inventory.model.ItemSafetyStockRow;
import org.example.stockitbe.store.inventory.model.StoreItemRow;
import org.example.stockitbe.store.inventory.model.StoreSkuRow;
import org.example.stockitbe.warehouse.inventory.model.WarehouseAggregateRow;
import org.example.stockitbe.warehouse.inventory.model.WarehouseSkuRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 전사 재고(품목 단위) 집계 페이지네이션.
     * GROUP BY product_master.code 로 itemCode 단위 합산 후 LIMIT/OFFSET.
     * safetyStock + UI status 라벨은 호출자(서비스) 가 페이지 후 메모리에서 계산.
     *
     * locationIds 가 비어있을 때 IN 절 SQL 에러를 피하기 위해 hasLocationIds 플래그 + 더미 리스트 패턴.
     */
    @Query(value = """
        SELECT
            pm.code AS itemCode,
            pm.name AS itemName,
            COALESCE(pc.name, cc.name) AS parentCategory,
            cc.name AS childCategory,
            COALESCE(SUM(i.quantity), 0) AS actualStock,
            COALESCE(SUM(i.available_quantity), 0) AS availableStock,
            MAX(i.update_date) AS updatedAt
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        JOIN infrastructure inf ON inf.id = i.location_id
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:locationType IS NULL OR inf.location_type = :locationType)
          AND (:hasLocationIds = false OR inf.id IN (:locationIds))
          AND (:status IS NULL OR i.inventory_status = :status)
          AND (:parentCategory IS NULL OR COALESCE(pc.name, cc.name) = :parentCategory)
          AND (:childCategory IS NULL OR cc.name = :childCategory)
          AND (:parentCategory IS NULL OR pc.name = :parentCategory)
          AND (:childCategory IS NULL OR cc.name = :childCategory)
          AND (:keyword IS NULL OR (
               LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
        GROUP BY pm.code, pm.name, cc.name, pc.name
        ORDER BY pm.code ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
          SELECT pm.code
          FROM inventory i
          JOIN product_sku ps ON ps.id = i.sku_id
          JOIN product_master pm ON pm.code = ps.product_code
          JOIN infrastructure inf ON inf.id = i.location_id
          LEFT JOIN category cc ON cc.code = pm.category_code
          LEFT JOIN category pc ON pc.id = cc.parent_id
          WHERE i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
            AND (:locationType IS NULL OR inf.location_type = :locationType)
            AND (:hasLocationIds = false OR inf.id IN (:locationIds))
            AND (:status IS NULL OR i.inventory_status = :status)
            AND (:parentCategory IS NULL OR COALESCE(pc.name, cc.name) = :parentCategory)
            AND (:childCategory IS NULL OR cc.name = :childCategory)
            AND (:parentCategory IS NULL OR pc.name = :parentCategory)
          AND (:childCategory IS NULL OR cc.name = :childCategory)
            AND (:keyword IS NULL OR (
                 LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
              OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              OR LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
              OR LOWER(inf.code) LIKE CONCAT('%', LOWER(:keyword), '%')
              OR LOWER(inf.name) LIKE CONCAT('%', LOWER(:keyword), '%')
            ))
          GROUP BY pm.code
        ) sub
        """,
        nativeQuery = true)
    Page<CompanyWideAggregateRow> findCompanyWideAggregated(
            @Param("locationType") String locationType,
            @Param("hasLocationIds") boolean hasLocationIds,
            @Param("locationIds") List<Long> locationIds,
            @Param("status") String status,
            @Param("parentCategory") String parentCategory,
            @Param("childCategory") String childCategory,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 전사 재고 SKU 단위 페이지네이션 (모드 토글 SKU 모드용 — 마스터 무관 모든 SKU 한 표).
     * GROUP BY ps.id 로 SKU 단위 합산. status 라벨(품절/부족/정상) 은 SQL CASE 로 계산해 HAVING 으로 필터.
     * safetyStock = row 단위 SUM (location_type 별 store/warehouse safety_stock) — 같은 location 의
     *   NORMAL+CIRCULAR_CANDIDATE 두 row 가 있으면 비례적으로 곱해지지만 available 도 같이 합산되어
     *   status 라벨 결과는 일관. 마스터(`findCompanyWideAggregated`) 와 동일한 단순화 패턴.
     * 카테고리 단일 파라미터: 부모 또는 자식 한글 이름 매칭 (FE CategoryFilter 호환).
     */
    @Query(value = """
        SELECT
            ps.sku_code AS skuCode,
            pm.code AS itemCode,
            pm.name AS itemName,
            COALESCE(pc.name, cc.name) AS parentCategory,
            cc.name AS childCategory,
            ps.color AS color,
            ps.size AS size,
            COALESCE(SUM(i.quantity), 0) AS actualStock,
            COALESCE(SUM(i.available_quantity), 0) AS availableStock,
            COALESCE(SUM(
                CASE WHEN inf.location_type = 'WAREHOUSE'
                     THEN COALESCE(pm.warehouse_safety_stock, 0)
                     ELSE COALESCE(pm.store_safety_stock, 0)
                END
            ), 0) AS safetyStock,
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) < COALESCE(SUM(
                    CASE WHEN inf.location_type = 'WAREHOUSE'
                         THEN COALESCE(pm.warehouse_safety_stock, 0)
                         ELSE COALESCE(pm.store_safety_stock, 0)
                    END
                ), 0) THEN '부족'
                ELSE '정상'
            END AS status
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        JOIN infrastructure inf ON inf.id = i.location_id
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:locationType IS NULL OR inf.location_type = :locationType)
          AND (:hasLocationIds = false OR inf.id IN (:locationIds))
          AND (:parentCategory IS NULL OR pc.name = :parentCategory)
          AND (:childCategory IS NULL OR cc.name = :childCategory)
          AND (:color IS NULL OR ps.color = :color)
          AND (:skuSize IS NULL OR ps.size = :skuSize)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
        GROUP BY ps.id, ps.sku_code, pm.code, pm.name, cc.name, pc.name, ps.color, ps.size
        ORDER BY ps.sku_code ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
            SELECT ps.id
            FROM inventory i
            JOIN product_sku ps ON ps.id = i.sku_id
            JOIN product_master pm ON pm.code = ps.product_code
            JOIN infrastructure inf ON inf.id = i.location_id
            LEFT JOIN category cc ON cc.code = pm.category_code
            LEFT JOIN category pc ON pc.id = cc.parent_id
            WHERE i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
              AND (:locationType IS NULL OR inf.location_type = :locationType)
              AND (:hasLocationIds = false OR inf.id IN (:locationIds))
              AND (:parentCategory IS NULL OR pc.name = :parentCategory)
          AND (:childCategory IS NULL OR cc.name = :childCategory)
              AND (:color IS NULL OR ps.color = :color)
              AND (:skuSize IS NULL OR ps.size = :skuSize)
              AND (:keyword IS NULL OR (
                   LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            GROUP BY ps.id
        ) sub
        """,
        nativeQuery = true)
    Page<CompanyWideSkuRow> findCompanyWideSkus(
            @Param("locationType") String locationType,
            @Param("hasLocationIds") boolean hasLocationIds,
            @Param("locationIds") List<Long> locationIds,
            @Param("parentCategory") String parentCategory,
            @Param("childCategory") String childCategory,
            @Param("color") String color,
            @Param("skuSize") String skuSize,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 전사 재고 SKU 칩 필터용 distinct 색상 — facets endpoint.
     * 같은 거점/카테고리/검색 필터 조건 안에서 가능한 색상 목록 반환.
     */
    @Query(value = """
        SELECT DISTINCT ps.color
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        JOIN infrastructure inf ON inf.id = i.location_id
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:locationType IS NULL OR inf.location_type = :locationType)
          AND (:hasLocationIds = false OR inf.id IN (:locationIds))
          AND (:parentCategory IS NULL OR pc.name = :parentCategory)
          AND (:childCategory IS NULL OR cc.name = :childCategory)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
          AND ps.color IS NOT NULL
        ORDER BY ps.color ASC
        """, nativeQuery = true)
    List<String> findCompanyWideSkuColors(
            @Param("locationType") String locationType,
            @Param("hasLocationIds") boolean hasLocationIds,
            @Param("locationIds") List<Long> locationIds,
            @Param("parentCategory") String parentCategory,
            @Param("childCategory") String childCategory,
            @Param("keyword") String keyword
    );

    /**
     * 전사 재고 SKU 칩 필터용 distinct 사이즈 — facets endpoint.
     */
    @Query(value = """
        SELECT DISTINCT ps.size
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        JOIN infrastructure inf ON inf.id = i.location_id
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:locationType IS NULL OR inf.location_type = :locationType)
          AND (:hasLocationIds = false OR inf.id IN (:locationIds))
          AND (:parentCategory IS NULL OR pc.name = :parentCategory)
          AND (:childCategory IS NULL OR cc.name = :childCategory)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
          AND ps.size IS NOT NULL
        ORDER BY ps.size ASC
        """, nativeQuery = true)
    List<String> findCompanyWideSkuSizes(
            @Param("locationType") String locationType,
            @Param("hasLocationIds") boolean hasLocationIds,
            @Param("locationIds") List<Long> locationIds,
            @Param("parentCategory") String parentCategory,
            @Param("childCategory") String childCategory,
            @Param("keyword") String keyword
    );

    /**
     * 전사 재고 페이지 후 itemCode 별 safetyStock 집계.
     * (sku, location) DISTINCT 별로 location_type 따라 store/warehouse safety_stock 합산.
     * 메인 페이지 query 와 같은 outer 필터 (locationType, locationIds, status) 적용.
     */
    @Query(value = """
        SELECT sub.code AS itemCode, COALESCE(SUM(sub.safety_per), 0) AS safetyStock
        FROM (
          SELECT DISTINCT pm.code AS code, i.sku_id, i.location_id,
                 CASE WHEN inf.location_type = 'WAREHOUSE'
                      THEN COALESCE(pm.warehouse_safety_stock, 0)
                      ELSE COALESCE(pm.store_safety_stock, 0)
                 END AS safety_per
          FROM inventory i
          JOIN product_sku ps ON ps.id = i.sku_id
          JOIN product_master pm ON pm.code = ps.product_code
          JOIN infrastructure inf ON inf.id = i.location_id
          WHERE pm.code IN (:itemCodes)
            AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
            AND (:locationType IS NULL OR inf.location_type = :locationType)
            AND (:hasLocationIds = false OR inf.id IN (:locationIds))
            AND (:status IS NULL OR i.inventory_status = :status)
        ) sub
        GROUP BY sub.code
        """,
        nativeQuery = true)
    List<ItemSafetyStockRow> aggregateSafetyStockByItemCode(
            @Param("itemCodes") List<String> itemCodes,
            @Param("locationType") String locationType,
            @Param("hasLocationIds") boolean hasLocationIds,
            @Param("locationIds") List<Long> locationIds,
            @Param("status") String status
    );

    /**
     * 창고 재고(품목 단위) 집계 페이지네이션.
     * 단일 창고(locationId) 기준, GROUP BY product_master.code.
     * safetyStock = COUNT(DISTINCT sku_id) * pm.warehouse_safety_stock 으로 SQL 단계 계산.
     * status UI 라벨(품절/부족/정상) 도 SQL CASE 로 계산해 HAVING 으로 필터.
     * category 단일 파라미터: 부모/자식 한글 이름 매칭 (FE 호환). parentCategory/childCategory 와 공존.
     * 정렬 = 메인 카테고리 우선순위 (상의/바지/치마/아우터) + childCategory + itemName.
     * updatedAt 응답 X (운영 추적용 DB 컬럼은 유지).
     */
    @Query(value = """
        SELECT
            pm.code AS itemCode,
            pm.name AS itemName,
            COALESCE(pc.name, cc.name) AS parentCategory,
            cc.name AS childCategory,
            COALESCE(SUM(i.quantity), 0) AS actualStock,
            COALESCE(SUM(i.available_quantity), 0) AS availableStock,
            (COUNT(DISTINCT i.sku_id) * COALESCE(pm.warehouse_safety_stock, 0)) AS safetyStock,
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) <
                     (COUNT(DISTINCT i.sku_id) * COALESCE(pm.warehouse_safety_stock, 0))
                  THEN '부족'
                ELSE '정상'
            END AS status
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.location_id = :locationId
          AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:parentCategory IS NULL OR COALESCE(pc.name, cc.name) = :parentCategory)
          AND (:childCategory IS NULL OR cc.name = :childCategory)
          AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
          AND (:keyword IS NULL OR (
               LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
        GROUP BY pm.code, pm.name, pm.warehouse_safety_stock, cc.name, pc.name
        HAVING (:status IS NULL OR (
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) <
                     (COUNT(DISTINCT i.sku_id) * COALESCE(pm.warehouse_safety_stock, 0))
                  THEN '부족'
                ELSE '정상'
            END
        ) = :status)
        ORDER BY
            CASE COALESCE(pc.name, cc.name)
                WHEN '상의' THEN 1
                WHEN '바지' THEN 2
                WHEN '치마' THEN 3
                WHEN '아우터' THEN 4
                ELSE 5
            END ASC,
            cc.name ASC,
            pm.name ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
          SELECT pm.code
          FROM inventory i
          JOIN product_sku ps ON ps.id = i.sku_id
          JOIN product_master pm ON pm.code = ps.product_code
          LEFT JOIN category cc ON cc.code = pm.category_code
          LEFT JOIN category pc ON pc.id = cc.parent_id
          WHERE i.location_id = :locationId
            AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
            AND (:parentCategory IS NULL OR COALESCE(pc.name, cc.name) = :parentCategory)
            AND (:childCategory IS NULL OR cc.name = :childCategory)
            AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
            AND (:keyword IS NULL OR (
                 LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
              OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              OR LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            ))
          GROUP BY pm.code, pm.warehouse_safety_stock
          HAVING (:status IS NULL OR (
              CASE
                  WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                  WHEN COALESCE(SUM(i.available_quantity), 0) <
                       (COUNT(DISTINCT i.sku_id) * COALESCE(pm.warehouse_safety_stock, 0))
                    THEN '부족'
                  ELSE '정상'
              END
          ) = :status)
        ) sub
        """,
        nativeQuery = true)
    Page<WarehouseAggregateRow> findWarehouseAggregated(
            @Param("locationId") Long locationId,
            @Param("parentCategory") String parentCategory,
            @Param("childCategory") String childCategory,
            @Param("category") String category,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 창고 재고 SKU 단위 페이지네이션 (모드 토글 SKU 모드용 — 마스터 무관 모든 SKU 한 표).
     * 단일 창고(locationId) 기준, GROUP BY ps.id.
     * safetyStock = pm.warehouse_safety_stock (창고 단일이라 row 단위 SUM 불필요 — sku 마다 1 row).
     * status UI 라벨(품절/부족/정상) 은 SQL CASE 로 계산해 HAVING 으로 필터.
     * 카테고리 단일 파라미터: 부모 또는 자식 한글 이름 매칭 (FE CategoryFilter 호환).
     */
    @Query(value = """
        SELECT
            ps.sku_code AS skuCode,
            pm.code AS itemCode,
            pm.name AS itemName,
            COALESCE(pc.name, cc.name) AS parentCategory,
            cc.name AS childCategory,
            ps.color AS color,
            ps.size AS size,
            COALESCE(SUM(i.quantity), 0) AS actualStock,
            COALESCE(SUM(i.available_quantity), 0) AS availableStock,
            COALESCE(pm.warehouse_safety_stock, 0) AS safetyStock,
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) < COALESCE(pm.warehouse_safety_stock, 0) THEN '부족'
                ELSE '정상'
            END AS status
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.location_id = :locationId
          AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
          AND (:color IS NULL OR ps.color = :color)
          AND (:skuSize IS NULL OR ps.size = :skuSize)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
        GROUP BY ps.id, ps.sku_code, pm.code, pm.name, pm.warehouse_safety_stock, cc.name, pc.name, ps.color, ps.size
        HAVING (:status IS NULL OR (
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) < COALESCE(pm.warehouse_safety_stock, 0) THEN '부족'
                ELSE '정상'
            END
        ) = :status)
        ORDER BY ps.sku_code ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
            SELECT ps.id
            FROM inventory i
            JOIN product_sku ps ON ps.id = i.sku_id
            JOIN product_master pm ON pm.code = ps.product_code
            LEFT JOIN category cc ON cc.code = pm.category_code
            LEFT JOIN category pc ON pc.id = cc.parent_id
            WHERE i.location_id = :locationId
              AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
              AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
              AND (:color IS NULL OR ps.color = :color)
              AND (:skuSize IS NULL OR ps.size = :skuSize)
              AND (:keyword IS NULL OR (
                   LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            GROUP BY ps.id, pm.warehouse_safety_stock
            HAVING (:status IS NULL OR (
                CASE
                    WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                    WHEN COALESCE(SUM(i.available_quantity), 0) < COALESCE(pm.warehouse_safety_stock, 0) THEN '부족'
                    ELSE '정상'
                END
            ) = :status)
        ) sub
        """,
        nativeQuery = true)
    Page<WarehouseSkuRow> findWarehouseSkus(
            @Param("locationId") Long locationId,
            @Param("category") String category,
            @Param("status") String status,
            @Param("color") String color,
            @Param("skuSize") String skuSize,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 창고 재고 SKU 칩 필터용 distinct 색상 — facets endpoint.
     * 같은 거점/카테고리/검색 필터 조건 안에서 가능한 색상 목록 반환.
     */
    @Query(value = """
        SELECT DISTINCT ps.color
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.location_id = :locationId
          AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
          AND ps.color IS NOT NULL
        ORDER BY ps.color ASC
        """, nativeQuery = true)
    List<String> findWarehouseSkuColors(
            @Param("locationId") Long locationId,
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    /**
     * 창고 재고 SKU 칩 필터용 distinct 사이즈 — facets endpoint.
     */
    @Query(value = """
        SELECT DISTINCT ps.size
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.location_id = :locationId
          AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
          AND ps.size IS NOT NULL
        ORDER BY ps.size ASC
        """, nativeQuery = true)
    List<String> findWarehouseSkuSizes(
            @Param("locationId") Long locationId,
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    /**
     * 매장 재고(품목 단위) 집계 페이지네이션.
     * 단일 매장(locationId) 기준, GROUP BY product_master.code.
     * safetyStock = COUNT(DISTINCT sku_id) * pm.store_safety_stock 으로 SQL 단계 계산 (창고 패턴 차용 — pm.warehouse_safety_stock 만 store_safety_stock 으로 교체).
     * status UI 라벨(품절/부족/정상) 도 SQL CASE 로 계산해 HAVING 으로 필터.
     * category 단일 파라미터: 부모/자식 한글 이름 매칭 (FE 호환).
     * 정렬 = 메인 카테고리 우선순위 (상의/바지/치마/아우터) + childCategory + itemName.
     * updatedAt 응답 X (운영 추적용 DB 컬럼은 유지).
     */
    @Query(value = """
        SELECT
            pm.code AS itemCode,
            pm.name AS itemName,
            COALESCE(pc.name, cc.name) AS parentCategory,
            cc.name AS childCategory,
            COALESCE(SUM(i.quantity), 0) AS actualStock,
            COALESCE(SUM(i.available_quantity), 0) AS availableStock,
            (COUNT(DISTINCT i.sku_id) * COALESCE(pm.store_safety_stock, 0)) AS safetyStock,
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) <
                     (COUNT(DISTINCT i.sku_id) * COALESCE(pm.store_safety_stock, 0))
                  THEN '부족'
                ELSE '정상'
            END AS status
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.location_id = :locationId
          AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
          AND (:keyword IS NULL OR (
               LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
        GROUP BY pm.code, pm.name, pm.store_safety_stock, cc.name, pc.name
        HAVING (:status IS NULL OR (
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) <
                     (COUNT(DISTINCT i.sku_id) * COALESCE(pm.store_safety_stock, 0))
                  THEN '부족'
                ELSE '정상'
            END
        ) = :status)
        ORDER BY
            CASE COALESCE(pc.name, cc.name)
                WHEN '상의' THEN 1
                WHEN '바지' THEN 2
                WHEN '치마' THEN 3
                WHEN '아우터' THEN 4
                ELSE 5
            END ASC,
            cc.name ASC,
            pm.name ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
          SELECT pm.code
          FROM inventory i
          JOIN product_sku ps ON ps.id = i.sku_id
          JOIN product_master pm ON pm.code = ps.product_code
          LEFT JOIN category cc ON cc.code = pm.category_code
          LEFT JOIN category pc ON pc.id = cc.parent_id
          WHERE i.location_id = :locationId
            AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
            AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
            AND (:keyword IS NULL OR (
                 LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
              OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              OR LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            ))
          GROUP BY pm.code, pm.store_safety_stock
          HAVING (:status IS NULL OR (
              CASE
                  WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                  WHEN COALESCE(SUM(i.available_quantity), 0) <
                       (COUNT(DISTINCT i.sku_id) * COALESCE(pm.store_safety_stock, 0))
                    THEN '부족'
                  ELSE '정상'
              END
          ) = :status)
        ) sub
        """,
        nativeQuery = true)
    Page<StoreItemRow> findStoreAggregated(
            @Param("locationId") Long locationId,
            @Param("category") String category,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 매장 재고 SKU 단위 페이지네이션 (모드 토글 SKU 모드용 — 마스터 무관 모든 SKU 한 표).
     * 단일 매장(locationId) 기준, GROUP BY ps.id.
     * safetyStock = pm.store_safety_stock (매장 단일이라 row 단위 SUM 불필요 — sku 마다 1 row).
     * status UI 라벨(품절/부족/정상) 은 SQL CASE 로 계산해 HAVING 으로 필터.
     * 카테고리 단일 파라미터: 부모 또는 자식 한글 이름 매칭 (FE CategoryFilter 호환).
     * skuSize 파라미터명 — Spring Pageable size 와 충돌 방지.
     */
    @Query(value = """
        SELECT
            ps.sku_code AS skuCode,
            pm.code AS itemCode,
            pm.name AS itemName,
            COALESCE(pc.name, cc.name) AS parentCategory,
            cc.name AS childCategory,
            ps.color AS color,
            ps.size AS size,
            COALESCE(SUM(i.quantity), 0) AS actualStock,
            COALESCE(SUM(i.available_quantity), 0) AS availableStock,
            COALESCE(pm.store_safety_stock, 0) AS safetyStock,
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) < COALESCE(pm.store_safety_stock, 0) THEN '부족'
                ELSE '정상'
            END AS status
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.location_id = :locationId
          AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
          AND (:color IS NULL OR ps.color = :color)
          AND (:skuSize IS NULL OR ps.size = :skuSize)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
        GROUP BY ps.id, ps.sku_code, pm.code, pm.name, pm.store_safety_stock, cc.name, pc.name, ps.color, ps.size
        HAVING (:status IS NULL OR (
            CASE
                WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                WHEN COALESCE(SUM(i.available_quantity), 0) < COALESCE(pm.store_safety_stock, 0) THEN '부족'
                ELSE '정상'
            END
        ) = :status)
        ORDER BY ps.sku_code ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
            SELECT ps.id
            FROM inventory i
            JOIN product_sku ps ON ps.id = i.sku_id
            JOIN product_master pm ON pm.code = ps.product_code
            LEFT JOIN category cc ON cc.code = pm.category_code
            LEFT JOIN category pc ON pc.id = cc.parent_id
            WHERE i.location_id = :locationId
              AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
              AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
              AND (:color IS NULL OR ps.color = :color)
              AND (:skuSize IS NULL OR ps.size = :skuSize)
              AND (:keyword IS NULL OR (
                   LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            GROUP BY ps.id, pm.store_safety_stock
            HAVING (:status IS NULL OR (
                CASE
                    WHEN COALESCE(SUM(i.available_quantity), 0) <= 0 THEN '품절'
                    WHEN COALESCE(SUM(i.available_quantity), 0) < COALESCE(pm.store_safety_stock, 0) THEN '부족'
                    ELSE '정상'
                END
            ) = :status)
        ) sub
        """,
        nativeQuery = true)
    Page<StoreSkuRow> findStoreSkus(
            @Param("locationId") Long locationId,
            @Param("category") String category,
            @Param("status") String status,
            @Param("color") String color,
            @Param("skuSize") String skuSize,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 매장 재고 SKU 칩 필터용 distinct 색상 — facets endpoint.
     * 같은 거점/카테고리/검색 필터 조건 안에서 가능한 색상 목록 반환.
     */
    @Query(value = """
        SELECT DISTINCT ps.color
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.location_id = :locationId
          AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
          AND ps.color IS NOT NULL
        ORDER BY ps.color ASC
        """, nativeQuery = true)
    List<String> findStoreSkuColors(
            @Param("locationId") Long locationId,
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    /**
     * 매장 재고 SKU 칩 필터용 distinct 사이즈 — facets endpoint.
     */
    @Query(value = """
        SELECT DISTINCT ps.size
        FROM inventory i
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.location_id = :locationId
          AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
          AND (:category IS NULL OR pc.name = :category OR cc.name = :category)
          AND (:keyword IS NULL OR (
               LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
          ))
          AND ps.size IS NOT NULL
        ORDER BY ps.size ASC
        """, nativeQuery = true)
    List<String> findStoreSkuSizes(
            @Param("locationId") Long locationId,
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    @Query(value = """
        SELECT
            i.id AS inventoryId,
            ps.sku_code AS skuCode,
            pm.code AS itemCode,
            pm.name AS itemName,
            inf.code AS warehouseCode,
            inf.name AS warehouseName,
            COALESCE(pc.name, cc.name) AS parentCategory,
            cc.name AS childCategory,
            ps.color AS color,
            ps.size AS size,
            COALESCE(i.available_quantity, 0) AS availableQuantity
        FROM inventory i
        JOIN infrastructure inf ON inf.id = i.location_id
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        WHERE i.inventory_status = 'CIRCULAR'
          AND inf.location_type = 'WAREHOUSE'
          AND COALESCE(i.available_quantity, 0) > 0
          AND (:hasWarehouseCodes = false OR inf.code IN (:warehouseCodes))
          AND (:keyword IS NULL OR (
               LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR EXISTS (
                SELECT 1
                FROM product_material_composition pmc_k
                JOIN material m_k ON m_k.id = pmc_k.material_id
                WHERE pmc_k.product_id = pm.id
                  AND LOWER(m_k.name_ko) LIKE CONCAT('%', LOWER(:keyword), '%')
            )
          ))
          AND (
              :materialGroupFilter IS NULL
              OR (
                :materialGroupFilter = 'NATURAL_SINGLE'
                AND (SELECT COUNT(*) FROM product_material_composition pmc_cnt WHERE pmc_cnt.product_id = pm.id) = 1
                AND EXISTS (
                    SELECT 1
                    FROM product_material_composition pmc_g
                    JOIN material m_g ON m_g.id = pmc_g.material_id
                    WHERE pmc_g.product_id = pm.id
                      AND UPPER(m_g.material_group) = 'NATURAL'
                )
              )
              OR (
                :materialGroupFilter = 'SYNTHETIC'
                AND (SELECT COUNT(*) FROM product_material_composition pmc_cnt WHERE pmc_cnt.product_id = pm.id) = 1
                AND EXISTS (
                    SELECT 1
                    FROM product_material_composition pmc_g
                    JOIN material m_g ON m_g.id = pmc_g.material_id
                    WHERE pmc_g.product_id = pm.id
                      AND UPPER(m_g.material_group) = 'SYNTHETIC'
                )
              )
              OR (
                :materialGroupFilter = 'BLEND'
                AND (SELECT COUNT(*) FROM product_material_composition pmc_cnt WHERE pmc_cnt.product_id = pm.id) >= 2
              )
          )
          AND (
             :materialName IS NULL
             OR EXISTS (
                SELECT 1
                FROM product_material_composition pmc_m
                JOIN material m_m ON m_m.id = pmc_m.material_id
                WHERE pmc_m.product_id = pm.id
                  AND m_m.name_ko = :materialName
                  AND (:minRatio <= 0 OR COALESCE(pmc_m.ratio, 0) >= :minRatio)
             )
          )
        ORDER BY
          CASE WHEN :sortField = 'quantity' AND :sortDirection = 'asc' THEN COALESCE(i.available_quantity, 0) END ASC,
          CASE WHEN :sortField = 'quantity' AND :sortDirection = 'desc' THEN COALESCE(i.available_quantity, 0) END DESC,
          CASE WHEN :sortField = 'skuCode' AND :sortDirection = 'desc' THEN ps.sku_code END DESC,
          ps.sku_code ASC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM inventory i
        JOIN infrastructure inf ON inf.id = i.location_id
        JOIN product_sku ps ON ps.id = i.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        WHERE i.inventory_status = 'CIRCULAR'
          AND inf.location_type = 'WAREHOUSE'
          AND COALESCE(i.available_quantity, 0) > 0
          AND (:hasWarehouseCodes = false OR inf.code IN (:warehouseCodes))
          AND (:keyword IS NULL OR (
               LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
            OR EXISTS (
                SELECT 1
                FROM product_material_composition pmc_k
                JOIN material m_k ON m_k.id = pmc_k.material_id
                WHERE pmc_k.product_id = pm.id
                  AND LOWER(m_k.name_ko) LIKE CONCAT('%', LOWER(:keyword), '%')
            )
          ))
          AND (
              :materialGroupFilter IS NULL
              OR (
                :materialGroupFilter = 'NATURAL_SINGLE'
                AND (SELECT COUNT(*) FROM product_material_composition pmc_cnt WHERE pmc_cnt.product_id = pm.id) = 1
                AND EXISTS (
                    SELECT 1
                    FROM product_material_composition pmc_g
                    JOIN material m_g ON m_g.id = pmc_g.material_id
                    WHERE pmc_g.product_id = pm.id
                      AND UPPER(m_g.material_group) = 'NATURAL'
                )
              )
              OR (
                :materialGroupFilter = 'SYNTHETIC'
                AND (SELECT COUNT(*) FROM product_material_composition pmc_cnt WHERE pmc_cnt.product_id = pm.id) = 1
                AND EXISTS (
                    SELECT 1
                    FROM product_material_composition pmc_g
                    JOIN material m_g ON m_g.id = pmc_g.material_id
                    WHERE pmc_g.product_id = pm.id
                      AND UPPER(m_g.material_group) = 'SYNTHETIC'
                )
              )
              OR (
                :materialGroupFilter = 'BLEND'
                AND (SELECT COUNT(*) FROM product_material_composition pmc_cnt WHERE pmc_cnt.product_id = pm.id) >= 2
              )
          )
          AND (
             :materialName IS NULL
             OR EXISTS (
                SELECT 1
                FROM product_material_composition pmc_m
                JOIN material m_m ON m_m.id = pmc_m.material_id
                WHERE pmc_m.product_id = pm.id
                  AND m_m.name_ko = :materialName
                  AND (:minRatio <= 0 OR COALESCE(pmc_m.ratio, 0) >= :minRatio)
             )
          )
        """,
            nativeQuery = true)
    Page<CircularInventoryPageRow> findCircularInventoriesPaged(
            @Param("hasWarehouseCodes") boolean hasWarehouseCodes,
            @Param("warehouseCodes") List<String> warehouseCodes,
            @Param("keyword") String keyword,
            @Param("materialGroupFilter") String materialGroupFilter,
            @Param("materialName") String materialName,
            @Param("minRatio") int minRatio,
            @Param("sortField") String sortField,
            @Param("sortDirection") String sortDirection,
            Pageable pageable
    );

    @Query(value = """
        SELECT
            pm.code AS itemCode,
            m.code AS materialCode,
            m.name_ko AS materialNameKo,
            m.material_group AS materialGroup,
            COALESCE(pmc.ratio, 0) AS ratio,
            COALESCE(pmc.composition_order, 0) AS compositionOrder
        FROM product_master pm
        JOIN product_material_composition pmc ON pmc.product_id = pm.id
        JOIN material m ON m.id = pmc.material_id
        WHERE pm.code IN (:itemCodes)
        ORDER BY pm.code ASC, pmc.composition_order ASC, pmc.id ASC
        """, nativeQuery = true)
    List<CircularInventoryCompositionRow> findCircularInventoryCompositionsByItemCodes(@Param("itemCodes") List<String> itemCodes);
}
