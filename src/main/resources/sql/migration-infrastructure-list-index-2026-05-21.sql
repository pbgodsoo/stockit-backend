-- hq/infrastructures 목록 조회 및 창고별 매장 수 집계 최적화

CREATE INDEX IF NOT EXISTS idx_infrastructure_type_status_region_id
    ON infrastructure (location_type, status, region, id);

CREATE INDEX IF NOT EXISTS idx_infrastructure_type_region_id
    ON infrastructure (location_type, region, id);

CREATE INDEX IF NOT EXISTS idx_store_warehouse_map_warehouse_id
    ON store_warehouse_map (warehouse_id);
