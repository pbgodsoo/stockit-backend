-- 14-circular-sales-dummy.sql
-- 순환판매(SALE 30건) + 기부(DONATION 10건) = 40건
--
-- ▣ 월별 금액 균형 설계
--   각 월에 고가(CASHMERE/SILK) + 중가(WOOL/LINEN/COTTON) + 저가(POLYESTER/ACRYLIC 등) 혼합
--   → 1~6월 ARRIVED 금액 최대/최소 비율 ≈ 2.4배 (이전 9배 → 개선)
--   1월 ARRIVED: CASHMERE+SILK+POLYESTER+ACRYLIC+BLEND      = 294,000
--   2월 ARRIVED: WOOL+COTTON+LINEN+POLYAMIDE+ELASTANE        = 317,000
--   3월 ARRIVED: CASHMERE+POLYESTER+WOOL+COTTON+BLEND        = 341,000
--   4월 ARRIVED: SILK+LINEN+POLYAMIDE+ELASTANE+ACRYLIC       = 270,000
--   5월 ARRIVED: CASHMERE+SILK+WOOL+COTTON (4건)             = 340,000
--   6월 ARRIVED: POLYESTER+ACRYLIC+POLYAMIDE (3건)           = 140,000
--
-- ▣ ESG 점수 정합성
--   circular_buyer_transaction.transacted_at = circular_sale_header.sold_at  ← 서비스 조회 기준
--
-- ▣ 소재 분류 표시 (UI "소재 분류" 컬럼)
--   circular_sale_item.main_category = circular_material_price_policy.material_group
--   ('NATURAL_SINGLE' | 'SYNTHETIC' | 'BLEND')

SET SQL_SAFE_UPDATES = 0;
DELETE FROM circular_sale_status_history
  WHERE sale_header_id IN (
    SELECT id FROM circular_sale_header
    WHERE sale_no LIKE 'CSR-2026%' OR sale_no LIKE 'DON-2026%');
DELETE FROM circular_sale_item_material
  WHERE sale_item_id IN (
    SELECT ci.id FROM circular_sale_item ci
    JOIN circular_sale_header ch ON ch.id = ci.sale_header_id
    WHERE ch.sale_no LIKE 'CSR-2026%' OR ch.sale_no LIKE 'DON-2026%');
DELETE FROM circular_sale_item
  WHERE sale_header_id IN (
    SELECT id FROM circular_sale_header
    WHERE sale_no LIKE 'CSR-2026%' OR sale_no LIKE 'DON-2026%');
DELETE FROM circular_sale_header
  WHERE sale_no LIKE 'CSR-2026%' OR sale_no LIKE 'DON-2026%';
SET SQL_SAFE_UPDATES = 1;

-- ============================================================
-- circular_sale_header: SALE 30건
-- total_amount = ROUND(actual_weight_kg) × price_per_kg
-- total_sold_quantity = ROUND(actual_weight_kg / 0.3) 추정 수량
--
-- 소재별 단가: CASHMERE 35000, SILK 20000, WOOL 10000, COTTON 5000, LINEN 5000
--             POLYESTER 3000, ACRYLIC 2000, POLYAMIDE 4000, ELASTANE 2000, BLEND 1000
-- 소재별 무게(일정): CASHMERE 3kg, SILK 4kg, WOOL 8kg, COTTON 15kg, LINEN 18kg
--                   POLYESTER 20kg, ACRYLIC 14kg, POLYAMIDE 13kg, ELASTANE 10kg, BLEND 21kg
-- ============================================================
INSERT INTO circular_sale_header
(sale_no, buyer_id, warehouse_id, sale_type, status,
 sold_by_member_id, sold_by_name, material_type,
 total_sku_count, total_requested_weight_kg, total_actual_weight_kg,
 total_sold_quantity, total_amount,
 sold_at, completed_at, outbound_header_id,
 create_date, update_date)
VALUES
-- ── 1월 ARRIVED: CASHMERE + SILK + POLYESTER + ACRYLIC + BLEND = 294,000 ──
('CSR-20260108-00001',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 0),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','CASHMERE',  1, 3.150, 3.000,10, 105000,'2026-01-08 09:00:00','2026-01-11 14:00:00',NULL,'2026-01-08 09:00:00','2026-01-11 14:00:00'),
('CSR-20260112-00002',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 1),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','SILK',      1, 4.200, 4.000,13,  80000,'2026-01-12 09:00:00','2026-01-15 14:00:00',NULL,'2026-01-12 09:00:00','2026-01-15 14:00:00'),
('CSR-20260116-00003',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 0),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','POLYESTER',  1,21.000,20.000,67,  60000,'2026-01-16 09:00:00','2026-01-19 14:00:00',NULL,'2026-01-16 09:00:00','2026-01-19 14:00:00'),
('CSR-20260120-00004',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 2),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','ACRYLIC',    1,14.700,14.000,47,  28000,'2026-01-20 09:00:00','2026-01-23 14:00:00',NULL,'2026-01-20 09:00:00','2026-01-23 14:00:00'),
('CSR-20260124-00005',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 1),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','BLEND',      1,22.050,21.000,70,  21000,'2026-01-24 09:00:00','2026-01-27 14:00:00',NULL,'2026-01-24 09:00:00','2026-01-27 14:00:00'),
-- ── 2월 ARRIVED: WOOL + COTTON + LINEN + POLYAMIDE + ELASTANE = 317,000 ──
('CSR-20260205-00006',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 3),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','WOOL',       1, 8.400, 8.000,27,  80000,'2026-02-05 09:00:00','2026-02-08 14:00:00',NULL,'2026-02-05 09:00:00','2026-02-08 14:00:00'),
('CSR-20260210-00007',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 2),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','COTTON',     1,15.750,15.000,50,  75000,'2026-02-10 09:00:00','2026-02-13 14:00:00',NULL,'2026-02-10 09:00:00','2026-02-13 14:00:00'),
('CSR-20260214-00008',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 4),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','LINEN',      1,18.900,18.000,60,  90000,'2026-02-14 09:00:00','2026-02-17 14:00:00',NULL,'2026-02-14 09:00:00','2026-02-17 14:00:00'),
('CSR-20260218-00009',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 3),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','POLYAMIDE',  1,13.650,13.000,43,  52000,'2026-02-18 09:00:00','2026-02-21 14:00:00',NULL,'2026-02-18 09:00:00','2026-02-21 14:00:00'),
('CSR-20260222-00010',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 5),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','ELASTANE',   1,10.500,10.000,33,  20000,'2026-02-22 09:00:00','2026-02-25 14:00:00',NULL,'2026-02-22 09:00:00','2026-02-25 14:00:00'),
-- ── 3월 ARRIVED: CASHMERE + POLYESTER + WOOL + COTTON + BLEND = 341,000 ──
('CSR-20260305-00011',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 4),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','CASHMERE',  1, 3.150, 3.000,10, 105000,'2026-03-05 09:00:00','2026-03-08 14:00:00',NULL,'2026-03-05 09:00:00','2026-03-08 14:00:00'),
('CSR-20260310-00012',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 6),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','POLYESTER',  1,21.000,20.000,67,  60000,'2026-03-10 09:00:00','2026-03-13 14:00:00',NULL,'2026-03-10 09:00:00','2026-03-13 14:00:00'),
('CSR-20260315-00013',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 5),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','WOOL',       1, 8.400, 8.000,27,  80000,'2026-03-15 09:00:00','2026-03-18 14:00:00',NULL,'2026-03-15 09:00:00','2026-03-18 14:00:00'),
('CSR-20260319-00014',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 7),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','COTTON',     1,15.750,15.000,50,  75000,'2026-03-19 09:00:00','2026-03-22 14:00:00',NULL,'2026-03-19 09:00:00','2026-03-22 14:00:00'),
('CSR-20260324-00015',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 6),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','BLEND',      1,22.050,21.000,70,  21000,'2026-03-24 09:00:00','2026-03-27 14:00:00',NULL,'2026-03-24 09:00:00','2026-03-27 14:00:00'),
-- ── 4월 ARRIVED: SILK + LINEN + POLYAMIDE + ELASTANE + ACRYLIC = 270,000 ──
('CSR-20260405-00016',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 8),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','SILK',       1, 4.200, 4.000,13,  80000,'2026-04-05 09:00:00','2026-04-08 14:00:00',NULL,'2026-04-05 09:00:00','2026-04-08 14:00:00'),
('CSR-20260410-00017',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 7),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','LINEN',      1,18.900,18.000,60,  90000,'2026-04-10 09:00:00','2026-04-13 14:00:00',NULL,'2026-04-10 09:00:00','2026-04-13 14:00:00'),
('CSR-20260415-00018',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 9),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','POLYAMIDE',  1,13.650,13.000,43,  52000,'2026-04-15 09:00:00','2026-04-18 14:00:00',NULL,'2026-04-15 09:00:00','2026-04-18 14:00:00'),
('CSR-20260420-00019',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 8),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','ELASTANE',   1,10.500,10.000,33,  20000,'2026-04-20 09:00:00','2026-04-23 14:00:00',NULL,'2026-04-20 09:00:00','2026-04-23 14:00:00'),
('CSR-20260425-00020',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 10),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','ACRYLIC',   1,14.700,14.000,47,  28000,'2026-04-25 09:00:00','2026-04-28 14:00:00',NULL,'2026-04-25 09:00:00','2026-04-28 14:00:00'),
-- ── 5월 ARRIVED 4건: CASHMERE + SILK + WOOL + COTTON = 340,000 ──
('CSR-20260508-00021',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 9),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','CASHMERE',  1, 3.150, 3.000,10, 105000,'2026-05-08 09:00:00','2026-05-11 14:00:00',NULL,'2026-05-08 09:00:00','2026-05-11 14:00:00'),
('CSR-20260513-00022',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 11),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','SILK',      1, 4.200, 4.000,13,  80000,'2026-05-13 09:00:00','2026-05-16 14:00:00',NULL,'2026-05-13 09:00:00','2026-05-16 14:00:00'),
('CSR-20260518-00023',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 10),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','WOOL',      1, 8.400, 8.000,27,  80000,'2026-05-18 09:00:00','2026-05-21 14:00:00',NULL,'2026-05-18 09:00:00','2026-05-21 14:00:00'),
('CSR-20260523-00024',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 12),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','COTTON',    1,15.750,15.000,50,  75000,'2026-05-23 09:00:00','2026-05-26 14:00:00',NULL,'2026-05-23 09:00:00','2026-05-26 14:00:00'),
-- ── 5월 IN_TRANSIT 1건: LINEN ──
('CSR-20260528-00025',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 11),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','IN_TRANSIT','wh-sl-0001','박지훈','LINEN',     1,18.900,18.000,60,  90000,'2026-05-28 09:00:00',NULL,NULL,'2026-05-28 09:00:00','2026-05-29 10:00:00'),
-- ── 6월 ARRIVED 3건: POLYESTER + ACRYLIC + POLYAMIDE = 140,000 ──
('CSR-20260602-00026',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 13),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','POLYESTER',  1,21.000,20.000,67,  60000,'2026-06-02 09:00:00','2026-06-05 14:00:00',NULL,'2026-06-02 09:00:00','2026-06-05 14:00:00'),
('CSR-20260604-00027',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 12),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','ARRIVED','wh-sl-0001','박지훈','ACRYLIC',   1,14.700,14.000,47,  28000,'2026-06-04 09:00:00','2026-06-07 14:00:00',NULL,'2026-06-04 09:00:00','2026-06-07 14:00:00'),
('CSR-20260605-00028',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 14),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','ARRIVED','wh-sl-0002','이수현','POLYAMIDE',  1,13.650,13.000,43,  52000,'2026-06-05 09:00:00','2026-06-08 14:00:00',NULL,'2026-06-05 09:00:00','2026-06-08 14:00:00'),
-- ── 6월 IN_TRANSIT 1건: ELASTANE ──
('CSR-20260606-00029',(SELECT id FROM circular_buyer WHERE partner_type='local_small' ORDER BY id LIMIT 1 OFFSET 13),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'SALE','IN_TRANSIT','wh-sl-0001','박지훈','ELASTANE',  1,10.500,10.000,33,  20000,'2026-06-06 09:00:00',NULL,NULL,'2026-06-06 09:00:00','2026-06-07 10:00:00'),
-- ── 6월 READY_TO_SHIP 1건: BLEND ──
('CSR-20260607-00030',(SELECT id FROM circular_buyer ORDER BY id LIMIT 1 OFFSET 15),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'SALE','READY_TO_SHIP','wh-sl-0002','이수현','BLEND',      1,22.050,21.000,70,  21000,'2026-06-07 09:00:00',NULL,NULL,'2026-06-07 09:00:00','2026-06-07 09:00:00');

-- ============================================================
-- circular_sale_header: DONATION 10건
-- ============================================================
INSERT INTO circular_sale_header
(sale_no, buyer_id, warehouse_id, sale_type, donee_name, status,
 sold_by_member_id, sold_by_name, material_type,
 total_sku_count, total_requested_weight_kg, total_actual_weight_kg,
 total_sold_quantity, total_amount,
 sold_at, completed_at, outbound_header_id,
 create_date, update_date)
VALUES
('DON-20260115-00031',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'DONATION','굿윌스토어 서울점',  'ARRIVED','wh-sl-0001','박지훈','CASHMERE',  1, 5.250, 5.000,17,0,'2026-01-15 09:00:00','2026-01-18 14:00:00',NULL,'2026-01-15 09:00:00','2026-01-18 14:00:00'),
('DON-20260212-00032',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'DONATION','아름다운가게 홍대점','ARRIVED','wh-sl-0002','이수현','WOOL',       1,12.600,12.000,40,0,'2026-02-12 09:00:00','2026-02-15 14:00:00',NULL,'2026-02-12 09:00:00','2026-02-15 14:00:00'),
('DON-20260318-00033',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'DONATION','옷캔(OtCan)',        'ARRIVED','wh-sl-0001','박지훈','SILK',       1, 6.300, 6.000,20,0,'2026-03-18 09:00:00','2026-03-21 14:00:00',NULL,'2026-03-18 09:00:00','2026-03-21 14:00:00'),
('DON-20260422-00034',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'DONATION','굿윌스토어 서울점',  'ARRIVED','wh-sl-0002','이수현','COTTON',     1,23.625,22.500,75,0,'2026-04-22 09:00:00','2026-04-25 14:00:00',NULL,'2026-04-22 09:00:00','2026-04-25 14:00:00'),
('DON-20260520-00035',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'DONATION','아름다운가게 홍대점','ARRIVED','wh-sl-0001','박지훈','LINEN',      1,28.350,27.000,90,0,'2026-05-20 09:00:00','2026-05-23 14:00:00',NULL,'2026-05-20 09:00:00','2026-05-23 14:00:00'),
('DON-20260603-00036',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'DONATION','옷캔(OtCan)',        'ARRIVED','wh-sl-0002','이수현','POLYESTER',  1,31.500,30.000,100,0,'2026-06-03 09:00:00','2026-06-06 14:00:00',NULL,'2026-06-03 09:00:00','2026-06-06 14:00:00'),
('DON-20260605-00037',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'DONATION','굿윌스토어 서울점',  'IN_TRANSIT','wh-sl-0001','박지훈','ACRYLIC',   1,21.000,20.000,67,0,'2026-06-05 09:00:00',NULL,NULL,'2026-06-05 09:00:00','2026-06-06 10:00:00'),
('DON-20260606-00038',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'DONATION','아름다운가게 홍대점','IN_TRANSIT','wh-sl-0002','이수현','POLYAMIDE', 1,20.475,19.500,65,0,'2026-06-06 09:00:00',NULL,NULL,'2026-06-06 09:00:00','2026-06-07 10:00:00'),
('DON-20260607-00039',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'DONATION','옷캔(OtCan)',        'READY_TO_SHIP','wh-sl-0001','박지훈','ELASTANE', 1,15.750,15.000,50,0,'2026-06-07 09:00:00',NULL,NULL,'2026-06-07 09:00:00','2026-06-07 09:00:00'),
('DON-20260607-00040',NULL,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'DONATION','굿윌스토어 서울점',  'READY_TO_SHIP','wh-sl-0002','이수현','BLEND',     1,33.075,31.500,105,0,'2026-06-07 14:00:00',NULL,NULL,'2026-06-07 14:00:00','2026-06-07 14:00:00');

-- ============================================================
-- circular_sale_item: ROW_NUMBER JOIN 으로 CIRCULAR inventory 순차 매칭
-- main_category = circular_material_price_policy.material_group
--   → UI '소재 분류' 컬럼: NATURAL_SINGLE/SYNTHETIC/BLEND 값 필요
-- ============================================================
INSERT INTO circular_sale_item
(sale_header_id, inventory_id, sku_id, sku_code, product_code, product_name,
 main_category, sub_category, color, size, material_type,
 requested_weight_kg, actual_weight_kg, estimated_quantity,
 sold_quantity, stock_quantity_snapshot, stock_weight_kg_snapshot,
 unit_price, line_amount, create_date, update_date)
SELECT
  h.id,
  ir.inv_id,
  ir.sku_id,
  ps.sku_code,
  ps.product_code,
  pm.name,
  p.material_group,   -- ← UI '소재 분류': NATURAL_SINGLE | SYNTHETIC | BLEND
  NULL,
  ps.color,
  ps.size,
  h.material_type,
  h.total_requested_weight_kg,
  h.total_actual_weight_kg,
  ROUND(h.total_actual_weight_kg / 0.300, 3),
  h.total_sold_quantity,
  150,
  45.000,
  p.price_per_kg,
  CAST(ROUND(h.total_actual_weight_kg) AS UNSIGNED) * p.price_per_kg,
  h.create_date,
  h.update_date
FROM (
  SELECT csh.id, csh.material_type, csh.total_requested_weight_kg, csh.total_actual_weight_kg,
         csh.total_sold_quantity, csh.total_amount, csh.create_date, csh.update_date,
         ROW_NUMBER() OVER (ORDER BY csh.id) AS rn
  FROM circular_sale_header csh
  WHERE csh.sale_no LIKE 'CSR-2026%' OR csh.sale_no LIKE 'DON-2026%'
) h
JOIN (
  SELECT inv.id AS inv_id, inv.sku_id,
         ROW_NUMBER() OVER (ORDER BY inv.id) AS rn
  FROM inventory inv
  WHERE inv.inventory_status = 'CIRCULAR'
) ir ON ir.rn = h.rn
JOIN product_sku ps ON ps.id = ir.sku_id
JOIN product_master pm ON pm.code = ps.product_code
JOIN circular_material_price_policy p ON p.material_code = h.material_type;

-- ============================================================
-- circular_sale_item_material: 단일 소재 100%
-- ============================================================
INSERT INTO circular_sale_item_material
(sale_item_id, material_code, material_name, ratio, sort_order, create_date, update_date)
SELECT
  ci.id,
  ci.material_type,
  CASE ci.material_type
    WHEN 'COTTON'    THEN '면'
    WHEN 'WOOL'      THEN '울'
    WHEN 'CASHMERE'  THEN '캐시미어'
    WHEN 'SILK'      THEN '실크'
    WHEN 'LINEN'     THEN '리넨'
    WHEN 'POLYESTER' THEN '폴리에스터'
    WHEN 'ACRYLIC'   THEN '아크릴'
    WHEN 'POLYAMIDE' THEN '폴리아미드'
    WHEN 'ELASTANE'  THEN '엘라스테인'
    WHEN 'BLEND'     THEN '혼방'
    ELSE ci.material_type
  END,
  100, 1,
  ci.create_date, ci.update_date
FROM circular_sale_item ci
JOIN circular_sale_header ch ON ch.id = ci.sale_header_id
WHERE ch.sale_no LIKE 'CSR-2026%' OR ch.sale_no LIKE 'DON-2026%';

-- ============================================================
-- circular_sale_status_history
-- ============================================================
INSERT INTO circular_sale_status_history
(sale_header_id, from_status, status, changed_at,
 changed_by_member_id, changed_by_name, create_date, update_date)
SELECT id, NULL, 'READY_TO_SHIP', sold_at,
       sold_by_member_id, sold_by_name, sold_at, sold_at
FROM circular_sale_header
WHERE sale_no LIKE 'CSR-2026%' OR sale_no LIKE 'DON-2026%';

INSERT INTO circular_sale_status_history
(sale_header_id, from_status, status, changed_at,
 changed_by_member_id, changed_by_name, create_date, update_date)
SELECT id, 'READY_TO_SHIP', 'IN_TRANSIT',
       DATE_ADD(sold_at, INTERVAL 1 DAY),
       sold_by_member_id, sold_by_name,
       DATE_ADD(sold_at, INTERVAL 1 DAY),
       DATE_ADD(sold_at, INTERVAL 1 DAY)
FROM circular_sale_header
WHERE (sale_no LIKE 'CSR-2026%' OR sale_no LIKE 'DON-2026%')
  AND status IN ('IN_TRANSIT','ARRIVED');

INSERT INTO circular_sale_status_history
(sale_header_id, from_status, status, changed_at,
 changed_by_member_id, changed_by_name, create_date, update_date)
SELECT id, 'IN_TRANSIT', 'ARRIVED', completed_at,
       sold_by_member_id, sold_by_name, completed_at, completed_at
FROM circular_sale_header
WHERE (sale_no LIKE 'CSR-2026%' OR sale_no LIKE 'DON-2026%')
  AND status = 'ARRIVED';

-- ============================================================
-- ▣ material_type → FE 한글 표시 라벨로 변환
--   FE materialTypeLabel() 함수 인식값: '천연 단일 섬유' | '합성 섬유' | '혼방'
--   circular_sale_header / circular_sale_item 모두 업데이트
--   (item INSERT 시 JOIN을 위해 먼저 영문 코드로 삽입 → 여기서 변환)
-- ============================================================
SET SQL_SAFE_UPDATES = 0;

UPDATE circular_sale_header
SET material_type = CASE material_type
  WHEN 'CASHMERE'  THEN '천연 단일 섬유'
  WHEN 'SILK'      THEN '천연 단일 섬유'
  WHEN 'WOOL'      THEN '천연 단일 섬유'
  WHEN 'COTTON'    THEN '천연 단일 섬유'
  WHEN 'LINEN'     THEN '천연 단일 섬유'
  WHEN 'POLYESTER' THEN '합성 섬유'
  WHEN 'ACRYLIC'   THEN '합성 섬유'
  WHEN 'POLYAMIDE' THEN '합성 섬유'
  WHEN 'ELASTANE'  THEN '합성 섬유'
  WHEN 'BLEND'     THEN '혼방'
  ELSE material_type
END,
update_date = NOW()
WHERE sale_no LIKE 'CSR-2026%' OR sale_no LIKE 'DON-2026%';

UPDATE circular_sale_item ci
JOIN circular_sale_header ch ON ch.id = ci.sale_header_id
SET ci.material_type = CASE ci.material_type
  WHEN 'CASHMERE'  THEN '천연 단일 섬유'
  WHEN 'SILK'      THEN '천연 단일 섬유'
  WHEN 'WOOL'      THEN '천연 단일 섬유'
  WHEN 'COTTON'    THEN '천연 단일 섬유'
  WHEN 'LINEN'     THEN '천연 단일 섬유'
  WHEN 'POLYESTER' THEN '합성 섬유'
  WHEN 'ACRYLIC'   THEN '합성 섬유'
  WHEN 'POLYAMIDE' THEN '합성 섬유'
  WHEN 'ELASTANE'  THEN '합성 섬유'
  WHEN 'BLEND'     THEN '혼방'
  ELSE ci.material_type
END,
ci.update_date = NOW()
WHERE ch.sale_no LIKE 'CSR-2026%' OR ch.sale_no LIKE 'DON-2026%';

SET SQL_SAFE_UPDATES = 1;
