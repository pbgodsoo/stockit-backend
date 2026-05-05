CREATE TABLE IF NOT EXISTS circular_material_price_policy (
  material_code VARCHAR(32) PRIMARY KEY,
  material_name_ko VARCHAR(32) NOT NULL,
  material_group VARCHAR(32) NOT NULL,
  price_per_kg INT NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO circular_material_price_policy
(material_code, material_name_ko, material_group, price_per_kg, active, create_date, update_date)
VALUES
('COTTON', '면', 'NATURAL_SINGLE', 5000, 1, NOW(), NOW()),
('WOOL', '울', 'NATURAL_SINGLE', 10000, 1, NOW(), NOW()),
('CASHMERE', '캐시미어', 'NATURAL_SINGLE', 35000, 1, NOW(), NOW()),
('SILK', '실크', 'NATURAL_SINGLE', 20000, 1, NOW(), NOW()),
('LINEN', '린넨', 'NATURAL_SINGLE', 5000, 1, NOW(), NOW()),
('POLYESTER', '폴리에스터', 'SYNTHETIC', 3000, 1, NOW(), NOW()),
('ACRYLIC', '아크릴', 'SYNTHETIC', 2000, 1, NOW(), NOW()),
('POLYAMIDE', '나일론', 'SYNTHETIC', 4000, 1, NOW(), NOW()),
('ELASTANE', '스판덱스', 'SYNTHETIC', 2000, 1, NOW(), NOW()),
('BLEND', '혼방', 'BLEND', 1000, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
material_name_ko = VALUES(material_name_ko),
material_group = VALUES(material_group),
price_per_kg = VALUES(price_per_kg),
active = VALUES(active),
update_date = NOW();
