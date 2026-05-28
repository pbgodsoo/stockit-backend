package org.example.stockitbe.hq.inventory;

import jakarta.persistence.LockModeType;
import org.example.stockitbe.hq.inventory.model.CircularInventoryCompositionRow;
import org.example.stockitbe.hq.inventory.model.CircularInventoryPageRow;
import org.example.stockitbe.hq.inventory.model.ImbalancedSkuRow;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
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
    WITH store_total AS (
        SELECT ssh.store_id,
               SUM(ssi.quantity) AS qty
        FROM store_sale_header ssh
        JOIN store_sale_item ssi ON ssi.sale_header_id = ssh.id
        WHERE ssh.sold_at >= :fromDt AND ssh.sold_at < :toDt
          AND ssh.status = 'COMPLETED'
        GROUP BY ssh.store_id
    ),
    warehouse_total AS (
        SELECT swm.warehouse_id,
               SUM(ssi.quantity) AS qty
        FROM store_warehouse_map swm
        JOIN store_sale_header ssh ON ssh.store_id = swm.store_id
        JOIN store_sale_item ssi  ON ssi.sale_header_id = ssh.id
        WHERE ssh.sold_at >= :fromDt AND ssh.sold_at < :toDt
          AND ssh.status = 'COMPLETED'
        GROUP BY swm.warehouse_id
    )
    SELECT inf.code          AS code,
           inf.name          AS name,
           inf.location_type AS location_type,
           IFNULL(SUM(i.quantity), 0) AS avg_inventory,
           IFNULL(
               CASE inf.location_type
                   WHEN 'STORE'     THEN MAX(st.qty)
                   WHEN 'WAREHOUSE' THEN MAX(wt.qty)
               END
           , 0) AS sales_qty
    FROM infrastructure inf
    LEFT JOIN inventory i         ON i.location_id = inf.id
    LEFT JOIN store_total st      ON st.store_id    = inf.id
    LEFT JOIN warehouse_total wt  ON wt.warehouse_id = inf.id
    WHERE inf.status = 'ACTIVE'
      AND ( :scope = 'ALL'
         OR (:scope = 'STORE'     AND inf.location_type = 'STORE')
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
    WITH store_sale_agg AS (
        SELECT ssi.sku_id,
               ssh.store_id,
               SUM(ssi.quantity) AS qty
        FROM store_sale_item ssi
        JOIN store_sale_header ssh ON ssh.id = ssi.sale_header_id
        WHERE ssh.sold_at >= :fromDt AND ssh.sold_at < :toDt
          AND ssh.status = 'COMPLETED'
        GROUP BY ssi.sku_id, ssh.store_id
    ),
    warehouse_sale_agg AS (
        SELECT ssi.sku_id,
               swm.warehouse_id,
               SUM(ssi.quantity) AS qty
        FROM store_sale_item ssi
        JOIN store_sale_header ssh ON ssh.id = ssi.sale_header_id
        JOIN store_warehouse_map swm ON swm.store_id = ssh.store_id
        WHERE ssh.sold_at >= :fromDt AND ssh.sold_at < :toDt
          AND ssh.status = 'COMPLETED'
        GROUP BY ssi.sku_id, swm.warehouse_id
    )
    SELECT ps.sku_code       AS sku_code,
           pm.name           AS product_name,
           pm.category_code  AS category_code,
           inf.code          AS location_code,
           inf.name          AS location_name,
           inf.location_type AS location_type,
           i.quantity        AS units,
           ps.unit_price     AS unit_price,
           IFNULL(
               CASE inf.location_type
                   WHEN 'STORE'     THEN ssa.qty
                   WHEN 'WAREHOUSE' THEN wsa.qty
               END
           , 0) AS sales_qty
    FROM inventory i
    JOIN product_sku ps      ON ps.id = i.sku_id
    JOIN product_master pm   ON pm.code = ps.product_code
    JOIN infrastructure inf  ON inf.id = i.location_id
    LEFT JOIN store_sale_agg ssa
        ON ssa.sku_id = i.sku_id AND ssa.store_id = inf.id
    LEFT JOIN warehouse_sale_agg wsa
        ON wsa.sku_id = i.sku_id AND wsa.warehouse_id = inf.id
    WHERE inf.status = 'ACTIVE'
      AND ( :scope = 'ALL'
         OR (:scope = 'STORE'     AND inf.location_type = 'STORE')
         OR (:scope = 'WAREHOUSE' AND inf.location_type = 'WAREHOUSE') )
      AND (:locationCode IS NULL OR inf.code = :locationCode)
    """, nativeQuery = true)
    List<Object[]> skuTurnoverList(@Param("fromDt") Date fromDt,
                                   @Param("toDt") Date toDt,
                                   @Param("scope") String scope,
                                   @Param("locationCode") String locationCode);

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
            COALESCE(MAX(ps.unit_price), 0) AS unitPrice,
            COALESCE(SUM(i.quantity), 0) AS actualStock,
            COALESCE(SUM(i.available_quantity), 0) AS availableStock,
            COALESCE(pm.store_safety_stock, 0) AS safetyStock,
            COALESCE((
                SELECT SUM(woi.requested_quantity)
                FROM wh_outbound_item woi
                JOIN wh_outbound_header woh ON woh.id = woi.outbound_header_id
                WHERE woi.sku_id = ps.id
                  AND woh.destination_type = 'STORE'
                  AND woh.destination_id = :locationId
                  AND woh.status IN ('READY_TO_SHIP', 'IN_TRANSIT')
            ), 0) AS inboundExpectedQuantity,
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

    /**
     * 순환재고 조회 전용 DB 페이지 쿼리.
     * - CIRCULAR + WAREHOUSE + available_quantity > 0 조건을 DB에서 먼저 거른다.
     * - 키워드/창고/소재 그룹/소재명(비율) 필터를 SQL에서 처리한다.
     * - 동적 정렬은 quantity/skuCode만 허용하고, 나머지는 skuCode ASC로 안정 정렬한다.
     */
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

    /**
     * 전사 창고 기준 SKU별 재고 현황을 단일 CTE 쿼리로 집계한다.
     * warehouse_sku_stock: (sku, 창고) 단위 가용재고 합산.
     * sku_shortage: SKU 단위로 부족 창고 수 + 부족 수량 집계.
     *   - HAVING 제거: 원본 Java 로직과 동일하게 창고 재고가 있는 모든 SKU를 반환.
     *     (shortageWarehouseCount == 0 인 SKU도 포함)
     * 결과는 shortage_warehouse_count DESC → total_shortage_qty DESC → sku_code ASC 정렬.
     */
    @Query(value = """
        WITH warehouse_sku_stock AS (
            SELECT i.sku_id,
                   i.location_id,
                   COALESCE(SUM(i.available_quantity), 0) AS total_available,
                   COALESCE(SUM(i.quantity), 0) AS total_on_hand
            FROM inventory i
            JOIN infrastructure inf ON inf.id = i.location_id
            WHERE inf.location_type = 'WAREHOUSE'
              AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
            GROUP BY i.sku_id, i.location_id
        ),
        sku_shortage AS (
            SELECT wss.sku_id,
                   pm.warehouse_safety_stock,
                   SUM(wss.total_on_hand) AS total_on_hand,
                   SUM(wss.total_available) AS total_available,
                   COUNT(CASE WHEN wss.total_available < COALESCE(pm.warehouse_safety_stock, 0) THEN 1 END) AS shortage_warehouse_count,
                   SUM(CASE WHEN wss.total_available < COALESCE(pm.warehouse_safety_stock, 0)
                            THEN COALESCE(pm.warehouse_safety_stock, 0) - wss.total_available
                            ELSE 0 END) AS total_shortage_qty
            FROM warehouse_sku_stock wss
            JOIN product_sku ps ON ps.id = wss.sku_id
            JOIN product_master pm ON pm.code = ps.product_code
            GROUP BY wss.sku_id, pm.warehouse_safety_stock
        )
        SELECT ps.sku_code AS skuCode,
               pm.code AS itemCode, pm.name AS itemName,
               CASE WHEN pc.name IS NOT NULL
                    THEN CONCAT(pc.name, ' > ', cc.name)
                    ELSE COALESCE(cc.name, '') END AS category,
               ps.color AS color, ps.size AS size,
               ss.total_on_hand AS totalOnHand,
               ss.total_available AS totalAvailable,
               ss.shortage_warehouse_count AS shortageWarehouseCount,
               ss.total_shortage_qty AS totalShortageQty
        FROM sku_shortage ss
        JOIN product_sku ps ON ps.id = ss.sku_id
        JOIN product_master pm ON pm.code = ps.product_code
        LEFT JOIN category cc ON cc.code = pm.category_code
        LEFT JOIN category pc ON pc.id = cc.parent_id
        ORDER BY ss.shortage_warehouse_count DESC,
                 ss.total_shortage_qty DESC,
                 ps.sku_code ASC
        """, nativeQuery = true)
    List<ImbalancedSkuRow> findImbalancedSkuRows();

    /**
     * 페이지 본문에 포함된 itemCode 집합 기준으로 소재 구성을 한 번에 조회한다.
     * 서비스에서 itemCode별 그룹핑하여 DTO 조립 시 사용한다.
     */
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
