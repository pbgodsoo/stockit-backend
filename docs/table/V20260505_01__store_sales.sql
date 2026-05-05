CREATE TABLE IF NOT EXISTS store_sale_header (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sale_no VARCHAR(40) NOT NULL,
    store_id BIGINT NOT NULL,
    sold_at DATETIME NOT NULL,
    total_quantity INT NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT pk_store_sale_header PRIMARY KEY (id),
    CONSTRAINT uk_store_sale_header_sale_no UNIQUE (sale_no),
    CONSTRAINT chk_store_sale_header_total_quantity CHECK (total_quantity >= 0),
    CONSTRAINT chk_store_sale_header_total_amount CHECK (total_amount >= 0),
    CONSTRAINT fk_store_sale_header_store FOREIGN KEY (store_id) REFERENCES infrastructure (id)
);

CREATE TABLE IF NOT EXISTS store_sale_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sale_header_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_code VARCHAR(32) NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    main_category VARCHAR(64) NOT NULL,
    sub_category VARCHAR(64) NOT NULL,
    color VARCHAR(32) NOT NULL,
    size VARCHAR(16) NOT NULL,
    quantity INT NOT NULL,
    unit_price BIGINT NOT NULL,
    line_amount BIGINT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT pk_store_sale_item PRIMARY KEY (id),
    CONSTRAINT chk_store_sale_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_store_sale_item_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_store_sale_item_line_amount CHECK (line_amount >= 0),
    CONSTRAINT fk_store_sale_item_header FOREIGN KEY (sale_header_id) REFERENCES store_sale_header (id),
    CONSTRAINT fk_store_sale_item_sku FOREIGN KEY (sku_id) REFERENCES product_sku (id)
);

CREATE INDEX idx_store_sale_header_store_soldat ON store_sale_header (store_id, sold_at DESC);
CREATE INDEX idx_store_sale_item_sale_header_id ON store_sale_item (sale_header_id);
CREATE INDEX idx_store_sale_item_sku_id ON store_sale_item (sku_id);

