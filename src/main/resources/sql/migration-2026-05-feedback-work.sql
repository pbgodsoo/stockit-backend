-- ================================================================================================
-- migration-2026-05-feedback-work.sql
-- 통합 마이그레이션 (feat/feedback-work 브랜치) — 2026-05-22 ~ 2026-05-23 작업분 일괄 정리
--
-- 포함된 개별 마이그레이션 (원본 6개 파일을 한 파일로 통합):
--   ① inventory UK 정합 (2026-05-22)              — 옛 commit 보완
--   ② circular_buyer_transaction Phase 2 컬럼     (2026-05-22)
--   ③ NotificationType ENUM 6→4 축소              (2026-05-22)
--   ④ notification_read 매핑 테이블 신설          (2026-05-23 step1)
--   ⑤ notification is_read/read_at 제거 + 인덱스   (2026-05-23 step4 cleanup)
--   ⑥ user @Version 컬럼 초기화                    (2026-05-23)
--
-- 실행 순서 의존성:
--   - ②/③/④ 는 서로 독립적이라 순서 무관 (BE 부팅 전이라면 어떤 순서든 OK)
--   - ⑤ 는 ④ 가 선행되어야 함 (notification_read 가 없으면 사용자별 추적 불가)
--   - ⑤ 는 BE 코드(Notification.read 필드 제거) 가 배포된 후 실행해야 함
--     (안 그러면 ddl-auto: update 가 컬럼을 다시 만들어버려 무효화됨)
--   - ⑥ 은 BE 부팅으로 ddl-auto 가 version 컬럼을 추가한 직후 실행
--
-- 적용 환경:
--   - dev : ddl-auto: update 가 컬럼/인덱스 자동 처리하는 항목이 많아 사실상 ⑥ 만 필수.
--           나머지는 ENUM 좁힘 / 옛 인덱스 정리 / 데이터 cleanup 등 명시적 통제용.
--   - prod: 본 파일을 트랜잭션 묶음으로 검토 후 수동 실행. DB 백업 권장.
--
-- 적용 후 통합 검증:
--   SHOW INDEX FROM inventory;                          -- uk_inventory_sku_location_status 만 남아 있어야
--   SHOW COLUMNS FROM circular_buyer_transaction;       -- main_material_code / main_material_ratio 존재
--   SHOW COLUMNS FROM notification LIKE 'type';         -- ENUM 4종만
--   SHOW COLUMNS FROM notification;                     -- is_read / read_at 없음
--   SHOW TABLES LIKE 'notification_read';               -- 존재
--   SELECT COUNT(*) FROM user WHERE version IS NULL;    -- 0
-- ================================================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- ① inventory 옛 유니크 키 정리 (2026-05-22)
-- ─────────────────────────────────────────────────────────────────────────────
-- 배경:
--   commit c27490072d4a3cc0322546bddcc8a81b4465109c (2026-05-05) 에서
--   Inventory.java 의 유니크 키를 (sku_id, location_id) → (sku_id, location_id, inventory_status) 로
--   변경했으나 마이그레이션 SQL 누락. ddl-auto: update 가 옛 인덱스를 자동 삭제 못해 두 인덱스 공존.
--
-- 증상:
--   POST /api/hq/inventories/circular-candidates/convert →
--   "Duplicate entry 'X-Y' for key 'uk_inventory_sku_location'" 500 에러.
--
-- 사전 확인: SHOW INDEX FROM inventory;  -- uk_inventory_sku_location 가 없으면 이 ALTER 건너뜀
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE inventory DROP INDEX uk_inventory_sku_location;


-- ─────────────────────────────────────────────────────────────────────────────
-- ② circular_buyer_transaction — 혼방 거래 70% 주 소재 메타 컬럼 추가 (2026-05-22)
-- ─────────────────────────────────────────────────────────────────────────────
-- 배경:
--   Phase 2 — 혼방 거래 가중 점수 산식 (70% 주 소재 factor × 0.7) 을 BE 가 계산하기 위해
--   거래 단위로 어떤 70% 소재가 사용되었는지 식별 가능해야 함.
--
-- 컬럼:
--   - main_material_code  : material_code='BLEND' 일 때만 채움. 단일 거래는 NULL.
--   - main_material_ratio : 주 소재 비율 (예: 0.70). 단일 거래는 NULL.
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE circular_buyer_transaction
    ADD COLUMN main_material_code  VARCHAR(32) NULL COMMENT '혼방 거래의 70% 주 소재 코드 (단일 거래는 NULL)',
    ADD COLUMN main_material_ratio DECIMAL(3,2) NULL COMMENT '주 소재 비율 (예: 0.70)';


-- ─────────────────────────────────────────────────────────────────────────────
-- ③ NotificationType ENUM 6종 → 4종 축소 (2026-05-22)
-- ─────────────────────────────────────────────────────────────────────────────
-- 배경:
--   '탄소 배출 관리' 기능 폐기 → ESG_QUOTA_WARNING / ESG_QUOTA_EXCEEDED 알림 폐기.
--   notification.type 이 ENUM 컬럼이라 ddl-auto: update 의 자동 좁힘이 안 들어갈 수 있어 명시 ALTER.
--
-- 주의: 기존 ESG_QUOTA_* row 가 남아 있으면 MODIFY COLUMN 실패 → DELETE 선행.
-- ─────────────────────────────────────────────────────────────────────────────
DELETE FROM notification
 WHERE type IN ('ESG_QUOTA_WARNING', 'ESG_QUOTA_EXCEEDED');

ALTER TABLE notification
    MODIFY COLUMN type ENUM(
        'INVENTORY_SHORTAGE',
        'INVENTORY_OUT_OF_STOCK',
        'USER_SIGNUP_PENDING',
        'CIRCULAR_CANDIDATE'
    ) NOT NULL;


-- ─────────────────────────────────────────────────────────────────────────────
-- ④ notification_read 매핑 테이블 신설 (2026-05-23, B3 broadcast 읽음 분리)
-- ─────────────────────────────────────────────────────────────────────────────
-- 배경:
--   notification.is_read 가 권한군 broadcast 전체에 공유되어 한 명이 읽으면 모두 읽음 처리되는 버그.
--   사용자별 읽음 상태를 별도 매핑 테이블로 분리해 해결.
--
-- 인덱스 설계:
--   - PK (notification_id, user_id)  → 같은 사용자가 같은 알림을 두 번 INSERT 못함
--   - idx_notification_read_user      → 본인 읽음 목록 조회 (LEFT JOIN 효율)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE notification_read (
    notification_id BIGINT   NOT NULL,
    user_id         BIGINT   NOT NULL,
    read_at         DATETIME NOT NULL,
    PRIMARY KEY (notification_id, user_id),
    INDEX idx_notification_read_user (user_id, notification_id)
);


-- ─────────────────────────────────────────────────────────────────────────────
-- ⑤ notification 테이블 V1 잔재 정리 (2026-05-23 step4 cleanup)
-- ─────────────────────────────────────────────────────────────────────────────
-- 전제:
--   - ④ 완료 (notification_read 존재)
--   - BE 코드에서 Notification.read / readAt 필드 제거 + 배포 완료
--     안 되어 있으면 BE 재시작 시 ddl-auto: update 가 컬럼을 다시 만들어 본 SQL 무효화됨.
--
-- 작업:
--   - 기존 broadcast 데이터 폐기 (TRUNCATE — dev clean start 정책)
--   - is_read 포함 옛 인덱스 2개 DROP
--   - is_read / read_at 컬럼 DROP
--   - is_read 미포함 신규 인덱스 2개 CREATE
-- ─────────────────────────────────────────────────────────────────────────────
TRUNCATE TABLE notification;

ALTER TABLE notification DROP INDEX idx_notification_role_loc_unread;
ALTER TABLE notification DROP INDEX idx_notification_user_unread;

ALTER TABLE notification DROP COLUMN is_read;
ALTER TABLE notification DROP COLUMN read_at;

CREATE INDEX idx_notification_role_loc
    ON notification(target_role, target_location_code, create_date);
CREATE INDEX idx_notification_user
    ON notification(target_user_id, create_date);

-- idx_notification_ref (ref_type, ref_id) 는 read 와 무관 — 그대로 유지.


-- ─────────────────────────────────────────────────────────────────────────────
-- ⑥ User @Version 컬럼 초기화 (2026-05-23, 낙관적 락 도입)
-- ─────────────────────────────────────────────────────────────────────────────
-- 배경:
--   - 동시 승인 방지 (두 admin 이 같은 PENDING 신청을 동시 승인 시 사번 중복/lost update 방지)
--   - 본인 정보 수정 (updatePhone / updatePassword) 의 동시 UPDATE 충돌 감지
--   - 모든 User UPDATE 지점에 OptimisticLockingFailureException → USER_CONCURRENT_MODIFICATION (4805)
--
-- 문제 시나리오 (본 UPDATE 누락 시):
--   - ddl-auto: update 가 version BIGINT NULL 로 컬럼만 추가
--   - 기존 행 모두 version = NULL → 첫 UPDATE 시 Hibernate NPE
--     ("Cannot invoke java.lang.Long.longValue() because current is null")
--   - 마이페이지 전화번호 수정 / 계정 승인 등 모든 User UPDATE 500 에러.
--
-- prod 권장 (한 번에 처리):
--   ALTER TABLE user ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
-- dev 권장 (ddl-auto 가 컬럼 추가한 뒤):
--   본 UPDATE 실행
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE user
   SET version = 0
 WHERE version IS NULL;
-- 실측 (dev, 2026-05-23 18:00): 1685 row(s) affected


-- ================================================================================================
-- 통합 마이그레이션 끝
-- ================================================================================================
