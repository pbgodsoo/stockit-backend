-- 2단계 카테고리 시드 데이터 (고정 코드셋)

-- ROOT
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L1-TOP', '상의', 'ROOT', NULL, 'ACTIVE', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L1-TOP');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L1-PNT', '바지', 'ROOT', NULL, 'ACTIVE', 2, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L1-PNT');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L1-SKT', '치마', 'ROOT', NULL, 'ACTIVE', 3, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L1-SKT');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L1-OUT', '아우터', 'ROOT', NULL, 'ACTIVE', 4, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L1-OUT');

-- TOP
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-TOP-SS', '반팔', 'CHILD', p.id, 'ACTIVE', 1, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-TOP'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-TOP-SS');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-TOP-LS', '긴팔', 'CHILD', p.id, 'ACTIVE', 2, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-TOP'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-TOP-LS');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-TOP-SH', '셔츠', 'CHILD', p.id, 'ACTIVE', 3, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-TOP'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-TOP-SH');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-TOP-KN', '니트', 'CHILD', p.id, 'ACTIVE', 4, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-TOP'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-TOP-KN');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-TOP-HD', '후드티', 'CHILD', p.id, 'ACTIVE', 5, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-TOP'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-TOP-HD');

-- PNT
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-PNT-DN', '청바지', 'CHILD', p.id, 'ACTIVE', 1, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-PNT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-PNT-DN');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-PNT-ST', '반바지', 'CHILD', p.id, 'ACTIVE', 2, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-PNT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-PNT-ST');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-PNT-LG', '긴바지', 'CHILD', p.id, 'ACTIVE', 3, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-PNT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-PNT-LG');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-PNT-TR', '츄리닝', 'CHILD', p.id, 'ACTIVE', 4, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-PNT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-PNT-TR');

-- SKT
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-SKT-MN', '미니스커트', 'CHILD', p.id, 'ACTIVE', 1, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-SKT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-SKT-MN');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-SKT-LG', '롱스커트', 'CHILD', p.id, 'ACTIVE', 2, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-SKT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-SKT-LG');

-- OUT
INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-OUT-PD', '패딩', 'CHILD', p.id, 'ACTIVE', 1, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-OUT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-OUT-PD');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-OUT-HZ', '후드집업', 'CHILD', p.id, 'ACTIVE', 2, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-OUT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-OUT-HZ');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-OUT-JK', '자켓', 'CHILD', p.id, 'ACTIVE', 3, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-OUT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-OUT-JK');

INSERT INTO category (code, name, level, parent_id, status, sort_order, create_date, update_date)
SELECT 'CAT-L2-OUT-CD', '가디건', 'CHILD', p.id, 'ACTIVE', 4, NOW(), NOW()
FROM category p WHERE p.code = 'CAT-L1-OUT'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAT-L2-OUT-CD');
