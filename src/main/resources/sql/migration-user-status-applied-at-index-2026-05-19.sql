-- ============================================================================
-- user(status, applied_at) 복합 인덱스 추가
--
-- 배경:
--   계정 관리 화면(/api/hq/account, /api/hq/account/pending) 의 쿼리 패턴이
--   "WHERE status = ? ORDER BY applied_at ASC/DESC" 로 고정됨.
--   단일 인덱스로는 필터 또는 정렬 한쪽만 최적화 가능 → 복합 인덱스로 동시 해결.
--
-- 인덱스 순서 (status, applied_at):
--   - status: 동등 조건 (=) → 인덱스의 첫 컬럼이어야 효과적
--   - applied_at: 정렬 컬럼 → 인덱스의 두 번째에 위치
--
-- Before (검증):
--   EXPLAIN: type=ALL, rows=1675, Extra="Using where; Using filesort"
--
-- After (검증):
--   대기 목록 (PENDING + ASC):
--     type=ref, key=idx_user_status_applied_at, rows=8, Using index condition
--     → 209× 효율 + filesort 제거
--   전체 목록 (APPROVED + STORE + DESC):
--     type=range, Extra=Using where (★ filesort 제거)
--     → 데이터 분포상 status 필터 효과는 작지만 filesort 비용은 0
--
-- 자동 적용:
--   User.java 의 @Table(indexes={...}) 어노테이션 + ddl-auto: update 로
--   BE 재시작 시 자동 ALTER TABLE 실행 (시나리오 C)
--   이 파일은 운영 환경 + DBA 협업 + Flyway 도입 대비용
-- ============================================================================

-- 1) 현황 확인
SHOW INDEX FROM user WHERE Key_name = 'idx_user_status_applied_at';

-- 2) 복합 인덱스 생성 (이미 있으면 무시)
CREATE INDEX IF NOT EXISTS idx_user_status_applied_at
    ON user(status, applied_at);

-- 3) 검증 — 대기 목록 (ASC)
EXPLAIN SELECT * FROM user
        WHERE status = 'PENDING'
        ORDER BY applied_at ASC
            LIMIT 20;

-- 4) 검증 — 전체 목록 (DESC)
EXPLAIN SELECT * FROM user
        WHERE status = 'APPROVED' AND role = 'STORE'
        ORDER BY applied_at DESC
            LIMIT 20;
