-- 제품 마스터/SKU/소재 더미 데이터
-- 실행 순서: category_two_level_seed.sql + vendor + infrastructure 이후

INSERT INTO material
(code, name_ko, material_group, carbon_factor, active, description, create_date, update_date)
VALUES
    -- material_group 어휘는 ProductMasterService.MATERIAL_GROUP_NATURAL='NATURAL' 상수 호환을 위해
    -- 'NATURAL' 유지 (Phase 1 옵션 C — 팀원 코드 기준에 맞춤). FE 는 esgStore.fetchMaterialFactors 에서
    -- NATURAL → NATURAL_SINGLE 로 정규화하여 일관성 유지.
    ('COTTON',    '면',         'NATURAL',   6.000, 1, '면(코튼) 천연 셀룰로오스 단일 섬유. 흡습성·통기성·세탁 내구성 우수, 의류·내의·침구·타올 광범위 사용. 폐 면 의류 회수 시 단일 천연 분리 깨끗 — 펄프 재생(한국제지·전주페이퍼·무림페이퍼)·재생 비스코스 원사 두 경로 적합. 솜 충전 재가공 → 매트리스·이불·인형 다용도. 데님 → 데님 순환. 농업 멀칭·바이오매스·자연 분해 친환경 처리. 한국 면 회수 산업 핵심.', NOW(), NOW()),
    ('WOOL',      '울',         'NATURAL',   5.000, 1, '울(양모) 천연 단백질 단일 섬유, 보온성·복원력 우수, 코트·정장·니트·스웨터·담요 사용. 폐 울 의류 회수 시 단백질 화학·기계 회수 → 재생 울사·매트리스 충전·자동차 흡음재·산업 펠트(악기·단열) 다용도. 양모 → 양모 순환 가능. 단일 천연 단백질 분리 깨끗·자연 분해 친환경. 한국 울 재생 산업 적용.', NOW(), NOW()),  -- cap 적용
    ('CASHMERE',  '캐시미어',   'NATURAL',   8.000, 1, '캐시미어 천연 단백질 단일 섬유 (캐시미어 염소 유래), 부드러움·보온성·고급 의류·코트·니트·머플러 사용. 폐 캐시미어 회수 시 단백질 회수·재생 캐시미어 원사 — 매우 고가 자원이라 재생 가치 ↑. 단일 천연 단백질 분리 깨끗·자연 분해 친환경. 한국 고급 의류 재생·매트리스 충전 다용도.', NOW(), NOW()),  -- cap 적용
    ('SILK',      '실크',       'NATURAL',   5.000, 1, '실크 천연 단백질 단일 섬유 (누에고치 유래), 광택·드레이프·고급 의류·한복·침구·스카프 사용. 폐 실크 한복·침구 회수 시 단백질 화학 추출 → 재생 실크사·화장품 단백질·바이오 의약 원료 고부가가치. 실크 단일 천연 분리 깨끗·자연 분해 친환경. 한국 전통 한복 실크 재생·전통 공방·바이오 산업 다방면.', NOW(), NOW()),  -- cap 적용
    ('LINEN',     '린넨',       'NATURAL',   1.800, 1, '린넨(아마) 천연 셀룰로오스 단일 섬유, 통기성·시원감 여름 의류·호텔 린넨·산업 천 사용. 폐 린넨 의류·호텔 시트 회수 → 펄프 재생 셀룰로오스 회수·재생 비스코스 원사. 린넨 단일 천연 분리 깨끗·자연 분해 친환경. 호텔/리조트 폐섬유 재가공(제클린)·산업 천·종이 다방면.', NOW(), NOW()),
    ('POLYESTER', '폴리에스터', 'SYNTHETIC', 6.500, 1, '폴리에스터(PET) 합성섬유, 내구성·형태 안정성 우수, 운동복·아웃도어·산업용 부직포 다용도. 폐 페트병·폐 폴리 의류 화학 재활용(효성티앤씨 리젠·휴비스 에코에버) → 모노머 회수 → 재생 PET 원사. PET 단일 합성 회수 분류 깨끗·GRS 인증 글로벌 표준. 자동차 흡음재·도아트림·헤드라이너(현대모비스)·건설 단열재(벽산·KCC)·매트리스 충전·신발 인솔·물류 패키징 다방면. PET 화학 재활용 순환경제 핵심.', NOW(), NOW()),
    ('ACRYLIC',   '아크릴',     'SYNTHETIC', 5.700, 1, '아크릴 합성섬유, 보온성·발색성 우수, 스웨터·니트·담요·인조 모피·산업 펠트 사용. 폐 아크릴 스웨터·니트 풀어서 원사 추출 → 재생 아크릴 원사 기계적 재활용. 단일 합성 분리 깨끗·매트리스 충전·자동차 흡음재·인테리어 매트 다방면. 한국 편직 산업 재활용.', NOW(), NOW()),
    ('POLYAMIDE', '나일론',     'SYNTHETIC', 8.000, 1, '폴리아미드(나일론) 합성섬유, 강도·내마모성·신축성 우수, 운동복·수영복·스타킹·어망·자동차 안전벨트 사용. 폐 나일론 어망·카펫·의류 회수 → 카프로락탐 모노머 화학 재활용 → 재생 PA 원사 (ECONYL 류). 단일 합성 화학 재활용 깨끗·자동차 시트·요가복·수영복·산업 로프 다용도. 폐 어망 → 신규 의류 순환 핵심.', NOW(), NOW()),
    ('ELASTANE',  '스판덱스',   'SYNTHETIC', 12.000, 1, '엘라스탄(스판덱스·라이크라) 합성 신축성 섬유, 운동복·요가복·수영복·란제리·언더웨어 사용 (대부분 폴리·나일론과 혼방 5~20%). 폐 운동복·요가복 → 화학 분리 → 엘라스탄 + 폴리/나일론 분리 회수. 스판덱스 분리 한국 특화 기술. 분리 후 → 재생 원사·신규 운동복 다방면. 화학 재활용 핵심.', NOW(), NOW()),
    ('BLEND',     '혼방',       'BLEND',     5.500, 1, '혼방 섬유는 두 가지 이상 소재가 결합된 재고로, 소재별 분리 난이도가 높아 기계적 재활용보다 선별·해체·화학 분리 공정이 중요하다. 면/폴리, 울/나일론, 스판 혼방 등 조성비에 따라 재생 원사·충전재·산업용 부직포·흡음재로 재가공한다.', NOW(), NOW())
    ON DUPLICATE KEY UPDATE
                         name_ko = VALUES(name_ko),
                         material_group = VALUES(material_group),
                         carbon_factor = VALUES(carbon_factor),
                         active = VALUES(active),
                         description = VALUES(description),
                         update_date = NOW();

INSERT INTO product_master
(code, name, category_code, base_price, lead_time_days, warehouse_safety_stock, store_safety_stock, main_vendor_code, status, create_date, update_date)
VALUES
('PRD-TOP-SS-001','코튼 에센셜 크루 반팔','CAT-L2-TOP-SS',19900,6,50,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-002','드라이핏 액티브 반팔','CAT-L2-TOP-SS',21900,7,55,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-001','슬림 베이스 레이어 긴팔','CAT-L2-TOP-LS',24900,7,65,14,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-002','소프트 코튼 롱슬리브','CAT-L2-TOP-LS',26900,8,70,15,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','CAT-L2-TOP-SH',32900,8,80,12,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-002','클린 포플린 셔츠','CAT-L2-TOP-SH',34900,9,50,13,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-001','파인게이지 라운드 니트','CAT-L2-TOP-KN',39900,9,60,10,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-002','캐시 블렌드 크루 니트','CAT-L2-TOP-KN',42900,10,65,11,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-001','헤비웨이트 로고 후드티','CAT-L2-TOP-HD',45900,10,75,14,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-002','브러시드 기모 후드티','CAT-L2-TOP-HD',48900,11,80,15,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-001','스트레이트 인디고 데님','CAT-L2-PNT-DN',49900,11,55,12,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-002','슬림 테이퍼드 데님','CAT-L2-PNT-DN',52900,5,60,13,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-001','코튼 치노 쇼츠','CAT-L2-PNT-ST',29900,5,70,10,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-002','데일리 밴딩 쇼츠','CAT-L2-PNT-ST',31900,6,75,11,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-001','와이드 플리츠 롱팬츠','CAT-L2-PNT-LG',44900,6,50,14,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-002','테일러드 스트레이트 팬츠','CAT-L2-PNT-LG',47900,7,55,15,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-001','소프트 조거 트레이닝','CAT-L2-PNT-TR',38900,7,65,12,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-002','테크 플리스 트랙 팬츠','CAT-L2-PNT-TR',41900,8,70,13,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-001','A라인 코튼 미니 스커트','CAT-L2-SKT-MN',32900,8,80,10,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-002','데님 하이라이즈 미니','CAT-L2-SKT-MN',35900,9,50,11,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-001','플리츠 플로우 롱스커트','CAT-L2-SKT-LG',42900,9,60,14,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-002','새틴 드레이프 롱스커트','CAT-L2-SKT-LG',45900,10,65,15,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-001','라이트 웜 숏 패딩','CAT-L2-OUT-PD',66900,10,75,12,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-002','볼륨넥 덕다운 패딩','CAT-L2-OUT-PD',69900,11,80,13,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-001','소프트 스웻 후드집업','CAT-L2-OUT-HZ',52900,11,55,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-002','테리 루즈핏 후드집업','CAT-L2-OUT-HZ',55900,5,60,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-001','클래식 싱글 자켓','CAT-L2-OUT-JK',64900,5,70,14,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-002','미니멀 크롭 블레이저','CAT-L2-OUT-JK',68900,6,75,15,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-001','브이넥 버튼 가디건','CAT-L2-OUT-CD',45900,6,50,12,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-002','케이블 포인트 가디건','CAT-L2-OUT-CD',48900,7,55,13,'VND-006','ACTIVE',NOW(),NOW())
ON DUPLICATE KEY UPDATE
name=VALUES(name), category_code=VALUES(category_code), base_price=VALUES(base_price), lead_time_days=VALUES(lead_time_days),
warehouse_safety_stock=VALUES(warehouse_safety_stock), store_safety_stock=VALUES(store_safety_stock), main_vendor_code=VALUES(main_vendor_code),
status=VALUES(status), update_date=NOW();

INSERT INTO product_material_composition
(product_id, material_id, ratio, composition_order, create_date, update_date)
SELECT pm.id, m.id, t.ratio, t.composition_order, NOW(), NOW()
FROM (
    SELECT 'PRD-TOP-SS-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
) t
JOIN product_master pm ON pm.code = t.product_code
JOIN material m ON m.code = t.material_code
ON DUPLICATE KEY UPDATE
ratio = VALUES(ratio),
composition_order = VALUES(composition_order),
update_date = NOW();

INSERT INTO product_sku
(sku_code, product_code, color, size, unit_price, status, create_date, update_date)
SELECT
  CONCAT(pm.code, '-', c.color, '-', s.size) AS sku_code,
  pm.code AS product_code,
  c.color,
  s.size,
  pm.base_price AS unit_price,
  'ACTIVE' AS status,
  NOW() AS create_date,
  NOW() AS update_date
FROM product_master pm
CROSS JOIN (
  SELECT 'BLK' AS color
  UNION ALL SELECT 'WHT'
  UNION ALL SELECT 'NVY'
) c
CROSS JOIN (
  SELECT 'S' AS size
  UNION ALL SELECT 'M'
  UNION ALL SELECT 'L'
) s
WHERE pm.status = 'ACTIVE'
ON DUPLICATE KEY UPDATE
  unit_price = VALUES(unit_price),
  status = VALUES(status),
  update_date = NOW();
