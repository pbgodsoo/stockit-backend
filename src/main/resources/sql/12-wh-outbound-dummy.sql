-- 12-wh-outbound-dummy.sql
-- 창고 출고 더미 데이터 (38건: 발주 40건 중 CANCELLED 2건 제외)
-- 의존: 11-store-orders-dummy.sql
--
-- 출고 상태 매핑 (발주 상태 → 출고 상태):
--   발주 COMPLETED  → 출고 ARRIVED      (confirmed + departed + arrived 모두 설정)
--   발주 APPROVED   → 출고 IN_TRANSIT   (confirmed + departed, arrived=NULL)
--   발주 REQUESTED  → 출고 READY_TO_SHIP (confirmed=NULL)
--   발주 CANCELLED  → 출고 없음 (orders 10, 29 제외)
--
-- outbound 1~9   ↔ order 1~9   (강남 COMPLETED)
-- outbound 10~13 ↔ order 11~14 (강남 APPROVED)
-- outbound 14~19 ↔ order 15~20 (강남 REQUESTED)
-- outbound 20~27 ↔ order 21~28 (홍대 COMPLETED)
-- outbound 28~31 ↔ order 30~33 (홍대 APPROVED)
-- outbound 32~38 ↔ order 34~40 (홍대 REQUESTED)

SET SQL_SAFE_UPDATES = 0;
DELETE FROM wh_outbound_status_history WHERE outbound_header_id BETWEEN 1 AND 38;
DELETE FROM wh_outbound_item           WHERE outbound_header_id BETWEEN 1 AND 38;
DELETE FROM wh_outbound_header         WHERE id BETWEEN 1 AND 38;
SET SQL_SAFE_UPDATES = 1;

-- ===== wh_outbound_header (38건) =====
-- total_requested_quantity: 11발주의 패턴에 맞춰 동기화
-- P1(%=1)=25, P2(%=2)=20, P3(%=3)=22, P4(%=4)=23, P5(%=5)=22
-- P6(%=6)=18, P7(%=7)=25, P8(%=8)=20, P9(%=9)=18, P10(%=0)=22
INSERT INTO wh_outbound_header
(id, outbound_no, source_type, source_ref_no, source_ref_seq, source_ref_id,
 warehouse_id, destination_type, destination_id,
 status, total_requested_quantity,
 requested_at, confirmed_at, departed_at, arrived_at,
 requested_by_member_id, requested_by_name,
 create_date, update_date)
VALUES
-- ── 강남 ARRIVED (orders 1~9) → WH-SL-0001 ──
(1,  'OUT-20260106-00001','STORE_ORDER','ORD-20260105-00001',1,1, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 25,'2026-01-06 09:00:00','2026-01-06 10:00:00','2026-01-07 08:00:00','2026-01-10 09:00:00','hq0001','본사관리자','2026-01-06 09:00:00','2026-01-10 09:00:00'),
(2,  'OUT-20260116-00002','STORE_ORDER','ORD-20260115-00002',1,2, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 20,'2026-01-16 09:00:00','2026-01-16 10:00:00','2026-01-17 08:00:00','2026-01-20 09:00:00','hq0001','본사관리자','2026-01-16 09:00:00','2026-01-20 09:00:00'),
(3,  'OUT-20260126-00003','STORE_ORDER','ORD-20260125-00003',1,3, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 22,'2026-01-26 09:00:00','2026-01-26 10:00:00','2026-01-27 08:00:00','2026-01-30 09:00:00','hq0001','본사관리자','2026-01-26 09:00:00','2026-01-30 09:00:00'),
(4,  'OUT-20260211-00004','STORE_ORDER','ORD-20260210-00004',1,4, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 23,'2026-02-11 09:00:00','2026-02-11 10:00:00','2026-02-12 08:00:00','2026-02-15 09:00:00','hq0001','본사관리자','2026-02-11 09:00:00','2026-02-15 09:00:00'),
(5,  'OUT-20260221-00005','STORE_ORDER','ORD-20260220-00005',1,5, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 22,'2026-02-21 09:00:00','2026-02-21 10:00:00','2026-02-22 08:00:00','2026-02-25 09:00:00','hq0001','본사관리자','2026-02-21 09:00:00','2026-02-25 09:00:00'),
(6,  'OUT-20260306-00006','STORE_ORDER','ORD-20260305-00006',1,6, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 18,'2026-03-06 09:00:00','2026-03-06 10:00:00','2026-03-07 08:00:00','2026-03-10 09:00:00','hq0001','본사관리자','2026-03-06 09:00:00','2026-03-10 09:00:00'),
(7,  'OUT-20260319-00007','STORE_ORDER','ORD-20260318-00007',1,7, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 25,'2026-03-19 09:00:00','2026-03-19 10:00:00','2026-03-20 08:00:00','2026-03-23 09:00:00','hq0001','본사관리자','2026-03-19 09:00:00','2026-03-23 09:00:00'),
(8,  'OUT-20260403-00008','STORE_ORDER','ORD-20260402-00008',1,8, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 20,'2026-04-03 09:00:00','2026-04-03 10:00:00','2026-04-04 08:00:00','2026-04-07 09:00:00','hq0001','본사관리자','2026-04-03 09:00:00','2026-04-07 09:00:00'),
(9,  'OUT-20260416-00009','STORE_ORDER','ORD-20260415-00009',1,9, (SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'ARRIVED', 18,'2026-04-16 09:00:00','2026-04-16 10:00:00','2026-04-17 08:00:00','2026-04-20 09:00:00','hq0001','본사관리자','2026-04-16 09:00:00','2026-04-20 09:00:00'),
-- ── 강남 IN_TRANSIT (orders 11~14) ──
(10,'OUT-20260506-00010','STORE_ORDER','ORD-20260505-00011',1,11,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'IN_TRANSIT',25,'2026-05-06 10:00:00','2026-05-06 11:00:00','2026-05-07 08:00:00',NULL,'hq0001','본사관리자','2026-05-06 10:00:00','2026-05-07 08:00:00'),
(11,'OUT-20260513-00011','STORE_ORDER','ORD-20260512-00012',1,12,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'IN_TRANSIT',20,'2026-05-13 10:00:00','2026-05-13 11:00:00','2026-05-14 08:00:00',NULL,'hq0001','본사관리자','2026-05-13 10:00:00','2026-05-14 08:00:00'),
(12,'OUT-20260521-00012','STORE_ORDER','ORD-20260520-00013',1,13,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'IN_TRANSIT',22,'2026-05-21 10:00:00','2026-05-21 11:00:00','2026-05-22 08:00:00',NULL,'hq0001','본사관리자','2026-05-21 10:00:00','2026-05-22 08:00:00'),
(13,'OUT-20260529-00013','STORE_ORDER','ORD-20260528-00014',1,14,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'IN_TRANSIT',23,'2026-05-29 10:00:00','2026-05-29 11:00:00','2026-05-30 08:00:00',NULL,'hq0001','본사관리자','2026-05-29 10:00:00','2026-05-30 08:00:00'),
-- ── 강남 READY_TO_SHIP (orders 15~20) ──
(14,'OUT-20260601-00014','STORE_ORDER','ORD-20260601-00015',1,15,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'READY_TO_SHIP',22,'2026-06-01 09:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-01 09:00:00','2026-06-01 09:00:00'),
(15,'OUT-20260602-00015','STORE_ORDER','ORD-20260602-00016',1,16,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'READY_TO_SHIP',18,'2026-06-02 10:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-02 10:00:00','2026-06-02 10:00:00'),
(16,'OUT-20260603-00016','STORE_ORDER','ORD-20260603-00017',1,17,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'READY_TO_SHIP',25,'2026-06-03 09:30:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-03 09:30:00','2026-06-03 09:30:00'),
(17,'OUT-20260604-00017','STORE_ORDER','ORD-20260604-00018',1,18,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'READY_TO_SHIP',20,'2026-06-04 14:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-04 14:00:00','2026-06-04 14:00:00'),
(18,'OUT-20260605-00018','STORE_ORDER','ORD-20260605-00019',1,19,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'READY_TO_SHIP',18,'2026-06-05 09:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-05 09:00:00','2026-06-05 09:00:00'),
(19,'OUT-20260606-00019','STORE_ORDER','ORD-20260606-00020',1,20,(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'READY_TO_SHIP',22,'2026-06-06 11:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-06 11:00:00','2026-06-06 11:00:00'),
-- ── 홍대 ARRIVED (orders 21~28) → WH-SL-0002 ──
(20,'OUT-20260109-00020','STORE_ORDER','ORD-20260108-00021',1,21,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'ARRIVED', 25,'2026-01-09 09:00:00','2026-01-09 10:00:00','2026-01-10 08:00:00','2026-01-13 09:00:00','hq0001','본사관리자','2026-01-09 09:00:00','2026-01-13 09:00:00'),
(21,'OUT-20260119-00021','STORE_ORDER','ORD-20260118-00022',1,22,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'ARRIVED', 20,'2026-01-19 09:00:00','2026-01-19 10:00:00','2026-01-20 08:00:00','2026-01-23 09:00:00','hq0001','본사관리자','2026-01-19 09:00:00','2026-01-23 09:00:00'),
(22,'OUT-20260129-00022','STORE_ORDER','ORD-20260128-00023',1,23,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'ARRIVED', 22,'2026-01-29 09:00:00','2026-01-29 10:00:00','2026-01-30 08:00:00','2026-02-02 09:00:00','hq0001','본사관리자','2026-01-29 09:00:00','2026-02-02 09:00:00'),
(23,'OUT-20260213-00023','STORE_ORDER','ORD-20260212-00024',1,24,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'ARRIVED', 23,'2026-02-13 09:00:00','2026-02-13 10:00:00','2026-02-14 08:00:00','2026-02-17 09:00:00','hq0001','본사관리자','2026-02-13 09:00:00','2026-02-17 09:00:00'),
(24,'OUT-20260223-00024','STORE_ORDER','ORD-20260222-00025',1,25,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'ARRIVED', 22,'2026-02-23 09:00:00','2026-02-23 10:00:00','2026-02-24 08:00:00','2026-02-27 09:00:00','hq0001','본사관리자','2026-02-23 09:00:00','2026-02-27 09:00:00'),
(25,'OUT-20260316-00025','STORE_ORDER','ORD-20260315-00026',1,26,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'ARRIVED', 18,'2026-03-16 09:00:00','2026-03-16 10:00:00','2026-03-17 08:00:00','2026-03-20 09:00:00','hq0001','본사관리자','2026-03-16 09:00:00','2026-03-20 09:00:00'),
(26,'OUT-20260329-00026','STORE_ORDER','ORD-20260328-00027',1,27,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'ARRIVED', 25,'2026-03-29 09:00:00','2026-03-29 10:00:00','2026-03-30 08:00:00','2026-04-02 09:00:00','hq0001','본사관리자','2026-03-29 09:00:00','2026-04-02 09:00:00'),
(27,'OUT-20260406-00027','STORE_ORDER','ORD-20260405-00028',1,28,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'ARRIVED', 20,'2026-04-06 09:00:00','2026-04-06 10:00:00','2026-04-07 08:00:00','2026-04-10 09:00:00','hq0001','본사관리자','2026-04-06 09:00:00','2026-04-10 09:00:00'),
-- ── 홍대 IN_TRANSIT (orders 30~33) ──
(28,'OUT-20260509-00028','STORE_ORDER','ORD-20260508-00030',1,30,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'IN_TRANSIT',22,'2026-05-09 10:00:00','2026-05-09 11:00:00','2026-05-10 08:00:00',NULL,'hq0001','본사관리자','2026-05-09 10:00:00','2026-05-10 08:00:00'),
(29,'OUT-20260516-00029','STORE_ORDER','ORD-20260515-00031',1,31,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'IN_TRANSIT',25,'2026-05-16 10:00:00','2026-05-16 11:00:00','2026-05-17 08:00:00',NULL,'hq0001','본사관리자','2026-05-16 10:00:00','2026-05-17 08:00:00'),
(30,'OUT-20260523-00030','STORE_ORDER','ORD-20260522-00032',1,32,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'IN_TRANSIT',20,'2026-05-23 10:00:00','2026-05-23 11:00:00','2026-05-24 08:00:00',NULL,'hq0001','본사관리자','2026-05-23 10:00:00','2026-05-24 08:00:00'),
(31,'OUT-20260531-00031','STORE_ORDER','ORD-20260530-00033',1,33,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'IN_TRANSIT',22,'2026-05-31 10:00:00','2026-05-31 11:00:00','2026-06-01 08:00:00',NULL,'hq0001','본사관리자','2026-05-31 10:00:00','2026-06-01 08:00:00'),
-- ── 홍대 READY_TO_SHIP (orders 34~40) ──
(32,'OUT-20260601-00032','STORE_ORDER','ORD-20260601-00034',1,34,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'READY_TO_SHIP',23,'2026-06-01 10:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-01 10:00:00','2026-06-01 10:00:00'),
(33,'OUT-20260602-00033','STORE_ORDER','ORD-20260602-00035',1,35,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'READY_TO_SHIP',22,'2026-06-02 09:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-02 09:00:00','2026-06-02 09:00:00'),
(34,'OUT-20260603-00034','STORE_ORDER','ORD-20260603-00036',1,36,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'READY_TO_SHIP',18,'2026-06-03 14:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-03 14:00:00','2026-06-03 14:00:00'),
(35,'OUT-20260604-00035','STORE_ORDER','ORD-20260604-00037',1,37,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'READY_TO_SHIP',25,'2026-06-04 09:30:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-04 09:30:00','2026-06-04 09:30:00'),
(36,'OUT-20260605-00036','STORE_ORDER','ORD-20260605-00038',1,38,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'READY_TO_SHIP',20,'2026-06-05 10:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-05 10:00:00','2026-06-05 10:00:00'),
(37,'OUT-20260606-00037','STORE_ORDER','ORD-20260606-00039',1,39,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'READY_TO_SHIP',18,'2026-06-06 09:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-06 09:00:00','2026-06-06 09:00:00'),
(38,'OUT-20260607-00038','STORE_ORDER','ORD-20260607-00040',1,40,(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'STORE',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'READY_TO_SHIP',22,'2026-06-07 14:00:00',NULL,NULL,NULL,'hq0001','본사관리자','2026-06-07 14:00:00','2026-06-07 14:00:00');


-- ===== wh_outbound_item (76건, 헤더당 2건) =====
-- source_line_ref_id = store_order_item.id (11번 파일 기준)
-- 각 outbound의 SKU/수량은 대응하는 store_order_item과 동일하게 동기화
INSERT INTO wh_outbound_item
(id, outbound_header_id, source_line_ref_id, sku_id, sku_code, product_code, product_name, main_category, sub_category, color, size, unit_price, requested_quantity, create_date, update_date)
VALUES
-- outbound 1 ↔ order 1 P1: OUT-PD-001×10 + TOP-SS-001×15
(1,  1,  1,  (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-01-06 09:00:00','2026-01-06 09:00:00'),
(2,  1,  2,  (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-01-06 09:00:00','2026-01-06 09:00:00'),
-- outbound 2 ↔ order 2 P2: OUT-HZ-002×8 + PNT-DN-002×12
(3,  2,  3,  (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-01-16 09:00:00','2026-01-16 09:00:00'),
(4,  2,  4,  (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-01-16 09:00:00','2026-01-16 09:00:00'),
-- outbound 3 ↔ order 3 P3: TOP-KN-001×12 + PNT-LG-001×10
(5,  3,  5,  (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-01-26 09:00:00','2026-01-26 09:00:00'),
(6,  3,  6,  (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-01-26 09:00:00','2026-01-26 09:00:00'),
-- outbound 4 ↔ order 4 P4: OUT-JK-001×8 + TOP-LS-002×15
(7,  4,  7,  (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-02-11 09:00:00','2026-02-11 09:00:00'),
(8,  4,  8,  (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-02-11 09:00:00','2026-02-11 09:00:00'),
-- outbound 5 ↔ order 5 P5: TOP-HD-001×10 + PNT-TR-002×12
(9,  5,  9,  (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-02-21 09:00:00','2026-02-21 09:00:00'),
(10, 5,  10, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-02-21 09:00:00','2026-02-21 09:00:00'),
-- outbound 6 ↔ order 6 P6: OUT-CD-002×10 + SKT-LG-001×8
(11, 6,  11, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-03-06 09:00:00','2026-03-06 09:00:00'),
(12, 6,  12, (SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-03-06 09:00:00','2026-03-06 09:00:00'),
-- outbound 7 ↔ order 7 P7: TOP-SH-001×15 + PNT-DN-001×10
(13, 7,  13, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-03-19 09:00:00','2026-03-19 09:00:00'),
(14, 7,  14, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-03-19 09:00:00','2026-03-19 09:00:00'),
-- outbound 8 ↔ order 8 P8: OUT-PD-002×8 + TOP-KN-002×12
(15, 8,  15, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-04-03 09:00:00','2026-04-03 09:00:00'),
(16, 8,  16, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-04-03 09:00:00','2026-04-03 09:00:00'),
-- outbound 9 ↔ order 9 P9: TOP-HD-002×10 + PNT-LG-002×8
(17, 9,  17, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-002-BLK-M'),'PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','브러시드 기모 후드티','상의','후드티','BLK','M',48900,10,'2026-04-16 09:00:00','2026-04-16 09:00:00'),
(18, 9,  18, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',47900,8,'2026-04-16 09:00:00','2026-04-16 09:00:00'),
-- outbound 10 ↔ order 11 P1: OUT-PD-001×10 + TOP-SS-001×15
(19,10,  21, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-05-06 10:00:00','2026-05-06 10:00:00'),
(20,10,  22, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-05-06 10:00:00','2026-05-06 10:00:00'),
-- outbound 11 ↔ order 12 P2: OUT-HZ-002×8 + PNT-DN-002×12
(21,11,  23, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-05-13 10:00:00','2026-05-13 10:00:00'),
(22,11,  24, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-05-13 10:00:00','2026-05-13 10:00:00'),
-- outbound 12 ↔ order 13 P3: TOP-KN-001×12 + PNT-LG-001×10
(23,12,  25, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-05-21 10:00:00','2026-05-21 10:00:00'),
(24,12,  26, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-05-21 10:00:00','2026-05-21 10:00:00'),
-- outbound 13 ↔ order 14 P4: OUT-JK-001×8 + TOP-LS-002×15
(25,13,  27, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-05-29 10:00:00','2026-05-29 10:00:00'),
(26,13,  28, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-05-29 10:00:00','2026-05-29 10:00:00'),
-- outbound 14 ↔ order 15 P5: TOP-HD-001×10 + PNT-TR-002×12
(27,14,  29, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-06-01 09:00:00','2026-06-01 09:00:00'),
(28,14,  30, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-06-01 09:00:00','2026-06-01 09:00:00'),
-- outbound 15 ↔ order 16 P6: OUT-CD-002×10 + SKT-LG-001×8
(29,15,  31, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-06-02 10:00:00','2026-06-02 10:00:00'),
(30,15,  32, (SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-06-02 10:00:00','2026-06-02 10:00:00'),
-- outbound 16 ↔ order 17 P7: TOP-SH-001×15 + PNT-DN-001×10
(31,16,  33, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-06-03 09:30:00','2026-06-03 09:30:00'),
(32,16,  34, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-06-03 09:30:00','2026-06-03 09:30:00'),
-- outbound 17 ↔ order 18 P8: OUT-PD-002×8 + TOP-KN-002×12
(33,17,  35, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-06-04 14:00:00','2026-06-04 14:00:00'),
(34,17,  36, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-06-04 14:00:00','2026-06-04 14:00:00'),
-- outbound 18 ↔ order 19 P9: TOP-HD-002×10 + PNT-LG-002×8
(35,18,  37, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-002-BLK-M'),'PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','브러시드 기모 후드티','상의','후드티','BLK','M',48900,10,'2026-06-05 09:00:00','2026-06-05 09:00:00'),
(36,18,  38, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',47900,8,'2026-06-05 09:00:00','2026-06-05 09:00:00'),
-- outbound 19 ↔ order 20 P10: OUT-HZ-001×10 + PNT-TR-001×12
(37,19,  39, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-WHT-M'),'PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','WHT','M',52900,10,'2026-06-06 11:00:00','2026-06-06 11:00:00'),
(38,19,  40, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',38900,12,'2026-06-06 11:00:00','2026-06-06 11:00:00'),
-- outbound 20 ↔ order 21 P1: OUT-PD-001×10 + TOP-SS-001×15
(39,20,  41, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-01-09 09:00:00','2026-01-09 09:00:00'),
(40,20,  42, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-01-09 09:00:00','2026-01-09 09:00:00'),
-- outbound 21 ↔ order 22 P2: OUT-HZ-002×8 + PNT-DN-002×12
(41,21,  43, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-01-19 09:00:00','2026-01-19 09:00:00'),
(42,21,  44, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-01-19 09:00:00','2026-01-19 09:00:00'),
-- outbound 22 ↔ order 23 P3: TOP-KN-001×12 + PNT-LG-001×10
(43,22,  45, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-01-29 09:00:00','2026-01-29 09:00:00'),
(44,22,  46, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-01-29 09:00:00','2026-01-29 09:00:00'),
-- outbound 23 ↔ order 24 P4: OUT-JK-001×8 + TOP-LS-002×15
(45,23,  47, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-02-13 09:00:00','2026-02-13 09:00:00'),
(46,23,  48, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-02-13 09:00:00','2026-02-13 09:00:00'),
-- outbound 24 ↔ order 25 P5: TOP-HD-001×10 + PNT-TR-002×12
(47,24,  49, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-02-23 09:00:00','2026-02-23 09:00:00'),
(48,24,  50, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-02-23 09:00:00','2026-02-23 09:00:00'),
-- outbound 25 ↔ order 26 P6: OUT-CD-002×10 + SKT-LG-001×8
(49,25,  51, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-03-16 09:00:00','2026-03-16 09:00:00'),
(50,25,  52, (SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-03-16 09:00:00','2026-03-16 09:00:00'),
-- outbound 26 ↔ order 27 P7: TOP-SH-001×15 + PNT-DN-001×10
(51,26,  53, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-03-29 09:00:00','2026-03-29 09:00:00'),
(52,26,  54, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-03-29 09:00:00','2026-03-29 09:00:00'),
-- outbound 27 ↔ order 28 P8: OUT-PD-002×8 + TOP-KN-002×12
(53,27,  55, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-04-06 09:00:00','2026-04-06 09:00:00'),
(54,27,  56, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-04-06 09:00:00','2026-04-06 09:00:00'),
-- outbound 28 ↔ order 30 P10: OUT-HZ-001×10 + PNT-TR-001×12
(55,28,  59, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-WHT-M'),'PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','WHT','M',52900,10,'2026-05-09 10:00:00','2026-05-09 10:00:00'),
(56,28,  60, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',38900,12,'2026-05-09 10:00:00','2026-05-09 10:00:00'),
-- outbound 29 ↔ order 31 P1: OUT-PD-001×10 + TOP-SS-001×15
(57,29,  61, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-05-16 10:00:00','2026-05-16 10:00:00'),
(58,29,  62, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-05-16 10:00:00','2026-05-16 10:00:00'),
-- outbound 30 ↔ order 32 P2: OUT-HZ-002×8 + PNT-DN-002×12
(59,30,  63, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-05-23 10:00:00','2026-05-23 10:00:00'),
(60,30,  64, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-05-23 10:00:00','2026-05-23 10:00:00'),
-- outbound 31 ↔ order 33 P3: TOP-KN-001×12 + PNT-LG-001×10
(61,31,  65, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-05-31 10:00:00','2026-05-31 10:00:00'),
(62,31,  66, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-05-31 10:00:00','2026-05-31 10:00:00'),
-- outbound 32 ↔ order 34 P4: OUT-JK-001×8 + TOP-LS-002×15
(63,32,  67, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-06-01 10:00:00','2026-06-01 10:00:00'),
(64,32,  68, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-06-01 10:00:00','2026-06-01 10:00:00'),
-- outbound 33 ↔ order 35 P5: TOP-HD-001×10 + PNT-TR-002×12
(65,33,  69, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-06-02 09:00:00','2026-06-02 09:00:00'),
(66,33,  70, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-06-02 09:00:00','2026-06-02 09:00:00'),
-- outbound 34 ↔ order 36 P6: OUT-CD-002×10 + SKT-LG-001×8
(67,34,  71, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-06-03 14:00:00','2026-06-03 14:00:00'),
(68,34,  72, (SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-06-03 14:00:00','2026-06-03 14:00:00'),
-- outbound 35 ↔ order 37 P7: TOP-SH-001×15 + PNT-DN-001×10
(69,35,  73, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-06-04 09:30:00','2026-06-04 09:30:00'),
(70,35,  74, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-06-04 09:30:00','2026-06-04 09:30:00'),
-- outbound 36 ↔ order 38 P8: OUT-PD-002×8 + TOP-KN-002×12
(71,36,  75, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-06-05 10:00:00','2026-06-05 10:00:00'),
(72,36,  76, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-06-05 10:00:00','2026-06-05 10:00:00'),
-- outbound 37 ↔ order 39 P9: TOP-HD-002×10 + PNT-LG-002×8
(73,37,  77, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-002-BLK-M'),'PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','브러시드 기모 후드티','상의','후드티','BLK','M',48900,10,'2026-06-06 09:00:00','2026-06-06 09:00:00'),
(74,37,  78, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',47900,8,'2026-06-06 09:00:00','2026-06-06 09:00:00'),
-- outbound 38 ↔ order 40 P10: OUT-HZ-001×10 + PNT-TR-001×12
(75,38,  79, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-WHT-M'),'PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','WHT','M',52900,10,'2026-06-07 14:00:00','2026-06-07 14:00:00'),
(76,38,  80, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',38900,12,'2026-06-07 14:00:00','2026-06-07 14:00:00');


-- ===== wh_outbound_status_history =====
INSERT INTO wh_outbound_status_history
(outbound_header_id, status, changed_at, changed_by_member_id, changed_by_name, reason, create_date, update_date)
VALUES
-- 강남 ARRIVED (outbound 1~9)
(1,'READY_TO_SHIP','2026-01-06 09:00:00','hq0001','본사관리자',NULL,'2026-01-06 09:00:00','2026-01-06 09:00:00'),
(1,'IN_TRANSIT',  '2026-01-07 08:00:00','hq0001','본사관리자',NULL,'2026-01-07 08:00:00','2026-01-07 08:00:00'),
(1,'ARRIVED',     '2026-01-10 09:00:00','hq0001','본사관리자',NULL,'2026-01-10 09:00:00','2026-01-10 09:00:00'),
(2,'READY_TO_SHIP','2026-01-16 09:00:00','hq0001','본사관리자',NULL,'2026-01-16 09:00:00','2026-01-16 09:00:00'),
(2,'IN_TRANSIT',  '2026-01-17 08:00:00','hq0001','본사관리자',NULL,'2026-01-17 08:00:00','2026-01-17 08:00:00'),
(2,'ARRIVED',     '2026-01-20 09:00:00','hq0001','본사관리자',NULL,'2026-01-20 09:00:00','2026-01-20 09:00:00'),
(3,'READY_TO_SHIP','2026-01-26 09:00:00','hq0001','본사관리자',NULL,'2026-01-26 09:00:00','2026-01-26 09:00:00'),
(3,'IN_TRANSIT',  '2026-01-27 08:00:00','hq0001','본사관리자',NULL,'2026-01-27 08:00:00','2026-01-27 08:00:00'),
(3,'ARRIVED',     '2026-01-30 09:00:00','hq0001','본사관리자',NULL,'2026-01-30 09:00:00','2026-01-30 09:00:00'),
(4,'READY_TO_SHIP','2026-02-11 09:00:00','hq0001','본사관리자',NULL,'2026-02-11 09:00:00','2026-02-11 09:00:00'),
(4,'IN_TRANSIT',  '2026-02-12 08:00:00','hq0001','본사관리자',NULL,'2026-02-12 08:00:00','2026-02-12 08:00:00'),
(4,'ARRIVED',     '2026-02-15 09:00:00','hq0001','본사관리자',NULL,'2026-02-15 09:00:00','2026-02-15 09:00:00'),
(5,'READY_TO_SHIP','2026-02-21 09:00:00','hq0001','본사관리자',NULL,'2026-02-21 09:00:00','2026-02-21 09:00:00'),
(5,'IN_TRANSIT',  '2026-02-22 08:00:00','hq0001','본사관리자',NULL,'2026-02-22 08:00:00','2026-02-22 08:00:00'),
(5,'ARRIVED',     '2026-02-25 09:00:00','hq0001','본사관리자',NULL,'2026-02-25 09:00:00','2026-02-25 09:00:00'),
(6,'READY_TO_SHIP','2026-03-06 09:00:00','hq0001','본사관리자',NULL,'2026-03-06 09:00:00','2026-03-06 09:00:00'),
(6,'IN_TRANSIT',  '2026-03-07 08:00:00','hq0001','본사관리자',NULL,'2026-03-07 08:00:00','2026-03-07 08:00:00'),
(6,'ARRIVED',     '2026-03-10 09:00:00','hq0001','본사관리자',NULL,'2026-03-10 09:00:00','2026-03-10 09:00:00'),
(7,'READY_TO_SHIP','2026-03-19 09:00:00','hq0001','본사관리자',NULL,'2026-03-19 09:00:00','2026-03-19 09:00:00'),
(7,'IN_TRANSIT',  '2026-03-20 08:00:00','hq0001','본사관리자',NULL,'2026-03-20 08:00:00','2026-03-20 08:00:00'),
(7,'ARRIVED',     '2026-03-23 09:00:00','hq0001','본사관리자',NULL,'2026-03-23 09:00:00','2026-03-23 09:00:00'),
(8,'READY_TO_SHIP','2026-04-03 09:00:00','hq0001','본사관리자',NULL,'2026-04-03 09:00:00','2026-04-03 09:00:00'),
(8,'IN_TRANSIT',  '2026-04-04 08:00:00','hq0001','본사관리자',NULL,'2026-04-04 08:00:00','2026-04-04 08:00:00'),
(8,'ARRIVED',     '2026-04-07 09:00:00','hq0001','본사관리자',NULL,'2026-04-07 09:00:00','2026-04-07 09:00:00'),
(9,'READY_TO_SHIP','2026-04-16 09:00:00','hq0001','본사관리자',NULL,'2026-04-16 09:00:00','2026-04-16 09:00:00'),
(9,'IN_TRANSIT',  '2026-04-17 08:00:00','hq0001','본사관리자',NULL,'2026-04-17 08:00:00','2026-04-17 08:00:00'),
(9,'ARRIVED',     '2026-04-20 09:00:00','hq0001','본사관리자',NULL,'2026-04-20 09:00:00','2026-04-20 09:00:00'),
-- 강남 IN_TRANSIT (outbound 10~13)
(10,'READY_TO_SHIP','2026-05-06 10:00:00','hq0001','본사관리자',NULL,'2026-05-06 10:00:00','2026-05-06 10:00:00'),
(10,'IN_TRANSIT',  '2026-05-07 08:00:00','hq0001','본사관리자',NULL,'2026-05-07 08:00:00','2026-05-07 08:00:00'),
(11,'READY_TO_SHIP','2026-05-13 10:00:00','hq0001','본사관리자',NULL,'2026-05-13 10:00:00','2026-05-13 10:00:00'),
(11,'IN_TRANSIT',  '2026-05-14 08:00:00','hq0001','본사관리자',NULL,'2026-05-14 08:00:00','2026-05-14 08:00:00'),
(12,'READY_TO_SHIP','2026-05-21 10:00:00','hq0001','본사관리자',NULL,'2026-05-21 10:00:00','2026-05-21 10:00:00'),
(12,'IN_TRANSIT',  '2026-05-22 08:00:00','hq0001','본사관리자',NULL,'2026-05-22 08:00:00','2026-05-22 08:00:00'),
(13,'READY_TO_SHIP','2026-05-29 10:00:00','hq0001','본사관리자',NULL,'2026-05-29 10:00:00','2026-05-29 10:00:00'),
(13,'IN_TRANSIT',  '2026-05-30 08:00:00','hq0001','본사관리자',NULL,'2026-05-30 08:00:00','2026-05-30 08:00:00'),
-- 강남 READY_TO_SHIP (outbound 14~19)
(14,'READY_TO_SHIP','2026-06-01 09:00:00','hq0001','본사관리자',NULL,'2026-06-01 09:00:00','2026-06-01 09:00:00'),
(15,'READY_TO_SHIP','2026-06-02 10:00:00','hq0001','본사관리자',NULL,'2026-06-02 10:00:00','2026-06-02 10:00:00'),
(16,'READY_TO_SHIP','2026-06-03 09:30:00','hq0001','본사관리자',NULL,'2026-06-03 09:30:00','2026-06-03 09:30:00'),
(17,'READY_TO_SHIP','2026-06-04 14:00:00','hq0001','본사관리자',NULL,'2026-06-04 14:00:00','2026-06-04 14:00:00'),
(18,'READY_TO_SHIP','2026-06-05 09:00:00','hq0001','본사관리자',NULL,'2026-06-05 09:00:00','2026-06-05 09:00:00'),
(19,'READY_TO_SHIP','2026-06-06 11:00:00','hq0001','본사관리자',NULL,'2026-06-06 11:00:00','2026-06-06 11:00:00'),
-- 홍대 ARRIVED (outbound 20~27)
(20,'READY_TO_SHIP','2026-01-09 09:00:00','hq0001','본사관리자',NULL,'2026-01-09 09:00:00','2026-01-09 09:00:00'),
(20,'IN_TRANSIT',  '2026-01-10 08:00:00','hq0001','본사관리자',NULL,'2026-01-10 08:00:00','2026-01-10 08:00:00'),
(20,'ARRIVED',     '2026-01-13 09:00:00','hq0001','본사관리자',NULL,'2026-01-13 09:00:00','2026-01-13 09:00:00'),
(21,'READY_TO_SHIP','2026-01-19 09:00:00','hq0001','본사관리자',NULL,'2026-01-19 09:00:00','2026-01-19 09:00:00'),
(21,'IN_TRANSIT',  '2026-01-20 08:00:00','hq0001','본사관리자',NULL,'2026-01-20 08:00:00','2026-01-20 08:00:00'),
(21,'ARRIVED',     '2026-01-23 09:00:00','hq0001','본사관리자',NULL,'2026-01-23 09:00:00','2026-01-23 09:00:00'),
(22,'READY_TO_SHIP','2026-01-29 09:00:00','hq0001','본사관리자',NULL,'2026-01-29 09:00:00','2026-01-29 09:00:00'),
(22,'IN_TRANSIT',  '2026-01-30 08:00:00','hq0001','본사관리자',NULL,'2026-01-30 08:00:00','2026-01-30 08:00:00'),
(22,'ARRIVED',     '2026-02-02 09:00:00','hq0001','본사관리자',NULL,'2026-02-02 09:00:00','2026-02-02 09:00:00'),
(23,'READY_TO_SHIP','2026-02-13 09:00:00','hq0001','본사관리자',NULL,'2026-02-13 09:00:00','2026-02-13 09:00:00'),
(23,'IN_TRANSIT',  '2026-02-14 08:00:00','hq0001','본사관리자',NULL,'2026-02-14 08:00:00','2026-02-14 08:00:00'),
(23,'ARRIVED',     '2026-02-17 09:00:00','hq0001','본사관리자',NULL,'2026-02-17 09:00:00','2026-02-17 09:00:00'),
(24,'READY_TO_SHIP','2026-02-23 09:00:00','hq0001','본사관리자',NULL,'2026-02-23 09:00:00','2026-02-23 09:00:00'),
(24,'IN_TRANSIT',  '2026-02-24 08:00:00','hq0001','본사관리자',NULL,'2026-02-24 08:00:00','2026-02-24 08:00:00'),
(24,'ARRIVED',     '2026-02-27 09:00:00','hq0001','본사관리자',NULL,'2026-02-27 09:00:00','2026-02-27 09:00:00'),
(25,'READY_TO_SHIP','2026-03-16 09:00:00','hq0001','본사관리자',NULL,'2026-03-16 09:00:00','2026-03-16 09:00:00'),
(25,'IN_TRANSIT',  '2026-03-17 08:00:00','hq0001','본사관리자',NULL,'2026-03-17 08:00:00','2026-03-17 08:00:00'),
(25,'ARRIVED',     '2026-03-20 09:00:00','hq0001','본사관리자',NULL,'2026-03-20 09:00:00','2026-03-20 09:00:00'),
(26,'READY_TO_SHIP','2026-03-29 09:00:00','hq0001','본사관리자',NULL,'2026-03-29 09:00:00','2026-03-29 09:00:00'),
(26,'IN_TRANSIT',  '2026-03-30 08:00:00','hq0001','본사관리자',NULL,'2026-03-30 08:00:00','2026-03-30 08:00:00'),
(26,'ARRIVED',     '2026-04-02 09:00:00','hq0001','본사관리자',NULL,'2026-04-02 09:00:00','2026-04-02 09:00:00'),
(27,'READY_TO_SHIP','2026-04-06 09:00:00','hq0001','본사관리자',NULL,'2026-04-06 09:00:00','2026-04-06 09:00:00'),
(27,'IN_TRANSIT',  '2026-04-07 08:00:00','hq0001','본사관리자',NULL,'2026-04-07 08:00:00','2026-04-07 08:00:00'),
(27,'ARRIVED',     '2026-04-10 09:00:00','hq0001','본사관리자',NULL,'2026-04-10 09:00:00','2026-04-10 09:00:00'),
-- 홍대 IN_TRANSIT (outbound 28~31)
(28,'READY_TO_SHIP','2026-05-09 10:00:00','hq0001','본사관리자',NULL,'2026-05-09 10:00:00','2026-05-09 10:00:00'),
(28,'IN_TRANSIT',  '2026-05-10 08:00:00','hq0001','본사관리자',NULL,'2026-05-10 08:00:00','2026-05-10 08:00:00'),
(29,'READY_TO_SHIP','2026-05-16 10:00:00','hq0001','본사관리자',NULL,'2026-05-16 10:00:00','2026-05-16 10:00:00'),
(29,'IN_TRANSIT',  '2026-05-17 08:00:00','hq0001','본사관리자',NULL,'2026-05-17 08:00:00','2026-05-17 08:00:00'),
(30,'READY_TO_SHIP','2026-05-23 10:00:00','hq0001','본사관리자',NULL,'2026-05-23 10:00:00','2026-05-23 10:00:00'),
(30,'IN_TRANSIT',  '2026-05-24 08:00:00','hq0001','본사관리자',NULL,'2026-05-24 08:00:00','2026-05-24 08:00:00'),
(31,'READY_TO_SHIP','2026-05-31 10:00:00','hq0001','본사관리자',NULL,'2026-05-31 10:00:00','2026-05-31 10:00:00'),
(31,'IN_TRANSIT',  '2026-06-01 08:00:00','hq0001','본사관리자',NULL,'2026-06-01 08:00:00','2026-06-01 08:00:00'),
-- 홍대 READY_TO_SHIP (outbound 32~38)
(32,'READY_TO_SHIP','2026-06-01 10:00:00','hq0001','본사관리자',NULL,'2026-06-01 10:00:00','2026-06-01 10:00:00'),
(33,'READY_TO_SHIP','2026-06-02 09:00:00','hq0001','본사관리자',NULL,'2026-06-02 09:00:00','2026-06-02 09:00:00'),
(34,'READY_TO_SHIP','2026-06-03 14:00:00','hq0001','본사관리자',NULL,'2026-06-03 14:00:00','2026-06-03 14:00:00'),
(35,'READY_TO_SHIP','2026-06-04 09:30:00','hq0001','본사관리자',NULL,'2026-06-04 09:30:00','2026-06-04 09:30:00'),
(36,'READY_TO_SHIP','2026-06-05 10:00:00','hq0001','본사관리자',NULL,'2026-06-05 10:00:00','2026-06-05 10:00:00'),
(37,'READY_TO_SHIP','2026-06-06 09:00:00','hq0001','본사관리자',NULL,'2026-06-06 09:00:00','2026-06-06 09:00:00'),
(38,'READY_TO_SHIP','2026-06-07 14:00:00','hq0001','본사관리자',NULL,'2026-06-07 14:00:00','2026-06-07 14:00:00');
