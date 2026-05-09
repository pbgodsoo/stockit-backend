-- 매장 발주 승인 오케스트레이션(출고/입고 분할) 스키마 변경

ALTER TABLE wh_outbound_header
    ADD COLUMN source_ref_seq INT NOT NULL DEFAULT 1 AFTER source_ref_no;

ALTER TABLE wh_outbound_header
    DROP INDEX uk_wh_outbound_source_ref;

ALTER TABLE wh_outbound_header
    ADD CONSTRAINT uk_wh_outbound_source_ref_seq UNIQUE (source_type, source_ref_no, source_ref_seq);

ALTER TABLE store_inbound_header
    DROP INDEX uk_store_inbound_source_ref;

ALTER TABLE store_inbound_header
    ADD COLUMN delivery_group_no VARCHAR(50) NULL AFTER received_by_name;

ALTER TABLE store_inbound_header
    ADD CONSTRAINT uk_store_inbound_outbound_no UNIQUE (outbound_no);
