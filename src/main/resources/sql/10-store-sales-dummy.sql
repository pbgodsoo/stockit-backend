-- 10-store-sales-dummy.sql
-- 매장 판매: 강남(ST-SL-0001) 44건 + 홍대(ST-SL-0002) 38건 = 82건
-- 패턴 = CAST(RIGHT(sale_no,5) AS UNSIGNED) % 10
--
-- P1(%=1): OUT-PD-002×4 + TOP-KN-001×4 + PNT-LG-002×3 = qty11, 582,900 [高]
-- P2(%=2): TOP-SH-001×5  + PNT-TR-001×4 + SKT-MN-001×3 = qty12, 418,800 [低]
-- P3(%=3): OUT-HZ-002×4  + TOP-KN-002×4 + PNT-DN-001×3 = qty11, 544,900 [中高]
-- P4(%=4): TOP-SH-002×4  + PNT-LG-001×3 + SKT-LG-001×3 = qty10, 403,000 [低]
-- P5(%=5): OUT-PD-001×4  + TOP-HD-001×4 + PNT-LG-002×3 = qty11, 594,900 [최高]
-- P6(%=6): OUT-CD-002×3  + TOP-LS-001×5 + PNT-TR-002×4 = qty12, 438,800 [中低]
-- P7(%=7): OUT-JK-001×5  + TOP-KN-001×4 + PNT-DN-002×2 = qty11, 589,900 [高]
-- P8(%=8): TOP-HD-001×3  + PNT-ST-001×5 + SKT-MN-001×3 = qty11, 385,900 [최低]
-- P9(%=9): OUT-HZ-001×5  + TOP-SH-001×3 + PNT-LG-001×4 = qty12, 542,800 [中高]
-- P10(%=0): OUT-CD-001×4 + TOP-SS-002×5 + PNT-DN-001×3 = qty12, 442,800 [中低]
--
-- ▣ 강남(00001~00044): 고가 아우터 위주, 1월9건/2월5건/3월11건/4월8건/5월5건/6월6건
-- ▣ 홍대(00045~00082): 캐주얼 위주, 1월7건/2월4건/3월9건/4월6건/5월6건/6월6건
--
-- 월별 합산 매출 예상:
--   1월 ~8.1M (겨울 성수기)
--   2월 ~4.4M (비수기)
--   3월 ~9.9M (봄 피크 ↑↑)
--   4월 ~6.9M
--   5월 ~5.4M (여름 전환)
--   6월 ~5.9M

SET SQL_SAFE_UPDATES = 0;
DELETE FROM store_sale_item
WHERE sale_header_id IN (
    SELECT id FROM (SELECT id FROM store_sale_header WHERE sale_no LIKE 'SLS-2026%') t
);
DELETE FROM store_sale_header WHERE sale_no LIKE 'SLS-2026%';
SET SQL_SAFE_UPDATES = 1;

-- ============================================================
-- 강남 플래그십점 (ST-SL-0001) — 44건 (00001~00044)
-- 번호 % 10 순서: P1,P2,P3,P4,P5,P6,P7,P8,P9,P10 반복
-- ============================================================

INSERT INTO store_sale_header (sale_no,store_id,status,sold_at,total_quantity,total_amount,create_date,update_date) VALUES
-- 1월 (9건: 00001~00009)
('SLS-20260105-00001',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-05 10:30:00',11,582900,'2026-01-05 10:30:00','2026-01-05 10:30:00'),
('SLS-20260108-00002',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-08 14:00:00',12,418800,'2026-01-08 14:00:00','2026-01-08 14:00:00'),
('SLS-20260111-00003',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-11 11:00:00',11,544900,'2026-01-11 11:00:00','2026-01-11 11:00:00'),
('SLS-20260114-00004',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-14 15:30:00',10,403000,'2026-01-14 15:30:00','2026-01-14 15:30:00'),
('SLS-20260117-00005',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-17 10:00:00',11,594900,'2026-01-17 10:00:00','2026-01-17 10:00:00'),
('SLS-20260120-00006',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-20 13:00:00',12,438800,'2026-01-20 13:00:00','2026-01-20 13:00:00'),
('SLS-20260123-00007',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-23 10:30:00',11,589900,'2026-01-23 10:30:00','2026-01-23 10:30:00'),
('SLS-20260126-00008',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-26 14:00:00',11,385900,'2026-01-26 14:00:00','2026-01-26 14:00:00'),
('SLS-20260129-00009',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-01-29 11:00:00',12,542800,'2026-01-29 11:00:00','2026-01-29 11:00:00'),
-- 2월 (5건: 00010~00014)
('SLS-20260204-00010',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-02-04 10:00:00',12,442800,'2026-02-04 10:00:00','2026-02-04 10:00:00'),
('SLS-20260210-00011',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-02-10 14:30:00',11,582900,'2026-02-10 14:30:00','2026-02-10 14:30:00'),
('SLS-20260216-00012',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-02-16 11:00:00',12,418800,'2026-02-16 11:00:00','2026-02-16 11:00:00'),
('SLS-20260221-00013',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-02-21 15:00:00',11,544900,'2026-02-21 15:00:00','2026-02-21 15:00:00'),
('SLS-20260226-00014',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-02-26 10:30:00',10,403000,'2026-02-26 10:30:00','2026-02-26 10:30:00'),
-- 3월 (11건: 00015~00025)
('SLS-20260302-00015',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-02 10:00:00',11,594900,'2026-03-02 10:00:00','2026-03-02 10:00:00'),
('SLS-20260305-00016',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-05 14:00:00',12,438800,'2026-03-05 14:00:00','2026-03-05 14:00:00'),
('SLS-20260308-00017',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-08 11:30:00',11,589900,'2026-03-08 11:30:00','2026-03-08 11:30:00'),
('SLS-20260311-00018',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-11 15:00:00',11,385900,'2026-03-11 15:00:00','2026-03-11 15:00:00'),
('SLS-20260313-00019',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-13 10:00:00',12,542800,'2026-03-13 10:00:00','2026-03-13 10:00:00'),
('SLS-20260315-00020',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-15 14:30:00',12,442800,'2026-03-15 14:30:00','2026-03-15 14:30:00'),
('SLS-20260317-00021',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-17 10:00:00',11,582900,'2026-03-17 10:00:00','2026-03-17 10:00:00'),
('SLS-20260319-00022',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-19 14:00:00',12,418800,'2026-03-19 14:00:00','2026-03-19 14:00:00'),
('SLS-20260321-00023',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-21 11:00:00',11,544900,'2026-03-21 11:00:00','2026-03-21 11:00:00'),
('SLS-20260324-00024',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-24 15:30:00',10,403000,'2026-03-24 15:30:00','2026-03-24 15:30:00'),
('SLS-20260327-00025',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-03-27 10:00:00',11,594900,'2026-03-27 10:00:00','2026-03-27 10:00:00'),
-- 4월 (8건: 00026~00033)
('SLS-20260402-00026',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-04-02 13:00:00',12,438800,'2026-04-02 13:00:00','2026-04-02 13:00:00'),
('SLS-20260406-00027',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-04-06 10:30:00',11,589900,'2026-04-06 10:30:00','2026-04-06 10:30:00'),
('SLS-20260410-00028',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-04-10 14:00:00',11,385900,'2026-04-10 14:00:00','2026-04-10 14:00:00'),
('SLS-20260414-00029',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-04-14 11:00:00',12,542800,'2026-04-14 11:00:00','2026-04-14 11:00:00'),
('SLS-20260417-00030',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-04-17 15:00:00',12,442800,'2026-04-17 15:00:00','2026-04-17 15:00:00'),
('SLS-20260421-00031',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-04-21 10:00:00',11,582900,'2026-04-21 10:00:00','2026-04-21 10:00:00'),
('SLS-20260424-00032',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-04-24 14:30:00',12,418800,'2026-04-24 14:30:00','2026-04-24 14:30:00'),
('SLS-20260428-00033',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-04-28 11:00:00',11,544900,'2026-04-28 11:00:00','2026-04-28 11:00:00'),
-- 5월 (5건: 00034~00038)
('SLS-20260505-00034',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-05-05 10:00:00',10,403000,'2026-05-05 10:00:00','2026-05-05 10:00:00'),
('SLS-20260511-00035',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-05-11 14:30:00',11,594900,'2026-05-11 14:30:00','2026-05-11 14:30:00'),
('SLS-20260517-00036',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-05-17 11:00:00',12,438800,'2026-05-17 11:00:00','2026-05-17 11:00:00'),
('SLS-20260522-00037',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-05-22 15:00:00',11,589900,'2026-05-22 15:00:00','2026-05-22 15:00:00'),
('SLS-20260528-00038',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-05-28 10:30:00',11,385900,'2026-05-28 10:30:00','2026-05-28 10:30:00'),
-- 6월 (6건: 00039~00044, 일별)
('SLS-20260602-00039',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-06-02 10:00:00',12,542800,'2026-06-02 10:00:00','2026-06-02 10:00:00'),
('SLS-20260603-00040',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-06-03 14:00:00',12,442800,'2026-06-03 14:00:00','2026-06-03 14:00:00'),
('SLS-20260604-00041',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-06-04 11:30:00',11,582900,'2026-06-04 11:30:00','2026-06-04 11:30:00'),
('SLS-20260605-00042',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-06-05 15:00:00',12,418800,'2026-06-05 15:00:00','2026-06-05 15:00:00'),
('SLS-20260606-00043',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-06-06 10:30:00',11,544900,'2026-06-06 10:30:00','2026-06-06 10:30:00'),
('SLS-20260607-00044',(SELECT id FROM infrastructure WHERE code='ST-SL-0001'),'COMPLETED','2026-06-07 13:00:00',10,403000,'2026-06-07 13:00:00','2026-06-07 13:00:00');

-- ============================================================
-- 홍대 라이프스타일점 (ST-SL-0002) — 38건 (00045~00082)
-- 번호 00045부터 시작 → % 10 결과가 강남과 달라져 다른 패턴
-- P5,P6,P7,P8,P9,P10,P1 ... 순으로 시작
-- ============================================================

INSERT INTO store_sale_header (sale_no,store_id,status,sold_at,total_quantity,total_amount,create_date,update_date) VALUES
-- 1월 (7건: 00045~00051)
('SLS-20260106-00045',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-01-06 10:00:00',11,594900,'2026-01-06 10:00:00','2026-01-06 10:00:00'),
('SLS-20260109-00046',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-01-09 14:30:00',12,438800,'2026-01-09 14:30:00','2026-01-09 14:30:00'),
('SLS-20260113-00047',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-01-13 11:00:00',11,589900,'2026-01-13 11:00:00','2026-01-13 11:00:00'),
('SLS-20260116-00048',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-01-16 15:00:00',11,385900,'2026-01-16 15:00:00','2026-01-16 15:00:00'),
('SLS-20260120-00049',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-01-20 10:00:00',12,542800,'2026-01-20 10:00:00','2026-01-20 10:00:00'),
('SLS-20260124-00050',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-01-24 13:30:00',12,442800,'2026-01-24 13:30:00','2026-01-24 13:30:00'),
('SLS-20260128-00051',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-01-28 10:00:00',11,582900,'2026-01-28 10:00:00','2026-01-28 10:00:00'),
-- 2월 (4건: 00052~00055)
('SLS-20260206-00052',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-02-06 11:30:00',12,418800,'2026-02-06 11:30:00','2026-02-06 11:30:00'),
('SLS-20260213-00053',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-02-13 15:00:00',11,544900,'2026-02-13 15:00:00','2026-02-13 15:00:00'),
('SLS-20260220-00054',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-02-20 10:00:00',10,403000,'2026-02-20 10:00:00','2026-02-20 10:00:00'),
('SLS-20260227-00055',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-02-27 13:00:00',11,594900,'2026-02-27 13:00:00','2026-02-27 13:00:00'),
-- 3월 (9건: 00056~00064)
('SLS-20260303-00056',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-03 10:30:00',12,438800,'2026-03-03 10:30:00','2026-03-03 10:30:00'),
('SLS-20260306-00057',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-06 14:00:00',11,589900,'2026-03-06 14:00:00','2026-03-06 14:00:00'),
('SLS-20260309-00058',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-09 11:00:00',11,385900,'2026-03-09 11:00:00','2026-03-09 11:00:00'),
('SLS-20260312-00059',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-12 15:30:00',12,542800,'2026-03-12 15:30:00','2026-03-12 15:30:00'),
('SLS-20260315-00060',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-15 10:00:00',12,442800,'2026-03-15 10:00:00','2026-03-15 10:00:00'),
('SLS-20260318-00061',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-18 14:30:00',11,582900,'2026-03-18 14:30:00','2026-03-18 14:30:00'),
('SLS-20260320-00062',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-20 11:00:00',12,418800,'2026-03-20 11:00:00','2026-03-20 11:00:00'),
('SLS-20260323-00063',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-23 15:00:00',11,544900,'2026-03-23 15:00:00','2026-03-23 15:00:00'),
('SLS-20260327-00064',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-03-27 10:30:00',10,403000,'2026-03-27 10:30:00','2026-03-27 10:30:00'),
-- 4월 (6건: 00065~00070)
('SLS-20260403-00065',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-04-03 10:00:00',11,594900,'2026-04-03 10:00:00','2026-04-03 10:00:00'),
('SLS-20260408-00066',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-04-08 14:30:00',12,438800,'2026-04-08 14:30:00','2026-04-08 14:30:00'),
('SLS-20260413-00067',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-04-13 11:00:00',11,589900,'2026-04-13 11:00:00','2026-04-13 11:00:00'),
('SLS-20260418-00068',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-04-18 15:00:00',11,385900,'2026-04-18 15:00:00','2026-04-18 15:00:00'),
('SLS-20260423-00069',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-04-23 10:30:00',12,542800,'2026-04-23 10:30:00','2026-04-23 10:30:00'),
('SLS-20260428-00070',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-04-28 13:30:00',12,442800,'2026-04-28 13:30:00','2026-04-28 13:30:00'),
-- 5월 (6건: 00071~00076)
('SLS-20260506-00071',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-05-06 10:00:00',11,582900,'2026-05-06 10:00:00','2026-05-06 10:00:00'),
('SLS-20260512-00072',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-05-12 14:00:00',12,418800,'2026-05-12 14:00:00','2026-05-12 14:00:00'),
('SLS-20260518-00073',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-05-18 11:30:00',11,544900,'2026-05-18 11:30:00','2026-05-18 11:30:00'),
('SLS-20260522-00074',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-05-22 15:00:00',10,403000,'2026-05-22 15:00:00','2026-05-22 15:00:00'),
('SLS-20260526-00075',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-05-26 10:30:00',11,594900,'2026-05-26 10:30:00','2026-05-26 10:30:00'),
('SLS-20260530-00076',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-05-30 13:00:00',12,438800,'2026-05-30 13:00:00','2026-05-30 13:00:00'),
-- 6월 (6건: 00077~00082, 일별)
('SLS-20260602-00077',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-06-02 10:30:00',11,589900,'2026-06-02 10:30:00','2026-06-02 10:30:00'),
('SLS-20260603-00078',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-06-03 14:30:00',11,385900,'2026-06-03 14:30:00','2026-06-03 14:30:00'),
('SLS-20260604-00079',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-06-04 11:00:00',12,542800,'2026-06-04 11:00:00','2026-06-04 11:00:00'),
('SLS-20260605-00080',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-06-05 15:30:00',12,442800,'2026-06-05 15:30:00','2026-06-05 15:30:00'),
('SLS-20260606-00081',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-06-06 10:00:00',11,582900,'2026-06-06 10:00:00','2026-06-06 10:00:00'),
('SLS-20260607-00082',(SELECT id FROM infrastructure WHERE code='ST-SL-0002'),'COMPLETED','2026-06-07 13:30:00',12,418800,'2026-06-07 13:30:00','2026-06-07 13:30:00');

-- ============================================================
-- store_sale_item: 패턴별 (sale_no 마지막 5자리 % 10)
-- ============================================================

-- P1(%=1): 볼륨넥 덕다운 패딩×4 + 파인게이지 라운드 니트×4 + 테일러드 스트레이트 팬츠×3 = 582,900
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-002-BLK-M'),'PRD-OUT-PD-002-BLK-M','PRD-OUT-PD-002','볼륨넥 덕다운 패딩','아우터','패딩','BLK','M',4,69900,279600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 1;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-BLK-M'),'PRD-TOP-KN-001-BLK-M','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','BLK','M',4,39900,159600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 1;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',3,47900,143700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 1;

-- P2(%=2): 옥스포드 버튼다운 셔츠×5 + 소프트 조거 트레이닝×4 + A라인 코튼 미니 스커트×3 = 418,800
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-BLK-M'),'PRD-TOP-SH-001-BLK-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','BLK','M',5,32900,164500,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 2;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-001-BLK-M'),'PRD-PNT-TR-001-BLK-M','PRD-PNT-TR-001','소프트 조거 트레이닝','바지','츄리닝','BLK','M',4,38900,155600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 2;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-SKT-MN-001-BLK-M'),'PRD-SKT-MN-001-BLK-M','PRD-SKT-MN-001','A라인 코튼 미니 스커트','치마','미니스커트','BLK','M',3,32900,98700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 2;

-- P3(%=3): 테리 루즈핏 후드집업×4 + 캐시 블렌드 크루 니트×4 + 스트레이트 인디고 데님×3 = 544,900
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-002-BLK-M'),'PRD-OUT-HZ-002-BLK-M','PRD-OUT-HZ-002','테리 루즈핏 후드집업','아우터','후드집업','BLK','M',4,55900,223600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 3;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-002-BLK-M'),'PRD-TOP-KN-002-BLK-M','PRD-TOP-KN-002','캐시 블렌드 크루 니트','상의','니트','BLK','M',4,42900,171600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 3;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',3,49900,149700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 3;

-- P4(%=4): 클린 포플린 셔츠×4 + 와이드 플리츠 롱팬츠×3 + 플리츠 플로우 롱스커트×3 = 403,000
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-002-BLK-M'),'PRD-TOP-SH-002-BLK-M','PRD-TOP-SH-002','클린 포플린 셔츠','상의','셔츠','BLK','M',4,34900,139600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 4;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',3,44900,134700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 4;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-SKT-LG-001-BLK-M'),'PRD-SKT-LG-001-BLK-M','PRD-SKT-LG-001','플리츠 플로우 롱스커트','치마','롱스커트','BLK','M',3,42900,128700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 4;

-- P5(%=5): 라이트 웜 숏 패딩×4 + 헤비웨이트 로고 후드티×4 + 테일러드 스트레이트 팬츠×3 = 594,900
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-PD-001-BLK-M'),'PRD-OUT-PD-001-BLK-M','PRD-OUT-PD-001','라이트 웜 숏 패딩','아우터','패딩','BLK','M',4,66900,267600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 5;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',4,45900,183600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 5;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-002-BLK-M'),'PRD-PNT-LG-002-BLK-M','PRD-PNT-LG-002','테일러드 스트레이트 팬츠','바지','긴바지','BLK','M',3,47900,143700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 5;

-- P6(%=6): 케이블 포인트 가디건×3 + 슬림 베이스 레이어 긴팔×5 + 테크 플리스 트랙 팬츠×4 = 438,800
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-002-BLK-M'),'PRD-OUT-CD-002-BLK-M','PRD-OUT-CD-002','케이블 포인트 가디건','아우터','가디건','BLK','M',3,48900,146700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 6;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-LS-001-BLK-M'),'PRD-TOP-LS-001-BLK-M','PRD-TOP-LS-001','슬림 베이스 레이어 긴팔','상의','긴팔','BLK','M',5,24900,124500,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 6;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-TR-002-BLK-M'),'PRD-PNT-TR-002-BLK-M','PRD-PNT-TR-002','테크 플리스 트랙 팬츠','바지','츄리닝','BLK','M',4,41900,167600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 6;

-- P7(%=7): 클래식 싱글 자켓×5 + 파인게이지 라운드 니트×4 + 슬림 테이퍼드 데님×2 = 589,900
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-JK-001-BLK-M'),'PRD-OUT-JK-001-BLK-M','PRD-OUT-JK-001','클래식 싱글 자켓','아우터','자켓','BLK','M',5,64900,324500,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 7;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-KN-001-BLK-M'),'PRD-TOP-KN-001-BLK-M','PRD-TOP-KN-001','파인게이지 라운드 니트','상의','니트','BLK','M',4,39900,159600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 7;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-002-BLK-M'),'PRD-PNT-DN-002-BLK-M','PRD-PNT-DN-002','슬림 테이퍼드 데님','바지','청바지','BLK','M',2,52900,105800,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 7;

-- P8(%=8): 헤비웨이트 로고 후드티×3 + 코튼 치노 쇼츠×5 + A라인 코튼 미니 스커트×3 = 385,900
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-HD-001-BLK-M'),'PRD-TOP-HD-001-BLK-M','PRD-TOP-HD-001','헤비웨이트 로고 후드티','상의','후드티','BLK','M',3,45900,137700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 8;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-ST-001-BLK-M'),'PRD-PNT-ST-001-BLK-M','PRD-PNT-ST-001','코튼 치노 쇼츠','바지','반바지','BLK','M',5,29900,149500,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 8;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-SKT-MN-001-BLK-M'),'PRD-SKT-MN-001-BLK-M','PRD-SKT-MN-001','A라인 코튼 미니 스커트','치마','미니스커트','BLK','M',3,32900,98700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 8;

-- P9(%=9): 소프트 스웻 후드집업×5 + 옥스포드 버튼다운 셔츠×3 + 와이드 플리츠 롱팬츠×4 = 542,800
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-HZ-001-BLK-M'),'PRD-OUT-HZ-001-BLK-M','PRD-OUT-HZ-001','소프트 스웻 후드집업','아우터','후드집업','BLK','M',5,52900,264500,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 9;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SH-001-BLK-M'),'PRD-TOP-SH-001-BLK-M','PRD-TOP-SH-001','옥스포드 버튼다운 셔츠','상의','셔츠','BLK','M',3,32900,98700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 9;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-LG-001-BLK-M'),'PRD-PNT-LG-001-BLK-M','PRD-PNT-LG-001','와이드 플리츠 롱팬츠','바지','긴바지','BLK','M',4,44900,179600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 9;

-- P10(%=0): 브이넥 버튼 가디건×4 + 드라이핏 액티브 반팔×5 + 스트레이트 인디고 데님×3 = 442,800
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-OUT-CD-001-BLK-M'),'PRD-OUT-CD-001-BLK-M','PRD-OUT-CD-001','브이넥 버튼 가디건','아우터','가디건','BLK','M',4,45900,183600,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 0;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-TOP-SS-002-BLK-M'),'PRD-TOP-SS-002-BLK-M','PRD-TOP-SS-002','드라이핏 액티브 반팔','상의','반팔','BLK','M',5,21900,109500,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 0;
INSERT INTO store_sale_item (sale_header_id,sku_id,sku_code,product_code,product_name,main_category,sub_category,color,size,quantity,unit_price,line_amount,create_date,update_date)
SELECT h.id,(SELECT id FROM product_sku WHERE sku_code='PRD-PNT-DN-001-BLK-M'),'PRD-PNT-DN-001-BLK-M','PRD-PNT-DN-001','스트레이트 인디고 데님','바지','청바지','BLK','M',3,49900,149700,h.sold_at,h.sold_at
FROM store_sale_header h WHERE h.sale_no LIKE 'SLS-2026%' AND CAST(RIGHT(h.sale_no,5) AS UNSIGNED) % 10 = 0;
