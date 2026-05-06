-- 통합 출고(물류창고) MVP 스키마 v2
-- source_ref_id: 선택값
-- total_requested_quantity: 헤더 합계 필드 추가
-- confirmed_quantity: 제거

CREATE TABLE IF NOT EXISTS wh_outbound_header (
    id BIGINT NOT NULL AUTO_INCREMENT,
    outbound_no VARCHAR(50) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_ref_no VARCHAR(50) NOT NULL,
    source_ref_id BIGINT NULL,
    warehouse_id BIGINT NOT NULL,
    destination_type VARCHAR(30) NOT NULL,
    destination_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_requested_quantity INT NOT NULL DEFAULT 0,
    requested_at DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    departed_at DATETIME NULL,
    arrived_at DATETIME NULL,
    requested_by_member_id VARCHAR(50) NULL,
    requested_by_name VARCHAR(100) NULL,
    memo VARCHAR(500) NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_wh_outbound_no UNIQUE (outbound_no),
    CONSTRAINT uk_wh_outbound_source_ref UNIQUE (source_type, source_ref_no),
    CONSTRAINT ck_wh_outbound_total_requested_quantity CHECK (total_requested_quantity >= 0),
    INDEX idx_wh_outbound_warehouse_status (warehouse_id, status),
    INDEX idx_wh_outbound_destination (destination_type, destination_id),
    INDEX idx_wh_outbound_requested_at (requested_at)
);

CREATE TABLE IF NOT EXISTS wh_outbound_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    outbound_header_id BIGINT NOT NULL,
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
    requested_quantity INT NOT NULL,
    memo VARCHAR(500) NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_wh_outbound_item_header_sku UNIQUE (outbound_header_id, sku_id),
    CONSTRAINT ck_wh_outbound_item_unit_price CHECK (unit_price IS NULL OR unit_price >= 0),
    CONSTRAINT ck_wh_outbound_item_requested_quantity CHECK (requested_quantity > 0),
    INDEX idx_wh_outbound_item_header (outbound_header_id),
    INDEX idx_wh_outbound_item_sku (sku_code),
    CONSTRAINT fk_wh_outbound_item_header FOREIGN KEY (outbound_header_id)
        REFERENCES wh_outbound_header (id)
);

CREATE TABLE IF NOT EXISTS wh_outbound_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    outbound_header_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    changed_at DATETIME NOT NULL,
    changed_by_member_id VARCHAR(50) NULL,
    changed_by_name VARCHAR(100) NULL,
    reason VARCHAR(500) NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_wh_outbound_history_header_changed (outbound_header_id, changed_at, id),
    CONSTRAINT fk_wh_outbound_history_header FOREIGN KEY (outbound_header_id)
        REFERENCES wh_outbound_header (id)
);

