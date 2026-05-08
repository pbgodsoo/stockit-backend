-- 재고 더미 데이터 (개발용 경량 버전, 상태별 다중 행)
-- 실행 순서:
-- 1) 01-infrastructure_dummy_data.sql
-- 2) 02-store_warehouse_map_dummy_data.sql
-- 3) 03-category_two_level_seed.sql
-- 4) 04-product_master_dummy_data.sql
-- 5) 본 파일(05-1-inventory_dummy_data_dev_light.sql) 실행
--
-- 안내:
-- - 개발 환경에서는 기존 05-inventory_dummy_data.sql 대신 본 파일을 선택 실행한다.
-- - 기존 규칙/포맷(컬럼, 상태, upsert)을 유지하되 대상 SKU/거점을 축소해 로딩 부담을 줄인다.

INSERT INTO inventory
(sku_id, location_id, inventory_status, quantity, reserved_quantity, in_transit_quantity, available_quantity, status_changed_at, last_movement_at, create_date, update_date)
SELECT
    x.sku_id,
    x.location_id,
    x.inventory_status,
    x.quantity,
    x.reserved_quantity,
    x.in_transit_quantity,
    GREATEST(x.quantity - x.reserved_quantity, 0) AS available_quantity,
    x.status_changed_at,
    x.last_movement_at,
    NOW(),
    NOW()
FROM (
    SELECT
        b.sku_id,
        b.location_id,
        st.inventory_status,
        b.target_sum,
        b.candidate_qty,
        b.normal_qty,
        CASE st.inventory_status
            WHEN 'NORMAL' THEN b.normal_qty
            WHEN 'CIRCULAR_CANDIDATE' THEN b.candidate_qty
            ELSE
                GREATEST(
                    1,
                    FLOOR(
                        b.target_sum * (0.03 + MOD(b.sku_id * 2 + b.location_id, 4) * 0.01)
                    )
                )
        END AS quantity,
        CASE st.inventory_status
            WHEN 'NORMAL' THEN
                GREATEST(
                    0,
                    LEAST(
                        FLOOR(b.normal_qty * 0.20),
                        MOD(b.sku_id * 11 + b.location_id * 7,
                            FLOOR(b.normal_qty * 0.25) + 1)
                    )
                )
            ELSE 0
        END AS reserved_quantity,
        CASE st.inventory_status
            WHEN 'NORMAL' THEN MOD(b.sku_id * 3 + b.location_id * 5, 25)
            ELSE 0
        END AS in_transit_quantity,
        CASE st.inventory_status
            WHEN 'NORMAL' THEN DATE_SUB(NOW(), INTERVAL MOD(b.sku_id * 2 + b.location_id * 3, 120) DAY)
            WHEN 'CIRCULAR_CANDIDATE' THEN DATE_SUB(NOW(), INTERVAL 90 + MOD(b.sku_id * 5 + b.location_id * 7, 180) DAY)
            ELSE DATE_SUB(NOW(), INTERVAL 240 + MOD(b.sku_id * 11 + b.location_id * 13, 420) DAY)
        END AS status_changed_at,
        CASE st.inventory_status
            WHEN 'NORMAL' THEN DATE_SUB(NOW(), INTERVAL MOD(b.sku_id * 7 + b.location_id * 3, 365) DAY)
            WHEN 'CIRCULAR_CANDIDATE' THEN DATE_SUB(NOW(), INTERVAL 400 + MOD(b.sku_id * 13 + b.location_id * 2, 500) DAY)
            ELSE DATE_SUB(NOW(), INTERVAL 760 + MOD(b.sku_id * 17 + b.location_id * 5, 700) DAY)
        END AS last_movement_at
    FROM (
        SELECT
            s.id AS sku_id,
            i.id AS location_id,
            CASE
                WHEN i.location_type = 'WAREHOUSE' THEN 50 + MOD(s.id * 17 + i.id * 19, 71)
                ELSE MOD(s.id * 13 + i.id * 11, 41)
            END AS target_sum,
            -- 후보는 (NORMAL + CANDIDATE) 합의 20% 미만 보장
            CASE
                WHEN (CASE WHEN i.location_type = 'WAREHOUSE' THEN 50 + MOD(s.id * 17 + i.id * 19, 71) ELSE MOD(s.id * 13 + i.id * 11, 41) END) <= 0
                    THEN 0
                ELSE LEAST(
                    FLOOR((((CASE WHEN i.location_type = 'WAREHOUSE' THEN 50 + MOD(s.id * 17 + i.id * 19, 71) ELSE MOD(s.id * 13 + i.id * 11, 41) END) - 1) * (0.05 + MOD(s.id + i.id, 15) * 0.01))),
                    FLOOR((((CASE WHEN i.location_type = 'WAREHOUSE' THEN 50 + MOD(s.id * 17 + i.id * 19, 71) ELSE MOD(s.id * 13 + i.id * 11, 41) END) - 1) * 0.19))
                )
            END AS candidate_qty,
            CASE
                WHEN (CASE WHEN i.location_type = 'WAREHOUSE' THEN 50 + MOD(s.id * 17 + i.id * 19, 71) ELSE MOD(s.id * 13 + i.id * 11, 41) END) <= 0
                    THEN 0
                ELSE (CASE WHEN i.location_type = 'WAREHOUSE' THEN 50 + MOD(s.id * 17 + i.id * 19, 71) ELSE MOD(s.id * 13 + i.id * 11, 41) END)
                    - LEAST(
                        FLOOR((((CASE WHEN i.location_type = 'WAREHOUSE' THEN 50 + MOD(s.id * 17 + i.id * 19, 71) ELSE MOD(s.id * 13 + i.id * 11, 41) END) - 1) * (0.05 + MOD(s.id + i.id, 15) * 0.01))),
                        FLOOR((((CASE WHEN i.location_type = 'WAREHOUSE' THEN 50 + MOD(s.id * 17 + i.id * 19, 71) ELSE MOD(s.id * 13 + i.id * 11, 41) END) - 1) * 0.19))
                    )
            END AS normal_qty
        FROM product_sku s
        CROSS JOIN infrastructure i
        WHERE i.status = 'ACTIVE'
          AND MOD(s.id, 5) = 1
          AND (
                (i.location_type = 'WAREHOUSE' AND MOD(i.id, 3) = 1)
                OR (i.location_type = 'STORE' AND MOD(i.id, 12) = 1)
              )
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
