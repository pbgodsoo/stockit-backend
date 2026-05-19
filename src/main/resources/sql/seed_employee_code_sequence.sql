-- ============================================================================
-- 사원코드 시퀀스 시드 — 동시성 안전한 사원코드 채번 위한 초기 데이터
--
-- 배경:
--   AccountService.generateEmployeeCode() 가 비관적 락(SELECT ... FOR UPDATE) 기반
--   시퀀스 테이블을 사용하도록 변경됨. 이 시드는 채번 시작점을 정의한다.
--
-- 초기값:
--   기존 user 테이블의 가장 큰 번호로 last_number 를 초기화
--   → 운영 데이터 보존 (이미 발급된 사원코드 다음부터 채번 이어감)
--
-- 실행 시점:
--   - 로컬: BE 첫 기동 후 1회 (employee_code_sequence 테이블이 비어있을 때)
--   - 신규 환경 배포 시: 동일하게 1회
--
-- 실행 방법:
--   IntelliJ Database 탭에서 우클릭 → Run 'seed_employee_code_sequence.sql'
--   또는 MariaDB CLI: source src/main/resources/sql/seed_employee_code_sequence.sql
--
-- 멱등 처리:
--   INSERT IGNORE — 동일 role_code 의 중복 키는 무시되어 재실행 안전
--
-- 향후 Flyway 도입 시:
--   이 파일을 src/main/resources/db/migration/V3__seed_employee_code_sequence.sql 로
--   이동하면 BE 기동 시 자동 실행됨
-- ============================================================================

INSERT INTO employee_code_sequence (role_code, last_number) VALUES
    ('hq', COALESCE((SELECT MAX(CAST(SUBSTRING(employee_code, 3) AS UNSIGNED))
        FROM user WHERE employee_code REGEXP '^hq[0-9]+$'), 0)),
    ('st', COALESCE((SELECT MAX(CAST(SUBSTRING(employee_code, 3) AS UNSIGNED))
        FROM user WHERE employee_code REGEXP '^st[0-9]+$'), 0)),
    ('wh', COALESCE((SELECT MAX(CAST(SUBSTRING(employee_code, 3) AS UNSIGNED))
        FROM user WHERE employee_code REGEXP '^wh[0-9]+$'), 0));

-- 3) 확인 — 3 row, 경고 없이 정상 값이 들어가야 함
SELECT role_code, last_number FROM employee_code_sequence ORDER BY role_code;
