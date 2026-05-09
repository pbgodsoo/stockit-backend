# 전사 재고 조회 API Postman 가이드

## 1. 개요
- 대상 API
  - `GET /api/hq/inventories/company-wide` (전사 재고 목록 조회)
  - `GET /api/hq/inventories/company-wide/{itemCode}/skus` (전사 재고 SKU 상세 조회)
- 공통 응답 형식: `BaseResponse`
- 문서 범위: 전사 조회 API만 포함 (순환 재고/창고간 이동 API는 별도 문서 참조)

## 2. 사전 준비
- 서버 실행 후 Base URL 확인
  - 예: `http://localhost:8080`
- 재고/인프라/상품 데이터가 먼저 준비되어 있어야 함
  - 예: `src/main/resources/sql/01-infrastructure_dummy_data.sql`
  - 예: `src/main/resources/sql/04-product_master_dummy_data.sql`
  - 예: `src/main/resources/sql/05-inventory_dummy_data.sql`

## 3. Postman 공통 설정
- Method: 모두 `GET`
- URL: `{{baseUrl}}` 환경변수 사용 권장
- Header
  - `Accept: application/json`
  - `Authorization: Bearer {{hqAccessToken}}`
- 인증/권한
  - 현재 보안 설정 기준 `/api/hq/**`는 `ROLE_HQ` 권한이 필요
  - 토큰 누락/만료 시 `401`, 권한 불일치 시 `403` 발생 가능

## 4. API별 사용법

### 4.1 전사 재고 목록 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/inventories/company-wide`
- Query Params (모두 선택)
  - `locationType`: `STORE` | `WAREHOUSE`
  - `locationIds`: Long 리스트 (반복 키 방식) 예) `locationIds=1&locationIds=2`
  - `parentCategory`: 상위 카테고리명 (정확 일치)
  - `childCategory`: 하위 카테고리명 (정확 일치)
  - `status`: `InventoryStatus` enum (`NORMAL`, `CIRCULAR_CANDIDATE`, `CIRCULAR`)
  - `keyword`: 품목코드/품목명/SKU코드/거점코드/거점명 부분 검색

요청 예시:
- `{{baseUrl}}/api/hq/inventories/company-wide`
- `{{baseUrl}}/api/hq/inventories/company-wide?locationType=WAREHOUSE`
- `{{baseUrl}}/api/hq/inventories/company-wide?locationType=STORE&locationIds=1&locationIds=2`
- `{{baseUrl}}/api/hq/inventories/company-wide?parentCategory=상의&childCategory=반팔티`
- `{{baseUrl}}/api/hq/inventories/company-wide?status=NORMAL&keyword=ITEM`

성공 시 확인:
- `result.items[]` 존재 여부
- `result.items[].itemCode`, `parentCategory`, `childCategory`, `itemName`
- `result.items[].actualStock`, `availableStock`, `safetyStock`, `updatedAt`
- `result.items[].status`는 UI 문자열 `정상` | `부족` | `품절`
- `result.locationOptions[]` 포함 여부
- `result.locationOptions[].id`, `code`, `name`

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
- `{{baseUrl}}/api/hq/inventories/company-wide/ITEM-0001/skus?status=NORMAL&keyword=BLACK`

성공 시 확인:
- `result[]` 반환
- 각 항목의 `skuCode`, `color`, `size`, `unitPrice`
- 각 항목의 `actualStock`, `availableStock`, `safetyStock`, `updatedAt`
- 각 항목의 `status`는 UI 문자열 `정상` | `부족` | `품절`

## 5. 자주 나는 오류와 점검 방법

### 5.1 조회 결과가 비어 있음
- 원인
  - `locationType`, `locationIds`, `status`, `keyword` 필터 조합이 너무 좁은 경우
  - 입력한 `itemCode`가 실제 데이터와 다른 경우
- 점검
  - 필터 없이 `GET /api/hq/inventories/company-wide` 먼저 호출
  - 이후 파라미터를 하나씩 추가하며 결과 비교

### 5.2 status 값으로 400 오류 발생
- 원인
  - enum 파라미터에 허용되지 않은 값 전달
- 점검
  - `NORMAL`, `CIRCULAR_CANDIDATE`, `CIRCULAR` 중 하나로 재시도

### 5.3 locationIds 필터 적용이 안 되는 것처럼 보임
- 원인
  - Postman에서 리스트 파라미터 전달 방식 오류
- 점검
  - `locationIds=1&locationIds=2` 형태로 같은 키를 반복 입력
  - 또는 Params 탭에서 `locationIds`를 여러 줄로 추가

### 5.4 인증 실패(401/403)
- 원인
  - HQ 권한 토큰 누락/만료/권한 불일치
- 점검
  - 로그인 후 발급된 `HQ` 계정 Access Token 사용
  - `Authorization: Bearer <token>` 형식 확인

## 6. 전사 재고 조회 테스트 권장 순서
1. 기본 목록 확인: `GET /api/hq/inventories/company-wide`
2. 위치 옵션 확인: `result.locationOptions[]`에서 `id/code/name` 확인
3. 필터 목록 조회: `locationType`, `locationIds`, `status`, `keyword` 순으로 필터 적용
4. 품목 SKU 상세 조회: `GET /api/hq/inventories/company-wide/{itemCode}/skus`

## 7. 빠른 테스트 세트

1) 전사 목록(기본)
- `GET {{baseUrl}}/api/hq/inventories/company-wide`

2) 전사 목록(창고 + 다중 locationIds)
- `GET {{baseUrl}}/api/hq/inventories/company-wide?locationType=WAREHOUSE&locationIds=1&locationIds=2`

3) 전사 목록(상태 + 키워드)
- `GET {{baseUrl}}/api/hq/inventories/company-wide?status=NORMAL&keyword=ITEM`

4) SKU 상세 조회
- `GET {{baseUrl}}/api/hq/inventories/company-wide/ITEM-0001/skus`
