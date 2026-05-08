-- ADR-021 AI 거래처 추천 — 임베딩 input 풍부화 (이슈 #218)
-- material 마스터 description 시드 — 추천 시점 product → composition → material join 으로 자연어 풍부화.
-- 각 description 은 거래처 임베딩(회사명·산업군·취급품목·메모·설명) 과의 코사인 매칭 신호 강화 목적 —
-- 사용처·재활용 특성·산업 어휘를 자연어로 충분히 깔아둔다.

UPDATE material SET description = '면(코튼). 천연 식물성 단일 섬유. 흡습성과 통기성이 우수해 의류 직물 침구 수건 위생용 거즈 마스크 패드 기저귀 등 광범위하게 사용. 천연 단섬유로 회수 재활용이 깨끗하며 셀룰로오스 펄프 솜 충전재로 재가공 가능. 폐의류 폐섬유 자원 순환의 대표 소재.', update_date = NOW() WHERE code = 'COTTON';

UPDATE material SET description = '울(양모). 천연 동물성 단일 섬유. 보온성 신축성 발수성이 뛰어나 겨울 의류 코트 스웨터 니트 양말 펠트 카펫 등에 사용. 단백질 섬유로 모직 재활용 펠트 충전재 단열재로 재가공 가능. 천연 분해성으로 친환경 처리 용이.', update_date = NOW() WHERE code = 'WOOL';

UPDATE material SET description = '캐시미어. 천연 동물성 고급 단일 섬유. 가볍고 부드럽고 보온성 탁월해 프리미엄 의류 머플러 코트 스웨터에 사용. 양모와 함께 단백질 섬유로 분류되며 펠트 가공 재활용 가능. 고급 의류 폐기물 처리 시장에서 가치 있는 회수 대상.', update_date = NOW() WHERE code = 'CASHMERE';

UPDATE material SET description = '실크(견). 천연 단백질 단일 섬유. 광택 흡습성 부드러움 우수해 고급 의류 블라우스 스카프 침구 한복 등에 사용. 누에고치 자연 단백질로 자연 분해 가능하며 재방사 단백질 추출 화장품 원료로 재가공. 프리미엄 천연 섬유 폐기물.', update_date = NOW() WHERE code = 'SILK';

UPDATE material SET description = '린넨(아마). 천연 식물성 단일 섬유. 통기성 흡습성 시원함이 우수해 여름 의류 셔츠 원피스 침구 테이블린넨 등에 사용. 셀룰로오스 천연 단섬유로 재활용 깨끗하며 펄프 종이 충전재 재가공 가능. 자연 분해성 친환경 소재.', update_date = NOW() WHERE code = 'LINEN';

UPDATE material SET description = '폴리에스터(PET). 합성 화학 장섬유. 강도 내구성 세탁성 형태 안정성이 우수해 의류 가방 산업용 텍스타일 운동복 침구 안감 등에 광범위 사용. 페트병 PET 자원과 동일 화학 구조로 화학적 재활용 순환경제 핵심 소재. 단일 합성 섬유 폐기물 회수 처리.', update_date = NOW() WHERE code = 'POLYESTER';

UPDATE material SET description = '아크릴. 합성 화학 장섬유. 양모와 비슷한 보온성 가벼움 발색력이 우수해 니트 스웨터 담요 인조모피 카펫 등에 사용. 단일 합성 섬유로 화학적 재활용 가능하며 폐기 시 단일 소재 분류 처리. 합성 모직 대체 소재.', update_date = NOW() WHERE code = 'ACRYLIC';

UPDATE material SET description = '나일론(폴리아미드). 합성 화학 장섬유. 강도 마찰 내구성 신축성이 매우 우수해 스타킹 양말 가방 등산복 운동복 산업용 안전벨트 어망 등에 사용. 단일 합성 섬유로 화학적 재활용 가능하며 폐어망 폐섬유 회수 자원 순환 대상.', update_date = NOW() WHERE code = 'POLYAMIDE';

UPDATE material SET description = '엘라스테인(스판덱스 라이크라). 합성 신축성 섬유. 매우 큰 신축성 회복력으로 거의 모든 혼방에 5~30% 소량 첨가되어 활동복 스포츠웨어 속옷 청바지 양말 레깅스 등에 사용. 혼방 재활용 시 분리 처리가 어려워 혼방 전문 거래처가 필요한 까다로운 소재.', update_date = NOW() WHERE code = 'ELASTANE';

UPDATE material SET description = '레이온(비스코스). 반합성 셀룰로오스 섬유. 천연 펄프 원료로 만들어 흡습성 부드러움 드레이프성이 우수해 블라우스 원피스 안감 스카프 침구 등에 사용. 천연 펄프 기반이라 자연 분해성과 재활용성 갖추며 셀룰로오스 회수 가능. 면과 비슷한 처리 거래처 적합.', update_date = NOW() WHERE code = 'RAYON';
