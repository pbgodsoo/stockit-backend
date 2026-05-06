-- 제품 마스터/SKU/소재 더미 데이터
-- 실행 순서: category_two_level_seed.sql + vendor + infrastructure 이후

INSERT INTO material
(code, name_ko, material_group, active, create_date, update_date)
VALUES
('COTTON','면','NATURAL',1,NOW(),NOW()),
('WOOL','울','NATURAL',1,NOW(),NOW()),
('CASHMERE','캐시미어','NATURAL',1,NOW(),NOW()),
('SILK','실크','NATURAL',1,NOW(),NOW()),
('LINEN','린넨','NATURAL',1,NOW(),NOW()),
('POLYESTER','폴리에스터','SYNTHETIC',1,NOW(),NOW()),
('ACRYLIC','아크릴','SYNTHETIC',1,NOW(),NOW()),
('POLYAMIDE','나일론','SYNTHETIC',1,NOW(),NOW()),
('ELASTANE','스판덱스','SYNTHETIC',1,NOW(),NOW()),
('RAYON','레이온','SYNTHETIC',1,NOW(),NOW())
ON DUPLICATE KEY UPDATE
name_ko=VALUES(name_ko), material_group=VALUES(material_group), active=VALUES(active), update_date=NOW();

INSERT INTO product_master
(code, name, category_code, base_price, lead_time_days, warehouse_safety_stock, store_safety_stock, main_vendor_code, status, create_date, update_date)
VALUES
('PRD-TOP-SS-001','에어리 코튼 크루넥 반팔','CAT-L2-TOP-SS',21100,6,90,30,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-002','소프트 터치 슬림 반팔','CAT-L2-TOP-SS',22300,7,100,35,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-003','드라이핏 데일리 반팔','CAT-L2-TOP-SS',23500,8,110,40,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-001','컴포트 스트레치 긴팔','CAT-L2-TOP-LS',24700,9,120,45,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-002','라이트 모달 롱슬리브','CAT-L2-TOP-LS',25900,10,130,25,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-003','베이직 레이어드 긴팔','CAT-L2-TOP-LS',27100,11,140,30,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-001','클린 옥스포드 버튼 셔츠','CAT-L2-TOP-SH',28300,5,80,35,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-002','루즈핏 코튼 포플린 셔츠','CAT-L2-TOP-SH',29500,6,90,40,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-003','미니멀 히든버튼 셔츠','CAT-L2-TOP-SH',30700,7,100,45,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-001','파인게이지 라운드 니트','CAT-L2-TOP-KN',31900,8,110,25,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-002','소프트 브러시드 니트','CAT-L2-TOP-KN',33100,9,120,30,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-003','에센셜 립 조직 니트','CAT-L2-TOP-KN',34300,10,130,35,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-001','헤비코튼 로고 후드티','CAT-L2-TOP-HD',35500,11,140,40,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-002','기모 스웻 풀오버 후드','CAT-L2-TOP-HD',36700,5,80,45,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-003','오버핏 워시드 후드티','CAT-L2-TOP-HD',37900,6,90,25,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-001','스트레이트 인디고 데님','CAT-L2-PNT-DN',39100,7,100,30,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-002','슬림 테이퍼드 블랙 데님','CAT-L2-PNT-DN',40300,8,110,35,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-003','릴랙스 워싱 데님 팬츠','CAT-L2-PNT-DN',41500,9,120,40,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-001','코튼 치노 버뮤다 쇼츠','CAT-L2-PNT-ST',42700,10,130,45,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-002','이지 밴딩 하프 팬츠','CAT-L2-PNT-ST',43900,11,140,25,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-003','데일리 카고 쇼츠','CAT-L2-PNT-ST',45100,5,80,30,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-001','와이드 플리츠 롱팬츠','CAT-L2-PNT-LG',46300,6,90,35,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-002','세미와이드 테일러드 팬츠','CAT-L2-PNT-LG',47500,7,100,40,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-003','컴포트 백밴딩 슬랙스','CAT-L2-PNT-LG',48700,8,110,45,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-001','소프트 조거 트레이닝','CAT-L2-PNT-TR',49900,9,120,25,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-002','테크 플리스 트랙 팬츠','CAT-L2-PNT-TR',51100,10,130,30,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-003','스트레치 코지 스웻팬츠','CAT-L2-PNT-TR',52300,11,140,35,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-001','A라인 코튼 미니 스커트','CAT-L2-SKT-MN',53500,5,80,40,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-002','하이웨이스트 데님 미니','CAT-L2-SKT-MN',54700,6,90,45,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-003','플랩포켓 트윌 미니스커트','CAT-L2-SKT-MN',55900,7,100,25,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-001','플리츠 플로우 롱스커트','CAT-L2-SKT-LG',57100,8,110,30,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-002','새틴 드레이프 롱스커트','CAT-L2-SKT-LG',58300,9,120,35,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-003','니트 밴딩 맥시 스커트','CAT-L2-SKT-LG',59500,10,130,40,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-001','라이트 웜 숏 패딩','CAT-L2-OUT-PD',60700,11,140,45,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-002','볼륨넥 덕다운 패딩','CAT-L2-OUT-PD',61900,5,80,25,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-003','에센셜 퀼팅 패딩 점퍼','CAT-L2-OUT-PD',63100,6,90,30,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-001','소프트 스웻 후드 집업','CAT-L2-OUT-HZ',64300,7,100,35,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-002','테리 루즈핏 후드 집업','CAT-L2-OUT-HZ',65500,8,110,40,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-003','트랙라인 코튼 집업 후디','CAT-L2-OUT-HZ',66700,9,120,45,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-001','클래식 싱글 브레스트 자켓','CAT-L2-OUT-JK',67900,10,130,25,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-002','미니멀 크롭 블레이저','CAT-L2-OUT-JK',69100,11,140,30,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-003','유틸리티 포켓 필드 자켓','CAT-L2-OUT-JK',70300,5,80,35,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-001','브이넥 버튼 니트 가디건','CAT-L2-OUT-CD',71500,6,90,40,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-002','케이블 포인트 가디건','CAT-L2-OUT-CD',72700,7,100,45,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-003','오픈프론트 롱 가디건','CAT-L2-OUT-CD',73900,8,110,25,'VND-005','ACTIVE',NOW(),NOW())
ON DUPLICATE KEY UPDATE
name=VALUES(name), category_code=VALUES(category_code), base_price=VALUES(base_price), lead_time_days=VALUES(lead_time_days),
warehouse_safety_stock=VALUES(warehouse_safety_stock), store_safety_stock=VALUES(store_safety_stock), main_vendor_code=VALUES(main_vendor_code),
status=VALUES(status), update_date=NOW();

INSERT INTO product_material_composition
(product_id, material_id, ratio, composition_order, create_date, update_date)
SELECT pm.id, m.id, t.ratio, t.composition_order, NOW(), NOW()
FROM (
    SELECT 'PRD-TOP-SS-001' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-SS-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-002','RAYON',30,2
    UNION ALL
    SELECT 'PRD-TOP-SS-003' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-LS-001' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-LS-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-002','POLYAMIDE',30,2
    UNION ALL
    SELECT 'PRD-TOP-LS-003' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-SH-001' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-SH-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-002','ELASTANE',30,2
    UNION ALL
    SELECT 'PRD-TOP-SH-003' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-KN-001' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-KN-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-002','POLYESTER',30,2
    UNION ALL
    SELECT 'PRD-TOP-KN-003' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-HD-001' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-TOP-HD-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-002','ACRYLIC',30,2
    UNION ALL
    SELECT 'PRD-TOP-HD-003' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-PNT-DN-001' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-PNT-DN-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-002','RAYON',30,2
    UNION ALL
    SELECT 'PRD-PNT-DN-003' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-PNT-ST-001' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-PNT-ST-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-002','POLYAMIDE',30,2
    UNION ALL
    SELECT 'PRD-PNT-ST-003' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-PNT-LG-001' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-PNT-LG-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-002','ELASTANE',30,2
    UNION ALL
    SELECT 'PRD-PNT-LG-003' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-PNT-TR-001' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-PNT-TR-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-002','POLYESTER',30,2
    UNION ALL
    SELECT 'PRD-PNT-TR-003' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-SKT-MN-001' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-SKT-MN-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-002','ACRYLIC',30,2
    UNION ALL
    SELECT 'PRD-SKT-MN-003' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-SKT-LG-001' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-SKT-LG-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-002','RAYON',30,2
    UNION ALL
    SELECT 'PRD-SKT-LG-003' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-OUT-PD-001' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-OUT-PD-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-002','POLYAMIDE',30,2
    UNION ALL
    SELECT 'PRD-OUT-PD-003' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-OUT-HZ-001' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-OUT-HZ-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-002','ELASTANE',30,2
    UNION ALL
    SELECT 'PRD-OUT-HZ-003' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-OUT-JK-001' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-OUT-JK-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-002','POLYESTER',30,2
    UNION ALL
    SELECT 'PRD-OUT-JK-003' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-OUT-CD-001' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL
    SELECT 'PRD-OUT-CD-002' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-002','ACRYLIC',30,2
    UNION ALL
    SELECT 'PRD-OUT-CD-003' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
) t
JOIN product_master pm ON pm.code = t.product_code
JOIN material m ON m.code = t.material_code
ON DUPLICATE KEY UPDATE
ratio = VALUES(ratio),
composition_order = VALUES(composition_order),
update_date = NOW();

INSERT INTO product_sku
(sku_code, product_code, color, size, unit_price, status, create_date, update_date)
VALUES
('PRD-TOP-SS-001-WHT-S','PRD-TOP-SS-001','WHT','S',21100,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-001-GRY-XL','PRD-TOP-SS-001','GRY','XL',21100,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-002-NVY-M','PRD-TOP-SS-002','NVY','M',22300,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-002-BLK-XS','PRD-TOP-SS-002','BLK','XS',22300,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-003-GRY-L','PRD-TOP-SS-003','GRY','L',23500,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-003-WHT-S','PRD-TOP-SS-003','WHT','S',23500,'ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-001-BLK-XL','PRD-TOP-LS-001','BLK','XL',24700,'ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-001-NVY-M','PRD-TOP-LS-001','NVY','M',24700,'ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-002-WHT-XS','PRD-TOP-LS-002','WHT','XS',25900,'ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-002-GRY-L','PRD-TOP-LS-002','GRY','L',25900,'ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-003-NVY-S','PRD-TOP-LS-003','NVY','S',27100,'ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-003-BLK-XL','PRD-TOP-LS-003','BLK','XL',27100,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-001-GRY-M','PRD-TOP-SH-001','GRY','M',28300,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-001-WHT-XS','PRD-TOP-SH-001','WHT','XS',28300,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-002-BLK-L','PRD-TOP-SH-002','BLK','L',29500,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-002-NVY-S','PRD-TOP-SH-002','NVY','S',29500,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-003-WHT-XL','PRD-TOP-SH-003','WHT','XL',30700,'ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-003-GRY-M','PRD-TOP-SH-003','GRY','M',30700,'ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-001-NVY-XS','PRD-TOP-KN-001','NVY','XS',31900,'ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-001-BLK-L','PRD-TOP-KN-001','BLK','L',31900,'ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-002-GRY-S','PRD-TOP-KN-002','GRY','S',33100,'ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-002-WHT-XL','PRD-TOP-KN-002','WHT','XL',33100,'ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-003-BLK-M','PRD-TOP-KN-003','BLK','M',34300,'ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-003-NVY-XS','PRD-TOP-KN-003','NVY','XS',34300,'ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-001-WHT-L','PRD-TOP-HD-001','WHT','L',35500,'ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-001-GRY-S','PRD-TOP-HD-001','GRY','S',35500,'ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-002-NVY-XL','PRD-TOP-HD-002','NVY','XL',36700,'ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-002-BLK-M','PRD-TOP-HD-002','BLK','M',36700,'ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-003-GRY-XS','PRD-TOP-HD-003','GRY','XS',37900,'ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-003-WHT-L','PRD-TOP-HD-003','WHT','L',37900,'ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-001-BLK-S','PRD-PNT-DN-001','BLK','S',39100,'ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-001-NVY-XL','PRD-PNT-DN-001','NVY','XL',39100,'ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-002-WHT-M','PRD-PNT-DN-002','WHT','M',40300,'ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-002-GRY-XS','PRD-PNT-DN-002','GRY','XS',40300,'ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-003-NVY-L','PRD-PNT-DN-003','NVY','L',41500,'ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-003-BLK-S','PRD-PNT-DN-003','BLK','S',41500,'ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-001-GRY-XL','PRD-PNT-ST-001','GRY','XL',42700,'ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-001-WHT-M','PRD-PNT-ST-001','WHT','M',42700,'ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-002-BLK-XS','PRD-PNT-ST-002','BLK','XS',43900,'ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-002-NVY-L','PRD-PNT-ST-002','NVY','L',43900,'ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-003-WHT-S','PRD-PNT-ST-003','WHT','S',45100,'ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-003-GRY-XL','PRD-PNT-ST-003','GRY','XL',45100,'ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-001-NVY-M','PRD-PNT-LG-001','NVY','M',46300,'ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-001-BLK-XS','PRD-PNT-LG-001','BLK','XS',46300,'ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-002-GRY-L','PRD-PNT-LG-002','GRY','L',47500,'ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-002-WHT-S','PRD-PNT-LG-002','WHT','S',47500,'ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-003-BLK-XL','PRD-PNT-LG-003','BLK','XL',48700,'ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-003-NVY-M','PRD-PNT-LG-003','NVY','M',48700,'ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-001-WHT-XS','PRD-PNT-TR-001','WHT','XS',49900,'ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-001-GRY-L','PRD-PNT-TR-001','GRY','L',49900,'ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-002-NVY-S','PRD-PNT-TR-002','NVY','S',51100,'ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-002-BLK-XL','PRD-PNT-TR-002','BLK','XL',51100,'ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-003-GRY-M','PRD-PNT-TR-003','GRY','M',52300,'ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-003-WHT-XS','PRD-PNT-TR-003','WHT','XS',52300,'ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-001-BLK-L','PRD-SKT-MN-001','BLK','L',53500,'ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-001-NVY-S','PRD-SKT-MN-001','NVY','S',53500,'ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-002-WHT-XL','PRD-SKT-MN-002','WHT','XL',54700,'ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-002-GRY-M','PRD-SKT-MN-002','GRY','M',54700,'ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-003-NVY-XS','PRD-SKT-MN-003','NVY','XS',55900,'ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-003-BLK-L','PRD-SKT-MN-003','BLK','L',55900,'ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-001-GRY-S','PRD-SKT-LG-001','GRY','S',57100,'ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-001-WHT-XL','PRD-SKT-LG-001','WHT','XL',57100,'ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-002-BLK-M','PRD-SKT-LG-002','BLK','M',58300,'ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-002-NVY-XS','PRD-SKT-LG-002','NVY','XS',58300,'ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-003-WHT-L','PRD-SKT-LG-003','WHT','L',59500,'ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-003-GRY-S','PRD-SKT-LG-003','GRY','S',59500,'ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-001-NVY-XL','PRD-OUT-PD-001','NVY','XL',60700,'ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','BLK','M',60700,'ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-002-GRY-XS','PRD-OUT-PD-002','GRY','XS',61900,'ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-002-WHT-L','PRD-OUT-PD-002','WHT','L',61900,'ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-003-BLK-S','PRD-OUT-PD-003','BLK','S',63100,'ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-003-NVY-XL','PRD-OUT-PD-003','NVY','XL',63100,'ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-001-WHT-M','PRD-OUT-HZ-001','WHT','M',64300,'ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-001-GRY-XS','PRD-OUT-HZ-001','GRY','XS',64300,'ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-002-NVY-L','PRD-OUT-HZ-002','NVY','L',65500,'ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-002-BLK-S','PRD-OUT-HZ-002','BLK','S',65500,'ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-003-GRY-XL','PRD-OUT-HZ-003','GRY','XL',66700,'ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-003-WHT-M','PRD-OUT-HZ-003','WHT','M',66700,'ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-001-BLK-XS','PRD-OUT-JK-001','BLK','XS',67900,'ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-001-NVY-L','PRD-OUT-JK-001','NVY','L',67900,'ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-002-WHT-S','PRD-OUT-JK-002','WHT','S',69100,'ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-002-GRY-XL','PRD-OUT-JK-002','GRY','XL',69100,'ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-003-NVY-M','PRD-OUT-JK-003','NVY','M',70300,'ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-003-BLK-XS','PRD-OUT-JK-003','BLK','XS',70300,'ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-001-GRY-L','PRD-OUT-CD-001','GRY','L',71500,'ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-001-WHT-S','PRD-OUT-CD-001','WHT','S',71500,'ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-002-BLK-XL','PRD-OUT-CD-002','BLK','XL',72700,'ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-002-NVY-M','PRD-OUT-CD-002','NVY','M',72700,'ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-003-WHT-XS','PRD-OUT-CD-003','WHT','XS',73900,'ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-003-GRY-L','PRD-OUT-CD-003','GRY','L',73900,'ACTIVE',NOW(),NOW())
ON DUPLICATE KEY UPDATE
product_code=VALUES(product_code), color=VALUES(color), size=VALUES(size), unit_price=VALUES(unit_price), status=VALUES(status), update_date=NOW();
