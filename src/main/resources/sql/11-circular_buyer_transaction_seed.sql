-- ============================================================
-- 11-circular_buyer_transaction_seed.sql
-- 순환재고 거래 이력 시드 — 600건 (300 buyer × 2 거래)
--
-- 분포 규칙:
--   - buyer.primary_material_fit 와 매칭되는 material 만 거래
--     · synthetic (100명)      → POLYESTER/ACRYLIC/POLYAMIDE/ELASTANE 중 buyer id 기반 deterministic
--     · blended (100명)        → BLEND (혼방). main_material_code 에 70% 주 소재 분배 (Phase 2)
--     · natural-single (100명) → COTTON/WOOL/CASHMERE/SILK/LINEN 중 선택
--   - 단가는 circular_material_price_policy.price_per_kg 정책 단가 (거래 시점 스냅샷)
--   - weight_kg: 200~5000 kg deterministic 분포
--   - transacted_at: 2026-01-01 ~ 2026-05-10 분포 (약 130일)
--
-- Phase 2 추가: main_material_code / main_material_ratio
--   - BLEND 거래일 때만 70% 주 소재 코드 (COTTON / POLYAMIDE / LINEN / WOOL) + ratio 0.70 채움
--   - 단일 거래는 NULL (혼방 산식 미적용 대상)
--   - ScoreEventsService 가 (material.carbon_factor[main_material_code] × main_material_ratio) 로 가중 점수 산출
--
-- 실행 전제:
--   - circular_buyer 시드 (08) 적용됨 (300건)
--   - circular_material_price_policy 시드 (06) 적용됨 (10건)
--   - circular_buyer_transaction 테이블 생성됨 (BE 재기동 시 Hibernate 가 자동 생성)
--   - Phase 2 마이그레이션 적용됨 (main_material_code, main_material_ratio 컬럼 존재)
--
-- 재실행 시: id IDENTITY 라 중복 INSERT 방지 위해 TRUNCATE 권장.
--   TRUNCATE TABLE circular_buyer_transaction;
-- ============================================================

INSERT INTO circular_buyer_transaction
(buyer_id, material_code, weight_kg, unit_price, total_amount, transacted_at, create_date,
 main_material_code, main_material_ratio)
SELECT
    src.buyer_id,
    src.material_code,
    src.weight_kg,
    pp.price_per_kg AS unit_price,
    src.weight_kg * pp.price_per_kg AS total_amount,
    src.transacted_at,
    NOW() AS create_date,
    -- Phase 2: BLEND 거래일 때만 70% 주 소재 코드 분배 (4가지 product 조합 패턴)
    --   COTTON+POLYESTER / POLYAMIDE+POLYESTER / LINEN+POLYESTER / WOOL+POLYESTER 의 주 소재
    CASE
        WHEN src.material_code = 'BLEND'
          THEN ELT(((src.buyer_id - 1) MOD 4) + 1, 'COTTON', 'POLYAMIDE', 'LINEN', 'WOOL')
        ELSE NULL
    END AS main_material_code,
    -- Phase 2: 혼방은 0.70 비율, 단일 거래는 NULL
    CASE WHEN src.material_code = 'BLEND' THEN 0.70 ELSE NULL END AS main_material_ratio
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
