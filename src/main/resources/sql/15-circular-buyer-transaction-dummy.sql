-- 15-circular-buyer-transaction-dummy.sql
-- ESG 점수 원천 데이터 (circular_buyer_transaction)
-- 14-circular-sales-dummy.sql 실행 후 실행해야 함
-- (실제 서비스: 판매 ARRIVED 전환 시 CircularSaleService.createTransactionsFromSale() 자동 INSERT)
--
-- ▣ ESG 핵심 정합성:
--   transacted_at = sold_at  ← ScoreEventsService.computeScoreForSaleHeader()가
--                               header.getSoldAt()으로 t.transacted_at = :transactedAt 조회
--                               completed_at 사용 시 매칭 실패 → 전부 0점!
--
-- ▣ material_code 정합성:
--   BE createTransactionsFromSale()은 product.getMaterialCompositions() → 실제 영문 코드 사용
--   → circular_sale_item_material.material_code JOIN으로 실제 코드 조회
--   → circular_sale_header.material_type(한글 그룹명)과 다름! 주의
--
-- ▣ 컬럼 매핑 (CircularBuyerTransaction 엔티티 기준):
--   buyer_id      = circular_sale_header.buyer_id (SALE), NULL (DONATION)
--   material_code = circular_sale_item_material.material_code  ← 실제 소재 코드
--   weight_kg     = ROUND(circular_sale_item.actual_weight_kg)  ← Integer
--   unit_price    = circular_sale_item.unit_price  ← kg당 단가 (Integer)
--   total_amount  = circular_sale_item.line_amount
--   transacted_at = circular_sale_header.sold_at  ← ★ ESG 조회 기준 (sold_at 일치 필수)
--   create_date   = circular_sale_header.sold_at  (NOT BaseEntity, 직접 관리)
--   sale_type     = circular_sale_header.sale_type
--   donee_name    = circular_sale_header.donee_name (DONATION만)
--
-- ARRIVED SALE:     24건 (CSR-00001~00024, 00026~00028)
-- ARRIVED DONATION: 6건  (DON-00031~00036)
-- 합계: 30건 (트랜잭션 30건 = 각 헤더 1건씩)

SET SQL_SAFE_UPDATES = 0;
DELETE FROM circular_buyer_transaction
WHERE transacted_at >= '2026-01-01' AND transacted_at < '2027-01-01';
SET SQL_SAFE_UPDATES = 1;

INSERT INTO circular_buyer_transaction
(buyer_id, material_code, weight_kg, unit_price, total_amount,
 transacted_at, create_date, sale_type, donee_name)
SELECT
  h.buyer_id,
  cim.material_code,                                   -- 실제 소재 영문 코드 (CASHMERE, POLYESTER 등)
  CAST(ROUND(ci.actual_weight_kg) AS UNSIGNED),        -- Integer 반올림
  CAST(ci.unit_price AS UNSIGNED),                     -- Integer (price_per_kg)
  ci.line_amount,                                      -- Long (total_amount)
  h.sold_at                AS transacted_at,           -- ★ ESG 조회 기준: sold_at과 반드시 일치
  h.sold_at                AS create_date,
  h.sale_type,
  h.donee_name
FROM circular_sale_header h
JOIN circular_sale_item ci
  ON ci.sale_header_id = h.id
JOIN circular_sale_item_material cim
  ON cim.sale_item_id = ci.id
  AND cim.sort_order = 1                               -- 대표 소재 1개 (1:1 구조이므로 항상 sort_order=1)
WHERE h.status = 'ARRIVED'
  AND (h.sale_no LIKE 'CSR-2026%' OR h.sale_no LIKE 'DON-2026%')
ORDER BY h.sold_at, h.id;
