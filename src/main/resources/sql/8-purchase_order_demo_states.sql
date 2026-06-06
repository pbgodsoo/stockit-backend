-- 8-purchase_order_demo_states.sql
-- 발표 시연용 — (A) 진행 중 상태 발주 8건 추가 + (B) 시연 발주 진행 이력 생성
--
-- 목적:
--   (A) 기존 10-purchase_order_dummy_data.sql 의 발주는 전부 COMPLETED/ARRIVED(완료 상태)뿐이라
--       강제 배치 [배치 처리] 단계 전환을 시연할 수 없다. 진행 중 상태 발주 8건을 추가한다.
--   (B) 시연 발주(1131~1138)의 상세 [진행 이력] 패널이 비지 않도록, 현재 상태까지의 전 단계 이력을 생성한다.
--       "현재 상태에 도달한 발주는 그 이전 단계를 모두 거쳤다"는 규칙으로 단계 경로를 채운다.
--   기존 10번 발주(1101~1130)는 대상에서 제외 — 본 파일이 추가한 시연 발주만 다룬다.
--
-- 정합성 (CRITICAL):
--   - SKU 코드 형식 = CONCAT(product_code,'-',color,'-',size). color ∈ {BLK,WHT,NVY}, size ∈ {S,M,L} (04 시드 기준).
--     → startInTransit hook(inventoryService.increaseAvailable)이 findBySkuCode 로 SKU 를 조회하므로
--       유효한 sku_code 가 아니면 PRODUCT_SKU_NOT_FOUND 로 단계 전환이 롤백된다.
--   - 발주 헤더 vendor = 품목 product 의 main_vendor 로 일치시킨다 (한 발주 = 한 거래처).
--   - purchase_order_item 은 vendor_product_id 를 하드코딩하지 않고 (product_code + vendor) 로 SELECT 조회 →
--     AUTO_INCREMENT 순서에 무관하게 항상 유효한 FK 를 얻는다.
--
-- 의존 (10-purchase_order 발주 더미는 불필요):
--   - 01-infrastructure_dummy_data.sql       (창고 — purchase_order.warehouse_id)
--   - 03-category_two_level_seed.sql
--   - 04-product_master_dummy_data.sql       (product_master / product_sku)
--   - 7-1-vendor_product_full_seed.sql      (vendor_product 보강)
--   - 7-2-vendor_orderable_all.sql          (vendor 8건 VND-001~008 + vendor_product)
--   → 적용 순서: 01 → 03 → 04 → 10-1 → 10-2 → 12  (10-purchase_order_dummy_data 는 생략 가능)
--
-- 신규 발주 상태 분포 (8건): REQUESTED 2 / APPROVED 2 / READY_TO_SHIP 2 / IN_TRANSIT 2
--   → 강제 배치 1회당 한 단계씩 전진: REQUESTED→APPROVED→READY_TO_SHIP→IN_TRANSIT→ARRIVED
--   id 헤더 1131~1138 / code PO-202605-0001~0008 (기존 1101~1130, ~202604 와 비충돌)
--
-- 발주별 거래처/품목 (모두 main_vendor 일치):
--   1131 REQUESTED     VND-001 테크서플라이   PRD-TOP-SS-001 / PRD-OUT-HZ-001
--   1132 REQUESTED     VND-002 한국생활물산   PRD-TOP-SS-002 / PRD-OUT-HZ-002
--   1133 APPROVED      VND-003 글로벌오피스   PRD-TOP-LS-001 / PRD-SKT-MN-001
--   1134 APPROVED      VND-004 위생물자       PRD-PNT-DN-002 / PRD-OUT-JK-002
--   1135 READY_TO_SHIP VND-005 스마트주방     PRD-TOP-SH-001 / PRD-OUT-CD-001
--   1136 READY_TO_SHIP VND-006 패션라인       PRD-PNT-ST-002 / PRD-OUT-CD-002
--   1137 IN_TRANSIT    VND-007 슈즈모아       PRD-TOP-KN-001 / PRD-OUT-PD-001
--   1138 IN_TRANSIT    VND-008 코스메틱플러스 PRD-TOP-KN-002 / PRD-OUT-PD-002

-- 재실행 안전 — 시연 발주(1131~1138)의 자식(이력·품목) 먼저 삭제 (헤더는 ON DUPLICATE KEY UPDATE 로 멱등).
DELETE FROM purchase_order_status_history WHERE purchase_order_id BETWEEN 1131 AND 1138;
DELETE FROM purchase_order_item WHERE purchase_order_id BETWEEN 1131 AND 1138;

-- ──────────────────────────────────────────────
-- 1) PURCHASE_ORDER 헤더 (시연용 8건, id 1131~1138) — total_amount 는 아래 품목 합과 일치
-- ──────────────────────────────────────────────
INSERT INTO purchase_order
(id, code, vendor_id, vendor_name, vendor_contact_name,
 warehouse_id, warehouse_name, member_id, member_name,
 status, total_amount, cancel_reason, create_date, update_date)
VALUES
-- REQUESTED (승인대기) — 강제 배치 시연 시작점
(1131, 'PO-202605-0001', 1, '(주)테크서플라이', '이공급', 1, '서울 도심 풀필먼트 허브',     'hq0001', '본사관리자', 'REQUESTED',     3577000, NULL, '2026-05-25 10:00:00', NOW()),
(1132, 'PO-202605-0002', 2, '한국생활물산',     '최생활', 2, '서울 동북권 스마트 물류센터', 'hq0001', '본사관리자', 'REQUESTED',     2870000, NULL, '2026-05-25 11:00:00', NOW()),
-- APPROVED (승인)
(1133, 'PO-202605-0003', 3, '글로벌오피스',     '정사무', 3, '경기 남부 통합 배송센터',     'hq0001', '본사관리자', 'APPROVED',      2316500, NULL, '2026-05-24 09:30:00', NOW()),
(1134, 'PO-202605-0004', 4, '위생물자(주)',     '윤위생', 4, '경기 북부 이커머스 물류센터', 'hq0001', '본사관리자', 'APPROVED',      2620500, NULL, '2026-05-24 14:00:00', NOW()),
-- READY_TO_SHIP (출고준비)
(1135, 'PO-202605-0005', 5, '스마트주방솔루션', '오주방', 1, '서울 도심 풀필먼트 허브',     'hq0001', '본사관리자', 'READY_TO_SHIP', 2563000, NULL, '2026-05-23 11:00:00', NOW()),
(1136, 'PO-202605-0006', 6, '패션라인(주)',     '김패션', 5, '인천 송도 국제물류센터',     'hq0001', '본사관리자', 'READY_TO_SHIP', 2009500, NULL, '2026-05-23 15:30:00', NOW()),
-- IN_TRANSIT (배송중)
(1137, 'PO-202605-0007', 7, '슈즈모아',         '박슈즈', 3, '경기 남부 통합 배송센터',     'hq0001', '본사관리자', 'IN_TRANSIT',    3333000, NULL, '2026-05-22 10:20:00', NOW()),
(1138, 'PO-202605-0008', 8, '코스메틱플러스',   '한코스', 4, '경기 북부 이커머스 물류센터', 'hq0001', '본사관리자', 'IN_TRANSIT',    1986000, NULL, '2026-05-22 16:40:00', NOW())
ON DUPLICATE KEY UPDATE
  vendor_id = VALUES(vendor_id),
  vendor_name = VALUES(vendor_name),
  vendor_contact_name = VALUES(vendor_contact_name),
  warehouse_id = VALUES(warehouse_id),
  warehouse_name = VALUES(warehouse_name),
  status = VALUES(status),
  total_amount = VALUES(total_amount),
  cancel_reason = VALUES(cancel_reason),
  update_date = NOW();

-- ──────────────────────────────────────────────
-- 2) PURCHASE_ORDER_ITEM (시연용 16건) — product_code + vendor 로 vendor_product/SKU 를 조회해 생성.
--    vendor_product_id 하드코딩 없음. product_sku JOIN 으로 유효 SKU 만 색인됨.
-- ──────────────────────────────────────────────
INSERT INTO purchase_order_item
(purchase_order_id, vendor_product_id, product_code, product_name, sku_code, color, size, unit_price, quantity, subtotal)
SELECT
  m.po_id,
  vp.id,
  pm.code,
  pm.name,
  ps.sku_code,
  m.color,
  m.size,
  pm.base_price,
  m.qty,
  pm.base_price * m.qty
FROM (
              SELECT 1131 AS po_id, 1 AS vendor_id, 'PRD-TOP-SS-001' AS product_code, 'BLK' AS color, 'M' AS size, 100 AS qty
    UNION ALL SELECT 1131, 1, 'PRD-OUT-HZ-001', 'NVY', 'L',  30
    UNION ALL SELECT 1132, 2, 'PRD-TOP-SS-002', 'WHT', 'M',  80
    UNION ALL SELECT 1132, 2, 'PRD-OUT-HZ-002', 'NVY', 'L',  20
    UNION ALL SELECT 1133, 3, 'PRD-TOP-LS-001', 'BLK', 'M',  60
    UNION ALL SELECT 1133, 3, 'PRD-SKT-MN-001', 'WHT', 'L',  25
    UNION ALL SELECT 1134, 4, 'PRD-PNT-DN-002', 'BLK', 'M',  30
    UNION ALL SELECT 1134, 4, 'PRD-OUT-JK-002', 'NVY', 'L',  15
    UNION ALL SELECT 1135, 5, 'PRD-TOP-SH-001', 'BLK', 'L',  50
    UNION ALL SELECT 1135, 5, 'PRD-OUT-CD-001', 'NVY', 'M',  20
    UNION ALL SELECT 1136, 6, 'PRD-PNT-ST-002', 'WHT', 'M',  40
    UNION ALL SELECT 1136, 6, 'PRD-OUT-CD-002', 'BLK', 'L',  15
    UNION ALL SELECT 1137, 7, 'PRD-TOP-KN-001', 'NVY', 'M',  50
    UNION ALL SELECT 1137, 7, 'PRD-OUT-PD-001', 'BLK', 'L',  20
    UNION ALL SELECT 1138, 8, 'PRD-TOP-KN-002', 'WHT', 'M',  30
    UNION ALL SELECT 1138, 8, 'PRD-OUT-PD-002', 'NVY', 'S',  10
) m
JOIN product_master pm ON pm.code = m.product_code
JOIN vendor_product  vp ON vp.product_code = m.product_code AND vp.vendor_id = m.vendor_id
JOIN product_sku     ps ON ps.sku_code = CONCAT(pm.code, '-', m.color, '-', m.size);

-- ──────────────────────────────────────────────
-- 3) PURCHASE_ORDER_STATUS_HISTORY — 시연 발주(1131~1138) 진행 이력 생성
--    "현재 상태에 도달한 발주는 이전 단계를 모두 거쳤다" — 단계별 INSERT...SELECT 로 경로 생성.
--    changed_by_name 규칙은 서비스(PurchaseOrderService.appendHistory)와 동일:
--      REQUESTED / CANCELLED = 본사 담당자 / APPROVED~ARRIVED = 거래처담당자(거래처명) / COMPLETED = 창고 입고확정자(창고명)
--    changed_at 은 create_date 기준 단계별 증가 — 상세 화면 정렬 시 단계 순서 보장 (상대 시각).
-- ──────────────────────────────────────────────

-- (1) REQUESTED — 모든 발주가 거침
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id, 'REQUESTED', create_date, '본사관리자 (본사)', NULL
FROM purchase_order WHERE id BETWEEN 1131 AND 1138;

-- (2) APPROVED — APPROVED 이상 도달한 발주
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id, 'APPROVED', DATE_ADD(create_date, INTERVAL 1 HOUR),
       CONCAT(vendor_contact_name, ' (', vendor_name, ')'), NULL
FROM purchase_order
WHERE id BETWEEN 1131 AND 1138
  AND status IN ('APPROVED', 'READY_TO_SHIP', 'IN_TRANSIT', 'ARRIVED', 'COMPLETED');

-- (3) READY_TO_SHIP
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id, 'READY_TO_SHIP', DATE_ADD(create_date, INTERVAL 2 HOUR),
       CONCAT(vendor_contact_name, ' (', vendor_name, ')'), NULL
FROM purchase_order
WHERE id BETWEEN 1131 AND 1138
  AND status IN ('READY_TO_SHIP', 'IN_TRANSIT', 'ARRIVED', 'COMPLETED');

-- (4) IN_TRANSIT
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id, 'IN_TRANSIT', DATE_ADD(create_date, INTERVAL 3 HOUR),
       CONCAT(vendor_contact_name, ' (', vendor_name, ')'), NULL
FROM purchase_order
WHERE id BETWEEN 1131 AND 1138
  AND status IN ('IN_TRANSIT', 'ARRIVED', 'COMPLETED');

-- (5) ARRIVED — 배송 도착(익일)
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id, 'ARRIVED', DATE_ADD(create_date, INTERVAL 1 DAY),
       CONCAT(vendor_contact_name, ' (', vendor_name, ')'), NULL
FROM purchase_order
WHERE id BETWEEN 1131 AND 1138
  AND status IN ('ARRIVED', 'COMPLETED');

-- ──────────────────────────────────────────────
-- (C) 입고관리 시연용 — WH-SL-0001 / WH-SL-0002 두 창고에 공급처 발주 입고 데이터 (창고당 20건)
--     PO(1141~1180) + 품목 + 이력 + wh_inbound_header + wh_inbound_item. 멱등(재실행 안전).
--     창고/거래처 id·이름은 DB 조회로 derive.
-- ──────────────────────────────────────────────
DELETE wi FROM wh_inbound_item wi JOIN wh_inbound_header wh ON wi.inbound_header_id=wh.id WHERE wh.source_ref_id BETWEEN 1141 AND 1180;
DELETE FROM wh_inbound_header WHERE source_ref_id BETWEEN 1141 AND 1180;
DELETE FROM purchase_order_status_history WHERE purchase_order_id BETWEEN 1141 AND 1180;
DELETE FROM purchase_order_item WHERE purchase_order_id BETWEEN 1141 AND 1180;

-- 1) 발주 헤더
INSERT INTO purchase_order
(id, code, vendor_id, vendor_name, vendor_contact_name, warehouse_id, warehouse_name, member_id, member_name, status, total_amount, cancel_reason, create_date, update_date)
SELECT m.id, m.code, m.vendor_id, v.name, '거래처담당', w.id, w.name, 'hq0001', '본사관리자', m.status, 0, NULL, m.cdate, NOW()
FROM (
            SELECT 1141 AS id, 'PO-202605-1141' AS code, 1 AS vendor_id, 'WH-SL-0001' AS wcode, 'ARRIVED' AS status, '2026-05-02 09:00:00' AS cdate
  UNION ALL SELECT 1142 AS id, 'PO-202605-1142' AS code, 2 AS vendor_id, 'WH-SL-0001' AS wcode, 'IN_TRANSIT' AS status, '2026-05-03 09:00:00' AS cdate
  UNION ALL SELECT 1143 AS id, 'PO-202605-1143' AS code, 3 AS vendor_id, 'WH-SL-0001' AS wcode, 'READY_TO_SHIP' AS status, '2026-05-04 09:00:00' AS cdate
  UNION ALL SELECT 1144 AS id, 'PO-202605-1144' AS code, 4 AS vendor_id, 'WH-SL-0001' AS wcode, 'ARRIVED' AS status, '2026-05-05 09:00:00' AS cdate
  UNION ALL SELECT 1145 AS id, 'PO-202605-1145' AS code, 5 AS vendor_id, 'WH-SL-0001' AS wcode, 'IN_TRANSIT' AS status, '2026-05-06 09:00:00' AS cdate
  UNION ALL SELECT 1146 AS id, 'PO-202605-1146' AS code, 6 AS vendor_id, 'WH-SL-0001' AS wcode, 'COMPLETED' AS status, '2026-05-07 09:00:00' AS cdate
  UNION ALL SELECT 1147 AS id, 'PO-202605-1147' AS code, 7 AS vendor_id, 'WH-SL-0001' AS wcode, 'ARRIVED' AS status, '2026-05-08 09:00:00' AS cdate
  UNION ALL SELECT 1148 AS id, 'PO-202605-1148' AS code, 8 AS vendor_id, 'WH-SL-0001' AS wcode, 'READY_TO_SHIP' AS status, '2026-05-09 09:00:00' AS cdate
  UNION ALL SELECT 1149 AS id, 'PO-202605-1149' AS code, 1 AS vendor_id, 'WH-SL-0001' AS wcode, 'IN_TRANSIT' AS status, '2026-05-10 09:00:00' AS cdate
  UNION ALL SELECT 1150 AS id, 'PO-202605-1150' AS code, 2 AS vendor_id, 'WH-SL-0001' AS wcode, 'ARRIVED' AS status, '2026-05-11 09:00:00' AS cdate
  UNION ALL SELECT 1151 AS id, 'PO-202605-1151' AS code, 3 AS vendor_id, 'WH-SL-0001' AS wcode, 'ARRIVED' AS status, '2026-05-12 09:00:00' AS cdate
  UNION ALL SELECT 1152 AS id, 'PO-202605-1152' AS code, 4 AS vendor_id, 'WH-SL-0001' AS wcode, 'IN_TRANSIT' AS status, '2026-05-13 09:00:00' AS cdate
  UNION ALL SELECT 1153 AS id, 'PO-202605-1153' AS code, 5 AS vendor_id, 'WH-SL-0001' AS wcode, 'READY_TO_SHIP' AS status, '2026-05-14 09:00:00' AS cdate
  UNION ALL SELECT 1154 AS id, 'PO-202605-1154' AS code, 6 AS vendor_id, 'WH-SL-0001' AS wcode, 'ARRIVED' AS status, '2026-05-15 09:00:00' AS cdate
  UNION ALL SELECT 1155 AS id, 'PO-202605-1155' AS code, 7 AS vendor_id, 'WH-SL-0001' AS wcode, 'IN_TRANSIT' AS status, '2026-05-16 09:00:00' AS cdate
  UNION ALL SELECT 1156 AS id, 'PO-202605-1156' AS code, 8 AS vendor_id, 'WH-SL-0001' AS wcode, 'COMPLETED' AS status, '2026-05-17 09:00:00' AS cdate
  UNION ALL SELECT 1157 AS id, 'PO-202605-1157' AS code, 1 AS vendor_id, 'WH-SL-0001' AS wcode, 'ARRIVED' AS status, '2026-05-18 09:00:00' AS cdate
  UNION ALL SELECT 1158 AS id, 'PO-202605-1158' AS code, 2 AS vendor_id, 'WH-SL-0001' AS wcode, 'READY_TO_SHIP' AS status, '2026-05-19 09:00:00' AS cdate
  UNION ALL SELECT 1159 AS id, 'PO-202605-1159' AS code, 3 AS vendor_id, 'WH-SL-0001' AS wcode, 'IN_TRANSIT' AS status, '2026-05-20 09:00:00' AS cdate
  UNION ALL SELECT 1160 AS id, 'PO-202605-1160' AS code, 4 AS vendor_id, 'WH-SL-0001' AS wcode, 'ARRIVED' AS status, '2026-05-21 09:00:00' AS cdate
  UNION ALL SELECT 1161 AS id, 'PO-202605-1161' AS code, 5 AS vendor_id, 'WH-SL-0002' AS wcode, 'ARRIVED' AS status, '2026-05-22 09:00:00' AS cdate
  UNION ALL SELECT 1162 AS id, 'PO-202605-1162' AS code, 6 AS vendor_id, 'WH-SL-0002' AS wcode, 'IN_TRANSIT' AS status, '2026-05-23 09:00:00' AS cdate
  UNION ALL SELECT 1163 AS id, 'PO-202605-1163' AS code, 7 AS vendor_id, 'WH-SL-0002' AS wcode, 'READY_TO_SHIP' AS status, '2026-05-24 09:00:00' AS cdate
  UNION ALL SELECT 1164 AS id, 'PO-202605-1164' AS code, 8 AS vendor_id, 'WH-SL-0002' AS wcode, 'ARRIVED' AS status, '2026-05-25 09:00:00' AS cdate
  UNION ALL SELECT 1165 AS id, 'PO-202605-1165' AS code, 1 AS vendor_id, 'WH-SL-0002' AS wcode, 'IN_TRANSIT' AS status, '2026-05-26 09:00:00' AS cdate
  UNION ALL SELECT 1166 AS id, 'PO-202605-1166' AS code, 2 AS vendor_id, 'WH-SL-0002' AS wcode, 'COMPLETED' AS status, '2026-05-01 09:00:00' AS cdate
  UNION ALL SELECT 1167 AS id, 'PO-202605-1167' AS code, 3 AS vendor_id, 'WH-SL-0002' AS wcode, 'ARRIVED' AS status, '2026-05-02 09:00:00' AS cdate
  UNION ALL SELECT 1168 AS id, 'PO-202605-1168' AS code, 4 AS vendor_id, 'WH-SL-0002' AS wcode, 'READY_TO_SHIP' AS status, '2026-05-03 09:00:00' AS cdate
  UNION ALL SELECT 1169 AS id, 'PO-202605-1169' AS code, 5 AS vendor_id, 'WH-SL-0002' AS wcode, 'IN_TRANSIT' AS status, '2026-05-04 09:00:00' AS cdate
  UNION ALL SELECT 1170 AS id, 'PO-202605-1170' AS code, 6 AS vendor_id, 'WH-SL-0002' AS wcode, 'ARRIVED' AS status, '2026-05-05 09:00:00' AS cdate
  UNION ALL SELECT 1171 AS id, 'PO-202605-1171' AS code, 7 AS vendor_id, 'WH-SL-0002' AS wcode, 'ARRIVED' AS status, '2026-05-06 09:00:00' AS cdate
  UNION ALL SELECT 1172 AS id, 'PO-202605-1172' AS code, 8 AS vendor_id, 'WH-SL-0002' AS wcode, 'IN_TRANSIT' AS status, '2026-05-07 09:00:00' AS cdate
  UNION ALL SELECT 1173 AS id, 'PO-202605-1173' AS code, 1 AS vendor_id, 'WH-SL-0002' AS wcode, 'READY_TO_SHIP' AS status, '2026-05-08 09:00:00' AS cdate
  UNION ALL SELECT 1174 AS id, 'PO-202605-1174' AS code, 2 AS vendor_id, 'WH-SL-0002' AS wcode, 'ARRIVED' AS status, '2026-05-09 09:00:00' AS cdate
  UNION ALL SELECT 1175 AS id, 'PO-202605-1175' AS code, 3 AS vendor_id, 'WH-SL-0002' AS wcode, 'IN_TRANSIT' AS status, '2026-05-10 09:00:00' AS cdate
  UNION ALL SELECT 1176 AS id, 'PO-202605-1176' AS code, 4 AS vendor_id, 'WH-SL-0002' AS wcode, 'COMPLETED' AS status, '2026-05-11 09:00:00' AS cdate
  UNION ALL SELECT 1177 AS id, 'PO-202605-1177' AS code, 5 AS vendor_id, 'WH-SL-0002' AS wcode, 'ARRIVED' AS status, '2026-05-12 09:00:00' AS cdate
  UNION ALL SELECT 1178 AS id, 'PO-202605-1178' AS code, 6 AS vendor_id, 'WH-SL-0002' AS wcode, 'READY_TO_SHIP' AS status, '2026-05-13 09:00:00' AS cdate
  UNION ALL SELECT 1179 AS id, 'PO-202605-1179' AS code, 7 AS vendor_id, 'WH-SL-0002' AS wcode, 'IN_TRANSIT' AS status, '2026-05-14 09:00:00' AS cdate
  UNION ALL SELECT 1180 AS id, 'PO-202605-1180' AS code, 8 AS vendor_id, 'WH-SL-0002' AS wcode, 'ARRIVED' AS status, '2026-05-15 09:00:00' AS cdate
) m
JOIN vendor v ON v.id = m.vendor_id
JOIN infrastructure w ON w.code = m.wcode
ON DUPLICATE KEY UPDATE warehouse_id=VALUES(warehouse_id), warehouse_name=VALUES(warehouse_name), vendor_id=VALUES(vendor_id), vendor_name=VALUES(vendor_name), status=VALUES(status), update_date=NOW();

-- 2) 발주 품목
INSERT INTO purchase_order_item
(purchase_order_id, vendor_product_id, product_code, product_name, sku_code, color, size, unit_price, quantity, subtotal)
SELECT m.po_id, vp.id, pm.code, pm.name, ps.sku_code, m.color, m.size, pm.base_price, m.qty, pm.base_price*m.qty
FROM (
            SELECT 1141 AS po_id, 1 AS vendor_id, 'PRD-TOP-SS-001' AS product_code, 'BLK' AS color, 'M' AS size, 40 AS qty
  UNION ALL SELECT 1141, 1, 'PRD-OUT-HZ-001', 'NVY', 'L', 20
  UNION ALL SELECT 1142, 2, 'PRD-TOP-SS-002', 'WHT', 'M', 50
  UNION ALL SELECT 1142, 2, 'PRD-OUT-HZ-002', 'NVY', 'L', 25
  UNION ALL SELECT 1143, 3, 'PRD-TOP-LS-001', 'BLK', 'M', 35
  UNION ALL SELECT 1143, 3, 'PRD-SKT-MN-001', 'WHT', 'L', 15
  UNION ALL SELECT 1144, 4, 'PRD-PNT-DN-002', 'BLK', 'M', 30
  UNION ALL SELECT 1144, 4, 'PRD-OUT-JK-002', 'NVY', 'L', 15
  UNION ALL SELECT 1145, 5, 'PRD-TOP-SH-001', 'BLK', 'L', 30
  UNION ALL SELECT 1145, 5, 'PRD-OUT-CD-001', 'NVY', 'M', 18
  UNION ALL SELECT 1146, 6, 'PRD-PNT-ST-002', 'WHT', 'M', 40
  UNION ALL SELECT 1146, 6, 'PRD-OUT-CD-002', 'BLK', 'L', 15
  UNION ALL SELECT 1147, 7, 'PRD-TOP-KN-001', 'NVY', 'M', 50
  UNION ALL SELECT 1147, 7, 'PRD-OUT-PD-001', 'BLK', 'L', 20
  UNION ALL SELECT 1148, 8, 'PRD-TOP-KN-002', 'WHT', 'M', 30
  UNION ALL SELECT 1148, 8, 'PRD-OUT-PD-002', 'NVY', 'S', 10
  UNION ALL SELECT 1149, 1, 'PRD-TOP-SS-001', 'BLK', 'M', 40
  UNION ALL SELECT 1149, 1, 'PRD-OUT-HZ-001', 'NVY', 'L', 20
  UNION ALL SELECT 1150, 2, 'PRD-TOP-SS-002', 'WHT', 'M', 50
  UNION ALL SELECT 1150, 2, 'PRD-OUT-HZ-002', 'NVY', 'L', 25
  UNION ALL SELECT 1151, 3, 'PRD-TOP-LS-001', 'BLK', 'M', 35
  UNION ALL SELECT 1151, 3, 'PRD-SKT-MN-001', 'WHT', 'L', 15
  UNION ALL SELECT 1152, 4, 'PRD-PNT-DN-002', 'BLK', 'M', 30
  UNION ALL SELECT 1152, 4, 'PRD-OUT-JK-002', 'NVY', 'L', 15
  UNION ALL SELECT 1153, 5, 'PRD-TOP-SH-001', 'BLK', 'L', 30
  UNION ALL SELECT 1153, 5, 'PRD-OUT-CD-001', 'NVY', 'M', 18
  UNION ALL SELECT 1154, 6, 'PRD-PNT-ST-002', 'WHT', 'M', 40
  UNION ALL SELECT 1154, 6, 'PRD-OUT-CD-002', 'BLK', 'L', 15
  UNION ALL SELECT 1155, 7, 'PRD-TOP-KN-001', 'NVY', 'M', 50
  UNION ALL SELECT 1155, 7, 'PRD-OUT-PD-001', 'BLK', 'L', 20
  UNION ALL SELECT 1156, 8, 'PRD-TOP-KN-002', 'WHT', 'M', 30
  UNION ALL SELECT 1156, 8, 'PRD-OUT-PD-002', 'NVY', 'S', 10
  UNION ALL SELECT 1157, 1, 'PRD-TOP-SS-001', 'BLK', 'M', 40
  UNION ALL SELECT 1157, 1, 'PRD-OUT-HZ-001', 'NVY', 'L', 20
  UNION ALL SELECT 1158, 2, 'PRD-TOP-SS-002', 'WHT', 'M', 50
  UNION ALL SELECT 1158, 2, 'PRD-OUT-HZ-002', 'NVY', 'L', 25
  UNION ALL SELECT 1159, 3, 'PRD-TOP-LS-001', 'BLK', 'M', 35
  UNION ALL SELECT 1159, 3, 'PRD-SKT-MN-001', 'WHT', 'L', 15
  UNION ALL SELECT 1160, 4, 'PRD-PNT-DN-002', 'BLK', 'M', 30
  UNION ALL SELECT 1160, 4, 'PRD-OUT-JK-002', 'NVY', 'L', 15
  UNION ALL SELECT 1161, 5, 'PRD-TOP-SH-001', 'BLK', 'L', 30
  UNION ALL SELECT 1161, 5, 'PRD-OUT-CD-001', 'NVY', 'M', 18
  UNION ALL SELECT 1162, 6, 'PRD-PNT-ST-002', 'WHT', 'M', 40
  UNION ALL SELECT 1162, 6, 'PRD-OUT-CD-002', 'BLK', 'L', 15
  UNION ALL SELECT 1163, 7, 'PRD-TOP-KN-001', 'NVY', 'M', 50
  UNION ALL SELECT 1163, 7, 'PRD-OUT-PD-001', 'BLK', 'L', 20
  UNION ALL SELECT 1164, 8, 'PRD-TOP-KN-002', 'WHT', 'M', 30
  UNION ALL SELECT 1164, 8, 'PRD-OUT-PD-002', 'NVY', 'S', 10
  UNION ALL SELECT 1165, 1, 'PRD-TOP-SS-001', 'BLK', 'M', 40
  UNION ALL SELECT 1165, 1, 'PRD-OUT-HZ-001', 'NVY', 'L', 20
  UNION ALL SELECT 1166, 2, 'PRD-TOP-SS-002', 'WHT', 'M', 50
  UNION ALL SELECT 1166, 2, 'PRD-OUT-HZ-002', 'NVY', 'L', 25
  UNION ALL SELECT 1167, 3, 'PRD-TOP-LS-001', 'BLK', 'M', 35
  UNION ALL SELECT 1167, 3, 'PRD-SKT-MN-001', 'WHT', 'L', 15
  UNION ALL SELECT 1168, 4, 'PRD-PNT-DN-002', 'BLK', 'M', 30
  UNION ALL SELECT 1168, 4, 'PRD-OUT-JK-002', 'NVY', 'L', 15
  UNION ALL SELECT 1169, 5, 'PRD-TOP-SH-001', 'BLK', 'L', 30
  UNION ALL SELECT 1169, 5, 'PRD-OUT-CD-001', 'NVY', 'M', 18
  UNION ALL SELECT 1170, 6, 'PRD-PNT-ST-002', 'WHT', 'M', 40
  UNION ALL SELECT 1170, 6, 'PRD-OUT-CD-002', 'BLK', 'L', 15
  UNION ALL SELECT 1171, 7, 'PRD-TOP-KN-001', 'NVY', 'M', 50
  UNION ALL SELECT 1171, 7, 'PRD-OUT-PD-001', 'BLK', 'L', 20
  UNION ALL SELECT 1172, 8, 'PRD-TOP-KN-002', 'WHT', 'M', 30
  UNION ALL SELECT 1172, 8, 'PRD-OUT-PD-002', 'NVY', 'S', 10
  UNION ALL SELECT 1173, 1, 'PRD-TOP-SS-001', 'BLK', 'M', 40
  UNION ALL SELECT 1173, 1, 'PRD-OUT-HZ-001', 'NVY', 'L', 20
  UNION ALL SELECT 1174, 2, 'PRD-TOP-SS-002', 'WHT', 'M', 50
  UNION ALL SELECT 1174, 2, 'PRD-OUT-HZ-002', 'NVY', 'L', 25
  UNION ALL SELECT 1175, 3, 'PRD-TOP-LS-001', 'BLK', 'M', 35
  UNION ALL SELECT 1175, 3, 'PRD-SKT-MN-001', 'WHT', 'L', 15
  UNION ALL SELECT 1176, 4, 'PRD-PNT-DN-002', 'BLK', 'M', 30
  UNION ALL SELECT 1176, 4, 'PRD-OUT-JK-002', 'NVY', 'L', 15
  UNION ALL SELECT 1177, 5, 'PRD-TOP-SH-001', 'BLK', 'L', 30
  UNION ALL SELECT 1177, 5, 'PRD-OUT-CD-001', 'NVY', 'M', 18
  UNION ALL SELECT 1178, 6, 'PRD-PNT-ST-002', 'WHT', 'M', 40
  UNION ALL SELECT 1178, 6, 'PRD-OUT-CD-002', 'BLK', 'L', 15
  UNION ALL SELECT 1179, 7, 'PRD-TOP-KN-001', 'NVY', 'M', 50
  UNION ALL SELECT 1179, 7, 'PRD-OUT-PD-001', 'BLK', 'L', 20
  UNION ALL SELECT 1180, 8, 'PRD-TOP-KN-002', 'WHT', 'M', 30
  UNION ALL SELECT 1180, 8, 'PRD-OUT-PD-002', 'NVY', 'S', 10
) m
JOIN product_master pm ON pm.code = m.product_code
JOIN vendor_product  vp ON vp.product_code = m.product_code AND vp.vendor_id = m.vendor_id
JOIN product_sku     ps ON ps.sku_code = CONCAT(pm.code,'-',m.color,'-',m.size);

UPDATE purchase_order p SET total_amount=(SELECT COALESCE(SUM(subtotal),0) FROM purchase_order_item WHERE purchase_order_id=p.id) WHERE p.id BETWEEN 1141 AND 1180;

INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id,'REQUESTED',create_date,'본사관리자 (본사)',NULL FROM purchase_order WHERE id BETWEEN 1141 AND 1180;
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id,'APPROVED',DATE_ADD(create_date,INTERVAL 1 HOUR),CONCAT(vendor_contact_name,' (',vendor_name,')'),NULL FROM purchase_order WHERE id BETWEEN 1141 AND 1180 AND status IN ('APPROVED','READY_TO_SHIP','IN_TRANSIT','ARRIVED','COMPLETED');
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id,'READY_TO_SHIP',DATE_ADD(create_date,INTERVAL 2 HOUR),CONCAT(vendor_contact_name,' (',vendor_name,')'),NULL FROM purchase_order WHERE id BETWEEN 1141 AND 1180 AND status IN ('READY_TO_SHIP','IN_TRANSIT','ARRIVED','COMPLETED');
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id,'IN_TRANSIT',DATE_ADD(create_date,INTERVAL 3 HOUR),CONCAT(vendor_contact_name,' (',vendor_name,')'),NULL FROM purchase_order WHERE id BETWEEN 1141 AND 1180 AND status IN ('IN_TRANSIT','ARRIVED','COMPLETED');
INSERT INTO purchase_order_status_history (purchase_order_id, status, changed_at, changed_by_name, note)
SELECT id,'ARRIVED',DATE_ADD(create_date,INTERVAL 1 DAY),CONCAT(vendor_contact_name,' (',vendor_name,')'),NULL FROM purchase_order WHERE id BETWEEN 1141 AND 1180 AND status IN ('ARRIVED','COMPLETED');

INSERT INTO wh_inbound_header
(inbound_code, inbound_type, source_ref_no, source_ref_id, warehouse_id, warehouse_name, source_name, total_quantity, total_amount, completed_at, create_date, update_date)
SELECT CONCAT('WIB-DEMO-',p.id),'PURCHASE_ORDER',p.code,p.id,p.warehouse_id,p.warehouse_name,p.vendor_name,
  (SELECT COALESCE(SUM(quantity),0) FROM purchase_order_item WHERE purchase_order_id=p.id), p.total_amount,
  CASE WHEN p.status='COMPLETED' THEN DATE_ADD(p.create_date,INTERVAL 2 DAY) ELSE NULL END,
  p.create_date, NOW()
FROM purchase_order p WHERE p.id BETWEEN 1141 AND 1180;

INSERT INTO wh_inbound_item
(inbound_header_id, product_code, product_name, sku_code, color, size, quantity, unit_price, subtotal, create_date, update_date)
SELECT h.id,i.product_code,i.product_name,i.sku_code,i.color,i.size,i.quantity,i.unit_price,i.subtotal,NOW(),NOW()
FROM wh_inbound_header h JOIN purchase_order_item i ON i.purchase_order_id=h.source_ref_id
WHERE h.source_ref_id BETWEEN 1141 AND 1180;
