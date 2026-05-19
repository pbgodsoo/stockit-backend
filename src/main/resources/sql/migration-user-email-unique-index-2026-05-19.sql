-- ============================================================================
-- user.email UNIQUE 인덱스 추가 (데이터 무결성 + 성능 개선)
--
-- 배경:
--   User.email = @Column(nullable = false, unique = true)
--   그러나 ddl-auto: update 가 이미 존재하는 테이블에 unique 제약을 자동 추가하지 않음.
--   → 실제 DB에 UNIQUE 인덱스가 누락된 상태였음 (SHOW INDEX 로 확인)
--
-- 영향:
--   1) existsByEmail 회원가입 중복 체크가 Full Table Scan (EXPLAIN type=ALL)
--   2) DB 레벨 UNIQUE 제약이 없어 race condition 시 중복 email 저장 가능
--
-- 효과:
--   1) Full Scan → const 매칭, 1675 rows → 1 row 검사
--   2) DB 레벨에서 중복 INSERT 자동 차단 (애플리케이션 체크와 이중 안전망)
--
-- 사전 점검:
--   중복 email 검사 결과 0건 확인 후 인덱스 추가
--   SELECT email, COUNT(*) FROM user GROUP BY email HAVING COUNT(*) > 1;
-- ============================================================================

-- 1) 현황 확인
SHOW INDEX FROM user WHERE Column_name = 'email';

-- 2) UNIQUE 인덱스 생성 (이미 있으면 에러 → 사전 SHOW INDEX 로 확인 후 주석 처리)
CREATE UNIQUE INDEX uk_user_email ON user(email);

-- 3) 검증
SHOW INDEX FROM user WHERE Column_name = 'email';
EXPLAIN SELECT 1 FROM user WHERE email = 'hong@stockit.com' LIMIT 1;
