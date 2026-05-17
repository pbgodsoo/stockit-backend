-- 순환재고 거래처 스키마 전환
-- 1) product_types -> factory_product
-- 2) product_note 제거
-- 3) address 추가 (NOT NULL), 기존 데이터는 임시값 백필

ALTER TABLE circular_buyer
  CHANGE COLUMN product_types factory_product JSON NULL;

ALTER TABLE circular_buyer
  ADD COLUMN address VARCHAR(256) NULL AFTER phone;

UPDATE circular_buyer
SET address = '미등록'
WHERE address IS NULL OR TRIM(address) = '';

ALTER TABLE circular_buyer
  MODIFY COLUMN address VARCHAR(256) NOT NULL;

ALTER TABLE circular_buyer
  DROP COLUMN product_note;
