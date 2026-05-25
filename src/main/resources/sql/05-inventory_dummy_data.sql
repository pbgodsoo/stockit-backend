-- 재고 더미 데이터 (부분 후보/순환 분리)
-- 실행 순서:
--   1) 01-infrastructure_dummy_data.sql
--   2) 02-store_warehouse_map_dummy_data.sql
--   3) 03-category_two_level_seed.sql
--   4) 04-product_master_dummy_data.sql
--   5) 본 파일
--
-- 운영 시작 시점 정합성 정책:
--   - NORMAL.quantity 는 후보/순환 분리 후 남는 정상 재고 수량
--   - reserved_quantity = 0, in_transit_quantity = 0 (실재고 == 가용재고)
--   - CIRCULAR_CANDIDATE / CIRCULAR 는 창고 안전재고 2.5배 초과분에서만 생성
--   - 후보 row 는 inventory_candidate_condition(condition_code = 2) 와 정합

DELETE icc
FROM inventory_candidate_condition icc
JOIN inventory i ON i.id = icc.inventory_id
WHERE i.inventory_status = 'CIRCULAR_CANDIDATE';

DELETE FROM inventory
WHERE inventory_status IN ('CIRCULAR_CANDIDATE', 'CIRCULAR');

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
    DATE_SUB(NOW(), INTERVAL MOD(x.sku_id * 2 + x.location_id * 3, 120) DAY) AS status_changed_at,
    DATE_SUB(NOW(), INTERVAL MOD(x.sku_id * 7 + x.location_id * 3, 365) DAY) AS last_movement_at,
    NOW(),
    NOW()
FROM (
    SELECT
        b.sku_id,
        b.location_id,
        b.total_qty - b.excess_qty AS normal_qty
    FROM (
        SELECT
            s.id AS sku_id,
            i.id AS location_id,
            CASE
                WHEN i.location_type = 'WAREHOUSE'
                    THEN m.warehouse_safety_stock * (3 + MOD(s.id + i.id, 2))
                ELSE
                    m.store_safety_stock * (2 + MOD(s.id + i.id, 2))
            END AS total_qty,
            CASE
                WHEN i.location_type = 'WAREHOUSE'
                    THEN GREATEST(
                        0,
                        (m.warehouse_safety_stock * (3 + MOD(s.id + i.id, 2)))
                            - FLOOR(m.warehouse_safety_stock * 2.5)
                    )
                ELSE 0
            END AS excess_qty
        FROM product_sku s
        JOIN product_master m ON m.code = s.product_code
        CROSS JOIN infrastructure i
        WHERE i.status = 'ACTIVE'
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
    x.sku_id,
    x.location_id,
    'CIRCULAR_CANDIDATE' AS inventory_status,
    x.candidate_qty AS quantity,
    0 AS reserved_quantity,
    0 AS in_transit_quantity,
    x.candidate_qty AS available_quantity,
    DATE_SUB(NOW(), INTERVAL 90 + MOD(x.sku_id * 5 + x.location_id * 7, 180) DAY) AS status_changed_at,
    DATE_SUB(NOW(), INTERVAL 400 + MOD(x.sku_id * 13 + x.location_id * 2, 500) DAY) AS last_movement_at,
    NOW(),
    NOW()
FROM (
    SELECT
        b.sku_id,
        b.location_id,
        b.excess_qty - CASE
            WHEN b.excess_qty > 1 THEN GREATEST(1, FLOOR(b.excess_qty * 0.40))
            ELSE 0
        END AS candidate_qty
    FROM (
        SELECT
            s.id AS sku_id,
            i.id AS location_id,
            GREATEST(
                0,
                (m.warehouse_safety_stock * (3 + MOD(s.id + i.id, 2)))
                    - FLOOR(m.warehouse_safety_stock * 2.5)
            ) AS excess_qty
        FROM product_sku s
        JOIN product_master m ON m.code = s.product_code
        CROSS JOIN infrastructure i
        WHERE i.status = 'ACTIVE'
          AND i.location_type = 'WAREHOUSE'
    ) b
) x
WHERE x.candidate_qty > 0
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
    x.sku_id,
    x.location_id,
    'CIRCULAR' AS inventory_status,
    x.circular_qty AS quantity,
    0 AS reserved_quantity,
    0 AS in_transit_quantity,
    x.circular_qty AS available_quantity,
    DATE_SUB(NOW(), INTERVAL 240 + MOD(x.sku_id * 11 + x.location_id * 13, 420) DAY) AS status_changed_at,
    DATE_SUB(NOW(), INTERVAL 760 + MOD(x.sku_id * 17 + x.location_id * 5, 700) DAY) AS last_movement_at,
    NOW(),
    NOW()
FROM (
    SELECT
        b.sku_id,
        b.location_id,
        CASE
            WHEN b.excess_qty > 1 THEN GREATEST(1, FLOOR(b.excess_qty * 0.40))
            ELSE 0
        END AS circular_qty
    FROM (
        SELECT
            s.id AS sku_id,
            i.id AS location_id,
            GREATEST(
                0,
                (m.warehouse_safety_stock * (3 + MOD(s.id + i.id, 2)))
                    - FLOOR(m.warehouse_safety_stock * 2.5)
            ) AS excess_qty
        FROM product_sku s
        JOIN product_master m ON m.code = s.product_code
        CROSS JOIN infrastructure i
        WHERE i.status = 'ACTIVE'
          AND i.location_type = 'WAREHOUSE'
    ) b
) x
WHERE x.circular_qty > 0
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
    i.id AS inventory_id,
    2 AS condition_code,
    '안전재고 대비 초과 누적 SKU' AS condition_label,
    i.status_changed_at AS matched_at,
    NOW(),
    NOW()
FROM inventory i
JOIN infrastructure inf ON inf.id = i.location_id
WHERE i.inventory_status = 'CIRCULAR_CANDIDATE'
  AND inf.location_type = 'WAREHOUSE'
  AND COALESCE(i.available_quantity, 0) > 0
ON DUPLICATE KEY UPDATE
condition_label = VALUES(condition_label),
matched_at = VALUES(matched_at),
update_date = NOW();
