CREATE TABLE IF NOT EXISTS store_order_header (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL,
    store_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    requested_by_member_id VARCHAR(50) NOT NULL,
    requested_by_name VARCHAR(100) NOT NULL,
    requested_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    total_sku_count INT NOT NULL DEFAULT 0,
    total_requested_quantity INT NOT NULL DEFAULT 0,
    memo VARCHAR(500) NULL,
    cancel_reason VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT pk_store_order_header PRIMARY KEY (id),
    CONSTRAINT uk_store_order_header_order_no UNIQUE (order_no),
    CONSTRAINT chk_store_order_header_status CHECK (status IN ('REQUESTED', 'APPROVED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_store_order_header_total_sku_count CHECK (total_sku_count >= 0),
    CONSTRAINT chk_store_order_header_total_requested_quantity CHECK (total_requested_quantity >= 0),
    CONSTRAINT fk_store_order_header_store FOREIGN KEY (store_id) REFERENCES infrastructure (id),
    CONSTRAINT fk_store_order_header_warehouse FOREIGN KEY (warehouse_id) REFERENCES infrastructure (id)
);

CREATE TABLE IF NOT EXISTS store_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_header_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_code VARCHAR(32) NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    main_category VARCHAR(64) NOT NULL,
    sub_category VARCHAR(64) NOT NULL,
    color VARCHAR(32) NOT NULL,
    size VARCHAR(16) NOT NULL,
    unit_price BIGINT NOT NULL DEFAULT 0,
    requested_quantity INT NOT NULL DEFAULT 1,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT pk_store_order_item PRIMARY KEY (id),
    CONSTRAINT uk_store_order_item_order_sku UNIQUE (order_header_id, sku_id),
    CONSTRAINT chk_store_order_item_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_store_order_item_requested_quantity CHECK (requested_quantity > 0),
    CONSTRAINT fk_store_order_item_header FOREIGN KEY (order_header_id) REFERENCES store_order_header (id),
    CONSTRAINT fk_store_order_item_sku FOREIGN KEY (sku_id) REFERENCES product_sku (id)
);

CREATE TABLE IF NOT EXISTS store_order_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_header_id BIGINT NOT NULL,
    history_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    changed_at DATETIME NOT NULL,
    changed_by_member_id VARCHAR(50) NULL,
    changed_by_name VARCHAR(100) NULL,
    reason VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT pk_store_order_status_history PRIMARY KEY (id),
    CONSTRAINT chk_store_order_status_history_type CHECK (history_type IN ('ORDER_STATUS')),
    CONSTRAINT fk_store_order_status_history_header FOREIGN KEY (order_header_id) REFERENCES store_order_header (id)
);

CREATE INDEX idx_store_order_header_store_requestedat ON store_order_header (store_id, requested_at DESC);
CREATE INDEX idx_store_order_header_status_requestedat ON store_order_header (status, requested_at DESC);
CREATE INDEX idx_store_order_header_warehouse_requestedat
    ON store_order_header (warehouse_id, requested_at DESC);
CREATE INDEX idx_store_order_item_order_header_id ON store_order_item (order_header_id);
CREATE INDEX idx_store_order_item_sku_id ON store_order_item (sku_id);
CREATE INDEX idx_store_order_status_history_order_changedat ON store_order_status_history (order_header_id, changed_at DESC);
CREATE INDEX idx_store_order_status_history_type_status_changedat
    ON store_order_status_history (history_type, status, changed_at DESC);

