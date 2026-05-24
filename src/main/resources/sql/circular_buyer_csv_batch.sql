-- circular_buyer CSV 대량 적재 배치 SQL
-- 전제:
--   1) tools/load_circular_buyer_from_csv.py 가 아래 staging 테이블에 데이터 적재
--   2) run_id 단위로 valid/reject 데이터가 분리됨
--
-- 실행 변수:
--   SET @run_id = '20260515_223000';

CREATE TABLE IF NOT EXISTS staging_circular_buyer_valid (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    run_id                VARCHAR(64)  NOT NULL,
    source_row_no         INT          NOT NULL,
    code                  VARCHAR(32)  NOT NULL,
    company_name          VARCHAR(128) NOT NULL,
    phone                 VARCHAR(32)  NOT NULL,
    address               VARCHAR(256) NOT NULL,
    description           TEXT         NULL,
    primary_material_fit  VARCHAR(32)  NOT NULL,
    manager_name          VARCHAR(64)  NOT NULL,
    industry_group        VARCHAR(64)  NOT NULL,
    partner_type          VARCHAR(32)  NOT NULL,
    factory_product       JSON         NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stg_cb_valid_run_row (run_id, source_row_no),
    KEY idx_stg_cb_valid_run (run_id)
);

CREATE TABLE IF NOT EXISTS staging_circular_buyer_reject (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    run_id                VARCHAR(64)   NOT NULL,
    source_row_no         INT           NOT NULL,
    company_name          VARCHAR(256)  NULL,
    phone                 VARCHAR(128)  NULL,
    factory_product_raw   TEXT          NULL,
    address               VARCHAR(512)  NULL,
    description           TEXT          NULL,
    material_fit_raw      VARCHAR(128)  NULL,
    manager_name          VARCHAR(256)  NULL,
    industry_group        VARCHAR(256)  NULL,
    partner_type_raw      VARCHAR(128)  NULL,
    reject_reason         TEXT          NOT NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stg_cb_reject_run_row (run_id, source_row_no),
    KEY idx_stg_cb_reject_run (run_id)
);

-- run_id 단위 본 테이블 전체 교체
START TRANSACTION;

DELETE FROM circular_buyer;

INSERT INTO circular_buyer (
    code, company_name, industry_group, factory_product, description,
    primary_material_fit, manager_name, phone, address, partner_type,
    embedding, create_date, update_date
)
SELECT
    v.code,
    v.company_name,
    v.industry_group,
    v.factory_product,
    v.description,
    v.primary_material_fit,
    v.manager_name,
    v.phone,
    v.address,
    v.partner_type,
    NULL,
    NOW(),
    NOW()
FROM staging_circular_buyer_valid v
WHERE v.run_id = @run_id
ORDER BY v.source_row_no;

COMMIT;
