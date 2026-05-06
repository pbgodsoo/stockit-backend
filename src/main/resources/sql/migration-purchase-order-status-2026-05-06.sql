-- 발주 상태머신 7단계 확장 마이그레이션 (2026-05-06)
--
-- 변경 이유: 기존 6단계(PENDING/APPROVED/SHIPPING/DELIVERED/COMPLETED/REJECTED) 를
-- 7단계(REQUESTED/APPROVED/READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED/CANCELLED) 로 확장.
-- enum 값 rename + 신규 단계(READY_TO_SHIP) 추가.
--
-- 매핑:
--   PENDING   → REQUESTED
--   APPROVED  → APPROVED   (그대로)
--   SHIPPING  → IN_TRANSIT
--   DELIVERED → ARRIVED
--   COMPLETED → COMPLETED  (그대로)
--   REJECTED  → CANCELLED
--
-- READY_TO_SHIP 단계는 신규 — 기존 row 의 history 에는 존재하지 않음 (정상).
--
-- 적용 순서: BE 서버 중지 → 본 SQL 실행 → BE 재시작.
-- ddl-auto: update 라 enum 컬럼 길이(VARCHAR(16)) 변경 불필요 — 신규 enum 값 모두 16자 안.

-- purchase_order 테이블
UPDATE purchase_order SET status = 'REQUESTED'  WHERE status = 'PENDING';
UPDATE purchase_order SET status = 'IN_TRANSIT' WHERE status = 'SHIPPING';
UPDATE purchase_order SET status = 'ARRIVED'    WHERE status = 'DELIVERED';
UPDATE purchase_order SET status = 'CANCELLED'  WHERE status = 'REJECTED';

-- purchase_order_status_history 테이블
UPDATE purchase_order_status_history SET status = 'REQUESTED'  WHERE status = 'PENDING';
UPDATE purchase_order_status_history SET status = 'IN_TRANSIT' WHERE status = 'SHIPPING';
UPDATE purchase_order_status_history SET status = 'ARRIVED'    WHERE status = 'DELIVERED';
UPDATE purchase_order_status_history SET status = 'CANCELLED'  WHERE status = 'REJECTED';
