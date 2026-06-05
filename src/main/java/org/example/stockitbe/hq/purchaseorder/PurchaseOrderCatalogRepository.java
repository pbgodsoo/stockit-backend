package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.purchaseorder.model.SkuRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 새 발주 카탈로그 read-only 페이지네이션 Repository.
 *
 * 진입점은 ProductSku. product_master / vendor_product / vendor 는 자연 키 또는 FK 로 JOIN.
 * Spring Data 가 Pageable.sort 를 native query 에 자동 적용한다 — alias 컬럼 (vendorName/productName/unitPrice/availableQty/id) 정렬.
 */
public interface PurchaseOrderCatalogRepository extends JpaRepository<ProductSku, Long> {

    // inventory_status 필터는 InventoryStatusPolicy.QUERY_ALLOWED_STATUSES 와 정합 —
    // {NORMAL, CIRCULAR_CANDIDATE}. 창고재고조회(WarehouseInventoryService) 와 같은 정책이라
    // 새 발주 카탈로그의 "재고" 열이 창고재고조회의 가용재고 합과 일치한다.
    @Query(value = """
            SELECT
                v.code AS vendorCode,
                v.name AS vendorName,
                vp.code AS vendorProductCode,
                pm.code AS productCode,
                pm.name AS productName,
                ps.id AS id,
                ps.sku_code AS skuCode,
                ps.color AS color,
                ps.size AS size,
                CONCAT(ps.color, '/', ps.size) AS displayOption,
                ps.unit_price AS unitPrice,
                vp.unit_price AS contractUnitPrice,
                COALESCE((
                    SELECT SUM(i.available_quantity)
                    FROM inventory i
                    JOIN infrastructure inf ON inf.id = i.location_id
                    WHERE i.sku_id = ps.id
                      AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
                      AND inf.location_type = 'WAREHOUSE'
                      AND (:warehouseId IS NULL OR i.location_id = :warehouseId)
                ), 0) AS availableQty,
                pm.warehouse_safety_stock AS warehouseSafetyStock
            FROM product_sku ps
            JOIN product_master pm ON pm.code = ps.product_code AND pm.status = 'ACTIVE'
            JOIN vendor_product vp ON vp.product_code = ps.product_code AND vp.status = 'ACTIVE'
            JOIN vendor v ON v.id = vp.vendor_id AND v.status = 'ACTIVE'
            WHERE ps.status = 'ACTIVE'
              AND (:vendorCode IS NULL OR v.code = :vendorCode)
              AND (:color IS NULL OR ps.color = :color)
              AND (:skuSize IS NULL OR ps.size = :skuSize)
              AND (:keyword IS NULL OR (
                   LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(v.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
              AND (:shortageOnly = false OR (
                   :warehouseId IS NOT NULL AND COALESCE((
                      SELECT SUM(i.available_quantity)
                      FROM inventory i
                      WHERE i.sku_id = ps.id
                        AND i.location_id = :warehouseId
                        AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
                   ), 0) < pm.warehouse_safety_stock
              ))
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM product_sku ps
            JOIN product_master pm ON pm.code = ps.product_code AND pm.status = 'ACTIVE'
            JOIN vendor_product vp ON vp.product_code = ps.product_code AND vp.status = 'ACTIVE'
            JOIN vendor v ON v.id = vp.vendor_id AND v.status = 'ACTIVE'
            WHERE ps.status = 'ACTIVE'
              AND (:vendorCode IS NULL OR v.code = :vendorCode)
              AND (:color IS NULL OR ps.color = :color)
              AND (:skuSize IS NULL OR ps.size = :skuSize)
              AND (:keyword IS NULL OR (
                   LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(v.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
              AND (:shortageOnly = false OR (
                   :warehouseId IS NOT NULL AND COALESCE((
                      SELECT SUM(i.available_quantity)
                      FROM inventory i
                      WHERE i.sku_id = ps.id
                        AND i.location_id = :warehouseId
                        AND i.inventory_status IN ('NORMAL', 'CIRCULAR_CANDIDATE')
                   ), 0) < pm.warehouse_safety_stock
              ))
            """,
            nativeQuery = true)
    Page<SkuRowProjection> findCatalogPage(
            @Param("vendorCode") String vendorCode,
            @Param("keyword") String keyword,
            @Param("color") String color,
            @Param("skuSize") String skuSize,
            @Param("shortageOnly") boolean shortageOnly,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT ps.color
            FROM product_sku ps
            JOIN product_master pm ON pm.code = ps.product_code AND pm.status = 'ACTIVE'
            JOIN vendor_product vp ON vp.product_code = ps.product_code AND vp.status = 'ACTIVE'
            JOIN vendor v ON v.id = vp.vendor_id AND v.status = 'ACTIVE'
            WHERE ps.status = 'ACTIVE'
              AND (:vendorCode IS NULL OR v.code = :vendorCode)
              AND (:keyword IS NULL OR (
                   LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(v.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            ORDER BY ps.color ASC
            """,
            nativeQuery = true)
    List<String> findDistinctColors(
            @Param("vendorCode") String vendorCode,
            @Param("keyword") String keyword
    );

    @Query(value = """
            SELECT DISTINCT ps.size
            FROM product_sku ps
            JOIN product_master pm ON pm.code = ps.product_code AND pm.status = 'ACTIVE'
            JOIN vendor_product vp ON vp.product_code = ps.product_code AND vp.status = 'ACTIVE'
            JOIN vendor v ON v.id = vp.vendor_id AND v.status = 'ACTIVE'
            WHERE ps.status = 'ACTIVE'
              AND (:vendorCode IS NULL OR v.code = :vendorCode)
              AND (:keyword IS NULL OR (
                   LOWER(ps.sku_code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.code) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(pm.name) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(v.name) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            ORDER BY ps.size ASC
            """,
            nativeQuery = true)
    List<String> findDistinctSizes(
            @Param("vendorCode") String vendorCode,
            @Param("keyword") String keyword
    );
}
