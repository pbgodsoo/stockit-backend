-- ============================================================
-- 11-circular_buyer_transaction_seed.sql
-- 순환재고 거래 이력 시드 — 600건 (300 buyer × 2 거래)
--
-- 분포 규칙:
--   - buyer.primary_material_fit 와 매칭되는 material 만 거래
--     · synthetic (100명)      → POLYESTER/ACRYLIC/POLYAMIDE/ELASTANE 중 buyer id 기반 deterministic
--     · blended (100명)        → BLEND 고정
--     · natural-single (100명) → COTTON/WOOL/CASHMERE/SILK/LINEN 중 선택
--   - 단가는 circular_material_price_policy.price_per_kg 정책 단가 (거래 시점 스냅샷)
--   - weight_kg: 200~5000 kg deterministic 분포
--   - transacted_at: 2026-01-01 ~ 2026-05-10 분포 (약 130일)
--
-- 실행 전제:
--   - circular_buyer 시드 (08) 적용됨 (300건)
--   - circular_material_price_policy 시드 (06) 적용됨 (10건)
--   - circular_buyer_transaction 테이블 생성됨 (BE 재기동 시 Hibernate 가 자동 생성)
-- ============================================================

INSERT INTO circular_buyer_transaction
(buyer_id, material_code, weight_kg, unit_price, total_amount, transacted_at, create_date)
SELECT
    src.buyer_id,
    src.material_code,
    src.weight_kg,
    pp.price_per_kg AS unit_price,
    src.weight_kg * pp.price_per_kg AS total_amount,
    src.transacted_at,
    NOW() AS create_date
FROM (
         -- 거래 1 (큰 거래, 1~5월 분포)
         SELECT
             cb.id AS buyer_id,
             CASE cb.primary_material_fit
                 WHEN 'synthetic'      THEN ELT(((cb.id - 1) MOD 4) + 1, 'POLYESTER', 'ACRYLIC', 'POLYAMIDE', 'ELASTANE')
                 WHEN 'blended'        THEN 'BLEND'
                 WHEN 'natural-single' THEN ELT(((cb.id - 1) MOD 5) + 1, 'COTTON', 'WOOL', 'CASHMERE', 'SILK', 'LINEN')
                 ELSE 'COTTON'
                 END AS material_code,
             500 + ((cb.id * 17) MOD 4500) AS weight_kg,
     DATE_ADD('2026-01-01', INTERVAL ((cb.id * 7) MOD 130) DAY) AS transacted_at
    FROM circular_buyer cb

UNION ALL

-- 거래 2 (다른 소재 회차, 다른 시점)
SELECT
    cb.id,
    CASE cb.primary_material_fit
        WHEN 'synthetic'      THEN ELT((cb.id MOD 4) + 1, 'POLYESTER', 'ACRYLIC', 'POLYAMIDE', 'ELASTANE')
        WHEN 'blended'        THEN 'BLEND'
        WHEN 'natural-single' THEN ELT((cb.id MOD 5) + 1, 'COTTON', 'WOOL', 'CASHMERE', 'SILK', 'LINEN')
        ELSE 'COTTON'
        END,
    300 + ((cb.id * 23) MOD 3000),
    DATE_ADD('2026-02-15', INTERVAL ((cb.id * 11) MOD 80) DAY)
FROM circular_buyer cb
    ) src
    JOIN circular_material_price_policy pp
ON pp.material_code = src.material_code AND pp.active = 1;
