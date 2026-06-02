-- ============================================================
-- 7-2-vendor_orderable_all.sql
-- 새 발주 카탈로그가 모든 ACTIVE product 를 노출하도록 vendor + vendor_product 일괄 시드.
--
-- 카탈로그 SQL (PurchaseOrderCatalogRepository.findCatalogPage) 가
--   FROM product_sku ps
--   JOIN product_master pm
--   JOIN vendor_product vp
--   JOIN vendor v
-- 내부 JOIN 이라 vendor / vendor_product 가 비면 무조건 0행이 떨어진다.
-- 10-purchase_order_dummy_data.sql + 7-1-vendor_product_full_seed.sql 의
-- vendor / vendor_product 만 통합한 단독 시드 (PO 더미 헤더/라인은 제외 — 발주 가능 상태만 만든다).
--
-- 의존:
--   - 04-product_master_dummy_data.sql  (product_master 30건 + main_vendor_code = 'VND-001'~'VND-008')
--
-- 멱등: ON DUPLICATE KEY UPDATE (vendor.code / vendor_product.code UNIQUE) — 반복 import 안전.
-- ============================================================

-- 1) VENDOR 마스터 (8건, id 1~8)
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
  status = VALUES(status),
  update_date = NOW();

-- 2) VENDOR_PRODUCT — ACTIVE product 전체에 대해 main_vendor_code 기준 자동 매핑.
INSERT INTO vendor_product
(code, vendor_id, product_code, product_name, unit_price, moq, lead_time_days,
 contract_start, contract_end, status, create_date, update_date)
SELECT
  CONCAT('VP-', SUBSTRING(pm.code, 5), '-V', LPAD(SUBSTRING(pm.main_vendor_code, 5), 2, '0')) AS code,
  v.id                                                                                       AS vendor_id,
  pm.code                                                                                    AS product_code,
  pm.name                                                                                    AS product_name,
  pm.base_price                                                                              AS unit_price,
  pm.warehouse_safety_stock                                                                  AS moq,
  pm.lead_time_days                                                                          AS lead_time_days,
  '2025-01-01'                                                                               AS contract_start,
  '2026-12-31'                                                                               AS contract_end,
  'ACTIVE'                                                                                   AS status,
  NOW()                                                                                      AS create_date,
  NOW()                                                                                      AS update_date
FROM product_master pm
JOIN vendor v ON v.code = pm.main_vendor_code
WHERE pm.status = 'ACTIVE'
ON DUPLICATE KEY UPDATE
  vendor_id    = VALUES(vendor_id),
  product_code = VALUES(product_code),
  product_name = VALUES(product_name),
  unit_price   = VALUES(unit_price),
  status       = VALUES(status),
  update_date  = NOW();
