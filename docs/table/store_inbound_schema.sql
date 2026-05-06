-- 매장 입고 MVP 스키마 v2
-- source_type 제거
-- from_warehouse_id, total_sku_count, total_expected_quantity, expected_arrival_at 추가
-- received_quantity 제거

CREATE TABLE IF NOT EXISTS store_inbound_header (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inbound_no VARCHAR(50) NOT NULL,
    source_ref_no VARCHAR(50) NOT NULL,
    source_ref_id BIGINT NULL,
    outbound_no VARCHAR(50) NOT NULL,
    store_id BIGINT NOT NULL,
    from_warehouse_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_sku_count INT NOT NULL DEFAULT 0,
    total_expected_quantity INT NOT NULL DEFAULT 0,
    expected_arrival_at DATETIME NOT NULL,
    requested_at DATETIME NOT NULL,
    received_at DATETIME NULL,
    requested_by_member_id VARCHAR(50) NULL,
    requested_by_name VARCHAR(100) NULL,
    received_by_member_id VARCHAR(50) NULL,
    received_by_name VARCHAR(100) NULL,
    memo VARCHAR(500) NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_inbound_no UNIQUE (inbound_no),
    CONSTRAINT uk_store_inbound_source_ref UNIQUE (source_ref_no),
    CONSTRAINT ck_store_inbound_total_sku_count CHECK (total_sku_count >= 0),
    CONSTRAINT ck_store_inbound_total_expected_quantity CHECK (total_expected_quantity >= 0),
    INDEX idx_store_inbound_store_status (store_id, status),
    INDEX idx_store_inbound_outbound_no (outbound_no),
    INDEX idx_store_inbound_expected_arrival (expected_arrival_at)
);

CREATE TABLE IF NOT EXISTS store_inbound_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inbound_header_id BIGINT NOT NULL,
    outbound_item_id BIGINT NULL,
    source_line_ref_id BIGINT NULL,
    sku_id BIGINT NOT NULL,
    sku_code VARCHAR(50) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    main_category VARCHAR(100) NULL,
    sub_category VARCHAR(100) NULL,
    color VARCHAR(50) NULL,
    size VARCHAR(50) NULL,
    unit_price BIGINT NULL,
    expected_quantity INT NOT NULL,
    memo VARCHAR(500) NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_inbound_item_header_sku UNIQUE (inbound_header_id, sku_id),
    CONSTRAINT ck_store_inbound_item_unit_price CHECK (unit_price IS NULL OR unit_price >= 0),
    CONSTRAINT ck_store_inbound_item_expected_quantity CHECK (expected_quantity > 0),
    INDEX idx_store_inbound_item_header (inbound_header_id),
    INDEX idx_store_inbound_item_sku (sku_code),
    CONSTRAINT fk_store_inbound_item_header FOREIGN KEY (inbound_header_id)
        REFERENCES store_inbound_header (id)
);

CREATE TABLE IF NOT EXISTS store_inbound_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inbound_header_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    changed_at DATETIME NOT NULL,
    changed_by_member_id VARCHAR(50) NULL,
    changed_by_name VARCHAR(100) NULL,
    reason VARCHAR(500) NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_store_inbound_history_header_changed (inbound_header_id, changed_at, id),
    CONSTRAINT fk_store_inbound_history_header FOREIGN KEY (inbound_header_id)
        REFERENCES store_inbound_header (id)
);

