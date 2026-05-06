-- 매장/창고 매핑 더미 데이터
INSERT INTO store_warehouse_map (store_id, warehouse_id, role, create_date, update_date)
SELECT s.id, w.id, 'PRIMARY', NOW(), NOW()
FROM infrastructure s
JOIN infrastructure w
  ON (
      (s.code IN ('ST-0001','ST-0002') AND w.code = 'WH-0001') OR
      (s.code IN ('ST-0003','ST-0004') AND w.code = 'WH-0002') OR
      (s.code IN ('ST-0005') AND w.code = 'WH-0003') OR
      (s.code IN ('ST-0006') AND w.code = 'WH-0004') OR
      (s.code IN ('ST-0007','ST-0008') AND w.code = 'WH-0005') OR
      (s.code IN ('ST-0009','ST-0010') AND w.code = 'WH-0006') OR
      (s.code IN ('ST-0011','ST-0012') AND w.code = 'WH-0007') OR
      (s.code IN ('ST-0013','ST-0014') AND w.code = 'WH-0008')
  )
WHERE s.location_type = 'STORE'
  AND w.location_type = 'WAREHOUSE'
ON DUPLICATE KEY UPDATE
  warehouse_id = VALUES(warehouse_id),
  update_date = NOW();
