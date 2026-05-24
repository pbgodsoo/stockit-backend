# 판매 API Postman 가이드

## 1. 개요
- 대상 API
  - `POST /api/store/sales` (판매 등록)
  - `GET /api/store/sales` (판매 목록 조회)
  - `GET /api/store/sales/{saleNo}` (판매 상세 조회)
- 공통 응답 형식: `BaseResponse`

## 2. 사전 준비
- 서버 실행 후 Base URL 확인
  - 예: `http://localhost:8080`
- 매장/상품/SKU/재고 데이터가 먼저 준비되어 있어야 함
- 예시 기준
  - 매장 코드: `ST-0001`
  - 매장 locationId: `2`

## 3. Postman 공통 설정
- Method에 맞게 `GET`/`POST` 선택
- `POST` 요청 시 Header
  - `Content-Type: application/json`
- 인증이 붙지 않은 현재 기준에서는 별도 토큰 불필요

## 4. API별 사용법

### 4.1 판매 등록
- Method: `POST`
- URL: `{{baseUrl}}/api/store/sales`
- Body: `raw` + `JSON`

요청 예시(단건):
```json
{
  "storeCode": "ST-0001",
  "items": [
    {
      "skuCode": "SKU-TOP-SS-001-BLK-S",
      "quantity": 1
    }
  ]
}
```

요청 예시(복수):
```json
{
  "storeCode": "ST-0001",
  "items": [
    { "skuCode": "SKU-TOP-SS-001-BLK-S", "quantity": 2 },
    { "skuCode": "SKU-TOP-LS-002-WHT-M", "quantity": 1 }
  ]
}
```

검증 포인트:
- `storeCode` 필수
- `items` 비어 있으면 실패
- `items[].skuCode` 필수
- `items[].quantity`는 1 이상
- SKU는 `ACTIVE` 상태여야 함
- 매장 재고가 충분해야 함

성공 시 확인:
- `result.saleNo` 생성 여부
- `result.totalQuantity`, `result.totalAmount`
- `result.items[]` 라인 정보

### 4.2 판매 목록 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/store/sales`
- Query Params (모두 선택)
  - `storeCode`: 예) `ST-0001`
  - `from`: `yyyy-MM-dd` 예) `2026-05-01`
  - `to`: `yyyy-MM-dd` 예) `2026-05-05`
  - `keyword`: 판매번호/상품명 검색어

요청 예시:
- `{{baseUrl}}/api/store/sales`
- `{{baseUrl}}/api/store/sales?storeCode=ST-0001`
- `{{baseUrl}}/api/store/sales?storeCode=ST-0001&from=2026-05-01&to=2026-05-05`
- `{{baseUrl}}/api/store/sales?keyword=SALE-20260505`

성공 시 확인:
- `result[]` 목록 반환
- 각 항목의 `saleNo`, `soldAt`, `totalAmount`, `headline`

### 4.3 판매 상세 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/store/sales/{saleNo}`

요청 예시:
- `{{baseUrl}}/api/store/sales/SALE-20260505-00001`

성공 시 확인:
- `result.saleNo`, `result.storeCode`, `result.status`
- `result.items[]` SKU/금액/수량 상세

## 5. 자주 나는 오류와 점검 방법

### 5.1 매장 정보를 찾을 수 없음
- 원인: `storeCode`가 `infrastructure(location_type=STORE)`에 없음
- 점검:
  - `GET /api/hq/infrastructures?type=STORE`
  - 요청 `storeCode`와 DB 코드가 정확히 같은지 확인

### 5.2 판매 SKU를 찾을 수 없음
- 원인:
  - SKU 코드 미존재
  - SKU 상태가 `ACTIVE` 아님
- 점검:
  - `GET /api/hq/products/{productCode}/skus`
  - `skuCode`, `status` 확인

### 5.3 재고 부족
- 원인: 요청 수량이 매장 실재고보다 큼
- 점검:
  - `GET /api/hq/inventories/company-wide?locationType=STORE&locationIds=2`
  - 해당 SKU의 수량 확인 후 재시도

## 6. 판매 테스트 권장 순서
1. 매장 확인: `GET /api/hq/infrastructures?type=STORE`
2. SKU 확인: `GET /api/hq/products/{productCode}/skus`
3. 매장 재고 확인: `GET /api/hq/inventories/company-wide?locationType=STORE&locationIds=2`
4. 판매 등록: `POST /api/store/sales`
5. 결과 확인: `GET /api/store/sales`, `GET /api/store/sales/{saleNo}`

## 7. 빠른 테스트 세트(ST-0001 기준)

1) 재고 조회
- `GET {{baseUrl}}/api/hq/inventories/company-wide?locationType=STORE&locationIds=2`

2) 판매 등록
```json
{
  "storeCode": "ST-0001",
  "items": [
    { "skuCode": "SKU-TOP-SS-001-BLK-S", "quantity": 1 }
  ]
}
```

3) 목록 조회
- `GET {{baseUrl}}/api/store/sales?storeCode=ST-0001`

4) 상세 조회
- 목록에서 받은 `saleNo`로
- `GET {{baseUrl}}/api/store/sales/{saleNo}`

