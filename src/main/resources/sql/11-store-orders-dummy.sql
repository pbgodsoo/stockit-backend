-- 11-store-orders-dummy.sql
-- 매장 발주 더미 데이터 (40건: 강남 20 + 홍대 20)
-- 의존: 01-infrastructure_dummy_data.sql, 04-product_master_dummy_data.sql
--
-- 상태 분포: COMPLETED 17건 / CANCELLED 2건 / APPROVED 8건 / REQUESTED 13건
--
-- 아이템 패턴 (order_id % 10):
--  P1(%=1): OUT-PD-001-BLK-M qty10 + TOP-SS-001-BLK-M qty15  = total_qty 25
--  P2(%=2): OUT-HZ-002-NVY-L qty8  + PNT-DN-002-BLK-M qty12  = total_qty 20
--  P3(%=3): TOP-KN-001-NVY-L qty12 + PNT-LG-001-BLK-M qty10  = total_qty 22
--  P4(%=4): OUT-JK-001-BLK-M qty8  + TOP-LS-002-WHT-M qty15  = total_qty 23
--  P5(%=5): TOP-HD-001-BLK-M qty10 + PNT-TR-002-NVY-L qty12  = total_qty 22
--  P6(%=6): OUT-CD-002-NVY-L qty10 + SKT-LG-001-BLK-M qty8   = total_qty 18
--  P7(%=7): TOP-SH-001-WHT-M qty15 + PNT-DN-001-BLK-M qty10  = total_qty 25
--  P8(%=8): OUT-PD-002-BLK-M qty8  + TOP-KN-002-NVY-L qty12  = total_qty 20
--  P9(%=9): TOP-HD-002-BLK-M qty10 + PNT-LG-002-BLK-M qty8   = total_qty 18
--  P10(%=0): OUT-HZ-001-WHT-M qty10 + PNT-TR-001-BLK-M qty12 = total_qty 22

SET SQL_SAFE_UPDATES = 0;
DELETE FROM store_order_status_history WHERE order_header_id BETWEEN 1 AND 40;
DELETE FROM store_order_item          WHERE order_header_id BETWEEN 1 AND 40;
DELETE FROM store_order_header        WHERE id BETWEEN 1 AND 40;
SET SQL_SAFE_UPDATES = 1;

-- ===== store_order_header (40건) =====
INSERT INTO store_order_header
(id, order_no, store_id, warehouse_id, requested_by_member_id, requested_by_name,
 requested_at, status, total_sku_count, total_requested_quantity, memo, create_date, update_date)
VALUES
-- ── 강남 (ST-SL-0001 → WH-SL-0001) ──
(1,  'ORD-20260105-00001',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-01-05 10:00:00','COMPLETED',2,25, NULL,'2026-01-05 10:00:00','2026-01-10 09:00:00'),
(2,  'ORD-20260115-00002',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-01-15 14:00:00','COMPLETED',2,20, NULL,'2026-01-15 14:00:00','2026-01-20 09:00:00'),
(3,  'ORD-20260125-00003',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-01-25 09:30:00','COMPLETED',2,22, NULL,'2026-01-25 09:30:00','2026-01-30 09:00:00'),
(4,  'ORD-20260210-00004',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-02-10 11:00:00','COMPLETED',2,23, NULL,'2026-02-10 11:00:00','2026-02-15 09:00:00'),
(5,  'ORD-20260220-00005',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-02-20 15:00:00','COMPLETED',2,22, NULL,'2026-02-20 15:00:00','2026-02-25 09:00:00'),
(6,  'ORD-20260305-00006',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-03-05 10:30:00','COMPLETED',2,18, NULL,'2026-03-05 10:30:00','2026-03-10 09:00:00'),
(7,  'ORD-20260318-00007',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-03-18 14:00:00','COMPLETED',2,25, NULL,'2026-03-18 14:00:00','2026-03-23 09:00:00'),
(8,  'ORD-20260402-00008',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-04-02 09:00:00','COMPLETED',2,20, NULL,'2026-04-02 09:00:00','2026-04-07 09:00:00'),
(9,  'ORD-20260415-00009',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-04-15 11:00:00','COMPLETED',2,18, NULL,'2026-04-15 11:00:00','2026-04-20 09:00:00'),
(10, 'ORD-20260422-00010',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-04-22 10:00:00','CANCELLED',2,22, NULL,'2026-04-22 10:00:00','2026-04-23 11:00:00'),
(11, 'ORD-20260505-00011',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-05-05 09:00:00','APPROVED', 2,25, NULL,'2026-05-05 09:00:00','2026-05-06 10:00:00'),
(12, 'ORD-20260512-00012',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-05-12 14:00:00','APPROVED', 2,20, NULL,'2026-05-12 14:00:00','2026-05-13 10:00:00'),
(13, 'ORD-20260520-00013',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-05-20 10:00:00','APPROVED', 2,22, NULL,'2026-05-20 10:00:00','2026-05-21 10:00:00'),
(14, 'ORD-20260528-00014',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-05-28 09:30:00','APPROVED', 2,23, NULL,'2026-05-28 09:30:00','2026-05-29 10:00:00'),
(15, 'ORD-20260601-00015',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-06-01 09:00:00','REQUESTED',2,22, NULL,'2026-06-01 09:00:00','2026-06-01 09:00:00'),
(16, 'ORD-20260602-00016',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-06-02 10:00:00','REQUESTED',2,18, NULL,'2026-06-02 10:00:00','2026-06-02 10:00:00'),
(17, 'ORD-20260603-00017',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-06-03 09:30:00','REQUESTED',2,25, NULL,'2026-06-03 09:30:00','2026-06-03 09:30:00'),
(18, 'ORD-20260604-00018',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-06-04 14:00:00','REQUESTED',2,20, NULL,'2026-06-04 14:00:00','2026-06-04 14:00:00'),
(19, 'ORD-20260605-00019',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-06-05 09:00:00','REQUESTED',2,18, NULL,'2026-06-05 09:00:00','2026-06-05 09:00:00'),
(20, 'ORD-20260606-00020',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),(SELECT id FROM infrastructure WHERE code='WH-SL-0001'),'st-sl-0001','강민재','2026-06-06 11:00:00','REQUESTED',2,22, NULL,'2026-06-06 11:00:00','2026-06-06 11:00:00'),
-- ── 홍대 (ST-SL-0002 → WH-SL-0002) ──
(21, 'ORD-20260108-00021',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-01-08 10:00:00','COMPLETED',2,25, NULL,'2026-01-08 10:00:00','2026-01-13 09:00:00'),
(22, 'ORD-20260118-00022',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-01-18 14:00:00','COMPLETED',2,20, NULL,'2026-01-18 14:00:00','2026-01-23 09:00:00'),
(23, 'ORD-20260128-00023',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-01-28 09:30:00','COMPLETED',2,22, NULL,'2026-01-28 09:30:00','2026-02-02 09:00:00'),
(24, 'ORD-20260212-00024',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-02-12 11:00:00','COMPLETED',2,23, NULL,'2026-02-12 11:00:00','2026-02-17 09:00:00'),
(25, 'ORD-20260222-00025',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-02-22 15:00:00','COMPLETED',2,22, NULL,'2026-02-22 15:00:00','2026-02-27 09:00:00'),
(26, 'ORD-20260315-00026',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-03-15 10:30:00','COMPLETED',2,18, NULL,'2026-03-15 10:30:00','2026-03-20 09:00:00'),
(27, 'ORD-20260328-00027',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-03-28 14:00:00','COMPLETED',2,25, NULL,'2026-03-28 14:00:00','2026-04-02 09:00:00'),
(28, 'ORD-20260405-00028',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-04-05 09:00:00','COMPLETED',2,20, NULL,'2026-04-05 09:00:00','2026-04-10 09:00:00'),
(29, 'ORD-20260418-00029',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-04-18 10:00:00','CANCELLED',2,18, NULL,'2026-04-18 10:00:00','2026-04-19 11:00:00'),
(30, 'ORD-20260508-00030',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-05-08 09:00:00','APPROVED', 2,22, NULL,'2026-05-08 09:00:00','2026-05-09 10:00:00'),
(31, 'ORD-20260515-00031',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-05-15 14:00:00','APPROVED', 2,25, NULL,'2026-05-15 14:00:00','2026-05-16 10:00:00'),
(32, 'ORD-20260522-00032',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-05-22 10:00:00','APPROVED', 2,20, NULL,'2026-05-22 10:00:00','2026-05-23 10:00:00'),
(33, 'ORD-20260530-00033',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-05-30 09:30:00','APPROVED', 2,22, NULL,'2026-05-30 09:30:00','2026-05-31 10:00:00'),
(34, 'ORD-20260601-00034',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-06-01 10:00:00','REQUESTED',2,23, NULL,'2026-06-01 10:00:00','2026-06-01 10:00:00'),
(35, 'ORD-20260602-00035',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-06-02 09:00:00','REQUESTED',2,22, NULL,'2026-06-02 09:00:00','2026-06-02 09:00:00'),
(36, 'ORD-20260603-00036',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-06-03 14:00:00','REQUESTED',2,18, NULL,'2026-06-03 14:00:00','2026-06-03 14:00:00'),
(37, 'ORD-20260604-00037',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-06-04 09:30:00','REQUESTED',2,25, NULL,'2026-06-04 09:30:00','2026-06-04 09:30:00'),
(38, 'ORD-20260605-00038',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-06-05 10:00:00','REQUESTED',2,20, NULL,'2026-06-05 10:00:00','2026-06-05 10:00:00'),
(39, 'ORD-20260606-00039',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-06-06 09:00:00','REQUESTED',2,18, NULL,'2026-06-06 09:00:00','2026-06-06 09:00:00'),
(40, 'ORD-20260607-00040',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),(SELECT id FROM infrastructure WHERE code='WH-SL-0002'),'st-sl-0002','박서윤','2026-06-07 14:00:00','REQUESTED',2,22, NULL,'2026-06-07 14:00:00','2026-06-07 14:00:00');

-- ===== store_order_item (80건, 헤더당 2건) =====
-- item_id = order_id*2-1, order_id*2
INSERT INTO store_order_item
(id, order_header_id, sku_id, sku_code, product_code, product_name, main_category, sub_category, color, size, unit_price, requested_quantity, create_date, update_date)
VALUES
-- Order 1 P1: 라이트 웜 숏 패딩×10 + 코튼 에센셜 크루 반팔×15
(1, 1,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-01-05 10:00:00','2026-01-05 10:00:00'),
(2, 1,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-01-05 10:00:00','2026-01-05 10:00:00'),
-- Order 2 P2: 테리 루즈핏 후드집업×8 + 슬림 테이퍼드 데님×12
(3, 2,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-01-15 14:00:00','2026-01-15 14:00:00'),
(4, 2,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-01-15 14:00:00','2026-01-15 14:00:00'),
-- Order 3 P3: 파인게이지 라운드 니트×12 + 와이드 플리츠 롱팬츠×10
(5, 3,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-01-25 09:30:00','2026-01-25 09:30:00'),
(6, 3,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-01-25 09:30:00','2026-01-25 09:30:00'),
-- Order 4 P4: 클래식 싱글 자켓×8 + 소프트 코튼 롱슬리브×15
(7, 4,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-02-10 11:00:00','2026-02-10 11:00:00'),
(8, 4,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-02-10 11:00:00','2026-02-10 11:00:00'),
-- Order 5 P5: 헤비웨이트 로고 후드티×10 + 테크 플리스 트랙 팬츠×12
(9, 5,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-02-20 15:00:00','2026-02-20 15:00:00'),
(10,5,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-02-20 15:00:00','2026-02-20 15:00:00'),
-- Order 6 P6: 케이블 포인트 가디건×10 + 플리츠 플로우 롱스커트×8
(11,6,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-03-05 10:30:00','2026-03-05 10:30:00'),
(12,6,(SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-03-05 10:30:00','2026-03-05 10:30:00'),
-- Order 7 P7: 옥스포드 버튼다운 셔츠×15 + 스트레이트 인디고 데님×10
(13,7,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-03-18 14:00:00','2026-03-18 14:00:00'),
(14,7,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-03-18 14:00:00','2026-03-18 14:00:00'),
-- Order 8 P8: 볼륨넥 덕다운 패딩×8 + 캐시 블렌드 크루 니트×12
(15,8,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-04-02 09:00:00','2026-04-02 09:00:00'),
(16,8,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-04-02 09:00:00','2026-04-02 09:00:00'),
-- Order 9 P9: 브러시드 기모 후드티×10 + 테일러드 스트레이트 팬츠×8
(17,9,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-002-BLK-M'),'PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','브러시드 기모 후드티','상의','후드티','BLK','M',48900,10,'2026-04-15 11:00:00','2026-04-15 11:00:00'),
(18,9,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',47900,8,'2026-04-15 11:00:00','2026-04-15 11:00:00'),
-- Order 10 P10 CANCELLED: 소프트 스웻 후드집업×10 + 소프트 조거 트레이닝×12
(19,10,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-WHT-M'),'PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','WHT','M',52900,10,'2026-04-22 10:00:00','2026-04-22 10:00:00'),
(20,10,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',38900,12,'2026-04-22 10:00:00','2026-04-22 10:00:00'),
-- Order 11 P1: 라이트 웜 숏 패딩×10 + 코튼 에센셜 크루 반팔×15
(21,11,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-05-05 09:00:00','2026-05-05 09:00:00'),
(22,11,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-05-05 09:00:00','2026-05-05 09:00:00'),
-- Order 12 P2: 테리 루즈핏 후드집업×8 + 슬림 테이퍼드 데님×12
(23,12,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-05-12 14:00:00','2026-05-12 14:00:00'),
(24,12,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-05-12 14:00:00','2026-05-12 14:00:00'),
-- Order 13 P3: 파인게이지 라운드 니트×12 + 와이드 플리츠 롱팬츠×10
(25,13,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-05-20 10:00:00','2026-05-20 10:00:00'),
(26,13,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-05-20 10:00:00','2026-05-20 10:00:00'),
-- Order 14 P4: 클래식 싱글 자켓×8 + 소프트 코튼 롱슬리브×15
(27,14,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-05-28 09:30:00','2026-05-28 09:30:00'),
(28,14,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-05-28 09:30:00','2026-05-28 09:30:00'),
-- Order 15 P5: 헤비웨이트 로고 후드티×10 + 테크 플리스 트랙 팬츠×12
(29,15,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-06-01 09:00:00','2026-06-01 09:00:00'),
(30,15,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-06-01 09:00:00','2026-06-01 09:00:00'),
-- Order 16 P6: 케이블 포인트 가디건×10 + 플리츠 플로우 롱스커트×8
(31,16,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-06-02 10:00:00','2026-06-02 10:00:00'),
(32,16,(SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-06-02 10:00:00','2026-06-02 10:00:00'),
-- Order 17 P7: 옥스포드 버튼다운 셔츠×15 + 스트레이트 인디고 데님×10
(33,17,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-06-03 09:30:00','2026-06-03 09:30:00'),
(34,17,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-06-03 09:30:00','2026-06-03 09:30:00'),
-- Order 18 P8: 볼륨넥 덕다운 패딩×8 + 캐시 블렌드 크루 니트×12
(35,18,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-06-04 14:00:00','2026-06-04 14:00:00'),
(36,18,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-06-04 14:00:00','2026-06-04 14:00:00'),
-- Order 19 P9: 브러시드 기모 후드티×10 + 테일러드 스트레이트 팬츠×8
(37,19,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-002-BLK-M'),'PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','브러시드 기모 후드티','상의','후드티','BLK','M',48900,10,'2026-06-05 09:00:00','2026-06-05 09:00:00'),
(38,19,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',47900,8,'2026-06-05 09:00:00','2026-06-05 09:00:00'),
-- Order 20 P10: 소프트 스웻 후드집업×10 + 소프트 조거 트레이닝×12
(39,20,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-WHT-M'),'PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','WHT','M',52900,10,'2026-06-06 11:00:00','2026-06-06 11:00:00'),
(40,20,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',38900,12,'2026-06-06 11:00:00','2026-06-06 11:00:00'),
-- Order 21 P1 홍대: 라이트 웜 숏 패딩×10 + 코튼 에센셜 크루 반팔×15
(41,21,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-01-08 10:00:00','2026-01-08 10:00:00'),
(42,21,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-01-08 10:00:00','2026-01-08 10:00:00'),
-- Order 22 P2: 테리 루즈핏 후드집업×8 + 슬림 테이퍼드 데님×12
(43,22,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-01-18 14:00:00','2026-01-18 14:00:00'),
(44,22,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-01-18 14:00:00','2026-01-18 14:00:00'),
-- Order 23 P3: 파인게이지 라운드 니트×12 + 와이드 플리츠 롱팬츠×10
(45,23,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-01-28 09:30:00','2026-01-28 09:30:00'),
(46,23,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-01-28 09:30:00','2026-01-28 09:30:00'),
-- Order 24 P4: 클래식 싱글 자켓×8 + 소프트 코튼 롱슬리브×15
(47,24,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-02-12 11:00:00','2026-02-12 11:00:00'),
(48,24,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-02-12 11:00:00','2026-02-12 11:00:00'),
-- Order 25 P5: 헤비웨이트 로고 후드티×10 + 테크 플리스 트랙 팬츠×12
(49,25,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-02-22 15:00:00','2026-02-22 15:00:00'),
(50,25,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-02-22 15:00:00','2026-02-22 15:00:00'),
-- Order 26 P6: 케이블 포인트 가디건×10 + 플리츠 플로우 롱스커트×8
(51,26,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-03-15 10:30:00','2026-03-15 10:30:00'),
(52,26,(SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-03-15 10:30:00','2026-03-15 10:30:00'),
-- Order 27 P7: 옥스포드 버튼다운 셔츠×15 + 스트레이트 인디고 데님×10
(53,27,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-03-28 14:00:00','2026-03-28 14:00:00'),
(54,27,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-03-28 14:00:00','2026-03-28 14:00:00'),
-- Order 28 P8: 볼륨넥 덕다운 패딩×8 + 캐시 블렌드 크루 니트×12
(55,28,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-04-05 09:00:00','2026-04-05 09:00:00'),
(56,28,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-04-05 09:00:00','2026-04-05 09:00:00'),
-- Order 29 P9 CANCELLED: 브러시드 기모 후드티×10 + 테일러드 스트레이트 팬츠×8
(57,29,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-002-BLK-M'),'PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','브러시드 기모 후드티','상의','후드티','BLK','M',48900,10,'2026-04-18 10:00:00','2026-04-18 10:00:00'),
(58,29,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',47900,8,'2026-04-18 10:00:00','2026-04-18 10:00:00'),
-- Order 30 P10: 소프트 스웻 후드집업×10 + 소프트 조거 트레이닝×12
(59,30,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-WHT-M'),'PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','WHT','M',52900,10,'2026-05-08 09:00:00','2026-05-08 09:00:00'),
(60,30,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',38900,12,'2026-05-08 09:00:00','2026-05-08 09:00:00'),
-- Order 31 P1: 라이트 웜 숏 패딩×10 + 코튼 에센셜 크루 반팔×15
(61,31,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',66900,10,'2026-05-15 14:00:00','2026-05-15 14:00:00'),
(62,31,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-001-BLK-M'),'PRD-TOP-SS-001-BLK-M','PRD-TOP-SS-001','코튼 에센셜 크루 반팔','상의','반팔','BLK','M',19900,15,'2026-05-15 14:00:00','2026-05-15 14:00:00'),
-- Order 32 P2: 테리 루즈핏 후드집업×8 + 슬림 테이퍼드 데님×12
(63,32,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-NVY-L'),'PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','NVY','L',55900,8,'2026-05-22 10:00:00','2026-05-22 10:00:00'),
(64,32,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',52900,12,'2026-05-22 10:00:00','2026-05-22 10:00:00'),
-- Order 33 P3: 파인게이지 라운드 니트×12 + 와이드 플리츠 롱팬츠×10
(65,33,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-NVY-L'),'PRD-TOP-KN-001-NVY-L','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','NVY','L',39900,12,'2026-05-30 09:30:00','2026-05-30 09:30:00'),
(66,33,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',44900,10,'2026-05-30 09:30:00','2026-05-30 09:30:00'),
-- Order 34 P4: 클래식 싱글 자켓×8 + 소프트 코튼 롱슬리브×15
(67,34,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',64900,8,'2026-06-01 10:00:00','2026-06-01 10:00:00'),
(68,34,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-002-WHT-M'),'PRD-TOP-LS-002-WHT-M','PRD-TOP-LS-002','소프트 코튼 롱슬리브','상의','긴팔','WHT','M',26900,15,'2026-06-01 10:00:00','2026-06-01 10:00:00'),
-- Order 35 P5: 헤비웨이트 로고 후드티×10 + 테크 플리스 트랙 팬츠×12
(69,35,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',45900,10,'2026-06-02 09:00:00','2026-06-02 09:00:00'),
(70,35,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-NVY-L'),'PRD-PNT-TR-002-NVY-L','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','NVY','L',41900,12,'2026-06-02 09:00:00','2026-06-02 09:00:00'),
-- Order 36 P6: 케이블 포인트 가디건×10 + 플리츠 플로우 롱스커트×8
(71,36,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-NVY-L'),'PRD-OUT-CD-002-NVY-L','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','NVY','L',48900,10,'2026-06-03 14:00:00','2026-06-03 14:00:00'),
(72,36,(SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',42900,8,'2026-06-03 14:00:00','2026-06-03 14:00:00'),
-- Order 37 P7: 옥스포드 버튼다운 셔츠×15 + 스트레이트 인디고 데님×10
(73,37,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-WHT-M'),'PRD-TOP-SH-001-WHT-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','WHT','M',32900,15,'2026-06-04 09:30:00','2026-06-04 09:30:00'),
(74,37,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',49900,10,'2026-06-04 09:30:00','2026-06-04 09:30:00'),
-- Order 38 P8: 볼륨넥 덕다운 패딩×8 + 캐시 블렌드 크루 니트×12
(75,38,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',69900,8,'2026-06-05 10:00:00','2026-06-05 10:00:00'),
(76,38,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-NVY-L'),'PRD-TOP-KN-002-NVY-L','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','NVY','L',42900,12,'2026-06-05 10:00:00','2026-06-05 10:00:00'),
-- Order 39 P9: 브러시드 기모 후드티×10 + 테일러드 스트레이트 팬츠×8
(77,39,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-002-BLK-M'),'PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','브러시드 기모 후드티','상의','후드티','BLK','M',48900,10,'2026-06-06 09:00:00','2026-06-06 09:00:00'),
(78,39,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',47900,8,'2026-06-06 09:00:00','2026-06-06 09:00:00'),
-- Order 40 P10: 소프트 스웻 후드집업×10 + 소프트 조거 트레이닝×12
(79,40,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-WHT-M'),'PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','WHT','M',52900,10,'2026-06-07 14:00:00','2026-06-07 14:00:00'),
(80,40,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',38900,12,'2026-06-07 14:00:00','2026-06-07 14:00:00');

-- ===== store_order_status_history =====
INSERT INTO store_order_status_history
(order_header_id, history_type, status, changed_at, changed_by_member_id, changed_by_name, reason, create_date, update_date)
VALUES
(1,'ORDER_STATUS','REQUESTED','2026-01-05 10:00:00','st-sl-0001','강민재',NULL,'2026-01-05 10:00:00','2026-01-05 10:00:00'),
(1,'ORDER_STATUS','APPROVED', '2026-01-06 09:00:00','hq0001','본사관리자',NULL,'2026-01-06 09:00:00','2026-01-06 09:00:00'),
(1,'ORDER_STATUS','COMPLETED','2026-01-10 09:00:00','hq0001','본사관리자',NULL,'2026-01-10 09:00:00','2026-01-10 09:00:00'),
(2,'ORDER_STATUS','REQUESTED','2026-01-15 14:00:00','st-sl-0001','강민재',NULL,'2026-01-15 14:00:00','2026-01-15 14:00:00'),
(2,'ORDER_STATUS','APPROVED', '2026-01-16 09:00:00','hq0001','본사관리자',NULL,'2026-01-16 09:00:00','2026-01-16 09:00:00'),
(2,'ORDER_STATUS','COMPLETED','2026-01-20 09:00:00','hq0001','본사관리자',NULL,'2026-01-20 09:00:00','2026-01-20 09:00:00'),
(3,'ORDER_STATUS','REQUESTED','2026-01-25 09:30:00','st-sl-0001','강민재',NULL,'2026-01-25 09:30:00','2026-01-25 09:30:00'),
(3,'ORDER_STATUS','APPROVED', '2026-01-26 09:00:00','hq0001','본사관리자',NULL,'2026-01-26 09:00:00','2026-01-26 09:00:00'),
(3,'ORDER_STATUS','COMPLETED','2026-01-30 09:00:00','hq0001','본사관리자',NULL,'2026-01-30 09:00:00','2026-01-30 09:00:00'),
(4,'ORDER_STATUS','REQUESTED','2026-02-10 11:00:00','st-sl-0001','강민재',NULL,'2026-02-10 11:00:00','2026-02-10 11:00:00'),
(4,'ORDER_STATUS','APPROVED', '2026-02-11 09:00:00','hq0001','본사관리자',NULL,'2026-02-11 09:00:00','2026-02-11 09:00:00'),
(4,'ORDER_STATUS','COMPLETED','2026-02-15 09:00:00','hq0001','본사관리자',NULL,'2026-02-15 09:00:00','2026-02-15 09:00:00'),
(5,'ORDER_STATUS','REQUESTED','2026-02-20 15:00:00','st-sl-0001','강민재',NULL,'2026-02-20 15:00:00','2026-02-20 15:00:00'),
(5,'ORDER_STATUS','APPROVED', '2026-02-21 09:00:00','hq0001','본사관리자',NULL,'2026-02-21 09:00:00','2026-02-21 09:00:00'),
(5,'ORDER_STATUS','COMPLETED','2026-02-25 09:00:00','hq0001','본사관리자',NULL,'2026-02-25 09:00:00','2026-02-25 09:00:00'),
(6,'ORDER_STATUS','REQUESTED','2026-03-05 10:30:00','st-sl-0001','강민재',NULL,'2026-03-05 10:30:00','2026-03-05 10:30:00'),
(6,'ORDER_STATUS','APPROVED', '2026-03-06 09:00:00','hq0001','본사관리자',NULL,'2026-03-06 09:00:00','2026-03-06 09:00:00'),
(6,'ORDER_STATUS','COMPLETED','2026-03-10 09:00:00','hq0001','본사관리자',NULL,'2026-03-10 09:00:00','2026-03-10 09:00:00'),
(7,'ORDER_STATUS','REQUESTED','2026-03-18 14:00:00','st-sl-0001','강민재',NULL,'2026-03-18 14:00:00','2026-03-18 14:00:00'),
(7,'ORDER_STATUS','APPROVED', '2026-03-19 09:00:00','hq0001','본사관리자',NULL,'2026-03-19 09:00:00','2026-03-19 09:00:00'),
(7,'ORDER_STATUS','COMPLETED','2026-03-23 09:00:00','hq0001','본사관리자',NULL,'2026-03-23 09:00:00','2026-03-23 09:00:00'),
(8,'ORDER_STATUS','REQUESTED','2026-04-02 09:00:00','st-sl-0001','강민재',NULL,'2026-04-02 09:00:00','2026-04-02 09:00:00'),
(8,'ORDER_STATUS','APPROVED', '2026-04-03 09:00:00','hq0001','본사관리자',NULL,'2026-04-03 09:00:00','2026-04-03 09:00:00'),
(8,'ORDER_STATUS','COMPLETED','2026-04-07 09:00:00','hq0001','본사관리자',NULL,'2026-04-07 09:00:00','2026-04-07 09:00:00'),
(9,'ORDER_STATUS','REQUESTED','2026-04-15 11:00:00','st-sl-0001','강민재',NULL,'2026-04-15 11:00:00','2026-04-15 11:00:00'),
(9,'ORDER_STATUS','APPROVED', '2026-04-16 09:00:00','hq0001','본사관리자',NULL,'2026-04-16 09:00:00','2026-04-16 09:00:00'),
(9,'ORDER_STATUS','COMPLETED','2026-04-20 09:00:00','hq0001','본사관리자',NULL,'2026-04-20 09:00:00','2026-04-20 09:00:00'),
(10,'ORDER_STATUS','REQUESTED','2026-04-22 10:00:00','st-sl-0001','강민재',NULL,'2026-04-22 10:00:00','2026-04-22 10:00:00'),
(10,'ORDER_STATUS','CANCELLED','2026-04-23 11:00:00','hq0001','본사관리자','재고 조정으로 인한 발주 취소','2026-04-23 11:00:00','2026-04-23 11:00:00'),
(11,'ORDER_STATUS','REQUESTED','2026-05-05 09:00:00','st-sl-0001','강민재',NULL,'2026-05-05 09:00:00','2026-05-05 09:00:00'),
(11,'ORDER_STATUS','APPROVED', '2026-05-06 10:00:00','hq0001','본사관리자',NULL,'2026-05-06 10:00:00','2026-05-06 10:00:00'),
(12,'ORDER_STATUS','REQUESTED','2026-05-12 14:00:00','st-sl-0001','강민재',NULL,'2026-05-12 14:00:00','2026-05-12 14:00:00'),
(12,'ORDER_STATUS','APPROVED', '2026-05-13 10:00:00','hq0001','본사관리자',NULL,'2026-05-13 10:00:00','2026-05-13 10:00:00'),
(13,'ORDER_STATUS','REQUESTED','2026-05-20 10:00:00','st-sl-0001','강민재',NULL,'2026-05-20 10:00:00','2026-05-20 10:00:00'),
(13,'ORDER_STATUS','APPROVED', '2026-05-21 10:00:00','hq0001','본사관리자',NULL,'2026-05-21 10:00:00','2026-05-21 10:00:00'),
(14,'ORDER_STATUS','REQUESTED','2026-05-28 09:30:00','st-sl-0001','강민재',NULL,'2026-05-28 09:30:00','2026-05-28 09:30:00'),
(14,'ORDER_STATUS','APPROVED', '2026-05-29 10:00:00','hq0001','본사관리자',NULL,'2026-05-29 10:00:00','2026-05-29 10:00:00'),
(15,'ORDER_STATUS','REQUESTED','2026-06-01 09:00:00','st-sl-0001','강민재',NULL,'2026-06-01 09:00:00','2026-06-01 09:00:00'),
(16,'ORDER_STATUS','REQUESTED','2026-06-02 10:00:00','st-sl-0001','강민재',NULL,'2026-06-02 10:00:00','2026-06-02 10:00:00'),
(17,'ORDER_STATUS','REQUESTED','2026-06-03 09:30:00','st-sl-0001','강민재',NULL,'2026-06-03 09:30:00','2026-06-03 09:30:00'),
(18,'ORDER_STATUS','REQUESTED','2026-06-04 14:00:00','st-sl-0001','강민재',NULL,'2026-06-04 14:00:00','2026-06-04 14:00:00'),
(19,'ORDER_STATUS','REQUESTED','2026-06-05 09:00:00','st-sl-0001','강민재',NULL,'2026-06-05 09:00:00','2026-06-05 09:00:00'),
(20,'ORDER_STATUS','REQUESTED','2026-06-06 11:00:00','st-sl-0001','강민재',NULL,'2026-06-06 11:00:00','2026-06-06 11:00:00'),
(21,'ORDER_STATUS','REQUESTED','2026-01-08 10:00:00','st-sl-0002','박서윤',NULL,'2026-01-08 10:00:00','2026-01-08 10:00:00'),
(21,'ORDER_STATUS','APPROVED', '2026-01-09 09:00:00','hq0001','본사관리자',NULL,'2026-01-09 09:00:00','2026-01-09 09:00:00'),
(21,'ORDER_STATUS','COMPLETED','2026-01-13 09:00:00','hq0001','본사관리자',NULL,'2026-01-13 09:00:00','2026-01-13 09:00:00'),
(22,'ORDER_STATUS','REQUESTED','2026-01-18 14:00:00','st-sl-0002','박서윤',NULL,'2026-01-18 14:00:00','2026-01-18 14:00:00'),
(22,'ORDER_STATUS','APPROVED', '2026-01-19 09:00:00','hq0001','본사관리자',NULL,'2026-01-19 09:00:00','2026-01-19 09:00:00'),
(22,'ORDER_STATUS','COMPLETED','2026-01-23 09:00:00','hq0001','본사관리자',NULL,'2026-01-23 09:00:00','2026-01-23 09:00:00'),
(23,'ORDER_STATUS','REQUESTED','2026-01-28 09:30:00','st-sl-0002','박서윤',NULL,'2026-01-28 09:30:00','2026-01-28 09:30:00'),
(23,'ORDER_STATUS','APPROVED', '2026-01-29 09:00:00','hq0001','본사관리자',NULL,'2026-01-29 09:00:00','2026-01-29 09:00:00'),
(23,'ORDER_STATUS','COMPLETED','2026-02-02 09:00:00','hq0001','본사관리자',NULL,'2026-02-02 09:00:00','2026-02-02 09:00:00'),
(24,'ORDER_STATUS','REQUESTED','2026-02-12 11:00:00','st-sl-0002','박서윤',NULL,'2026-02-12 11:00:00','2026-02-12 11:00:00'),
(24,'ORDER_STATUS','APPROVED', '2026-02-13 09:00:00','hq0001','본사관리자',NULL,'2026-02-13 09:00:00','2026-02-13 09:00:00'),
(24,'ORDER_STATUS','COMPLETED','2026-02-17 09:00:00','hq0001','본사관리자',NULL,'2026-02-17 09:00:00','2026-02-17 09:00:00'),
(25,'ORDER_STATUS','REQUESTED','2026-02-22 15:00:00','st-sl-0002','박서윤',NULL,'2026-02-22 15:00:00','2026-02-22 15:00:00'),
(25,'ORDER_STATUS','APPROVED', '2026-02-23 09:00:00','hq0001','본사관리자',NULL,'2026-02-23 09:00:00','2026-02-23 09:00:00'),
(25,'ORDER_STATUS','COMPLETED','2026-02-27 09:00:00','hq0001','본사관리자',NULL,'2026-02-27 09:00:00','2026-02-27 09:00:00'),
(26,'ORDER_STATUS','REQUESTED','2026-03-15 10:30:00','st-sl-0002','박서윤',NULL,'2026-03-15 10:30:00','2026-03-15 10:30:00'),
(26,'ORDER_STATUS','APPROVED', '2026-03-16 09:00:00','hq0001','본사관리자',NULL,'2026-03-16 09:00:00','2026-03-16 09:00:00'),
(26,'ORDER_STATUS','COMPLETED','2026-03-20 09:00:00','hq0001','본사관리자',NULL,'2026-03-20 09:00:00','2026-03-20 09:00:00'),
(27,'ORDER_STATUS','REQUESTED','2026-03-28 14:00:00','st-sl-0002','박서윤',NULL,'2026-03-28 14:00:00','2026-03-28 14:00:00'),
(27,'ORDER_STATUS','APPROVED', '2026-03-29 09:00:00','hq0001','본사관리자',NULL,'2026-03-29 09:00:00','2026-03-29 09:00:00'),
(27,'ORDER_STATUS','COMPLETED','2026-04-02 09:00:00','hq0001','본사관리자',NULL,'2026-04-02 09:00:00','2026-04-02 09:00:00'),
(28,'ORDER_STATUS','REQUESTED','2026-04-05 09:00:00','st-sl-0002','박서윤',NULL,'2026-04-05 09:00:00','2026-04-05 09:00:00'),
(28,'ORDER_STATUS','APPROVED', '2026-04-06 09:00:00','hq0001','본사관리자',NULL,'2026-04-06 09:00:00','2026-04-06 09:00:00'),
(28,'ORDER_STATUS','COMPLETED','2026-04-10 09:00:00','hq0001','본사관리자',NULL,'2026-04-10 09:00:00','2026-04-10 09:00:00'),
(29,'ORDER_STATUS','REQUESTED','2026-04-18 10:00:00','st-sl-0002','박서윤',NULL,'2026-04-18 10:00:00','2026-04-18 10:00:00'),
(29,'ORDER_STATUS','CANCELLED','2026-04-19 11:00:00','hq0001','본사관리자','시즌 변경으로 인한 발주 취소','2026-04-19 11:00:00','2026-04-19 11:00:00'),
(30,'ORDER_STATUS','REQUESTED','2026-05-08 09:00:00','st-sl-0002','박서윤',NULL,'2026-05-08 09:00:00','2026-05-08 09:00:00'),
(30,'ORDER_STATUS','APPROVED', '2026-05-09 10:00:00','hq0001','본사관리자',NULL,'2026-05-09 10:00:00','2026-05-09 10:00:00'),
(31,'ORDER_STATUS','REQUESTED','2026-05-15 14:00:00','st-sl-0002','박서윤',NULL,'2026-05-15 14:00:00','2026-05-15 14:00:00'),
(31,'ORDER_STATUS','APPROVED', '2026-05-16 10:00:00','hq0001','본사관리자',NULL,'2026-05-16 10:00:00','2026-05-16 10:00:00'),
(32,'ORDER_STATUS','REQUESTED','2026-05-22 10:00:00','st-sl-0002','박서윤',NULL,'2026-05-22 10:00:00','2026-05-22 10:00:00'),
(32,'ORDER_STATUS','APPROVED', '2026-05-23 10:00:00','hq0001','본사관리자',NULL,'2026-05-23 10:00:00','2026-05-23 10:00:00'),
(33,'ORDER_STATUS','REQUESTED','2026-05-30 09:30:00','st-sl-0002','박서윤',NULL,'2026-05-30 09:30:00','2026-05-30 09:30:00'),
(33,'ORDER_STATUS','APPROVED', '2026-05-31 10:00:00','hq0001','본사관리자',NULL,'2026-05-31 10:00:00','2026-05-31 10:00:00'),
(34,'ORDER_STATUS','REQUESTED','2026-06-01 10:00:00','st-sl-0002','박서윤',NULL,'2026-06-01 10:00:00','2026-06-01 10:00:00'),
(35,'ORDER_STATUS','REQUESTED','2026-06-02 09:00:00','st-sl-0002','박서윤',NULL,'2026-06-02 09:00:00','2026-06-02 09:00:00'),
(36,'ORDER_STATUS','REQUESTED','2026-06-03 14:00:00','st-sl-0002','박서윤',NULL,'2026-06-03 14:00:00','2026-06-03 14:00:00'),
(37,'ORDER_STATUS','REQUESTED','2026-06-04 09:30:00','st-sl-0002','박서윤',NULL,'2026-06-04 09:30:00','2026-06-04 09:30:00'),
(38,'ORDER_STATUS','REQUESTED','2026-06-05 10:00:00','st-sl-0002','박서윤',NULL,'2026-06-05 10:00:00','2026-06-05 10:00:00'),
(39,'ORDER_STATUS','REQUESTED','2026-06-06 09:00:00','st-sl-0002','박서윤',NULL,'2026-06-06 09:00:00','2026-06-06 09:00:00'),
(40,'ORDER_STATUS','REQUESTED','2026-06-07 14:00:00','st-sl-0002','박서윤',NULL,'2026-06-07 14:00:00','2026-06-07 14:00:00');
