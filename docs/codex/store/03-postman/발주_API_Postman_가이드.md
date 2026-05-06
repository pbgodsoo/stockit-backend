# 발주 API Postman 가이드

## 1. 공통
- Base URL: `http://localhost:8080`
- Header: `Content-Type: application/json`
- 응답 형식: `BaseResponse`

## 2. API 목록
- `POST /api/store/orders`
- `PUT /api/store/orders/{orderNo}`
- `PATCH /api/store/orders/{orderNo}/cancel`
- `PATCH /api/store/orders/{orderNo}/approve`
- `GET /api/store/orders`
- `GET /api/store/orders/{orderNo}`
- `GET /api/store/orders/analytics`

## 3. 요청 예시
### 3.1 발주 생성
`POST {{baseUrl}}/api/store/orders`

```json
{
  "storeCode": "ST-0001",
  "storeLocationId": "2",
  "requestedByMemberId": "st0001",
  "requestedByName": "매장 관리자",
  "memo": "주말 행사 대비",
  "items": [
    { "skuCode": "SKU-TOP-SS-001-WHT-L", "requestedQuantity": 4 },
    { "skuCode": "SKU-OUT-CD-004-IVR-S", "requestedQuantity": 3 }
  ]
}
```

### 3.2 발주 수정
`PUT {{baseUrl}}/api/store/orders/{orderNo}`

```json
{
  "memo": "수량 조정",
  "items": [
    { "skuCode": "SKU-TOP-SS-001-WHT-L", "requestedQuantity": 5 },
    { "skuCode": "SKU-OUT-CD-004-IVR-S", "requestedQuantity": 2 }
  ]
}
```

### 3.3 발주 취소
`PATCH {{baseUrl}}/api/store/orders/{orderNo}/cancel`

```json
{
  "cancelReason": "매장 사정으로 취소",
  "cancelledByMemberId": "st0001",
  "cancelledByName": "매장 관리자"
}
```

### 3.4 발주 승인 (가용재고 반영)
`PATCH {{baseUrl}}/api/store/orders/{orderNo}/approve`

```json
{
  "approvedByMemberId": "system",
  "approvedByName": "시스템"
}
```

### 3.5 발주 목록 조회
`GET {{baseUrl}}/api/store/orders?storeCode=ST-0001&status=APPROVED&from=2026-05-01&to=2026-05-31&keyword=티셔츠`

### 3.6 발주 상세 조회
`GET {{baseUrl}}/api/store/orders/{orderNo}`

### 3.7 발주 분석 조회
`GET {{baseUrl}}/api/store/orders/analytics?storeCode=ST-0001&from=2026-05-01&to=2026-05-31`

## 4. 추천 테스트 순서
1. `POST /api/store/orders`
2. `PATCH /api/store/orders/{orderNo}/approve`
3. `GET /api/store/orders/{orderNo}` (상태/이행상태/이력 확인)
4. `GET /api/store/orders`
5. `GET /api/store/orders/analytics`

## 5. 주요 실패 케이스
- 빈 `items`
- `requestedQuantity <= 0`
- 존재하지 않는 `orderNo`
- `cancelReason` 누락
- 상태전환 불가:
  - 승인: `REQUESTED`가 아닌 경우
