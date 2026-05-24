-- CircularBuyer ES 동기화 실패 재시도용 outbox 테이블
-- 운영(production)은 hibernate ddl-auto=validate 이므로 수동 반영 필요

CREATE TABLE IF NOT EXISTS es_sync_outbox (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    entity_type   VARCHAR(64)  NOT NULL,
    entity_key    VARCHAR(128) NOT NULL,
    op_type       VARCHAR(16)  NOT NULL,
    payload       TEXT         NULL,
    status        VARCHAR(16)  NOT NULL,
    retry_count   INT          NOT NULL DEFAULT 0,
    next_retry_at DATETIME     NOT NULL,
    last_error    TEXT         NULL,
    create_date   DATETIME     NOT NULL,
    update_date   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_es_sync_outbox_status_next_retry (status, next_retry_at),
    KEY idx_es_sync_outbox_entity (entity_type, entity_key)
);
