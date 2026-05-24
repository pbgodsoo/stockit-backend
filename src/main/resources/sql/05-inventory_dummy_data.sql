-- 재고 더미 데이터 (상태별 다중 행)
-- 실행 순서:
--   1) 01-infrastructure_dummy_data.sql
--   2) 02-store_warehouse_map_dummy_data.sql
--   3) 03-category_two_level_seed.sql
--   4) 04-product_master_dummy_data.sql
--   5) 본 파일
--
-- 운영 시작 시점 정합성 정책:
--   - NORMAL.quantity = safety_stock × 2 ~ 3 (모든 NORMAL row 가 UI 상 "정상" 으로 표시됨)
--   - reserved_quantity = 0, in_transit_quantity = 0 (실재고 == 가용재고)
--   - CIRCULAR_CANDIDATE.quantity ≈ NORMAL × 7%, CIRCULAR.quantity ≈ NORMAL × 4%
--
-- 기존 dev light 분리본 (05-1-*) 은 폐기:
--   매장 PRIMARY 창고가 시드에서 누락되어 매장 발주 자동 승인이 4611 로 실패하던 원인.
--   풀 시드 1개로 통합.

INSERT INTO inventory
(sku_id, location_id, inventory_status, quantity, reserved_quantity, in_transit_quantity, available_quantity, status_changed_at, last_movement_at, create_date, update_date)
SELECT
    x.sku_id,
    x.location_id,
    x.inventory_status,
    x.quantity,
    0 AS reserved_quantity,
    0 AS in_transit_quantity,
    GREATEST(x.quantity, 0) AS available_quantity,
    x.status_changed_at,
    x.last_movement_at,
    NOW(),
    NOW()
FROM (
    SELECT
        b.sku_id,
        b.location_id,
        st.inventory_status,
        b.normal_qty,
        CASE st.inventory_status
            WHEN 'NORMAL'             THEN b.normal_qty
            WHEN 'CIRCULAR_CANDIDATE' THEN GREATEST(1, FLOOR(b.normal_qty * 0.07))
            ELSE                           GREATEST(1, FLOOR(b.normal_qty * 0.04))
        END AS quantity,
        CASE st.inventory_status
            WHEN 'NORMAL'             THEN DATE_SUB(NOW(), INTERVAL MOD(b.sku_id * 2 + b.location_id * 3, 120) DAY)
            WHEN 'CIRCULAR_CANDIDATE' THEN DATE_SUB(NOW(), INTERVAL 90 + MOD(b.sku_id * 5 + b.location_id * 7, 180) DAY)
            ELSE                           DATE_SUB(NOW(), INTERVAL 240 + MOD(b.sku_id * 11 + b.location_id * 13, 420) DAY)
        END AS status_changed_at,
        CASE st.inventory_status
            WHEN 'NORMAL'             THEN DATE_SUB(NOW(), INTERVAL MOD(b.sku_id * 7 + b.location_id * 3, 365) DAY)
            WHEN 'CIRCULAR_CANDIDATE' THEN DATE_SUB(NOW(), INTERVAL 400 + MOD(b.sku_id * 13 + b.location_id * 2, 500) DAY)
            ELSE                           DATE_SUB(NOW(), INTERVAL 760 + MOD(b.sku_id * 17 + b.location_id * 5, 700) DAY)
        END AS last_movement_at
    FROM (
        SELECT
            s.id AS sku_id,
            i.id AS location_id,
            CASE
                WHEN i.location_type = 'WAREHOUSE'
                    THEN m.warehouse_safety_stock * (2 + MOD(s.id + i.id, 2))
                ELSE
                    m.store_safety_stock * (2 + MOD(s.id + i.id, 2))
            END AS normal_qty
        FROM product_sku s
        JOIN product_master m ON m.code = s.product_code
        CROSS JOIN infrastructure i
        WHERE i.status = 'ACTIVE'
    ) b
    CROSS JOIN (
        SELECT 'NORMAL' AS inventory_status
        UNION ALL SELECT 'CIRCULAR_CANDIDATE'
        UNION ALL SELECT 'CIRCULAR'
    ) st
) x
ON DUPLICATE KEY UPDATE
quantity = VALUES(quantity),
reserved_quantity = VALUES(reserved_quantity),
in_transit_quantity = VALUES(in_transit_quantity),
available_quantity = VALUES(available_quantity),
status_changed_at = VALUES(status_changed_at),
last_movement_at = VALUES(last_movement_at),
update_date = NOW();
