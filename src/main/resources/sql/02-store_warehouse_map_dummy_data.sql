-- 매장/창고 매핑 더미 데이터
-- 규칙:
-- 1) 매장 1~5번 PRIMARY -> WH-XX-0001, BACKUP -> WH-XX-0002
-- 2) 매장 6~10번 PRIMARY -> WH-XX-0002, BACKUP -> WH-XX-0001

INSERT INTO store_warehouse_map (store_id, warehouse_id, role, create_date, update_date)
SELECT s.id AS store_id,
       w.id AS warehouse_id,
       'PRIMARY' AS role,
       NOW(), NOW()
FROM infrastructure s
JOIN infrastructure w
  ON w.code = CONCAT(
      'WH-',
      SUBSTRING(s.code, 4, 2),
      '-',
      CASE
        WHEN CAST(SUBSTRING(s.code, 7, 4) AS UNSIGNED) <= 5 THEN '0001'
        ELSE '0002'
      END
  )
WHERE s.location_type = 'STORE'
  AND w.location_type = 'WAREHOUSE'
ON DUPLICATE KEY UPDATE
  warehouse_id = VALUES(warehouse_id),
  update_date = NOW();

INSERT INTO store_warehouse_map (store_id, warehouse_id, role, create_date, update_date)
SELECT s.id AS store_id,
       w.id AS warehouse_id,
       'BACKUP' AS role,
       NOW(), NOW()
FROM infrastructure s
JOIN infrastructure w
  ON w.code = CONCAT(
      'WH-',
      SUBSTRING(s.code, 4, 2),
      '-',
      CASE
        WHEN CAST(SUBSTRING(s.code, 7, 4) AS UNSIGNED) <= 5 THEN '0002'
        ELSE '0001'
      END
  )
WHERE s.location_type = 'STORE'
  AND w.location_type = 'WAREHOUSE'
ON DUPLICATE KEY UPDATE
  warehouse_id = VALUES(warehouse_id),
  update_date = NOW();
