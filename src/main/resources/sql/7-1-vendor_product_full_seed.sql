-- ============================================================
-- 7-1-vendor_product_full_seed.sql
-- 04-product_master_dummy_data.sql 의 30건 product 와
-- 10-purchase_order_dummy_data.sql 의 8개 vendor 를 자동 매핑.
--
-- 기존 10 시드는 vendor_product 21건만 INSERT (발주 더미용 일부 매핑).
-- 새 발주 페이지 카탈로그가 ACTIVE vendor_product 전체를 펼치므로
-- product_master.main_vendor_code 기준으로 누락된 vendor_product 도 채워준다.
--
-- 의존:
--   - 03-category_two_level_seed.sql
--   - 04-product_master_dummy_data.sql  (product_master.main_vendor_code = 'VND-001'~'VND-008')
--   - 10-purchase_order_dummy_data.sql  (vendor 8건 + vendor_product 21건)
--
-- 적용 후 vendor_product 총 30건 (10 시드 21건은 ON DUPLICATE KEY UPDATE 로 동기화).
-- 카탈로그 페이지 진입 시 ACTIVE product 30건 × 그 SKU 들이 노출된다.
--
-- 멱등: ON DUPLICATE KEY UPDATE (code UNIQUE) — 반복 import 안전.
-- ============================================================

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
