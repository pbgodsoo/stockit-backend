-- 13-store-inbound-dummy.sql
-- 매장 입고 더미 데이터 (25건)
-- 의존: 12-wh-outbound-dummy.sql
--
-- 실제 엔티티 컬럼 기준:
--   store_inbound_header: id, inbound_no, source_ref_no(=order_no), source_ref_id(=order_header.id),
--                         outbound_no, store_id, from_warehouse_id, status,
--                         total_sku_count, total_expected_quantity,
--                         expected_arrival_at, requested_at, received_at,
--                         requested_by_member_id, requested_by_name,
--                         received_by_member_id(nullable), received_by_name(nullable),
--                         create_date, update_date
--   store_inbound_item:   id, inbound_header_id, outbound_item_id, source_line_ref_id,
--                         sku_id, sku_code, product_code, product_name,
--                         main_category, sub_category, color, size, unit_price,
--                         expected_quantity, create_date, update_date
--   store_inbound_status_history: id, inbound_header_id, status, changed_at,
--                                  changed_by_member_id, changed_by_name, reason,
--                                  create_date, update_date
--
-- 입고 상태 매핑:
--   출고 ARRIVED     → 입고 RECEIVED        (강남 9건 + 홍대 8건 = 17건)
--   출고 IN_TRANSIT  → 입고 PENDING_RECEIPT  (강남 4건 + 홍대 4건 = 8건)
--   출고 READY_TO_SHIP → 입고 없음

SET SQL_SAFE_UPDATES = 0;
DELETE FROM store_inbound_status_history WHERE inbound_header_id BETWEEN 1 AND 25;
DELETE FROM store_inbound_item           WHERE inbound_header_id BETWEEN 1 AND 25;
DELETE FROM store_inbound_header         WHERE id BETWEEN 1 AND 25;
SET SQL_SAFE_UPDATES = 1;

-- ===== store_inbound_header (25건) =====
-- source_ref_no = 발주 번호(order_no), source_ref_id = 발주 헤더 ID
-- outbound_no   = 출고 번호(outbound_no)
-- from_warehouse_id = 출고 창고
-- requested_at  = 출고 departed_at (입고 등록 시점)
INSERT INTO store_inbound_header
(id, inbound_no, source_ref_no, source_ref_id, outbound_no,
 store_id, from_warehouse_id, status,
 total_sku_count, total_expected_quantity,
 expected_arrival_at, requested_at, received_at,
 requested_by_member_id, requested_by_name,
 received_by_member_id, received_by_name,
 create_date, update_date)
VALUES
-- ── 강남 RECEIVED (outbound 1~9 → ARRIVED) ──
(1,  'INB-20260110-00001','ORD-20260105-00001',1,  'OUT-20260106-00001',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,25,'2026-01-09 09:00:00','2026-01-07 08:00:00','2026-01-10 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-01-07 08:00:00','2026-01-10 10:00:00'),
(2,  'INB-20260120-00002','ORD-20260115-00002',2,  'OUT-20260116-00002',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,20,'2026-01-19 09:00:00','2026-01-17 08:00:00','2026-01-20 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-01-17 08:00:00','2026-01-20 10:00:00'),
(3,  'INB-20260130-00003','ORD-20260125-00003',3,  'OUT-20260126-00003',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,22,'2026-01-29 09:00:00','2026-01-27 08:00:00','2026-01-30 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-01-27 08:00:00','2026-01-30 10:00:00'),
(4,  'INB-20260215-00004','ORD-20260210-00004',4,  'OUT-20260211-00004',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,23,'2026-02-14 09:00:00','2026-02-12 08:00:00','2026-02-15 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-02-12 08:00:00','2026-02-15 10:00:00'),
(5,  'INB-20260225-00005','ORD-20260220-00005',5,  'OUT-20260221-00005',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,22,'2026-02-24 09:00:00','2026-02-22 08:00:00','2026-02-25 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-02-22 08:00:00','2026-02-25 10:00:00'),
(6,  'INB-20260310-00006','ORD-20260305-00006',6,  'OUT-20260306-00006',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,18,'2026-03-09 09:00:00','2026-03-07 08:00:00','2026-03-10 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-03-07 08:00:00','2026-03-10 10:00:00'),
(7,  'INB-20260323-00007','ORD-20260318-00007',7,  'OUT-20260319-00007',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,25,'2026-03-22 09:00:00','2026-03-20 08:00:00','2026-03-23 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-03-20 08:00:00','2026-03-23 10:00:00'),
(8,  'INB-20260407-00008','ORD-20260402-00008',8,  'OUT-20260403-00008',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,20,'2026-04-06 09:00:00','2026-04-04 08:00:00','2026-04-07 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-04-04 08:00:00','2026-04-07 10:00:00'),
(9,  'INB-20260420-00009','ORD-20260415-00009',9,  'OUT-20260416-00009',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'RECEIVED',2,18,'2026-04-19 09:00:00','2026-04-17 08:00:00','2026-04-20 10:00:00','st-sl-0001','강민재','st-sl-0001','강민재','2026-04-17 08:00:00','2026-04-20 10:00:00'),
-- ── 강남 PENDING_RECEIPT (outbound 10~13 → IN_TRANSIT) ──
(10, 'INB-20260508-00010','ORD-20260505-00011',11, 'OUT-20260506-00010',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'PENDING_RECEIPT',2,25,'2026-05-09 08:00:00','2026-05-07 08:00:00',NULL,'st-sl-0001','강민재',NULL,NULL,'2026-05-07 08:00:00','2026-05-07 08:00:00'),
(11, 'INB-20260515-00011','ORD-20260512-00012',12, 'OUT-20260513-00011',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'PENDING_RECEIPT',2,20,'2026-05-16 08:00:00','2026-05-14 08:00:00',NULL,'st-sl-0001','강민재',NULL,NULL,'2026-05-14 08:00:00','2026-05-14 08:00:00'),
(12, 'INB-20260523-00012','ORD-20260520-00013',13, 'OUT-20260521-00012',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'PENDING_RECEIPT',2,22,'2026-05-24 08:00:00','2026-05-22 08:00:00',NULL,'st-sl-0001','강민재',NULL,NULL,'2026-05-22 08:00:00','2026-05-22 08:00:00'),
(13, 'INB-20260531-00013','ORD-20260528-00014',14, 'OUT-20260529-00013',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'PENDING_RECEIPT',2,23,'2026-05-31 08:00:00','2026-05-30 08:00:00',NULL,'st-sl-0001','강민재',NULL,NULL,'2026-05-30 08:00:00','2026-05-30 08:00:00'),
-- ── 홍대 RECEIVED (outbound 20~27 → ARRIVED) ──
(14, 'INB-20260113-00014','ORD-20260108-00021',21, 'OUT-20260109-00020',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'RECEIVED',2,25,'2026-01-12 09:00:00','2026-01-10 08:00:00','2026-01-13 10:00:00','st-sl-0002','박서윤','st-sl-0002','박서윤','2026-01-10 08:00:00','2026-01-13 10:00:00'),
(15, 'INB-20260123-00015','ORD-20260118-00022',22, 'OUT-20260119-00021',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'RECEIVED',2,20,'2026-01-22 09:00:00','2026-01-20 08:00:00','2026-01-23 10:00:00','st-sl-0002','박서윤','st-sl-0002','박서윤','2026-01-20 08:00:00','2026-01-23 10:00:00'),
(16, 'INB-20260202-00016','ORD-20260128-00023',23, 'OUT-20260129-00022',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'RECEIVED',2,22,'2026-02-01 09:00:00','2026-01-30 08:00:00','2026-02-02 10:00:00','st-sl-0002','박서윤','st-sl-0002','박서윤','2026-01-30 08:00:00','2026-02-02 10:00:00'),
(17, 'INB-20260217-00017','ORD-20260212-00024',24, 'OUT-20260213-00023',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'RECEIVED',2,23,'2026-02-16 09:00:00','2026-02-14 08:00:00','2026-02-17 10:00:00','st-sl-0002','박서윤','st-sl-0002','박서윤','2026-02-14 08:00:00','2026-02-17 10:00:00'),
(18, 'INB-20260227-00018','ORD-20260222-00025',25, 'OUT-20260223-00024',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'RECEIVED',2,22,'2026-02-26 09:00:00','2026-02-24 08:00:00','2026-02-27 10:00:00','st-sl-0002','박서윤','st-sl-0002','박서윤','2026-02-24 08:00:00','2026-02-27 10:00:00'),
(19, 'INB-20260320-00019','ORD-20260315-00026',26, 'OUT-20260316-00025',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'RECEIVED',2,18,'2026-03-19 09:00:00','2026-03-17 08:00:00','2026-03-20 10:00:00','st-sl-0002','박서윤','st-sl-0002','박서윤','2026-03-17 08:00:00','2026-03-20 10:00:00'),
(20, 'INB-20260402-00020','ORD-20260328-00027',27, 'OUT-20260329-00026',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'RECEIVED',2,25,'2026-04-01 09:00:00','2026-03-30 08:00:00','2026-04-02 10:00:00','st-sl-0002','박서윤','st-sl-0002','박서윤','2026-03-30 08:00:00','2026-04-02 10:00:00'),
(21, 'INB-20260410-00021','ORD-20260405-00028',28, 'OUT-20260406-00027',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'RECEIVED',2,20,'2026-04-09 09:00:00','2026-04-07 08:00:00','2026-04-10 10:00:00','st-sl-0002','박서윤','st-sl-0002','박서윤','2026-04-07 08:00:00','2026-04-10 10:00:00'),
-- ── 홍대 PENDING_RECEIPT (outbound 28~31 → IN_TRANSIT) ──
(22, 'INB-20260511-00022','ORD-20260508-00030',30, 'OUT-20260509-00028',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'PENDING_RECEIPT',2,22,'2026-05-12 08:00:00','2026-05-10 08:00:00',NULL,'st-sl-0002','박서윤',NULL,NULL,'2026-05-10 08:00:00','2026-05-10 08:00:00'),
(23, 'INB-20260518-00023','ORD-20260515-00031',31, 'OUT-20260516-00029',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'PENDING_RECEIPT',2,25,'2026-05-19 08:00:00','2026-05-17 08:00:00',NULL,'st-sl-0002','박서윤',NULL,NULL,'2026-05-17 08:00:00','2026-05-17 08:00:00'),
(24, 'INB-20260525-00024','ORD-20260522-00032',32, 'OUT-20260523-00030',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'PENDING_RECEIPT',2,20,'2026-05-26 08:00:00','2026-05-24 08:00:00',NULL,'st-sl-0002','박서윤',NULL,NULL,'2026-05-24 08:00:00','2026-05-24 08:00:00'),
(25, 'INB-20260602-00025','ORD-20260530-00033',33, 'OUT-20260531-00031',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'PENDING_RECEIPT',2,22,'2026-06-02 08:00:00','2026-06-01 08:00:00',NULL,'st-sl-0002','박서윤',NULL,NULL,'2026-06-01 08:00:00','2026-06-01 08:00:00');


-- ===== store_inbound_item (50건, 헤더당 2건) =====
-- 컬럼: expected_quantity (received_quantity 없음)
-- outbound_item_id = wh_outbound_item.id, source_line_ref_id = store_order_item.id
INSERT INTO store_inbound_item
(id, inbound_header_id, outbound_item_id, source_line_ref_id,
 sku_id, sku_code, product_code, product_name, main_category, sub_category, color, size,
 unit_price, expected_quantity, create_date, update_date)
VALUES
-- 강남 RECEIVED inbound 1 ↔ outbound 1 (P1): OUT-PD-001×10, TOP-SS-001×15
(1,  1,  1,  1,  (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-01-07 08:00:00','2026-01-10 10:00:00'),
(2,  1,  2,  2,  (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-01-07 08:00:00','2026-01-10 10:00:00'),
-- inbound 2 ↔ outbound 2 (P2): OUT-HZ-002×8, PNT-DN-002×12
(3,  2,  3,  3,  (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-01-17 08:00:00','2026-01-20 10:00:00'),
(4,  2,  4,  4,  (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-01-17 08:00:00','2026-01-20 10:00:00'),
-- inbound 3 ↔ outbound 3 (P3): TOP-KN-001×12, PNT-LG-001×10
(5,  3,  5,  5,  (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-01-27 08:00:00','2026-01-30 10:00:00'),
(6,  3,  6,  6,  (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-01-27 08:00:00','2026-01-30 10:00:00'),
-- inbound 4 ↔ outbound 4 (P4): OUT-JK-001×8, TOP-LS-002×15
(7,  4,  7,  7,  (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-02-12 08:00:00','2026-02-15 10:00:00'),
(8,  4,  8,  8,  (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-02-12 08:00:00','2026-02-15 10:00:00'),
-- inbound 5 ↔ outbound 5 (P5): TOP-HD-001×10, PNT-TR-002×12
(9,  5,  9,  9,  (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-02-22 08:00:00','2026-02-25 10:00:00'),
(10, 5,  10, 10, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-02-22 08:00:00','2026-02-25 10:00:00'),
-- inbound 6 ↔ outbound 6 (P6): OUT-CD-002×10, SKT-LG-001×8
(11, 6,  11, 11, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-03-07 08:00:00','2026-03-10 10:00:00'),
(12, 6,  12, 12, (SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-03-07 08:00:00','2026-03-10 10:00:00'),
-- inbound 7 ↔ outbound 7 (P7): TOP-SH-001×15, PNT-DN-001×10
(13, 7,  13, 13, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-03-20 08:00:00','2026-03-23 10:00:00'),
(14, 7,  14, 14, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-03-20 08:00:00','2026-03-23 10:00:00'),
-- inbound 8 ↔ outbound 8 (P8): OUT-PD-002×8, TOP-KN-002×12
(15, 8,  15, 15, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-04-04 08:00:00','2026-04-07 10:00:00'),
(16, 8,  16, 16, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-04-04 08:00:00','2026-04-07 10:00:00'),
-- inbound 9 ↔ outbound 9 (P9): TOP-HD-002×10, PNT-LG-002×8
(17, 9,  17, 17, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-002-BLK-M'),'PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','브러시드 기모 후드티','상의','후드티','BLK','M',48900,10,'2026-04-17 08:00:00','2026-04-20 10:00:00'),
(18, 9,  18, 18, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',47900,8,'2026-04-17 08:00:00','2026-04-20 10:00:00'),
-- 강남 PENDING_RECEIPT inbound 10 ↔ outbound 10 (P1): OUT-PD-001×10, TOP-SS-001×15
(19,10,  19, 21, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-05-07 08:00:00','2026-05-07 08:00:00'),
(20,10,  20, 22, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-05-07 08:00:00','2026-05-07 08:00:00'),
-- inbound 11 ↔ outbound 11 (P2): OUT-HZ-002×8, PNT-DN-002×12
(21,11,  21, 23, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-05-14 08:00:00','2026-05-14 08:00:00'),
(22,11,  22, 24, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-05-14 08:00:00','2026-05-14 08:00:00'),
-- inbound 12 ↔ outbound 12 (P3): TOP-KN-001×12, PNT-LG-001×10
(23,12,  23, 25, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-05-22 08:00:00','2026-05-22 08:00:00'),
(24,12,  24, 26, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-05-22 08:00:00','2026-05-22 08:00:00'),
-- inbound 13 ↔ outbound 13 (P4): OUT-JK-001×8, TOP-LS-002×15
(25,13,  25, 27, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-05-30 08:00:00','2026-05-30 08:00:00'),
(26,13,  26, 28, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-05-30 08:00:00','2026-05-30 08:00:00'),
-- 홍대 RECEIVED inbound 14 ↔ outbound 20 (P1): OUT-PD-001×10, TOP-SS-001×15
(27,14,  39, 41, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-01-10 08:00:00','2026-01-13 10:00:00'),
(28,14,  40, 42, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-01-10 08:00:00','2026-01-13 10:00:00'),
-- inbound 15 ↔ outbound 21 (P2): OUT-HZ-002×8, PNT-DN-002×12
(29,15,  41, 43, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-01-20 08:00:00','2026-01-23 10:00:00'),
(30,15,  42, 44, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-01-20 08:00:00','2026-01-23 10:00:00'),
-- inbound 16 ↔ outbound 22 (P3): TOP-KN-001×12, PNT-LG-001×10
(31,16,  43, 45, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-01-30 08:00:00','2026-02-02 10:00:00'),
(32,16,  44, 46, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-01-30 08:00:00','2026-02-02 10:00:00'),
-- inbound 17 ↔ outbound 23 (P4): OUT-JK-001×8, TOP-LS-002×15
(33,17,  45, 47, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-02-14 08:00:00','2026-02-17 10:00:00'),
(34,17,  46, 48, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-02-14 08:00:00','2026-02-17 10:00:00'),
-- inbound 18 ↔ outbound 24 (P5): TOP-HD-001×10, PNT-TR-002×12
(35,18,  47, 49, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-02-24 08:00:00','2026-02-27 10:00:00'),
(36,18,  48, 50, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-02-24 08:00:00','2026-02-27 10:00:00'),
-- inbound 19 ↔ outbound 25 (P6): OUT-CD-002×10, SKT-LG-001×8
(37,19,  49, 51, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-03-17 08:00:00','2026-03-20 10:00:00'),
(38,19,  50, 52, (SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-03-17 08:00:00','2026-03-20 10:00:00'),
-- inbound 20 ↔ outbound 26 (P7): TOP-SH-001×15, PNT-DN-001×10
(39,20,  51, 53, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-03-30 08:00:00','2026-04-02 10:00:00'),
(40,20,  52, 54, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-03-30 08:00:00','2026-04-02 10:00:00'),
-- inbound 21 ↔ outbound 27 (P8): OUT-PD-002×8, TOP-KN-002×12
(41,21,  53, 55, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-04-07 08:00:00','2026-04-10 10:00:00'),
(42,21,  54, 56, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-04-07 08:00:00','2026-04-10 10:00:00'),
-- 홍대 PENDING_RECEIPT inbound 22 ↔ outbound 28 (P10): OUT-HZ-001×10, PNT-TR-001×12
(43,22,  55, 59, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-WHT-M'),'PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','WHT','M',52900,10,'2026-05-10 08:00:00','2026-05-10 08:00:00'),
(44,22,  56, 60, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',38900,12,'2026-05-10 08:00:00','2026-05-10 08:00:00'),
-- inbound 23 ↔ outbound 29 (P1): OUT-PD-001×10, TOP-SS-001×15
(45,23,  57, 61, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-05-17 08:00:00','2026-05-17 08:00:00'),
(46,23,  58, 62, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-05-17 08:00:00','2026-05-17 08:00:00'),
-- inbound 24 ↔ outbound 30 (P2): OUT-HZ-002×8, PNT-DN-002×12
(47,24,  59, 63, (SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-05-24 08:00:00','2026-05-24 08:00:00'),
(48,24,  60, 64, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-05-24 08:00:00','2026-05-24 08:00:00'),
-- inbound 25 ↔ outbound 31 (P3): TOP-KN-001×12, PNT-LG-001×10
(49,25,  61, 65, (SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-06-01 08:00:00','2026-06-01 08:00:00'),
(50,25,  62, 66, (SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-06-01 08:00:00','2026-06-01 08:00:00');


-- ===== store_inbound_status_history =====
INSERT INTO store_inbound_status_history
(inbound_header_id, status, changed_at, changed_by_member_id, changed_by_name, reason, create_date, update_date)
VALUES
-- 강남 RECEIVED (inbound 1~9)
(1, 'PENDING_RECEIPT','2026-01-07 08:00:00','st-sl-0001','강민재',NULL,'2026-01-07 08:00:00','2026-01-07 08:00:00'),
(1, 'RECEIVED',       '2026-01-10 10:00:00','st-sl-0001','강민재',NULL,'2026-01-10 10:00:00','2026-01-10 10:00:00'),
(2, 'PENDING_RECEIPT','2026-01-17 08:00:00','st-sl-0001','강민재',NULL,'2026-01-17 08:00:00','2026-01-17 08:00:00'),
(2, 'RECEIVED',       '2026-01-20 10:00:00','st-sl-0001','강민재',NULL,'2026-01-20 10:00:00','2026-01-20 10:00:00'),
(3, 'PENDING_RECEIPT','2026-01-27 08:00:00','st-sl-0001','강민재',NULL,'2026-01-27 08:00:00','2026-01-27 08:00:00'),
(3, 'RECEIVED',       '2026-01-30 10:00:00','st-sl-0001','강민재',NULL,'2026-01-30 10:00:00','2026-01-30 10:00:00'),
(4, 'PENDING_RECEIPT','2026-02-12 08:00:00','st-sl-0001','강민재',NULL,'2026-02-12 08:00:00','2026-02-12 08:00:00'),
(4, 'RECEIVED',       '2026-02-15 10:00:00','st-sl-0001','강민재',NULL,'2026-02-15 10:00:00','2026-02-15 10:00:00'),
(5, 'PENDING_RECEIPT','2026-02-22 08:00:00','st-sl-0001','강민재',NULL,'2026-02-22 08:00:00','2026-02-22 08:00:00'),
(5, 'RECEIVED',       '2026-02-25 10:00:00','st-sl-0001','강민재',NULL,'2026-02-25 10:00:00','2026-02-25 10:00:00'),
(6, 'PENDING_RECEIPT','2026-03-07 08:00:00','st-sl-0001','강민재',NULL,'2026-03-07 08:00:00','2026-03-07 08:00:00'),
(6, 'RECEIVED',       '2026-03-10 10:00:00','st-sl-0001','강민재',NULL,'2026-03-10 10:00:00','2026-03-10 10:00:00'),
(7, 'PENDING_RECEIPT','2026-03-20 08:00:00','st-sl-0001','강민재',NULL,'2026-03-20 08:00:00','2026-03-20 08:00:00'),
(7, 'RECEIVED',       '2026-03-23 10:00:00','st-sl-0001','강민재',NULL,'2026-03-23 10:00:00','2026-03-23 10:00:00'),
(8, 'PENDING_RECEIPT','2026-04-04 08:00:00','st-sl-0001','강민재',NULL,'2026-04-04 08:00:00','2026-04-04 08:00:00'),
(8, 'RECEIVED',       '2026-04-07 10:00:00','st-sl-0001','강민재',NULL,'2026-04-07 10:00:00','2026-04-07 10:00:00'),
(9, 'PENDING_RECEIPT','2026-04-17 08:00:00','st-sl-0001','강민재',NULL,'2026-04-17 08:00:00','2026-04-17 08:00:00'),
(9, 'RECEIVED',       '2026-04-20 10:00:00','st-sl-0001','강민재',NULL,'2026-04-20 10:00:00','2026-04-20 10:00:00'),
-- 강남 PENDING_RECEIPT (inbound 10~13)
(10,'PENDING_RECEIPT','2026-05-07 08:00:00','st-sl-0001','강민재',NULL,'2026-05-07 08:00:00','2026-05-07 08:00:00'),
(11,'PENDING_RECEIPT','2026-05-14 08:00:00','st-sl-0001','강민재',NULL,'2026-05-14 08:00:00','2026-05-14 08:00:00'),
(12,'PENDING_RECEIPT','2026-05-22 08:00:00','st-sl-0001','강민재',NULL,'2026-05-22 08:00:00','2026-05-22 08:00:00'),
(13,'PENDING_RECEIPT','2026-05-30 08:00:00','st-sl-0001','강민재',NULL,'2026-05-30 08:00:00','2026-05-30 08:00:00'),
-- 홍대 RECEIVED (inbound 14~21)
(14,'PENDING_RECEIPT','2026-01-10 08:00:00','st-sl-0002','박서윤',NULL,'2026-01-10 08:00:00','2026-01-10 08:00:00'),
(14,'RECEIVED',       '2026-01-13 10:00:00','st-sl-0002','박서윤',NULL,'2026-01-13 10:00:00','2026-01-13 10:00:00'),
(15,'PENDING_RECEIPT','2026-01-20 08:00:00','st-sl-0002','박서윤',NULL,'2026-01-20 08:00:00','2026-01-20 08:00:00'),
(15,'RECEIVED',       '2026-01-23 10:00:00','st-sl-0002','박서윤',NULL,'2026-01-23 10:00:00','2026-01-23 10:00:00'),
(16,'PENDING_RECEIPT','2026-01-30 08:00:00','st-sl-0002','박서윤',NULL,'2026-01-30 08:00:00','2026-01-30 08:00:00'),
(16,'RECEIVED',       '2026-02-02 10:00:00','st-sl-0002','박서윤',NULL,'2026-02-02 10:00:00','2026-02-02 10:00:00'),
(17,'PENDING_RECEIPT','2026-02-14 08:00:00','st-sl-0002','박서윤',NULL,'2026-02-14 08:00:00','2026-02-14 08:00:00'),
(17,'RECEIVED',       '2026-02-17 10:00:00','st-sl-0002','박서윤',NULL,'2026-02-17 10:00:00','2026-02-17 10:00:00'),
(18,'PENDING_RECEIPT','2026-02-24 08:00:00','st-sl-0002','박서윤',NULL,'2026-02-24 08:00:00','2026-02-24 08:00:00'),
(18,'RECEIVED',       '2026-02-27 10:00:00','st-sl-0002','박서윤',NULL,'2026-02-27 10:00:00','2026-02-27 10:00:00'),
(19,'PENDING_RECEIPT','2026-03-17 08:00:00','st-sl-0002','박서윤',NULL,'2026-03-17 08:00:00','2026-03-17 08:00:00'),
(19,'RECEIVED',       '2026-03-20 10:00:00','st-sl-0002','박서윤',NULL,'2026-03-20 10:00:00','2026-03-20 10:00:00'),
(20,'PENDING_RECEIPT','2026-03-30 08:00:00','st-sl-0002','박서윤',NULL,'2026-03-30 08:00:00','2026-03-30 08:00:00'),
(20,'RECEIVED',       '2026-04-02 10:00:00','st-sl-0002','박서윤',NULL,'2026-04-02 10:00:00','2026-04-02 10:00:00'),
(21,'PENDING_RECEIPT','2026-04-07 08:00:00','st-sl-0002','박서윤',NULL,'2026-04-07 08:00:00','2026-04-07 08:00:00'),
(21,'RECEIVED',       '2026-04-10 10:00:00','st-sl-0002','박서윤',NULL,'2026-04-10 10:00:00','2026-04-10 10:00:00'),
-- 홍대 PENDING_RECEIPT (inbound 22~25)
(22,'PENDING_RECEIPT','2026-05-10 08:00:00','st-sl-0002','박서윤',NULL,'2026-05-10 08:00:00','2026-05-10 08:00:00'),
(23,'PENDING_RECEIPT','2026-05-17 08:00:00','st-sl-0002','박서윤',NULL,'2026-05-17 08:00:00','2026-05-17 08:00:00'),
(24,'PENDING_RECEIPT','2026-05-24 08:00:00','st-sl-0002','박서윤',NULL,'2026-05-24 08:00:00','2026-05-24 08:00:00'),
(25,'PENDING_RECEIPT','2026-06-01 08:00:00','st-sl-0002','박서윤',NULL,'2026-06-01 08:00:00','2026-06-01 08:00:00');
