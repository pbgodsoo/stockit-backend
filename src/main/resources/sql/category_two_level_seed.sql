-- 2단계 카테고리 시드 데이터
-- 실행 대상: MariaDB/MySQL
-- 정책: 중복 실행 가능(코드 기준으로 미존재 시에만 INSERT)
-- 구성: ROOT(상의/바지/치마/아우터) + CHILD

-- ROOT 카테고리
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9001', '상의', 'ROOT', NULL, 'ACTIVE', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9001');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9002', '바지', 'ROOT', NULL, 'ACTIVE', 2, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9002');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9003', '치마', 'ROOT', NULL, 'ACTIVE', 3, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9003');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9004', '아우터', 'ROOT', NULL, 'ACTIVE', 4, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9004');

-- 상의 자식: 반팔, 긴팔, 셔츠, 니트, 후드티
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9101', '반팔', 'CHILD', p.id, 'ACTIVE', 1, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9001'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9101');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9102', '긴팔', 'CHILD', p.id, 'ACTIVE', 2, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9001'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9102');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9103', '셔츠', 'CHILD', p.id, 'ACTIVE', 3, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9001'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9103');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9104', '니트', 'CHILD', p.id, 'ACTIVE', 4, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9001'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9104');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9105', '후드티', 'CHILD', p.id, 'ACTIVE', 5, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9001'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9105');

-- 바지 자식: 청바지, 반바지, 긴바지, 츄리닝
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9201', '청바지', 'CHILD', p.id, 'ACTIVE', 1, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9002'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9201');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9202', '반바지', 'CHILD', p.id, 'ACTIVE', 2, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9002'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9202');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9203', '긴바지', 'CHILD', p.id, 'ACTIVE', 3, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9002'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9203');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9204', '츄리닝', 'CHILD', p.id, 'ACTIVE', 4, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9002'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9204');

-- 치마 자식: 미니스커트, 롱스커트
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9301', '미니스커트', 'CHILD', p.id, 'ACTIVE', 1, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9003'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9301');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9302', '롱스커트', 'CHILD', p.id, 'ACTIVE', 2, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9003'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9302');

-- 아우터 자식: 패딩, 후드집업, 자켓, 가디건
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9401', '패딩', 'CHILD', p.id, 'ACTIVE', 1, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9004'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9401');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9402', '후드집업', 'CHILD', p.id, 'ACTIVE', 2, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9004'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9402');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9403', '자켓', 'CHILD', p.id, 'ACTIVE', 3, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9004'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9403');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-9404', '가디건', 'CHILD', p.id, 'ACTIVE', 4, NOW(), NOW()
FROM category p
WHERE p.code = 'CAT-9004'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-9404');
