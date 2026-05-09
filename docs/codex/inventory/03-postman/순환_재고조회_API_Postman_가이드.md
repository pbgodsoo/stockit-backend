# 순환 재고 조회 API Postman 가이드

## 1. 개요
- 대상 API
  - `POST /api/hq/inventories/circular-candidates/refresh` (순환 재고 후보 재산정)
  - `GET /api/hq/inventories/circular-candidates` (순환 재고 후보 조회)
  - `GET /api/hq/inventories/circular` (순환 재고 조회)
- 공통 응답 형식: `BaseResponse`

## 2. 사전 준비
- Base URL 예시: `http://localhost:8080`
- 인증/권한
  - `Authorization: Bearer {{hqAccessToken}}`
  - `/api/hq/**`는 `ROLE_HQ` 권한 필요
- 후보/순환 재고를 확인하려면 재고 데이터가 충분해야 함

## 3. 권장 실행 순서
1. 후보 재산정: `POST /api/hq/inventories/circular-candidates/refresh`
2. 후보 조회: `GET /api/hq/inventories/circular-candidates`
3. 순환 조회: `GET /api/hq/inventories/circular`

## 4. API별 사용법

### 4.1 순환 재고 후보 재산정
- Method: `POST`
- URL: `{{baseUrl}}/api/hq/inventories/circular-candidates/refresh`
- Request Body: 없음

성공 시 확인:
- `result.scannedCount`: 스캔한 NORMAL 재고 수
- `result.convertedCount`: 후보 상태로 전환된 건수

### 4.2 순환 재고 후보 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/inventories/circular-candidates`

성공 시 확인:
- `result[]` 반환
- 주요 필드
  - `inventoryId`, `skuCode`, `itemCode`, `itemName`
  - `parentCategory`, `childCategory`
  - `warehouseCode`, `warehouseName`
  - `color`, `size`
  - `actualStock`, `availableStock`, `convertibleStock`
  - `updatedAt`
  - `matchedConditionCodes[]` (후보 조건 코드)

### 4.3 순환 재고 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/inventories/circular`

성공 시 확인:
- `result[]` 반환
- 주요 필드
  - `inventoryId`, `skuCode`, `itemCode`, `itemName`
  - `warehouseCode`, `warehouseName`
  - `parentCategory`, `childCategory`, `color`, `size`
  - `availableQuantity`
  - `materialType`
  - `materialCompositions[]` (`materialCode`, `materialNameKo`, `ratio`)
  - `materialKgPrice`, `unitWeightKg`, `totalWeightKg`, `circularSalePrice`

## 5. 빈 결과/조건 미충족 케이스

### 5.1 후보 조회 결과가 비어 있음
- 원인
  - 후보 재산정 미실행
  - 후보 조건에 맞는 재고가 없음
  - 창고(`WAREHOUSE`) 재고가 아님
- 점검
  - refresh API 먼저 실행
  - 기본 전사 재고 조회에서 창고 재고와 수량 확인

### 5.2 순환 조회 결과가 비어 있음
- 원인
  - 아직 후보를 순환(`CIRCULAR`) 상태로 전환하지 않음
- 점검
  - 전환 API(`POST /api/hq/inventories/circular-candidates/convert`) 실행 후 재조회

### 5.3 인증 실패(401/403)
- 원인
  - HQ 토큰 누락/만료/권한 불일치
- 점검
  - HQ 계정 토큰 재발급 후 재호출

## 6. 빠른 테스트 세트
1) 후보 재산정
- `POST {{baseUrl}}/api/hq/inventories/circular-candidates/refresh`

2) 후보 조회
- `GET {{baseUrl}}/api/hq/inventories/circular-candidates`

3) 순환 조회
- `GET {{baseUrl}}/api/hq/inventories/circular`
