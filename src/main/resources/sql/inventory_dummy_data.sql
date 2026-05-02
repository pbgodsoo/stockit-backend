-- 재고 더미 데이터
-- 실행 순서:
-- 1) category_two_level_seed.sql
-- 2) infrastructure_dummy_data.sql
-- 3) product_master_dummy_data.sql

INSERT INTO inventory
(sku_id, location_id, inventory_status, quantity, reserved_quantity, in_transit_quantity, available_quantity, status_changed_at, last_movement_at, create_date, update_date)
SELECT
    s.id AS sku_id,
    i.id AS location_id,
    CASE
      WHEN MOD(s.id + i.id, 11) IN (0,1) THEN 'CIRCULAR'
      WHEN MOD(s.id + i.id, 11) IN (2,3,4) THEN 'CIRCULAR_CANDIDATE'
      ELSE 'NORMAL'
    END AS inventory_status,
    (20 + MOD(s.id * 13 + i.id * 7, 260)) AS quantity,
    MOD(s.id * 5 + i.id * 3, 30) AS reserved_quantity,
    MOD(s.id * 3 + i.id * 2, 18) AS in_transit_quantity,
    GREATEST((20 + MOD(s.id * 13 + i.id * 7, 260)) - MOD(s.id * 5 + i.id * 3, 30), 0) AS available_quantity,
    DATE_SUB(NOW(), INTERVAL MOD(s.id + i.id, 45) DAY) AS status_changed_at,
    DATE_SUB(NOW(), INTERVAL MOD(s.id * 2 + i.id, 30) DAY) AS last_movement_at,
    NOW(),
    NOW()
FROM product_sku s
CROSS JOIN infrastructure i
WHERE i.status = 'ACTIVE'
  AND MOD(s.id + i.id, 4) <> 0
ON DUPLICATE KEY UPDATE
inventory_status = VALUES(inventory_status),
quantity = VALUES(quantity),
reserved_quantity = VALUES(reserved_quantity),
in_transit_quantity = VALUES(in_transit_quantity),
available_quantity = VALUES(available_quantity),
status_changed_at = VALUES(status_changed_at),
last_movement_at = VALUES(last_movement_at),
update_date = NOW();
