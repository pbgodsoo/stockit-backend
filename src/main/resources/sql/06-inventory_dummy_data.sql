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
    'NORMAL' AS inventory_status,
    (30 + MOD(s.id * 17 + i.id * 11, 420)) AS quantity,
    MOD(s.id * 9 + i.id * 7, 56) AS reserved_quantity,
    MOD(s.id * 5 + i.id * 3, 36) AS in_transit_quantity,
    GREATEST((30 + MOD(s.id * 17 + i.id * 11, 420)) - MOD(s.id * 9 + i.id * 7, 56), 0) AS available_quantity,
    DATE_SUB(NOW(), INTERVAL MOD(s.id * 5 + i.id * 3, 180) DAY) AS status_changed_at,
    DATE_SUB(NOW(), INTERVAL MOD(s.id * 11 + i.id * 13, 1200) DAY) AS last_movement_at,
    NOW(),
    NOW()
FROM product_sku s
CROSS JOIN infrastructure i
WHERE i.status = 'ACTIVE'
  AND MOD(s.id + i.id, 5) <> 0
ON DUPLICATE KEY UPDATE
inventory_status = VALUES(inventory_status),
quantity = VALUES(quantity),
reserved_quantity = VALUES(reserved_quantity),
in_transit_quantity = VALUES(in_transit_quantity),
available_quantity = VALUES(available_quantity),
status_changed_at = VALUES(status_changed_at),
last_movement_at = VALUES(last_movement_at),
update_date = NOW();

-- ===== 순환재고 후보 조건 충족 데이터 보강 =====
-- refresh API 는 WAREHOUSE + NORMAL 만 스캔하므로 상태는 NORMAL 유지

-- [조건1] 장기 미판매 후보(last_movement_at 730일 이상)
UPDATE inventory inv
JOIN infrastructure infra ON infra.id = inv.location_id
SET
  inv.inventory_status = 'NORMAL',
  inv.last_movement_at = DATE_SUB(NOW(), INTERVAL 760 + MOD(inv.sku_id * 7 + inv.location_id * 3, 220) DAY),
  inv.status_changed_at = DATE_SUB(NOW(), INTERVAL 120 + MOD(inv.sku_id + inv.location_id, 140) DAY),
  inv.update_date = NOW()
WHERE infra.location_type = 'WAREHOUSE'
  AND infra.status = 'ACTIVE'
  AND inv.id > 0
  AND MOD(inv.sku_id + inv.location_id, 9) IN (0, 1, 2);

-- [조건2] 안전재고 대비 초과 누적 테스트용(가용재고 높게)
UPDATE inventory inv
JOIN infrastructure infra ON infra.id = inv.location_id
SET
  inv.inventory_status = 'NORMAL',
  inv.quantity = GREATEST(240, inv.quantity),
  inv.available_quantity = 140 + MOD(inv.sku_id * 5 + inv.location_id * 2, 120), -- 140~259
  inv.reserved_quantity = GREATEST(inv.quantity - inv.available_quantity, 0),
  inv.in_transit_quantity = MOD(inv.sku_id + inv.location_id, 8),
  inv.status_changed_at = DATE_SUB(NOW(), INTERVAL 30 + MOD(inv.sku_id + inv.location_id, 60) DAY),
  inv.update_date = NOW()
WHERE infra.location_type = 'WAREHOUSE'
  AND infra.status = 'ACTIVE'
  AND inv.id > 0
  AND MOD(inv.sku_id * 2 + inv.location_id * 3, 8) IN (0, 3, 5);

-- [조건3] 사이즈/컬러 편중률 테스트용 분포 강화
UPDATE inventory inv
JOIN infrastructure infra ON infra.id = inv.location_id
JOIN product_sku sku ON sku.id = inv.sku_id
SET
  inv.inventory_status = 'NORMAL',
  inv.quantity = GREATEST(90, inv.quantity),
  inv.available_quantity = GREATEST(18, LEAST(inv.quantity, 24 + MOD(inv.sku_id + inv.location_id, 16))),
  inv.status_changed_at = DATE_SUB(NOW(), INTERVAL 20 + MOD(inv.sku_id + inv.location_id, 40) DAY),
  inv.update_date = NOW()
WHERE infra.location_type = 'WAREHOUSE'
  AND infra.status = 'ACTIVE'
  AND inv.id > 0
  AND (
    UPPER(TRIM(sku.size)) IN ('XS', 'XL')
    OR UPPER(TRIM(sku.color)) IN ('BLK', 'WHT')
  );

-- ===== 상태 분리 공존 샘플 =====
-- 같은 sku+location 에서 NORMAL / CIRCULAR_CANDIDATE / CIRCULAR 동시 존재
INSERT INTO inventory
(sku_id, location_id, inventory_status, quantity, reserved_quantity, in_transit_quantity, available_quantity, status_changed_at, last_movement_at, create_date, update_date)
SELECT
    inv.sku_id,
    inv.location_id,
    'CIRCULAR_CANDIDATE',
    LEAST(40, GREATEST(10, FLOOR(inv.available_quantity * 0.35))),
    0,
    0,
    LEAST(40, GREATEST(10, FLOOR(inv.available_quantity * 0.35))),
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    inv.last_movement_at,
    NOW(),
    NOW()
FROM inventory inv
JOIN infrastructure infra ON infra.id = inv.location_id
WHERE inv.inventory_status = 'NORMAL'
  AND infra.location_type = 'WAREHOUSE'
  AND MOD(inv.sku_id + inv.location_id, 11) IN (0, 4)
ON DUPLICATE KEY UPDATE
quantity = VALUES(quantity),
available_quantity = VALUES(available_quantity),
update_date = NOW();

INSERT INTO inventory
(sku_id, location_id, inventory_status, quantity, reserved_quantity, in_transit_quantity, available_quantity, status_changed_at, last_movement_at, create_date, update_date)
SELECT
    inv.sku_id,
    inv.location_id,
    'CIRCULAR',
    LEAST(25, GREATEST(5, FLOOR(inv.available_quantity * 0.2))),
    0,
    0,
    LEAST(25, GREATEST(5, FLOOR(inv.available_quantity * 0.2))),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    inv.last_movement_at,
    NOW(),
    NOW()
FROM inventory inv
JOIN infrastructure infra ON infra.id = inv.location_id
WHERE inv.inventory_status = 'NORMAL'
  AND infra.location_type = 'WAREHOUSE'
  AND MOD(inv.sku_id * 3 + inv.location_id, 13) IN (2, 7)
ON DUPLICATE KEY UPDATE
quantity = VALUES(quantity),
available_quantity = VALUES(available_quantity),
update_date = NOW();
