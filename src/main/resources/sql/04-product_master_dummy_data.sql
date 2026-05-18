-- 제품 마스터/SKU/소재 더미 데이터
-- 실행 순서: category_two_level_seed.sql + vendor + infrastructure 이후

INSERT INTO material
(code, name_ko, material_group, carbon_factor, active, create_date, update_date)
VALUES
('COTTON','면','NATURAL', 1.800,1,NOW(),NOW()),
('WOOL','울','NATURAL',1.200, 1,NOW(),NOW()),
('CASHMERE','캐시미어','NATURAL',1.300, 1,NOW(),NOW()),
('SILK','실크','NATURAL',1.300, 1,NOW(),NOW()),
('LINEN','린넨','NATURAL',1.700, 1,NOW(),NOW()),
('POLYESTER','폴리에스터','SYNTHETIC', 2.300, 1,NOW(),NOW()),
('ACRYLIC','아크릴','SYNTHETIC',2.400, 1,NOW(),NOW()),
('POLYAMIDE','나일론','SYNTHETIC',2.500, 1,NOW(),NOW()),
('ELASTANE','스판덱스','SYNTHETIC',2.200, 1,NOW(),NOW()),
('RAYON','레이온','SYNTHETIC',2.700,1,NOW(),NOW())
ON DUPLICATE KEY UPDATE
name_ko=VALUES(name_ko), material_group=VALUES(material_group), active=VALUES(active), update_date=NOW();

INSERT INTO product_master
(code, name, category_code, base_price, lead_time_days, warehouse_safety_stock, store_safety_stock, main_vendor_code, status, create_date, update_date)
VALUES
('PRD-TOP-SS-001','코튼 에센셜 크루 반팔','CAT-L2-TOP-SS',21100,6,50,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-002','드라이핏 액티브 반팔','CAT-L2-TOP-SS',22300,7,55,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-003','실켓 터치 라운드 반팔','CAT-L2-TOP-SS',23500,8,60,12,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-004','오버핏 그래픽 반팔','CAT-L2-TOP-SS',24700,9,65,13,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-005','쿨맥스 베이직 반팔','CAT-L2-TOP-SS',25900,10,70,14,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-006','미니로고 포인트 반팔','CAT-L2-TOP-SS',27100,11,75,15,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-007','피그먼트 워시 반팔','CAT-L2-TOP-SS',28300,5,80,10,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-008','소프트 슬럽 반팔','CAT-L2-TOP-SS',29500,6,50,11,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-009','스트레치 레이어 반팔','CAT-L2-TOP-SS',30700,7,55,12,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-SS-010','에어리 컴포트 반팔','CAT-L2-TOP-SS',31900,8,60,13,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-001','슬림 베이스 레이어 긴팔','CAT-L2-TOP-LS',33100,7,65,14,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-002','소프트 코튼 롱슬리브','CAT-L2-TOP-LS',34300,8,70,15,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-003','스트레치 핏 롱티','CAT-L2-TOP-LS',35500,9,75,10,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-004','와플 텍스처 긴팔','CAT-L2-TOP-LS',36700,10,80,11,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-005','라이트 웜업 긴팔','CAT-L2-TOP-LS',37900,11,50,12,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-006','미니멀 실루엣 긴팔','CAT-L2-TOP-LS',39100,5,55,13,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-007','데일리 헤비코튼 긴팔','CAT-L2-TOP-LS',40300,6,60,14,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-008','모달 터치 롱티','CAT-L2-TOP-LS',41500,7,65,15,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-009','컴포트 립 롱슬리브','CAT-L2-TOP-LS',42700,8,70,10,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-LS-010','심플 로고 긴팔','CAT-L2-TOP-LS',43900,9,75,11,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','CAT-L2-TOP-SH',45100,8,80,12,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-002','클린 포플린 셔츠','CAT-L2-TOP-SH',46300,9,50,13,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-003','릴랙스 스트라이프 셔츠','CAT-L2-TOP-SH',47500,10,55,14,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-004','미니멀 히든버튼 셔츠','CAT-L2-TOP-SH',48700,11,60,15,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-005','코튼 트윌 셔츠','CAT-L2-TOP-SH',49900,5,65,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-006','루즈 핏 체크 셔츠','CAT-L2-TOP-SH',51100,6,70,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-007','스탠다드 솔리드 셔츠','CAT-L2-TOP-SH',52300,7,75,12,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-008','소프트 브러시 셔츠','CAT-L2-TOP-SH',53500,8,80,13,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-009','유틸리티 포켓 셔츠','CAT-L2-TOP-SH',54700,9,50,14,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-SH-010','세미오버 베이직 셔츠','CAT-L2-TOP-SH',55900,10,55,15,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-001','파인게이지 라운드 니트','CAT-L2-TOP-KN',57100,9,60,10,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-002','캐시 블렌드 크루 니트','CAT-L2-TOP-KN',58300,10,65,11,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-003','립 조직 하프넥 니트','CAT-L2-TOP-KN',59500,11,70,12,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-004','소프트 터치 브이넥 니트','CAT-L2-TOP-KN',60700,5,75,13,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-005','울 블렌드 베이직 니트','CAT-L2-TOP-KN',61900,6,80,14,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-006','오버핏 케이블 니트','CAT-L2-TOP-KN',63100,7,50,15,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-007','미니멀 슬릿 니트','CAT-L2-TOP-KN',64300,8,55,10,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-008','코지 하이넥 니트','CAT-L2-TOP-KN',65500,9,60,11,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-009','라이트 웨이트 니트','CAT-L2-TOP-KN',66700,10,65,12,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-TOP-KN-010','데일리 버튼 니트','CAT-L2-TOP-KN',67900,11,70,13,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-001','헤비웨이트 로고 후드티','CAT-L2-TOP-HD',69100,10,75,14,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-002','브러시드 기모 후드티','CAT-L2-TOP-HD',70300,11,80,15,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-003','오버핏 워시드 후드티','CAT-L2-TOP-HD',71500,5,50,10,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-004','에센셜 풀오버 후드','CAT-L2-TOP-HD',72700,6,55,11,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-005','코튼 플리스 후드티','CAT-L2-TOP-HD',73900,7,60,12,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-006','미니자수 포인트 후드','CAT-L2-TOP-HD',75100,8,65,13,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-007','소프트 테리 후드티','CAT-L2-TOP-HD',76300,9,70,14,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-008','유틸리티 포켓 후드','CAT-L2-TOP-HD',77500,10,75,15,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-009','리브 트림 후드티','CAT-L2-TOP-HD',78700,11,80,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-TOP-HD-010','컴포트 루즈 후드티','CAT-L2-TOP-HD',79900,5,50,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-001','스트레이트 인디고 데님','CAT-L2-PNT-DN',81100,11,55,12,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-002','슬림 테이퍼드 데님','CAT-L2-PNT-DN',82300,5,60,13,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-003','와이드 워시드 데님','CAT-L2-PNT-DN',83500,6,65,14,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-004','세미부츠컷 데님','CAT-L2-PNT-DN',84700,7,70,15,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-005','크롭 앵클 데님','CAT-L2-PNT-DN',85900,8,75,10,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-006','릴랙스 핏 데님','CAT-L2-PNT-DN',87100,9,80,11,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-007','다크톤 클래식 데님','CAT-L2-PNT-DN',88300,10,50,12,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-008','하이라이즈 스탠다드 데님','CAT-L2-PNT-DN',89500,11,55,13,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-009','소프트 스트레치 데님','CAT-L2-PNT-DN',90700,5,60,14,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-PNT-DN-010','유즈드 워싱 데님','CAT-L2-PNT-DN',91900,6,65,15,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-001','코튼 치노 쇼츠','CAT-L2-PNT-ST',93100,5,70,10,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-002','데일리 밴딩 쇼츠','CAT-L2-PNT-ST',94300,6,75,11,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-003','유틸리티 카고 쇼츠','CAT-L2-PNT-ST',95500,7,80,12,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-004','린넨 블렌드 쇼츠','CAT-L2-PNT-ST',96700,8,50,13,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-005','테리 컴포트 쇼츠','CAT-L2-PNT-ST',97900,9,55,14,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-006','라이트 트래블 쇼츠','CAT-L2-PNT-ST',99100,10,60,15,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-007','포켓 디테일 쇼츠','CAT-L2-PNT-ST',100300,11,65,10,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-008','스탠다드 하프 팬츠','CAT-L2-PNT-ST',101500,5,70,11,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-009','스트레치 액티브 쇼츠','CAT-L2-PNT-ST',102700,6,75,12,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-PNT-ST-010','쿨터치 나일론 쇼츠','CAT-L2-PNT-ST',103900,7,80,13,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-001','와이드 플리츠 롱팬츠','CAT-L2-PNT-LG',105100,6,50,14,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-002','테일러드 스트레이트 팬츠','CAT-L2-PNT-LG',106300,7,55,15,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-003','컴포트 백밴딩 슬랙스','CAT-L2-PNT-LG',107500,8,60,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-004','세미와이드 드레스 팬츠','CAT-L2-PNT-LG',108700,9,65,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-005','이지핏 코튼 팬츠','CAT-L2-PNT-LG',109900,10,70,12,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-006','미니멀 테이퍼드 팬츠','CAT-L2-PNT-LG',111100,11,75,13,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-007','소프트 터치 롱팬츠','CAT-L2-PNT-LG',112300,5,80,14,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-008','투턱 클래식 팬츠','CAT-L2-PNT-LG',113500,6,50,15,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-009','스트레치 데일리 팬츠','CAT-L2-PNT-LG',114700,7,55,10,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-PNT-LG-010','릴랙스 밴딩 롱팬츠','CAT-L2-PNT-LG',115900,8,60,11,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-001','소프트 조거 트레이닝','CAT-L2-PNT-TR',117100,7,65,12,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-002','테크 플리스 트랙 팬츠','CAT-L2-PNT-TR',118300,8,70,13,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-003','코지 스웻 팬츠','CAT-L2-PNT-TR',119500,9,75,14,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-004','사이드라인 트랙 팬츠','CAT-L2-PNT-TR',120700,10,80,15,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-005','헤비코튼 조거 팬츠','CAT-L2-PNT-TR',121900,11,50,10,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-006','컴포트 밴딩 트레이닝','CAT-L2-PNT-TR',123100,5,55,11,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-007','라이트 웜업 팬츠','CAT-L2-PNT-TR',124300,6,60,12,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-008','에센셜 스웨트 조거','CAT-L2-PNT-TR',125500,7,65,13,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-009','스트레치 액티브 팬츠','CAT-L2-PNT-TR',126700,8,70,14,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-PNT-TR-010','데일리 릴랙스 조거','CAT-L2-PNT-TR',127900,9,75,15,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-001','A라인 코튼 미니 스커트','CAT-L2-SKT-MN',129100,8,80,10,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-002','데님 하이라이즈 미니','CAT-L2-SKT-MN',130300,9,50,11,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-003','트윌 포켓 미니 스커트','CAT-L2-SKT-MN',131500,10,55,12,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-004','플리츠 테니스 미니','CAT-L2-SKT-MN',132700,11,60,13,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-005','랩 디테일 미니 스커트','CAT-L2-SKT-MN',133900,5,65,14,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-006','버튼 포인트 미니 스커트','CAT-L2-SKT-MN',135100,6,70,15,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-007','슬림핏 베이직 미니','CAT-L2-SKT-MN',136300,7,75,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-008','카고 스타일 미니 스커트','CAT-L2-SKT-MN',137500,8,80,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-009','스탠다드 밴딩 미니','CAT-L2-SKT-MN',138700,9,50,12,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-SKT-MN-010','코듀로이 미니 스커트','CAT-L2-SKT-MN',139900,10,55,13,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-001','플리츠 플로우 롱스커트','CAT-L2-SKT-LG',141100,9,60,14,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-002','새틴 드레이프 롱스커트','CAT-L2-SKT-LG',142300,10,65,15,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-003','니트 밴딩 맥시 스커트','CAT-L2-SKT-LG',143500,11,70,10,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-004','A라인 롱스커트','CAT-L2-SKT-LG',144700,5,75,11,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-005','슬릿 포인트 롱스커트','CAT-L2-SKT-LG',145900,6,80,12,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-006','티어드 코튼 롱스커트','CAT-L2-SKT-LG',147100,7,50,13,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-007','미니멀 스트레이트 롱스커트','CAT-L2-SKT-LG',148300,8,55,14,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-008','랩 스타일 롱스커트','CAT-L2-SKT-LG',149500,9,60,15,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-009','소프트 텍스처 롱스커트','CAT-L2-SKT-LG',150700,10,65,10,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-SKT-LG-010','컴포트 웨이스트 롱스커트','CAT-L2-SKT-LG',151900,11,70,11,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-001','라이트 웜 숏 패딩','CAT-L2-OUT-PD',153100,10,75,12,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-002','볼륨넥 덕다운 패딩','CAT-L2-OUT-PD',154300,11,80,13,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-003','에센셜 퀼팅 패딩','CAT-L2-OUT-PD',155500,5,50,14,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-004','하이넥 미니 패딩','CAT-L2-OUT-PD',156700,6,55,15,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-005','컴팩트 경량 패딩','CAT-L2-OUT-PD',157900,7,60,10,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-006','데일리 후드 패딩','CAT-L2-OUT-PD',159100,8,65,11,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-007','소프트 매트 패딩','CAT-L2-OUT-PD',160300,9,70,12,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-008','유틸리티 포켓 패딩','CAT-L2-OUT-PD',161500,10,75,13,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-009','윈터 베이직 패딩','CAT-L2-OUT-PD',162700,11,80,14,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-OUT-PD-010','크롭 실루엣 패딩','CAT-L2-OUT-PD',163900,5,50,15,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-001','소프트 스웻 후드집업','CAT-L2-OUT-HZ',165100,11,55,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-002','테리 루즈핏 후드집업','CAT-L2-OUT-HZ',166300,5,60,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-003','트랙라인 코튼 집업','CAT-L2-OUT-HZ',167500,6,65,12,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-004','미니로고 데일리 집업','CAT-L2-OUT-HZ',168700,7,70,13,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-005','헤비코튼 후드집업','CAT-L2-OUT-HZ',169900,8,75,14,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-006','플리스 안감 후드집업','CAT-L2-OUT-HZ',171100,9,80,15,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-007','오버핏 워시드 집업','CAT-L2-OUT-HZ',172300,10,50,10,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-008','라이트 저지 후드집업','CAT-L2-OUT-HZ',173500,11,55,11,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-009','유틸리티 포켓 집업','CAT-L2-OUT-HZ',174700,5,60,12,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-OUT-HZ-010','스탠다드 핏 후드집업','CAT-L2-OUT-HZ',175900,6,65,13,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-001','클래식 싱글 자켓','CAT-L2-OUT-JK',177100,5,70,14,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-002','미니멀 크롭 블레이저','CAT-L2-OUT-JK',178300,6,75,15,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-003','유틸리티 필드 자켓','CAT-L2-OUT-JK',179500,7,80,10,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-004','더블브레스트 자켓','CAT-L2-OUT-JK',180700,8,50,11,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-005','소프트 테일러드 자켓','CAT-L2-OUT-JK',181900,9,55,12,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-006','캐주얼 코튼 자켓','CAT-L2-OUT-JK',183100,10,60,13,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-007','스트레치 데일리 자켓','CAT-L2-OUT-JK',184300,11,65,14,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-008','하프기장 포인트 자켓','CAT-L2-OUT-JK',185500,5,70,15,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-009','워시드 워크 자켓','CAT-L2-OUT-JK',186700,6,75,10,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-OUT-JK-010','에센셜 베이직 자켓','CAT-L2-OUT-JK',187900,7,80,11,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-001','브이넥 버튼 가디건','CAT-L2-OUT-CD',189100,6,50,12,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-002','케이블 포인트 가디건','CAT-L2-OUT-CD',190300,7,55,13,'VND-006','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-003','오픈프론트 롱 가디건','CAT-L2-OUT-CD',191500,8,60,14,'VND-007','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-004','소프트 크루 가디건','CAT-L2-OUT-CD',192700,9,65,15,'VND-008','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-005','울 블렌드 가디건','CAT-L2-OUT-CD',193900,10,70,10,'VND-001','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-006','미니멀 크롭 가디건','CAT-L2-OUT-CD',195100,11,75,11,'VND-002','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-007','리브 조직 가디건','CAT-L2-OUT-CD',196300,5,80,12,'VND-003','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-008','데일리 베이직 가디건','CAT-L2-OUT-CD',197500,6,50,13,'VND-004','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-009','루즈핏 니트 가디건','CAT-L2-OUT-CD',198700,7,55,14,'VND-005','ACTIVE',NOW(),NOW()),
('PRD-OUT-CD-010','라이트 터치 가디건','CAT-L2-OUT-CD',199900,8,60,15,'VND-006','ACTIVE',NOW(),NOW())
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
    UNION ALL SELECT 'PRD-TOP-SS-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-004' AS product_code, 'POLYAMIDE' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-004' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-008' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-008' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SS-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-004' AS product_code, 'WOOL' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-004' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-008' AS product_code, 'LINEN' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-008' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-LS-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-004' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-004' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-008' AS product_code, 'POLYAMIDE' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-008' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-SH-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-004' AS product_code, 'LINEN' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-004' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-008' AS product_code, 'WOOL' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-008' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-KN-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-004' AS product_code, 'POLYAMIDE' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-004' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-008' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-008' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-TOP-HD-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-004' AS product_code, 'WOOL' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-004' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-008' AS product_code, 'LINEN' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-008' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-DN-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-004' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-004' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-008' AS product_code, 'POLYAMIDE' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-008' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-ST-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-004' AS product_code, 'LINEN' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-004' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-008' AS product_code, 'WOOL' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-008' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-LG-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-004' AS product_code, 'POLYAMIDE' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-004' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-008' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-008' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-PNT-TR-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-004' AS product_code, 'WOOL' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-004' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-008' AS product_code, 'LINEN' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-008' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-MN-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-004' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-004' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-008' AS product_code, 'POLYAMIDE' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-008' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-SKT-LG-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-004' AS product_code, 'LINEN' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-004' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-008' AS product_code, 'WOOL' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-008' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-PD-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-004' AS product_code, 'POLYAMIDE' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-004' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-008' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-008' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-HZ-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-004' AS product_code, 'WOOL' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-004' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-008' AS product_code, 'LINEN' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-008' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-JK-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-001' AS product_code, 'POLYESTER' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-002' AS product_code, 'WOOL' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-003' AS product_code, 'RAYON' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-004' AS product_code, 'COTTON' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-004' AS product_code, 'POLYESTER' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-005' AS product_code, 'POLYAMIDE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-006' AS product_code, 'ACRYLIC' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-007' AS product_code, 'SILK' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-008' AS product_code, 'POLYAMIDE' AS material_code, 70 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-008' AS product_code, 'RAYON' AS material_code, 30 AS ratio, 2 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-009' AS product_code, 'CASHMERE' AS material_code, 100 AS ratio, 1 AS composition_order
    UNION ALL SELECT 'PRD-OUT-CD-010' AS product_code, 'COTTON' AS material_code, 100 AS ratio, 1 AS composition_order
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
) s;
