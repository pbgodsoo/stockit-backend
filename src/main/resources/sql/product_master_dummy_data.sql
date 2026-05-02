-- 제품 마스터/SKU 더미 데이터 (color/size 구조)
-- 실행 순서: category_two_level_seed.sql + vendor + infrastructure 이후
-- 주의:
-- 1) product_sku 는 option_name/option_value 를 사용하지 않습니다.
-- 2) product_sku 유니크 키는 (product_code, color, size) 기준입니다.

INSERT INTO product_master
(code, name, category_code, base_price, lead_time_days, warehouse_safety_stock, store_safety_stock, main_vendor_code, material_spec, status, create_date, update_date)
VALUES
('PM-2001','코튼 베이직 반팔 티셔츠','CAT-9101',19900,5,120,60,'VND-001','{"materialType":"NATURAL_SINGLE","compositions":[{"materialCode":"COTTON","ratio":100}]}','ACTIVE',NOW(),NOW()),
('PM-2002','슬림핏 긴팔 티셔츠','CAT-9102',22900,5,110,55,'VND-001','{"materialType":"BLEND","compositions":[{"materialCode":"COTTON","ratio":80},{"materialCode":"ELASTANE","ratio":20}]}','ACTIVE',NOW(),NOW()),
('PM-2003','오버핏 옥스포드 셔츠','CAT-9103',39900,7,100,45,'VND-002','{"materialType":"NATURAL_SINGLE","compositions":[{"materialCode":"COTTON","ratio":100}]}','ACTIVE',NOW(),NOW()),
('PM-2004','라운드넥 소프트 니트','CAT-9104',49900,8,90,40,'VND-005','{"materialType":"BLEND","compositions":[{"materialCode":"WOOL","ratio":60},{"materialCode":"ACRYLIC","ratio":40}]}','ACTIVE',NOW(),NOW()),
('PM-2005','헤비웨이트 로고 후드티','CAT-9105',45900,6,100,50,'VND-006','{"materialType":"BLEND","compositions":[{"materialCode":"COTTON","ratio":70},{"materialCode":"POLYESTER","ratio":30}]}','ACTIVE',NOW(),NOW()),
('PM-2006','스트레이트 워싱 데님','CAT-9201',55900,9,95,35,'VND-003','{"materialType":"BLEND","compositions":[{"materialCode":"COTTON","ratio":98},{"materialCode":"ELASTANE","ratio":2}]}','ACTIVE',NOW(),NOW()),
('PM-2007','라이트 코튼 쇼츠','CAT-9202',32900,6,85,30,'VND-004','{"materialType":"NATURAL_SINGLE","compositions":[{"materialCode":"COTTON","ratio":100}]}','ACTIVE',NOW(),NOW()),
('PM-2008','와이드 밴딩 팬츠','CAT-9203',37900,6,90,30,'VND-004','{"materialType":"BLEND","compositions":[{"materialCode":"POLYESTER","ratio":65},{"materialCode":"RAYON","ratio":35}]}','ACTIVE',NOW(),NOW()),
('PM-2009','A라인 데님 미니스커트','CAT-9301',34900,7,70,30,'VND-008','{"materialType":"BLEND","compositions":[{"materialCode":"COTTON","ratio":99},{"materialCode":"ELASTANE","ratio":1}]}','ACTIVE',NOW(),NOW()),
('PM-2010','플리츠 롱스커트','CAT-9302',42900,7,75,28,'VND-008','{"materialType":"SYNTHETIC","compositions":[{"materialCode":"POLYESTER","ratio":100}]}','ACTIVE',NOW(),NOW()),
('PM-2011','라이트 숏 패딩','CAT-9401',89900,12,80,25,'VND-007','{"materialType":"SYNTHETIC","compositions":[{"materialCode":"POLYESTER","ratio":100}]}','ACTIVE',NOW(),NOW()),
('PM-2012','싱글 브레스트 자켓','CAT-9403',79900,10,65,22,'VND-006','{"materialType":"BLEND","compositions":[{"materialCode":"WOOL","ratio":50},{"materialCode":"POLYESTER","ratio":50}]}','ACTIVE',NOW(),NOW())
ON DUPLICATE KEY UPDATE
name=VALUES(name), category_code=VALUES(category_code), base_price=VALUES(base_price), lead_time_days=VALUES(lead_time_days),
warehouse_safety_stock=VALUES(warehouse_safety_stock), store_safety_stock=VALUES(store_safety_stock), main_vendor_code=VALUES(main_vendor_code),
material_spec=VALUES(material_spec), status=VALUES(status), update_date=NOW();

INSERT INTO product_sku
(sku_code, product_code, color, size, unit_price, status, create_date, update_date)
VALUES
('PS-3001','PM-2001','검정','S',19900,'ACTIVE',NOW(),NOW()),('PS-3002','PM-2001','검정','M',19900,'ACTIVE',NOW(),NOW()),('PS-3003','PM-2001','검정','L',19900,'ACTIVE',NOW(),NOW()),
('PS-3004','PM-2001','흰색','S',19900,'ACTIVE',NOW(),NOW()),('PS-3005','PM-2001','흰색','M',19900,'ACTIVE',NOW(),NOW()),('PS-3006','PM-2001','흰색','L',19900,'ACTIVE',NOW(),NOW()),
('PS-3007','PM-2002','검정','M',22900,'ACTIVE',NOW(),NOW()),('PS-3008','PM-2002','검정','L',22900,'ACTIVE',NOW(),NOW()),('PS-3009','PM-2002','아이보리','M',22900,'ACTIVE',NOW(),NOW()),
('PS-3010','PM-2003','흰색','M',39900,'ACTIVE',NOW(),NOW()),('PS-3011','PM-2003','흰색','L',39900,'ACTIVE',NOW(),NOW()),('PS-3012','PM-2003','블루','L',39900,'ACTIVE',NOW(),NOW()),
('PS-3013','PM-2004','그레이','M',49900,'ACTIVE',NOW(),NOW()),('PS-3014','PM-2004','그레이','L',49900,'ACTIVE',NOW(),NOW()),('PS-3015','PM-2004','네이비','L',49900,'ACTIVE',NOW(),NOW()),
('PS-3016','PM-2005','검정','M',45900,'ACTIVE',NOW(),NOW()),('PS-3017','PM-2005','검정','L',45900,'ACTIVE',NOW(),NOW()),('PS-3018','PM-2005','그레이','L',45900,'ACTIVE',NOW(),NOW()),
('PS-3019','PM-2006','인디고','30',55900,'ACTIVE',NOW(),NOW()),('PS-3020','PM-2006','인디고','32',55900,'ACTIVE',NOW(),NOW()),('PS-3021','PM-2006','블랙','32',55900,'ACTIVE',NOW(),NOW()),
('PS-3022','PM-2007','베이지','M',32900,'ACTIVE',NOW(),NOW()),('PS-3023','PM-2007','베이지','L',32900,'ACTIVE',NOW(),NOW()),('PS-3024','PM-2007','카키','L',32900,'ACTIVE',NOW(),NOW()),
('PS-3025','PM-2008','블랙','M',37900,'ACTIVE',NOW(),NOW()),('PS-3026','PM-2008','블랙','L',37900,'ACTIVE',NOW(),NOW()),('PS-3027','PM-2008','차콜','L',37900,'ACTIVE',NOW(),NOW()),
('PS-3028','PM-2009','데님','S',34900,'ACTIVE',NOW(),NOW()),('PS-3029','PM-2009','데님','M',34900,'ACTIVE',NOW(),NOW()),('PS-3030','PM-2009','데님','L',34900,'ACTIVE',NOW(),NOW()),
('PS-3031','PM-2010','블랙','S',42900,'ACTIVE',NOW(),NOW()),('PS-3032','PM-2010','블랙','M',42900,'ACTIVE',NOW(),NOW()),('PS-3033','PM-2010','아이보리','M',42900,'ACTIVE',NOW(),NOW()),
('PS-3034','PM-2011','블랙','M',89900,'ACTIVE',NOW(),NOW()),('PS-3035','PM-2011','블랙','L',89900,'ACTIVE',NOW(),NOW()),('PS-3036','PM-2011','카키','L',89900,'ACTIVE',NOW(),NOW()),
('PS-3037','PM-2012','그레이','M',79900,'ACTIVE',NOW(),NOW()),('PS-3038','PM-2012','그레이','L',79900,'ACTIVE',NOW(),NOW()),('PS-3039','PM-2012','네이비','L',79900,'ACTIVE',NOW(),NOW())
ON DUPLICATE KEY UPDATE
product_code=VALUES(product_code), color=VALUES(color), size=VALUES(size), unit_price=VALUES(unit_price), status=VALUES(status), update_date=NOW();
