# 전사 재고 조회 API Postman 가이드

## 1. 개요
- 대상 API
  - `GET /api/hq/inventories/company-wide` (전사 재고 목록 조회)
  - `GET /api/hq/inventories/company-wide/{itemCode}/skus` (전사 재고 SKU 상세 조회)
  - `GET /api/hq/inventories/circular-candidates` (순환재고 후보 조회)
  - `GET /api/hq/inventories/circular` (순환재고 조회)
- 공통 응답 형식: `BaseResponse`

## 2. 사전 준비
- 서버 실행 후 Base URL 확인
  - 예: `http://localhost:8080`
- 재고/인프라/상품 데이터가 먼저 준비되어 있어야 함
  - 예: `src/main/resources/sql/infrastructure_dummy_data.sql`
  - 예: `src/main/resources/sql/product_master_dummy_data.sql`
  - 예: `src/main/resources/sql/inventory_dummy_data.sql`

## 3. Postman 공통 설정
- Method: 모두 `GET`
- URL: `{{baseUrl}}` 환경변수 사용 권장
- Header
  - 조회 API 기준 필수 Header 없음
  - 필요 시 `Accept: application/json`
- 인증
  - 현재 코드 기준 인증 토큰 필수 아님

## 4. API별 사용법

### 4.1 전사 재고 목록 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/inventories/company-wide`
- Query Params (모두 선택)
  - `locationType`: `STORE` | `WAREHOUSE`
  - `locationIds`: Long 리스트 (반복 키 방식) 예) `locationIds=1&locationIds=2`
  - `parentCategory`: 상위 카테고리명
  - `childCategory`: 하위 카테고리명
  - `status`: `NORMAL` | `LOW` | `OUT`
  - `keyword`: 품목코드/품목명 검색어

요청 예시:
- `{{baseUrl}}/api/hq/inventories/company-wide`
- `{{baseUrl}}/api/hq/inventories/company-wide?locationType=WAREHOUSE`
- `{{baseUrl}}/api/hq/inventories/company-wide?locationType=STORE&locationIds=1&locationIds=2`
- `{{baseUrl}}/api/hq/inventories/company-wide?parentCategory=상의&childCategory=반팔티`
- `{{baseUrl}}/api/hq/inventories/company-wide?status=LOW&keyword=SKU-TOP`

성공 시 확인:
- `result.items[]` 존재 여부
- `result.items[].itemCode`, `itemName`, `actualStock`, `availableStock`, `safetyStock`, `status`
- `result.locationOptions[]`의 `id`, `code`, `name`

### 4.2 전사 재고 SKU 상세 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/inventories/company-wide/{itemCode}/skus`
- Path Variable
  - `itemCode` (필수): 조회할 품목 코드
- Query Params (모두 선택)
  - `locationType`, `locationIds`, `parentCategory`, `childCategory`, `status`, `keyword`

요청 예시:
- `{{baseUrl}}/api/hq/inventories/company-wide/ITEM-0001/skus`
- `{{baseUrl}}/api/hq/inventories/company-wide/ITEM-0001/skus?locationType=WAREHOUSE&locationIds=1`
- `{{baseUrl}}/api/hq/inventories/company-wide/ITEM-0001/skus?status=OUT`

성공 시 확인:
- `result[]` 반환
- 각 항목의 `skuCode`, `color`, `size`, `unitPrice`
- 각 항목의 `actualStock`, `availableStock`, `safetyStock`, `status`, `updatedAt`

### 4.3 순환재고 후보 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/inventories/circular-candidates`

요청 예시:
- `{{baseUrl}}/api/hq/inventories/circular-candidates`

성공 시 확인:
- `result[]` 반환
- 각 항목의 `inventoryId`, `skuCode`, `itemCode`, `itemName`
- `warehouseCode`, `warehouseName`, `convertibleStock`
- `matchedConditionCodes[]`

### 4.4 순환재고 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/inventories/circular`

요청 예시:
- `{{baseUrl}}/api/hq/inventories/circular`

성공 시 확인:
- `result[]` 반환
- 각 항목의 `inventoryId`, `skuCode`, `itemCode`, `itemName`
- `availableQuantity`, `materialType`, `materialKgPrice`, `unitWeightKg`, `totalWeightKg`, `circularSalePrice`
- `materialCompositions[]`의 `materialCode`, `materialNameKo`, `ratio`

## 5. 자주 나는 오류와 점검 방법

### 5.1 조회 결과가 비어 있음
- 원인:
  - `locationType`, `locationIds`, `status`, `keyword` 필터 조합이 너무 좁은 경우
- 점검:
  - 필터 없이 `GET /api/hq/inventories/company-wide` 먼저 호출
  - 이후 파라미터를 하나씩 추가하며 결과 비교

### 5.2 잘못된 itemCode로 SKU 상세 조회
- 원인:
  - `/company-wide/{itemCode}/skus`의 `itemCode` 오타 또는 미존재
- 점검:
  - `GET /api/hq/inventories/company-wide`에서 `itemCode`를 먼저 확인
  - 확인한 `itemCode`로 상세 조회 재시도

### 5.3 locationIds 필터 적용이 안 되는 것처럼 보임
- 원인:
  - Postman에서 리스트 파라미터 전달 방식 오류
- 점검:
  - `locationIds=1&locationIds=2` 형태로 같은 키를 반복 입력
  - 또는 Params 탭에서 `locationIds`를 여러 줄로 추가

### 5.4 status 값 오류
- 원인:
  - 허용값(`NORMAL`, `LOW`, `OUT`) 외 값 사용
- 점검:
  - Query의 `status`를 허용값 중 하나로 변경

## 6. 전사 재고 조회 테스트 권장 순서
1. 기본 목록 확인: `GET /api/hq/inventories/company-wide`
2. 위치 옵션 확인: `result.locationOptions[]`에서 `id/code/name` 확인
3. 필터 목록 조회: `locationType`, `locationIds`, `status`, `keyword` 순으로 필터 적용
4. 품목 SKU 상세 조회: `GET /api/hq/inventories/company-wide/{itemCode}/skus`
5. 순환재고 후보 조회: `GET /api/hq/inventories/circular-candidates`
6. 순환재고 조회: `GET /api/hq/inventories/circular`

## 7. 빠른 테스트 세트

1) 전사 목록(기본)
- `GET {{baseUrl}}/api/hq/inventories/company-wide`

2) 전사 목록(창고 + 다중 locationIds)
- `GET {{baseUrl}}/api/hq/inventories/company-wide?locationType=WAREHOUSE&locationIds=1&locationIds=2`

3) 전사 목록(상태 + 키워드)
- `GET {{baseUrl}}/api/hq/inventories/company-wide?status=LOW&keyword=ITEM`

4) SKU 상세 조회
- `GET {{baseUrl}}/api/hq/inventories/company-wide/ITEM-0001/skus`

5) 순환재고 후보 조회
- `GET {{baseUrl}}/api/hq/inventories/circular-candidates`

6) 순환재고 조회
- `GET {{baseUrl}}/api/hq/inventories/circular`
