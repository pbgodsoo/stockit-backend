# 발주 API Postman 가이드

## 1. 개요
- 대상 API
  - POST /api/store/orders
  - PUT /api/store/orders/{orderNo}
  - PATCH /api/store/orders/{orderNo}/cancel
  - GET /api/store/orders
  - GET /api/store/orders/{orderNo}
  - GET /api/store/orders/analytics
- 공통 응답 포맷: BaseResponse

## 2. 사전 준비
- Base URL: http://localhost:8080
- Header: Content-Type: application/json

## 3. API별 요청

### 3.1 발주 생성
- POST {{baseUrl}}/api/store/orders

```json
{
  "storeCode": "STORE-001",
  "storeLocationId": "1",
  "requestedByMemberId": "manager01",
  "requestedByName": "홍길동",
  "memo": "긴급 발주",
  "items": [
    { "skuCode": "SKU-001", "requestedQuantity": 10 },
    { "skuCode": "SKU-002", "requestedQuantity": 5 }
  ]
}
```

### 3.2 발주 수정
- PUT {{baseUrl}}/api/store/orders/{orderNo}

```json
{
  "memo": "수량 조정",
  "items": [
    { "skuCode": "SKU-001", "requestedQuantity": 7 },
    { "skuCode": "SKU-003", "requestedQuantity": 4 }
  ]
}
```

### 3.3 발주 취소
- PATCH {{baseUrl}}/api/store/orders/{orderNo}/cancel

```json
{
  "cancelReason": "매장 사정으로 취소",
  "cancelledByMemberId": "manager01",
  "cancelledByName": "홍길동"
}
```

### 3.4 발주 목록 조회
- GET {{baseUrl}}/api/store/orders
- Query
  - storeCode
  - status (REQUESTED|APPROVED|COMPLETED|CANCELLED)
  - from (yyyy-MM-dd)
  - to (yyyy-MM-dd)
  - keyword

### 3.5 발주 상세 조회
- GET {{baseUrl}}/api/store/orders/{orderNo}

### 3.6 발주 분석 조회
- GET {{baseUrl}}/api/store/orders/analytics
- Query
  - storeCode
  - from (yyyy-MM-dd)
  - to (yyyy-MM-dd)

## 4. 오류 검증 케이스
- 빈 items
- requestedQuantity <= 0
- 존재하지 않는 orderNo
- cancelReason 누락
- 존재하지 않는 storeCode/skuCode

## 5. 추천 테스트 순서
1. POST /api/store/orders
2. GET /api/store/orders
3. GET /api/store/orders/{orderNo}
4. PUT /api/store/orders/{orderNo}
5. PATCH /api/store/orders/{orderNo}/cancel
6. GET /api/store/orders/analytics
