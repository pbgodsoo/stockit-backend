-- 재고 더미 데이터 (전 위치/전 SKU + 순환 후보/순환 재고)
-- 실행 순서:
--   1) 01-infrastructure_dummy_data.sql
--   2) 02-store_warehouse_map_dummy_data.sql
--   3) 03-category_two_level_seed.sql
--   4) 04-product_master_dummy_data.sql
--   5) 본 파일
--
-- 정책:
--   - 모든 ACTIVE 매장/창고에 모든 SKU 의 NORMAL 재고를 생성한다.
--   - 매장 재고는 품절/부족/정상(대체로 24~37개)을 섞는다.
--   - 창고 재고는 부족/정상/초과/편중 패턴을 섞고, 같은 SKU 도 위치별 수량이 달라지게 한다.
--   - CIRCULAR_CANDIDATE / CIRCULAR 는 창고에만 생성한다.
--   - 후보 조건 코드는 InventoryService 와 동일하다.
--     1: 최근 24개월 이상 판매 이력이 없는 SKU
--     2: 안전재고 대비 초과 누적 SKU
--     3: 극단 사이즈 재고 또는 특정 컬러 재고에 편중된 SKU

DELETE FROM inventory_candidate_condition;
DELETE FROM inventory;

INSERT INTO inventory
(sku_id, location_id, inventory_status, quantity, reserved_quantity, in_transit_quantity, available_quantity, status_changed_at, last_movement_at, create_date, update_date)
SELECT
    x.sku_id,
    x.location_id,
    'NORMAL' AS inventory_status,
    x.normal_qty AS quantity,
    0 AS reserved_quantity,
    0 AS in_transit_quantity,
    x.normal_qty AS available_quantity,
    DATE_SUB(NOW(), INTERVAL MOD(x.seed * 3 + x.location_id, 120) DAY) AS status_changed_at,
    CASE
        WHEN x.location_type = 'WAREHOUSE' AND MOD(x.seed, 17) = 0
            THEN DATE_SUB(NOW(), INTERVAL 760 + MOD(x.seed, 260) DAY)
        ELSE DATE_SUB(NOW(), INTERVAL MOD(x.seed * 7 + x.location_id, 540) DAY)
    END AS last_movement_at,
    NOW(),
    NOW()
FROM (
    SELECT
        b.sku_id,
        b.location_id,
        b.location_type,
        b.seed,
        CASE
            WHEN b.location_type = 'STORE' THEN
                CASE
                    WHEN MOD(b.seed, 19) = 0 THEN 0
                    WHEN MOD(b.seed, 19) IN (1, 2, 3)
                        THEN GREATEST(1, b.store_safety_stock - (1 + MOD(b.seed, 5)))
                    ELSE 24 + MOD(b.seed, 14)
                END
            ELSE
                CASE
                    WHEN MOD(b.seed, 29) = 0 THEN 0
                    WHEN MOD(b.seed, 29) IN (1, 2, 3, 4)
                        THEN GREATEST(1, FLOOR(b.warehouse_safety_stock * 0.55) + MOD(b.seed, 8))
                    WHEN MOD(CRC32(b.product_code) + b.location_id, 11) = 0 AND b.color = 'BLK'
                        THEN 130 + MOD(b.seed, 37)
                    WHEN MOD(CRC32(b.product_code) + b.location_id, 13) = 0 AND b.size = 'L'
                        THEN 125 + MOD(b.seed, 41)
                    WHEN MOD(b.seed, 9) = 0
                        THEN FLOOR(b.warehouse_safety_stock * 3.1) + MOD(b.seed, 23)
                    ELSE b.warehouse_safety_stock + 12 + MOD(b.seed, 40)
                END
        END AS normal_qty
    FROM (
        SELECT
            ps.id AS sku_id,
            ps.product_code,
            ps.color,
            ps.size,
            inf.id AS location_id,
            inf.location_type,
            pm.warehouse_safety_stock,
            pm.store_safety_stock,
            MOD(ps.id * 37 + inf.id * 17 + CRC32(ps.sku_code), 100000) AS seed
        FROM product_sku ps
        JOIN product_master pm ON pm.code = ps.product_code
        CROSS JOIN infrastructure inf
        WHERE ps.status = 'ACTIVE'
          AND pm.status = 'ACTIVE'
          AND inf.status = 'ACTIVE'
    ) b
) x
ON DUPLICATE KEY UPDATE
quantity = VALUES(quantity),
reserved_quantity = VALUES(reserved_quantity),
in_transit_quantity = VALUES(in_transit_quantity),
available_quantity = VALUES(available_quantity),
status_changed_at = VALUES(status_changed_at),
last_movement_at = VALUES(last_movement_at),
update_date = NOW();

INSERT INTO inventory
(sku_id, location_id, inventory_status, quantity, reserved_quantity, in_transit_quantity, available_quantity, status_changed_at, last_movement_at, create_date, update_date)
SELECT
    c.sku_id,
    c.location_id,
    'CIRCULAR_CANDIDATE' AS inventory_status,
    MAX(c.candidate_qty) AS quantity,
    0 AS reserved_quantity,
    0 AS in_transit_quantity,
    MAX(c.candidate_qty) AS available_quantity,
    DATE_SUB(NOW(), INTERVAL 30 + MOD(c.sku_id * 5 + c.location_id * 7, 90) DAY) AS status_changed_at,
    CASE
        WHEN SUM(CASE WHEN c.condition_code = 1 THEN 1 ELSE 0 END) > 0
            THEN DATE_SUB(NOW(), INTERVAL 760 + MOD(c.sku_id * 11 + c.location_id * 3, 280) DAY)
        ELSE DATE_SUB(NOW(), INTERVAL 180 + MOD(c.sku_id * 13 + c.location_id * 5, 360) DAY)
    END AS last_movement_at,
    NOW(),
    NOW()
FROM (
    SELECT
        inv.sku_id,
        inv.location_id,
        1 AS condition_code,
        GREATEST(8, LEAST(inv.available_quantity, 18 + MOD(inv.sku_id + inv.location_id, 19))) AS candidate_qty
    FROM inventory inv
    JOIN infrastructure inf ON inf.id = inv.location_id
    WHERE inv.inventory_status = 'NORMAL'
      AND inf.location_type = 'WAREHOUSE'
      AND inv.available_quantity > 0
      AND MOD(inv.sku_id * 7 + inv.location_id * 11, 17) = 0

    UNION ALL

    SELECT
        inv.sku_id,
        inv.location_id,
        2 AS condition_code,
        inv.available_quantity - FLOOR(pm.warehouse_safety_stock * 2.5) AS candidate_qty
    FROM inventory inv
    JOIN infrastructure inf ON inf.id = inv.location_id
    JOIN product_sku ps ON ps.id = inv.sku_id
    JOIN product_master pm ON pm.code = ps.product_code
    WHERE inv.inventory_status = 'NORMAL'
      AND inf.location_type = 'WAREHOUSE'
      AND inv.available_quantity > FLOOR(pm.warehouse_safety_stock * 2.5)

    UNION ALL

    SELECT
        inv.sku_id,
        inv.location_id,
        3 AS condition_code,
        GREATEST(5, FLOOR(inv.available_quantity * 0.25)) AS candidate_qty
    FROM inventory inv
    JOIN infrastructure inf ON inf.id = inv.location_id
    JOIN product_sku ps ON ps.id = inv.sku_id
    WHERE inv.inventory_status = 'NORMAL'
      AND inf.location_type = 'WAREHOUSE'
      AND inv.available_quantity > 0
      AND (
          (MOD(CRC32(ps.product_code) + inv.location_id, 11) = 0 AND ps.color = 'BLK')
          OR (MOD(CRC32(ps.product_code) + inv.location_id, 13) = 0 AND ps.size = 'L')
      )
) c
WHERE c.candidate_qty > 0
GROUP BY c.sku_id, c.location_id
ON DUPLICATE KEY UPDATE
quantity = VALUES(quantity),
reserved_quantity = VALUES(reserved_quantity),
in_transit_quantity = VALUES(in_transit_quantity),
available_quantity = VALUES(available_quantity),
status_changed_at = VALUES(status_changed_at),
last_movement_at = VALUES(last_movement_at),
update_date = NOW();

INSERT INTO inventory
(sku_id, location_id, inventory_status, quantity, reserved_quantity, in_transit_quantity, available_quantity, status_changed_at, last_movement_at, create_date, update_date)
SELECT
    ps.id AS sku_id,
    inf.id AS location_id,
    'CIRCULAR' AS inventory_status,
    50 + MOD(ps.id * 19 + inf.id * 23, 80) AS quantity,
    0 AS reserved_quantity,
    0 AS in_transit_quantity,
    50 + MOD(ps.id * 19 + inf.id * 23, 80) AS available_quantity,
    DATE_SUB(NOW(), INTERVAL 180 + MOD(ps.id * 11 + inf.id * 13, 360) DAY) AS status_changed_at,
    DATE_SUB(NOW(), INTERVAL 730 + MOD(ps.id * 17 + inf.id * 5, 500) DAY) AS last_movement_at,
    NOW(),
    NOW()
FROM product_sku ps
CROSS JOIN infrastructure inf
WHERE ps.status = 'ACTIVE'
  AND inf.status = 'ACTIVE'
  AND inf.location_type = 'WAREHOUSE'
  AND MOD(ps.id * 11 + inf.id * 5, 10) = 0
ON DUPLICATE KEY UPDATE
quantity = VALUES(quantity),
reserved_quantity = VALUES(reserved_quantity),
in_transit_quantity = VALUES(in_transit_quantity),
available_quantity = VALUES(available_quantity),
status_changed_at = VALUES(status_changed_at),
last_movement_at = VALUES(last_movement_at),
update_date = NOW();

INSERT INTO inventory_candidate_condition
(inventory_id, condition_code, condition_label, matched_at, create_date, update_date)
SELECT
    cand.id AS inventory_id,
    c.condition_code,
    CASE c.condition_code
        WHEN 1 THEN '최근 24개월 이상 판매 이력이 없는 SKU'
        WHEN 2 THEN '안전재고 대비 초과 누적 SKU'
        WHEN 3 THEN '극단 사이즈 재고 또는 특정 컬러 재고에 편중된 SKU'
    END AS condition_label,
    cand.status_changed_at AS matched_at,
    NOW(),
    NOW()
FROM (
    SELECT
        inv.sku_id,
        inv.location_id,
        1 AS condition_code
    FROM inventory inv
    JOIN infrastructure inf ON inf.id = inv.location_id
    WHERE inv.inventory_status = 'NORMAL'
      AND inf.location_type = 'WAREHOUSE'
      AND inv.available_quantity > 0
      AND MOD(inv.sku_id * 7 + inv.location_id * 11, 17) = 0

    UNION

    SELECT
        inv.sku_id,
        inv.location_id,
        2 AS condition_code
    FROM inventory inv
    JOIN infrastructure inf ON inf.id = inv.location_id
    JOIN product_sku ps ON ps.id = inv.sku_id
    JOIN product_master pm ON pm.code = ps.product_code
    WHERE inv.inventory_status = 'NORMAL'
      AND inf.location_type = 'WAREHOUSE'
      AND inv.available_quantity > FLOOR(pm.warehouse_safety_stock * 2.5)

    UNION

    SELECT
        inv.sku_id,
        inv.location_id,
        3 AS condition_code
    FROM inventory inv
    JOIN infrastructure inf ON inf.id = inv.location_id
    JOIN product_sku ps ON ps.id = inv.sku_id
    WHERE inv.inventory_status = 'NORMAL'
      AND inf.location_type = 'WAREHOUSE'
      AND inv.available_quantity > 0
      AND (
          (MOD(CRC32(ps.product_code) + inv.location_id, 11) = 0 AND ps.color = 'BLK')
          OR (MOD(CRC32(ps.product_code) + inv.location_id, 13) = 0 AND ps.size = 'L')
      )
) c
JOIN inventory cand
  ON cand.sku_id = c.sku_id
 AND cand.location_id = c.location_id
 AND cand.inventory_status = 'CIRCULAR_CANDIDATE'
ON DUPLICATE KEY UPDATE
condition_label = VALUES(condition_label),
matched_at = VALUES(matched_at),
update_date = NOW();
