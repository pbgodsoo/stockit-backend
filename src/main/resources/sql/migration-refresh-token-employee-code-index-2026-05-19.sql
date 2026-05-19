-- ============================================================================
-- refresh_token.employee_code 인덱스 명시화
--
-- 배경:
--   deleteAllByEmployeeCode(employeeCode) 가 다음 시점마다 실행됨:
--     1. 모든 로그인 (RefreshTokenService.replaceRefreshToken)
--     2. 탈퇴 처리 (AccountService.withdraw)
--     3. 강제 로그아웃 (UserController.logout)
--   인덱스 없으면 Full Table Scan → RT 누적 시 점진적 성능 저하 (O(N))
--
-- 현재 상태:
--   JwtRefresh.@Index 어노테이션 + ddl-auto: update 로 인덱스는 이미 적용됨
--   (이름: id_refresh_employee_code)
--   → 컨벤션상 SQL 마이그레이션으로도 명시화하여 운영 환경 재현성 보장
--
-- 효과:
--   B-Tree Index Scan, O(log N) 처리. 1만 row 기준 약 50× 빠름
--
-- 멱등 처리:
--   IF NOT EXISTS 로 중복 생성 방지 (MariaDB 10.5+)
-- ============================================================================

-- 기존 인덱스 확인
SHOW INDEX FROM refresh_token WHERE Column_name = 'employee_code';

-- 이미 있으면 무시, 없으면 생성 (이름 통일: 기존 자동 생성된 이름 유지)
CREATE INDEX IF NOT EXISTS id_refresh_employee_code
    ON refresh_token(employee_code);

-- 최종 확인
SHOW INDEX FROM refresh_token;
