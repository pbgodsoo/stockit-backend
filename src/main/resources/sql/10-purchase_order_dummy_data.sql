-- ============================================================
-- 10-purchase_order_dummy_data.sql
-- 본사 발주 더미 데이터 (정산/통계 - 발주량 통계 페이지 검증용)
--
-- 분포:
--   - vendor 마스터 8건 (id 1~8) — 사용자 DB 와 일치
--   - vendor_product 21건 (id 1~26 중 일부 공백) — product_master 의 fashion 상품 매핑
--   - purchase_order 헤더 30건 (id 1101~1130, 6개월 분포 2025-11 ~ 2026-04)
--   - purchase_order_item 라인 68건 (id 6001~6068)
--   - status 분포: COMPLETED 26 / ARRIVED 4 / CANCELLED 1
-- 의존:
--   - infrastructure (창고 id 1~5)
--   - product_master (PRD-* 코드들 04 시드에서 생성됨)
-- ============================================================

-- ──────────────────────────────────────────────
-- 1) VENDOR 마스터 (8건, id 1~8)
-- ──────────────────────────────────────────────
INSERT INTO vendor
(id, code, name, contact_name, contact_phone, contact_email, status, create_date, update_date)
VALUES
(1, 'VND-001', '(주)테크서플라이', '이공급', '02-1111-1111', 'lee@techsupply.co.kr',     'ACTIVE', NOW(), NOW()),
(2, 'VND-002', '한국생활물산',     '최생활', '02-2222-2222', 'choi@klife.co.kr',          'ACTIVE', NOW(), NOW()),
(3, 'VND-003', '글로벌오피스',     '정사무', '02-3333-3333', 'jung@goffice.co.kr',        'ACTIVE', NOW(), NOW()),
(4, 'VND-004', '위생물자(주)',     '윤위생', '02-4444-4444', 'yoon@hygiene.co.kr',        'ACTIVE', NOW(), NOW()),
(5, 'VND-005', '스마트주방솔루션', '오주방', '02-5555-5555', 'oh@smartkitchen.co.kr',     'ACTIVE', NOW(), NOW()),
(6, 'VND-006', '패션라인(주)',     '김패션', '02-6666-6666', 'kim@fashionline.co.kr',     'ACTIVE', NOW(), NOW()),
(7, 'VND-007', '슈즈모아',         '박슈즈', '02-7777-7777', 'park@shoemoa.co.kr',        'ACTIVE', NOW(), NOW()),
(8, 'VND-008', '코스메틱플러스',   '한코스', '02-8888-8888', 'han@cosplus.co.kr',         'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  contact_name = VALUES(contact_name),
  contact_phone = VALUES(contact_phone),
  contact_email = VALUES(contact_email),
  update_date = NOW();

-- ──────────────────────────────────────────────
-- 2) VENDOR_PRODUCT (21건, id 1~26 중 일부 공백 — 알파벳 정렬)
-- ──────────────────────────────────────────────
ALTER TABLE vendor_product AUTO_INCREMENT = 1;

INSERT INTO vendor_product
(id, code, vendor_id, product_code, product_name, unit_price, moq, lead_time_days,
 contract_start, contract_end, status, create_date, update_date)
VALUES
(1, 'VP-OUT-CD-001-V05', 5, 'PRD-OUT-CD-001', '브이넥 버튼 가디건',         189100, 50,  6, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(2, 'VP-OUT-CD-002-V06', 6, 'PRD-OUT-CD-002', '케이블 포인트 가디건',       190300, 50,  7, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(4, 'VP-OUT-HZ-001-V01', 1, 'PRD-OUT-HZ-001', '소프트 스웻 후드집업',       165100, 50, 11, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(5, 'VP-OUT-HZ-002-V02', 2, 'PRD-OUT-HZ-002', '테리 루즈핏 후드집업',       166300, 50,  5, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(7, 'VP-OUT-JK-001-V03', 3, 'PRD-OUT-JK-001', '클래식 싱글 자켓',           177100, 50,  5, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(8, 'VP-OUT-JK-002-V04', 4, 'PRD-OUT-JK-002', '미니멀 크롭 블레이저',       178300, 50,  6, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(10, 'VP-OUT-PD-001-V07', 7, 'PRD-OUT-PD-001', '라이트 웜 숏 패딩',          153100, 50, 10, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(11, 'VP-OUT-PD-002-V08', 8, 'PRD-OUT-PD-002', '볼륨넥 덕다운 패딩',         154300, 50, 11, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(13, 'VP-PNT-DN-001-V03', 3, 'PRD-PNT-DN-001', '스트레이트 인디고 데님',     81100,  50, 11, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(14, 'VP-PNT-DN-002-V04', 4, 'PRD-PNT-DN-002', '슬림 테이퍼드 데님',         82300,  50,  5, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(16, 'VP-PNT-LG-001-V07', 7, 'PRD-PNT-LG-001', '와이드 플리츠 롱팬츠',       105100, 50,  6, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(17, 'VP-PNT-LG-002-V08', 8, 'PRD-PNT-LG-002', '테일러드 스트레이트 팬츠',   106300, 50,  7, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(18, 'VP-PNT-ST-001-V05', 5, 'PRD-PNT-ST-001', '코튼 치노 쇼츠',             93100,  50,  5, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(19, 'VP-SKT-LG-001-V05', 5, 'PRD-SKT-LG-001', '플리츠 플로우 롱스커트',     141100, 50,  9, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(20, 'VP-SKT-MN-001-V03', 3, 'PRD-SKT-MN-001', 'A라인 코튼 미니 스커트',     129100, 50,  8, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(21, 'VP-TOP-KN-001-V07', 7, 'PRD-TOP-KN-001', '파인게이지 라운드 니트',     57100,  50,  9, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(22, 'VP-TOP-LS-001-V03', 3, 'PRD-TOP-LS-001', '슬림 베이스 레이어 긴팔',    33100,  50,  7, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(23, 'VP-TOP-LS-002-V04', 4, 'PRD-TOP-LS-002', '소프트 코튼 롱슬리브',       34300,  50,  8, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(24, 'VP-TOP-SH-001-V05', 5, 'PRD-TOP-SH-001', '옥스포드 버튼다운 셔츠',     45100,  50,  8, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(25, 'VP-TOP-SS-001-V01', 1, 'PRD-TOP-SS-001', '코튼 에센셜 크루 반팔',      21100,  50,  6, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW()),
(26, 'VP-TOP-SS-002-V02', 2, 'PRD-TOP-SS-002', '드라이핏 액티브 반팔',       22300,  50,  7, '2025-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  vendor_id = VALUES(vendor_id),
  product_code = VALUES(product_code),
  product_name = VALUES(product_name),
  unit_price = VALUES(unit_price),
  update_date = NOW();

-- ──────────────────────────────────────────────
-- 3) PURCHASE_ORDER 헤더 (30건, id 1101~1130)
-- ──────────────────────────────────────────────
ALTER TABLE purchase_order AUTO_INCREMENT = 1101;

INSERT INTO purchase_order
(id, code, vendor_id, vendor_name, vendor_contact_name,
 warehouse_id, warehouse_name, member_id, member_name,
 status, total_amount, cancel_reason, create_date, update_date)
VALUES
-- 2025-11
(1101, 'PO-202511-0001', 6, '패션라인(주)', '김패션', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'COMPLETED', 4340000, NULL, '2025-11-03 10:15:00', NOW()),
(1102, 'PO-202511-0002', 1, '(주)테크서플라이', '이공급', 2, '서울 동북권 스마트 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 2353000, NULL, '2025-11-08 13:25:00', NOW()),
(1103, 'PO-202511-0003', 6, '패션라인(주)', '김패션', 3, '경기 남부 통합 배송센터', 'hq0001', '본사관리자', 'COMPLETED', 3682500, NULL, '2025-11-15 09:40:00', NOW()),
(1104, 'PO-202511-0004', 7, '슈즈모아', '박슈즈', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'COMPLETED', 1979300, NULL, '2025-11-22 15:50:00', NOW()),
-- 2025-12
(1105, 'PO-202512-0001', 6, '패션라인(주)', '김패션', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'COMPLETED', 5691000, NULL, '2025-12-01 11:00:00', NOW()),
(1106, 'PO-202512-0002', 2, '한국생활물산', '최생활', 4, '경기 북부 이커머스 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 3068000, NULL, '2025-12-08 14:30:00', NOW()),
(1107, 'PO-202512-0003', 6, '패션라인(주)', '김패션', 5, '인천 송도 국제물류센터', 'hq0001', '본사관리자', 'COMPLETED', 4472100, NULL, '2025-12-15 10:20:00', NOW()),
(1108, 'PO-202512-0004', 1, '(주)테크서플라이', '이공급', 2, '서울 동북권 스마트 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 2684000, NULL, '2025-12-22 16:10:00', NOW()),
(1109, 'PO-202512-0005', 8, '코스메틱플러스', '한코스', 3, '경기 남부 통합 배송센터', 'hq0001', '본사관리자', 'COMPLETED', 1959000, NULL, '2025-12-28 13:00:00', NOW()),
-- 2026-01
(1110, 'PO-202601-0001', 6, '패션라인(주)', '김패션', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'COMPLETED', 3845500, NULL, '2026-01-05 09:30:00', NOW()),
(1111, 'PO-202601-0002', 7, '슈즈모아', '박슈즈', 4, '경기 북부 이커머스 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 2278700, NULL, '2026-01-12 14:50:00', NOW()),
(1112, 'PO-202601-0003', 6, '패션라인(주)', '김패션', 2, '서울 동북권 스마트 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 4796100, NULL, '2026-01-18 11:25:00', NOW()),
(1113, 'PO-202601-0004', 3, '글로벌오피스', '정사무', 5, '인천 송도 국제물류센터', 'hq0001', '본사관리자', 'COMPLETED', 3425400, NULL, '2026-01-23 16:15:00', NOW()),
(1114, 'PO-202601-0005', 6, '패션라인(주)', '김패션', 3, '경기 남부 통합 배송센터', 'hq0001', '본사관리자', 'CANCELLED', 1679000, NULL, '2026-01-29 10:00:00', NOW()),
-- 2026-02
(1115, 'PO-202602-0001', 6, '패션라인(주)', '김패션', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'COMPLETED', 5349000, NULL, '2026-02-04 13:20:00', NOW()),
(1116, 'PO-202602-0002', 5, '스마트주방솔루션', '오주방', 2, '서울 동북권 스마트 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 2619200, NULL, '2026-02-10 11:40:00', NOW()),
(1117, 'PO-202602-0003', 6, '패션라인(주)', '김패션', 4, '경기 북부 이커머스 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 4145500, NULL, '2026-02-16 15:00:00', NOW()),
(1118, 'PO-202602-0004', 7, '슈즈모아', '박슈즈', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'COMPLETED', 1984000, NULL, '2026-02-22 09:50:00', NOW()),
(1119, 'PO-202602-0005', 4, '위생물자(주)', '윤위생', 5, '인천 송도 국제물류센터', 'hq0001', '본사관리자', 'COMPLETED', 3696500, NULL, '2026-02-27 14:25:00', NOW()),
-- 2026-03
(1120, 'PO-202603-0001', 6, '패션라인(주)', '김패션', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'COMPLETED', 6148000, NULL, '2026-03-03 10:35:00', NOW()),
(1121, 'PO-202603-0002', 6, '패션라인(주)', '김패션', 3, '경기 남부 통합 배송센터', 'hq0001', '본사관리자', 'COMPLETED', 3858800, NULL, '2026-03-09 16:20:00', NOW()),
(1122, 'PO-202603-0003', 1, '(주)테크서플라이', '이공급', 2, '서울 동북권 스마트 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 2914000, NULL, '2026-03-15 11:15:00', NOW()),
(1123, 'PO-202603-0004', 6, '패션라인(주)', '김패션', 4, '경기 북부 이커머스 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 4822000, NULL, '2026-03-20 13:45:00', NOW()),
(1124, 'PO-202603-0005', 7, '슈즈모아', '박슈즈', 5, '인천 송도 국제물류센터', 'hq0001', '본사관리자', 'COMPLETED', 2458500, NULL, '2026-03-26 15:10:00', NOW()),
-- 2026-04
(1125, 'PO-202604-0001', 6, '패션라인(주)', '김패션', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'COMPLETED', 5318800, NULL, '2026-04-02 10:00:00', NOW()),
(1126, 'PO-202604-0002', 6, '패션라인(주)', '김패션', 2, '서울 동북권 스마트 물류센터', 'hq0001', '본사관리자', 'COMPLETED', 3736800, NULL, '2026-04-08 14:40:00', NOW()),
(1127, 'PO-202604-0003', 6, '패션라인(주)', '김패션', 3, '경기 남부 통합 배송센터', 'hq0001', '본사관리자', 'ARRIVED', 4305800, NULL, '2026-04-15 11:30:00', NOW()),
(1128, 'PO-202604-0004', 7, '슈즈모아', '박슈즈', 4, '경기 북부 이커머스 물류센터', 'hq0001', '본사관리자', 'ARRIVED', 2564000, NULL, '2026-04-20 16:50:00', NOW()),
(1129, 'PO-202604-0005', 6, '패션라인(주)', '김패션', 1, '서울 도심 풀필먼트 허브', 'hq0001', '본사관리자', 'ARRIVED', 3857500, NULL, '2026-04-25 13:15:00', NOW()),
(1130, 'PO-202604-0006', 6, '패션라인(주)', '김패션', 5, '인천 송도 국제물류센터', 'hq0001', '본사관리자', 'ARRIVED', 2852800, NULL, '2026-04-28 09:20:00', NOW())
ON DUPLICATE KEY UPDATE
  vendor_id = VALUES(vendor_id),
  vendor_name = VALUES(vendor_name),
  warehouse_id = VALUES(warehouse_id),
  warehouse_name = VALUES(warehouse_name),
  status = VALUES(status),
  total_amount = VALUES(total_amount),
  update_date = NOW();

-- ──────────────────────────────────────────────
-- 4) PURCHASE_ORDER_ITEM (68건, id 6001~6068)
-- ──────────────────────────────────────────────
ALTER TABLE purchase_order_item AUTO_INCREMENT = 6001;

INSERT INTO purchase_order_item
(purchase_order_id, vendor_product_id, product_code, product_name, sku_code, color, size, unit_price, quantity, subtotal)
VALUES
-- 1101 (4,340,000) 패션라인 COMPLETED
(1101, 25, 'PRD-TOP-SS-001', '코튼 에센셜 크루 반팔', 'SKU-PRD-TOP-SS-001-BLK-M', 'BLACK', 'M', 21100, 100, 2110000),
(1101, 26, 'PRD-TOP-SS-002', '드라이핏 액티브 반팔', 'SKU-PRD-TOP-SS-002-NVY-L', 'NAVY', 'L', 22300, 100, 2230000),
-- 1102 (2,353,000) 테크서플라이 COMPLETED
(1102, 22, 'PRD-TOP-LS-001', '슬림 베이스 레이어 긴팔', 'SKU-PRD-TOP-LS-001-WHT-M', 'WHITE', 'M', 33100, 40, 1324000),
(1102, 23, 'PRD-TOP-LS-002', '소프트 코튼 롱슬리브', 'SKU-PRD-TOP-LS-002-GRY-L', 'GREY', 'L', 34300, 30, 1029000),
-- 1103 (3,682,500) 패션라인 COMPLETED
(1103, 24, 'PRD-TOP-SH-001', '옥스포드 버튼다운 셔츠', 'SKU-PRD-TOP-SH-001-BLK-L', 'BLACK', 'L', 45100, 50, 2255000),
(1103, 21, 'PRD-TOP-KN-001', '파인게이지 라운드 니트', 'SKU-PRD-TOP-KN-001-BEI-M', 'BEIGE', 'M', 57100, 25, 1427500),
-- 1104 (1,979,300) 슈즈모아 COMPLETED
(1104, 14, 'PRD-PNT-DN-002', '슬림 테이퍼드 데님', 'SKU-PRD-PNT-DN-002-BLU-M', 'BLUE', 'M', 82300, 15, 1234500),
(1104, 18, 'PRD-PNT-ST-001', '코튼 치노 쇼츠', 'SKU-PRD-PNT-ST-001-BEI-L', 'BEIGE', 'L', 93100, 8, 744800),
-- 1105 (5,691,000) 패션라인 COMPLETED
(1105, 1, 'PRD-OUT-CD-001', '브이넥 버튼 가디건', 'SKU-PRD-OUT-CD-001-BLK-M', 'BLACK', 'M', 189100, 15, 2836500),
(1105, 2, 'PRD-OUT-CD-002', '케이블 포인트 가디건', 'SKU-PRD-OUT-CD-002-BRN-L', 'BROWN', 'L', 190300, 15, 2854500),
-- 1106 (3,068,000) 한국생활물산 COMPLETED
(1106, 10, 'PRD-OUT-PD-001', '라이트 웜 숏 패딩', 'SKU-PRD-OUT-PD-001-BLK-L', 'BLACK', 'L', 153100, 15, 2296500),
(1106, 11, 'PRD-OUT-PD-002', '볼륨넥 덕다운 패딩', 'SKU-PRD-OUT-PD-002-NVY-M', 'NAVY', 'M', 154300, 5, 771500),
-- 1107 (4,472,100) 패션라인 COMPLETED
(1107, 4, 'PRD-OUT-HZ-001', '소프트 스웻 후드집업', 'SKU-PRD-OUT-HZ-001-GRY-L', 'GREY', 'L', 165100, 15, 2476500),
(1107, 5, 'PRD-OUT-HZ-002', '테리 루즈핏 후드집업', 'SKU-PRD-OUT-HZ-002-BLK-M', 'BLACK', 'M', 166300, 12, 1995600),
-- 1108 (2,684,000) 테크서플라이 COMPLETED
(1108, 22, 'PRD-TOP-LS-001', '슬림 베이스 레이어 긴팔', 'SKU-PRD-TOP-LS-001-NVY-M', 'NAVY', 'M', 33100, 50, 1655000),
(1108, 23, 'PRD-TOP-LS-002', '소프트 코튼 롱슬리브', 'SKU-PRD-TOP-LS-002-WHT-L', 'WHITE', 'L', 34300, 30, 1029000),
-- 1109 (2,019,000) 코스메틱플러스 COMPLETED
(1109, 26, 'PRD-TOP-SS-002', '드라이핏 액티브 반팔', 'SKU-PRD-TOP-SS-002-BEI-S', 'BEIGE', 'S', 22300, 50, 1115000),
(1109, 25, 'PRD-TOP-SS-001', '코튼 에센셜 크루 반팔', 'SKU-PRD-TOP-SS-001-GRY-M', 'GREY', 'M', 21100, 40, 844000),
-- 1110 (3,863,500) 패션라인 COMPLETED
(1110, 11, 'PRD-OUT-PD-002', '볼륨넥 덕다운 패딩', 'SKU-PRD-OUT-PD-002-BLK-L', 'BLACK', 'L', 154300, 15, 2314500),
(1110, 10, 'PRD-OUT-PD-001', '라이트 웜 숏 패딩', 'SKU-PRD-OUT-PD-001-NVY-M', 'NAVY', 'M', 153100, 10, 1531000),
-- 1111 (2,278,700) 슈즈모아 COMPLETED
(1111, 20, 'PRD-SKT-MN-001', 'A라인 코튼 미니 스커트', 'SKU-PRD-SKT-MN-001-BLK-M', 'BLACK', 'M', 129100, 10, 1291000),
(1111, 19, 'PRD-SKT-LG-001', '플리츠 플로우 롱스커트', 'SKU-PRD-SKT-LG-001-BEI-L', 'BEIGE', 'L', 141100, 7, 987700),
-- 1112 (4,796,100) 패션라인 COMPLETED
(1112, 7, 'PRD-OUT-JK-001', '클래식 싱글 자켓', 'SKU-PRD-OUT-JK-001-BLK-L', 'BLACK', 'L', 177100, 15, 2656500),
(1112, 8, 'PRD-OUT-JK-002', '미니멀 크롭 블레이저', 'SKU-PRD-OUT-JK-002-GRY-M', 'GREY', 'M', 178300, 12, 2139600),
-- 1113 (3,437,400) 글로벌오피스 COMPLETED
(1113, 2, 'PRD-OUT-CD-002', '케이블 포인트 가디건', 'SKU-PRD-OUT-CD-002-BRN-L', 'BROWN', 'L', 190300, 10, 1903000),
(1113, 2, 'PRD-OUT-CD-002', '케이블 포인트 가디건', 'SKU-PRD-OUT-CD-002-BEI-M', 'BEIGE', 'M', 190300, 8, 1522400),
-- 1114 (1,679,000) 패션라인 CANCELLED
(1114, 22, 'PRD-TOP-LS-001', '슬림 베이스 레이어 긴팔', 'SKU-PRD-TOP-LS-001-BLK-M', 'BLACK', 'M', 33100, 30, 993000),
(1114, 23, 'PRD-TOP-LS-002', '소프트 코튼 롱슬리브', 'SKU-PRD-TOP-LS-002-NVY-L', 'NAVY', 'L', 34300, 20, 686000),
-- 1115 (5,367,000) 패션라인 COMPLETED
(1115, 8, 'PRD-OUT-JK-002', '미니멀 크롭 블레이저', 'SKU-PRD-OUT-JK-002-BLK-L', 'BLACK', 'L', 178300, 15, 2674500),
(1115, 8, 'PRD-OUT-JK-002', '미니멀 크롭 블레이저', 'SKU-PRD-OUT-JK-002-NVY-M', 'NAVY', 'M', 178300, 15, 2674500),
-- 1116 (2,643,200) 스마트주방솔루션 COMPLETED
(1116, 14, 'PRD-PNT-DN-002', '슬림 테이퍼드 데님', 'SKU-PRD-PNT-DN-002-BLU-L', 'BLUE', 'L', 82300, 20, 1646000),
(1116, 13, 'PRD-PNT-DN-001', '스트레이트 인디고 데님', 'SKU-PRD-PNT-DN-001-BLU-M', 'BLUE', 'M', 81100, 12, 973200),
-- 1117 (4,163,500) 패션라인 COMPLETED
(1117, 5, 'PRD-OUT-HZ-002', '테리 루즈핏 후드집업', 'SKU-PRD-OUT-HZ-002-BLK-L', 'BLACK', 'L', 166300, 15, 2494500),
(1117, 4, 'PRD-OUT-HZ-001', '소프트 스웻 후드집업', 'SKU-PRD-OUT-HZ-001-NVY-M', 'NAVY', 'M', 165100, 10, 1651000),
-- 1118 (1,984,000) 슈즈모아 COMPLETED
(1118, 24, 'PRD-TOP-SH-001', '옥스포드 버튼다운 셔츠', 'SKU-PRD-TOP-SH-001-WHT-M', 'WHITE', 'M', 45100, 25, 1127500),
(1118, 21, 'PRD-TOP-KN-001', '파인게이지 라운드 니트', 'SKU-PRD-TOP-KN-001-BEI-L', 'BEIGE', 'L', 57100, 15, 856500),
-- 1119 (3,696,500) 위생물자 COMPLETED
(1119, 16, 'PRD-PNT-LG-001', '와이드 플리츠 롱팬츠', 'SKU-PRD-PNT-LG-001-BLK-L', 'BLACK', 'L', 105100, 20, 2102000),
(1119, 17, 'PRD-PNT-LG-002', '테일러드 스트레이트 팬츠', 'SKU-PRD-PNT-LG-002-GRY-M', 'GREY', 'M', 106300, 15, 1594500),
-- 1120 (6,148,000) 패션라인 COMPLETED
(1120, 11, 'PRD-OUT-PD-002', '볼륨넥 덕다운 패딩', 'SKU-PRD-OUT-PD-002-BLK-L', 'BLACK', 'L', 154300, 20, 3086000),
(1120, 10, 'PRD-OUT-PD-001', '라이트 웜 숏 패딩', 'SKU-PRD-OUT-PD-001-GRY-M', 'GREY', 'M', 153100, 20, 3062000),
-- 1121 (3,858,800) 패션라인 COMPLETED — 3 items
(1121, 1, 'PRD-OUT-CD-001', '브이넥 버튼 가디건', 'SKU-PRD-OUT-CD-001-BLK-M', 'BLACK', 'M', 189100, 8, 1512800),
(1121, 22, 'PRD-TOP-LS-001', '슬림 베이스 레이어 긴팔', 'SKU-PRD-TOP-LS-001-GRY-L', 'GREY', 'L', 33100, 30, 993000),
(1121, 24, 'PRD-TOP-SH-001', '옥스포드 버튼다운 셔츠', 'SKU-PRD-TOP-SH-001-WHT-M', 'WHITE', 'M', 45100, 30, 1353000),
-- 1122 (2,962,000) 테크서플라이 COMPLETED — 3 items
(1122, 22, 'PRD-TOP-LS-001', '슬림 베이스 레이어 긴팔', 'SKU-PRD-TOP-LS-001-BEI-M', 'BEIGE', 'M', 33100, 30, 993000),
(1122, 23, 'PRD-TOP-LS-002', '소프트 코튼 롱슬리브', 'SKU-PRD-TOP-LS-002-BRN-L', 'BROWN', 'L', 34300, 30, 1029000),
(1122, 26, 'PRD-TOP-SS-002', '드라이핏 액티브 반팔', 'SKU-PRD-TOP-SS-002-GRY-S', 'GREY', 'S', 22300, 40, 892000),
-- 1123 (4,822,000) 패션라인 COMPLETED — 3 items
(1123, 7, 'PRD-OUT-JK-001', '클래식 싱글 자켓', 'SKU-PRD-OUT-JK-001-NVY-L', 'NAVY', 'L', 177100, 15, 2656500),
(1123, 14, 'PRD-PNT-DN-002', '슬림 테이퍼드 데님', 'SKU-PRD-PNT-DN-002-BLU-M', 'BLUE', 'M', 82300, 15, 1234500),
(1123, 18, 'PRD-PNT-ST-001', '코튼 치노 쇼츠', 'SKU-PRD-PNT-ST-001-BEI-L', 'BEIGE', 'L', 93100, 10, 931000),
-- 1124 (2,494,500) 슈즈모아 COMPLETED — 3 items
(1124, 24, 'PRD-TOP-SH-001', '옥스포드 버튼다운 셔츠', 'SKU-PRD-TOP-SH-001-WHT-L', 'WHITE', 'L', 45100, 25, 1127500),
(1124, 22, 'PRD-TOP-LS-001', '슬림 베이스 레이어 긴팔', 'SKU-PRD-TOP-LS-001-BLK-M', 'BLACK', 'M', 33100, 20, 662000),
(1124, 26, 'PRD-TOP-SS-002', '드라이핏 액티브 반팔', 'SKU-PRD-TOP-SS-002-NVY-S', 'NAVY', 'S', 22300, 30, 669000),
-- 1125 (5,330,800) 패션라인 COMPLETED — 3 items
(1125, 2, 'PRD-OUT-CD-002', '케이블 포인트 가디건', 'SKU-PRD-OUT-CD-002-BRN-L', 'BROWN', 'L', 190300, 10, 1903000),
(1125, 2, 'PRD-OUT-CD-002', '케이블 포인트 가디건', 'SKU-PRD-OUT-CD-002-BEI-M', 'BEIGE', 'M', 190300, 10, 1903000),
(1125, 1, 'PRD-OUT-CD-001', '브이넥 버튼 가디건', 'SKU-PRD-OUT-CD-001-BLK-L', 'BLACK', 'L', 189100, 8, 1512800),
-- 1126 (3,736,800) 패션라인 COMPLETED — 3 items
(1126, 8, 'PRD-OUT-JK-002', '미니멀 크롭 블레이저', 'SKU-PRD-OUT-JK-002-GRY-M', 'GREY', 'M', 178300, 12, 2139600),
(1126, 20, 'PRD-SKT-MN-001', 'A라인 코튼 미니 스커트', 'SKU-PRD-SKT-MN-001-BLK-M', 'BLACK', 'M', 129100, 8, 1032800),
(1126, 19, 'PRD-SKT-LG-001', '플리츠 플로우 롱스커트', 'SKU-PRD-SKT-LG-001-BEI-L', 'BEIGE', 'L', 141100, 4, 564400),
-- 1127 (4,307,000) 패션라인 ARRIVED — 3 items
(1127, 4, 'PRD-OUT-HZ-001', '소프트 스웻 후드집업', 'SKU-PRD-OUT-HZ-001-BLK-L', 'BLACK', 'L', 165100, 15, 2476500),
(1127, 5, 'PRD-OUT-HZ-002', '테리 루즈핏 후드집업', 'SKU-PRD-OUT-HZ-002-NVY-M', 'NAVY', 'M', 166300, 10, 1663000),
(1127, 5, 'PRD-OUT-HZ-002', '테리 루즈핏 후드집업', 'SKU-PRD-OUT-HZ-002-GRY-L', 'GREY', 'L', 166300, 1, 166300),
-- 1128 (2,564,000) 슈즈모아 ARRIVED — 3 items
(1128, 22, 'PRD-TOP-LS-001', '슬림 베이스 레이어 긴팔', 'SKU-PRD-TOP-LS-001-NVY-M', 'NAVY', 'M', 33100, 30, 993000),
(1128, 24, 'PRD-TOP-SH-001', '옥스포드 버튼다운 셔츠', 'SKU-PRD-TOP-SH-001-BLK-L', 'BLACK', 'L', 45100, 20, 902000),
(1128, 26, 'PRD-TOP-SS-002', '드라이핏 액티브 반팔', 'SKU-PRD-TOP-SS-002-WHT-S', 'WHITE', 'S', 22300, 30, 669000),
-- 1129 (3,875,500) 패션라인 ARRIVED — 2 items
(1129, 11, 'PRD-OUT-PD-002', '볼륨넥 덕다운 패딩', 'SKU-PRD-OUT-PD-002-BLK-L', 'BLACK', 'L', 154300, 15, 2314500),
(1129, 11, 'PRD-OUT-PD-002', '볼륨넥 덕다운 패딩', 'SKU-PRD-OUT-PD-002-NVY-M', 'NAVY', 'M', 154300, 10, 1543000),
-- 1130 (2,860,000) 패션라인 ARRIVED — 2 items
(1130, 8, 'PRD-OUT-JK-002', '미니멀 크롭 블레이저', 'SKU-PRD-OUT-JK-002-BLK-L', 'BLACK', 'L', 178300, 10, 1783000),
(1130, 8, 'PRD-OUT-JK-002', '미니멀 크롭 블레이저', 'SKU-PRD-OUT-JK-002-GRY-M', 'GREY', 'M', 178300, 6, 1069800);

-- ──────────────────────────────────────────────
-- 5) AUTO_INCREMENT 안전 위치로 이동
-- ──────────────────────────────────────────────
ALTER TABLE vendor             AUTO_INCREMENT = 100;
ALTER TABLE vendor_product     AUTO_INCREMENT = 100;
ALTER TABLE purchase_order     AUTO_INCREMENT = 1200;
ALTER TABLE purchase_order_item AUTO_INCREMENT = 7000;
