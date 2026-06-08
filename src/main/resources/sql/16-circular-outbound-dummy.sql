-- 16-circular-outbound-dummy.sql
-- 순환판매/기부 1건당 출고 1건 생성 (wh_outbound_header/item/status_history)
-- circular_sale_header.outbound_header_id 업데이트 포함
--
-- 실행 전제: 14-circular-sales-dummy.sql 실행 완료
--
-- ▣ 상태 분포 (circular_sale_header.status 와 동기화)
--   1~4월 (ARRIVED)  → 출고 ARRIVED  : READY_TO_SHIP → IN_TRANSIT → ARRIVED 이력 3건
--   5월   (ARRIVED)  → 출고 ARRIVED  : 동일
--   5월   IN_TRANSIT → 출고 IN_TRANSIT: READY_TO_SHIP → IN_TRANSIT 이력 2건
--   6월   (ARRIVED)  → 출고 ARRIVED  : 동일
--   6월   IN_TRANSIT → 출고 IN_TRANSIT: READY_TO_SHIP → IN_TRANSIT 이력 2건
--   6월   RTS        → 출고 READY_TO_SHIP: READY_TO_SHIP 이력 1건
--
-- ▣ 타이밍
--   confirmed_at = departed_at = sold_at + 1일 (IN_TRANSIT 이상)
--   arrived_at   = completed_at               (ARRIVED만)
--
-- ▣ 번호 규칙: WOB-YYYYMMDD-{outbound_id} (AUTO_INCREMENT 후 UPDATE)
-- ▣ source_type = 'CIRCULAR_SALE'
-- ▣ destination_type = 'CIRCULAR_BUYER' (SALE) | 'DONATION' (DONATION)

-- ============================================================
-- STEP 0. 기존 더미 출고 정리 (CIRCULAR_SALE source 한정)
-- ============================================================
SET SQL_SAFE_UPDATES = 0;

DELETE sh FROM wh_outbound_status_history sh
JOIN wh_outbound_header woh ON woh.id = sh.outbound_header_id
WHERE woh.source_type = 'CIRCULAR_SALE'
  AND (woh.source_ref_no LIKE 'CSR-2026%' OR woh.source_ref_no LIKE 'DON-2026%');

DELETE wi FROM wh_outbound_item wi
JOIN wh_outbound_header woh ON woh.id = wi.outbound_header_id
WHERE woh.source_type = 'CIRCULAR_SALE'
  AND (woh.source_ref_no LIKE 'CSR-2026%' OR woh.source_ref_no LIKE 'DON-2026%');

DELETE FROM wh_outbound_header
WHERE source_type = 'CIRCULAR_SALE'
  AND (source_ref_no LIKE 'CSR-2026%' OR source_ref_no LIKE 'DON-2026%');

-- outbound 연결 초기화
UPDATE circular_sale_header
SET outbound_header_id = NULL
WHERE sale_no LIKE 'CSR-2026%' OR sale_no LIKE 'DON-2026%';

SET SQL_SAFE_UPDATES = 1;

-- ============================================================
-- STEP 1. wh_outbound_header INSERT
--   outbound_no 는 임시값('TMP-{sale_header_id}') → STEP2에서 정식 번호로 UPDATE
-- ============================================================
INSERT INTO wh_outbound_header (
  outbound_no,
  source_type, source_ref_no, source_ref_seq, source_ref_id,
  warehouse_id,
  destination_type, destination_id, destination_name,
  status,
  total_requested_quantity,
  requested_at, confirmed_at, departed_at, arrived_at,
  requested_by_member_id, requested_by_name,
  create_date, update_date
)
SELECT
  CONCAT('TMP-CSR-', csh.id)                                  AS outbound_no,
  'CIRCULAR_SALE'                                              AS source_type,
  csh.sale_no                                                  AS source_ref_no,
  1                                                            AS source_ref_seq,
  csh.id                                                       AS source_ref_id,
  csh.warehouse_id,
  -- SALE → CIRCULAR_BUYER / DONATION → DONATION
  CASE WHEN csh.sale_type = 'DONATION' THEN 'DONATION' ELSE 'CIRCULAR_BUYER' END AS destination_type,
  CASE WHEN csh.sale_type = 'DONATION' THEN NULL ELSE csh.buyer_id END           AS destination_id,
  CASE WHEN csh.sale_type = 'DONATION' THEN csh.donee_name ELSE NULL END         AS destination_name,
  -- 판매 상태와 동기화
  CASE csh.status
    WHEN 'ARRIVED'       THEN 'ARRIVED'
    WHEN 'IN_TRANSIT'    THEN 'IN_TRANSIT'
    ELSE                      'READY_TO_SHIP'
  END                                                          AS status,
  csh.total_sold_quantity,
  csh.sold_at                                                  AS requested_at,
  -- confirmed_at / departed_at: IN_TRANSIT 이상이면 sold_at + 1일
  CASE WHEN csh.status IN ('IN_TRANSIT','ARRIVED')
       THEN DATE_ADD(csh.sold_at, INTERVAL 1 DAY) ELSE NULL END AS confirmed_at,
  CASE WHEN csh.status IN ('IN_TRANSIT','ARRIVED')
       THEN DATE_ADD(csh.sold_at, INTERVAL 1 DAY) ELSE NULL END AS departed_at,
  -- arrived_at: ARRIVED만
  CASE WHEN csh.status = 'ARRIVED'
       THEN csh.completed_at ELSE NULL END                     AS arrived_at,
  csh.sold_by_member_id,
  csh.sold_by_name,
  csh.sold_at                                                  AS create_date,
  COALESCE(csh.completed_at, csh.sold_at)                     AS update_date
FROM circular_sale_header csh
WHERE csh.sale_no LIKE 'CSR-2026%' OR csh.sale_no LIKE 'DON-2026%'
ORDER BY csh.sold_at, csh.id;

-- ============================================================
-- STEP 2. outbound_no 정식 번호로 UPDATE: WOB-YYYYMMDD-{id}
-- ============================================================
SET SQL_SAFE_UPDATES = 0;
UPDATE wh_outbound_header
SET outbound_no = CONCAT('WOB-', DATE_FORMAT(requested_at, '%Y%m%d'), '-', LPAD(id, 5, '0'))
WHERE outbound_no LIKE 'TMP-CSR-%'
  AND source_type = 'CIRCULAR_SALE';
SET SQL_SAFE_UPDATES = 1;

-- ============================================================
-- STEP 3. circular_sale_header.outbound_header_id 연결
-- ============================================================
SET SQL_SAFE_UPDATES = 0;
UPDATE circular_sale_header csh
JOIN wh_outbound_header woh
  ON woh.source_ref_no  = csh.sale_no
 AND woh.source_type    = 'CIRCULAR_SALE'
SET csh.outbound_header_id = woh.id,
    csh.update_date        = NOW()
WHERE csh.sale_no LIKE 'CSR-2026%' OR csh.sale_no LIKE 'DON-2026%';
SET SQL_SAFE_UPDATES = 1;

-- ============================================================
-- STEP 4. wh_outbound_item INSERT
--   circular_sale_item 1:1 복사
--   main_category/sub_category: 출고 아이템은 circular_sale_item 그대로
-- ============================================================
INSERT INTO wh_outbound_item (
  outbound_header_id,
  source_line_ref_id,
  sku_id, sku_code,
  product_code, product_name,
  main_category, sub_category,
  color, size,
  unit_price, requested_quantity,
  memo,
  create_date, update_date
)
SELECT
  woh.id              AS outbound_header_id,
  ci.id               AS source_line_ref_id,
  ci.sku_id,
  ci.sku_code,
  ci.product_code,
  ci.product_name,
  ci.main_category,
  ci.sub_category,
  ci.color,
  ci.size,
  ci.unit_price,
  ci.sold_quantity    AS requested_quantity,
  NULL                AS memo,
  ci.create_date,
  ci.update_date
FROM wh_outbound_header woh
JOIN circular_sale_header csh
  ON csh.sale_no   = woh.source_ref_no
 AND woh.source_type = 'CIRCULAR_SALE'
JOIN circular_sale_item ci
  ON ci.sale_header_id = csh.id
WHERE csh.sale_no LIKE 'CSR-2026%' OR csh.sale_no LIKE 'DON-2026%'
ORDER BY woh.id, ci.id;

-- ============================================================
-- STEP 5. wh_outbound_status_history INSERT
--   - 모든 건: READY_TO_SHIP (sold_at)
--   - IN_TRANSIT 이상: IN_TRANSIT (sold_at + 1일)
--   - ARRIVED만: ARRIVED (completed_at)
-- ============================================================

-- 5-1) READY_TO_SHIP (전체)
INSERT INTO wh_outbound_status_history (
  outbound_header_id, status, changed_at,
  changed_by_member_id, changed_by_name, reason,
  create_date, update_date
)
SELECT
  woh.id,
  'READY_TO_SHIP',
  woh.requested_at,
  csh.sold_by_member_id,
  csh.sold_by_name,
  'CIRCULAR_SALE_CREATE',
  woh.requested_at,
  woh.requested_at
FROM wh_outbound_header woh
JOIN circular_sale_header csh
  ON csh.sale_no   = woh.source_ref_no
 AND woh.source_type = 'CIRCULAR_SALE'
WHERE csh.sale_no LIKE 'CSR-2026%' OR csh.sale_no LIKE 'DON-2026%'
ORDER BY woh.id;

-- 5-2) IN_TRANSIT (IN_TRANSIT 이상인 건만)
INSERT INTO wh_outbound_status_history (
  outbound_header_id, status, changed_at,
  changed_by_member_id, changed_by_name, reason,
  create_date, update_date
)
SELECT
  woh.id,
  'IN_TRANSIT',
  woh.departed_at,
  csh.sold_by_member_id,
  csh.sold_by_name,
  'DEPARTURE',
  woh.departed_at,
  woh.departed_at
FROM wh_outbound_header woh
JOIN circular_sale_header csh
  ON csh.sale_no   = woh.source_ref_no
 AND woh.source_type = 'CIRCULAR_SALE'
WHERE (csh.sale_no LIKE 'CSR-2026%' OR csh.sale_no LIKE 'DON-2026%')
  AND woh.status IN ('IN_TRANSIT','ARRIVED')
ORDER BY woh.id;

-- 5-3) ARRIVED (ARRIVED인 건만)
INSERT INTO wh_outbound_status_history (
  outbound_header_id, status, changed_at,
  changed_by_member_id, changed_by_name, reason,
  create_date, update_date
)
SELECT
  woh.id,
  'ARRIVED',
  woh.arrived_at,
  csh.sold_by_member_id,
  csh.sold_by_name,
  'ARRIVAL_CONFIRMED',
  woh.arrived_at,
  woh.arrived_at
FROM wh_outbound_header woh
JOIN circular_sale_header csh
  ON csh.sale_no   = woh.source_ref_no
 AND woh.source_type = 'CIRCULAR_SALE'
WHERE (csh.sale_no LIKE 'CSR-2026%' OR csh.sale_no LIKE 'DON-2026%')
  AND woh.status = 'ARRIVED'
ORDER BY woh.id;
