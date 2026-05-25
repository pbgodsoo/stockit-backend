-- ShedLock 분산 락 테이블
-- Stage 1: 다중 Pod 중복 실행 방지용
-- 실행 환경: dev, prod 공통
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
