-- 매장/창고 매핑 더미 데이터
INSERT INTO store_warehouse_map (store_id, warehouse_id, role, create_date, update_date)
SELECT s.id, w.id, 'PRIMARY', NOW(), NOW()
FROM infrastructure s
JOIN infrastructure w
  ON (
      (s.code IN ('ST-SL-0001','ST-SL-0002') AND w.code = 'WH-SL-0001') OR
      (s.code IN ('ST-GG-0001','ST-GG-0002') AND w.code = 'WH-GG-0001') OR
      (s.code IN ('ST-BS-0001') AND w.code = 'WH-YN-0001') OR
      (s.code IN ('ST-DJ-0001') AND w.code = 'WH-CN-0001') OR
      (s.code IN ('ST-IC-0001','ST-IC-0002') AND w.code = 'WH-IC-0001') OR
      (s.code IN ('ST-GW-0001','ST-GW-0002') AND w.code = 'WH-GW-0001') OR
      (s.code IN ('ST-GJ-0001','ST-GJ-0002') AND w.code = 'WH-HN-0001') OR
      (s.code IN ('ST-JJ-0001','ST-JJ-0002') AND w.code = 'WH-JJ-0001')
  )
WHERE s.location_type = 'STORE'
  AND w.location_type = 'WAREHOUSE'
ON DUPLICATE KEY UPDATE
  warehouse_id = VALUES(warehouse_id),
  update_date = NOW();
