-- HQ 인프라 관리 더미 데이터 (MariaDB/MySQL)
-- 실행 순서: warehouse -> store

INSERT INTO warehouse
    (code, name, region, manager_name, contact, address, capacity, status, create_date, update_date)
VALUES
    ('WH-0001', '수도권 통합 물류센터', '서울', '박지훈', '02-3011-1001', '서울특별시 강서구 물류로 101', '10,000 PLT / 5,000㎡', 'ACTIVE', NOW(), NOW()),
    ('WH-0002', '경기 남부 허브 창고', '경기', '최민서', '031-422-2002', '경기도 화성시 산업단지로 88', '8,000 PLT / 4,200㎡', 'ACTIVE', NOW(), NOW()),
    ('WH-0003', '영남권 거점 창고', '부산', '김도윤', '051-711-3003', '부산광역시 강서구 유통단지 55', '7,500 PLT / 3,900㎡', 'SUSPENDED', NOW(), NOW()),
    ('WH-0004', '충청권 중계 센터', '대전', '이서연', '042-611-4004', '대전광역시 유성구 물류밸리 12', '6,000 PLT / 3,100㎡', 'INACTIVE', NOW(), NOW()),
    ('WH-0005', '인천 항만 물류센터', '인천', '문하늘', '032-811-5005', '인천광역시 연수구 송도물류로 77', '9,200 PLT / 4,700㎡', 'ACTIVE', NOW(), NOW()),
    ('WH-0006', '강원 동부 보관창고', '강원', '남시우', '033-622-6006', '강원특별자치도 강릉시 유통로 19', '4,500 PLT / 2,400㎡', 'ACTIVE', NOW(), NOW()),
    ('WH-0007', '호남권 메가 허브', '광주', '배유진', '062-933-7007', '광주광역시 광산구 산업대로 244', '8,700 PLT / 4,100㎡', 'SUSPENDED', NOW(), NOW()),
    ('WH-0008', '제주 저온 복합창고', '제주', '신우람', '064-744-8008', '제주특별자치도 제주시 물류단지길 33', '3,800 PLT / 2,000㎡', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    region = VALUES(region),
    manager_name = VALUES(manager_name),
    contact = VALUES(contact),
    address = VALUES(address),
    capacity = VALUES(capacity),
    status = VALUES(status),
    update_date = NOW();

INSERT INTO store
    (code, name, region, type, manager_name, contact, address, warehouse_code, status, create_date, update_date)
VALUES
    ('ST-0001', '강남 플래그십점', '서울', 'DIRECT', '정현우', '010-1111-1111', '서울특별시 강남구 테헤란로 201', 'WH-0001', 'ACTIVE', NOW(), NOW()),
    ('ST-0002', '홍대 라이프스타일점', '서울', 'DIRECT', '오지안', '010-2222-2222', '서울특별시 마포구 양화로 121', 'WH-0001', 'ACTIVE', NOW(), NOW()),
    ('ST-0003', '수원 광교점', '경기', 'FRANCHISE', '한지민', '010-3333-3333', '경기도 수원시 영통구 광교중앙로 85', 'WH-0002', 'ACTIVE', NOW(), NOW()),
    ('ST-0004', '분당 서현점', '경기', 'FRANCHISE', '윤태성', '010-4444-4444', '경기도 성남시 분당구 서현로 210', 'WH-0002', 'INACTIVE', NOW(), NOW()),
    ('ST-0005', '부산 센텀점', '부산', 'DIRECT', '임수빈', '010-5555-5555', '부산광역시 해운대구 센텀동로 45', 'WH-0003', 'SUSPENDED', NOW(), NOW()),
    ('ST-0006', '대전 둔산점', '대전', 'FRANCHISE', '강민우', '010-6666-6666', '대전광역시 서구 둔산로 77', 'WH-0004', 'ACTIVE', NOW(), NOW()),
    ('ST-0007', '인천 송도점', '인천', 'DIRECT', '조다인', '010-7777-7777', '인천광역시 연수구 센트럴로 160', 'WH-0005', 'ACTIVE', NOW(), NOW()),
    ('ST-0008', '인천 부평점', '인천', 'FRANCHISE', '차도현', '010-8888-8888', '인천광역시 부평구 경원대로 1415', 'WH-0005', 'ACTIVE', NOW(), NOW()),
    ('ST-0009', '강릉 중앙점', '강원', 'FRANCHISE', '표나연', '010-9999-9999', '강원특별자치도 강릉시 금성로 52', 'WH-0006', 'INACTIVE', NOW(), NOW()),
    ('ST-0010', '원주 혁신점', '강원', 'DIRECT', '구민재', '010-1212-1212', '강원특별자치도 원주시 혁신로 35', 'WH-0006', 'ACTIVE', NOW(), NOW()),
    ('ST-0011', '광주 상무점', '광주', 'DIRECT', '노아린', '010-3434-3434', '광주광역시 서구 상무중앙로 90', 'WH-0007', 'SUSPENDED', NOW(), NOW()),
    ('ST-0012', '광주 충장점', '광주', 'FRANCHISE', '도지후', '010-5656-5656', '광주광역시 동구 충장로 102', 'WH-0007', 'ACTIVE', NOW(), NOW()),
    ('ST-0013', '제주 노형점', '제주', 'DIRECT', '류서하', '010-7878-7878', '제주특별자치도 제주시 노형로 312', 'WH-0008', 'ACTIVE', NOW(), NOW()),
    ('ST-0014', '제주 서귀포점', '제주', 'FRANCHISE', '마태오', '010-9090-9090', '제주특별자치도 서귀포시 중앙로 65', 'WH-0008', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    region = VALUES(region),
    type = VALUES(type),
    manager_name = VALUES(manager_name),
    contact = VALUES(contact),
    address = VALUES(address),
    warehouse_code = VALUES(warehouse_code),
    status = VALUES(status),
    update_date = NOW();
