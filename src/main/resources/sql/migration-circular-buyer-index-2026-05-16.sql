-- 순환 거래처 목록 조회 성능 보강 인덱스
-- page API 에서 material_fit 필터 + 페이지 조회 빈도가 높아 인덱스 추가
-- keyword 는 '%...%' 부분일치라 일반 B-Tree 활용이 제한적이지만,
-- manager/company 정렬/접두 검색 여지를 위해 보조 인덱스로 둔다.

CREATE INDEX idx_circular_buyer_material_fit ON circular_buyer (primary_material_fit);
CREATE INDEX idx_circular_buyer_company_name ON circular_buyer (company_name);
CREATE INDEX idx_circular_buyer_manager_name ON circular_buyer (manager_name);
