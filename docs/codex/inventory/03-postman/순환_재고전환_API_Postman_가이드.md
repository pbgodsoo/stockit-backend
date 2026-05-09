# 순환 재고 전환 API Postman 가이드

## 1. 개요
- 대상 API
  - `POST /api/hq/inventories/circular-candidates/convert`
- 기능
  - `CIRCULAR_CANDIDATE` 상태 재고를 `CIRCULAR` 재고로 전환
  - 다건 요청 가능 (배열 입력)
- 공통 응답 형식: `BaseResponse`

## 2. 사전 준비
- Base URL 예시: `http://localhost:8080`
- 인증/권한
  - `Authorization: Bearer {{hqAccessToken}}`
  - `/api/hq/**`는 `ROLE_HQ` 권한 필요
- 전환 대상 후보 재고가 있어야 함
  - 필요 시 먼저 `POST /api/hq/inventories/circular-candidates/refresh` 실행

## 3. API 사용법
- Method: `POST`
- URL: `{{baseUrl}}/api/hq/inventories/circular-candidates/convert`
- Request Body: JSON 배열

요청 스키마:
- `inventoryId` (Long, 필수)
- `convertQuantity` (Integer, 필수, `>= 1`)

요청 예시(전체 성공):
```json
[
  {
    "inventoryId": 101,
    "convertQuantity": 10
  },
  {
    "inventoryId": 102,
    "convertQuantity": 5
  }
]
```

요청 예시(부분 성공):
```json
[
  {
    "inventoryId": 101,
    "convertQuantity": 3
  },
  {
    "inventoryId": 999999,
    "convertQuantity": 2
  },
  {
    "inventoryId": 103,
    "convertQuantity": 99999
  }
]
```

## 4. 응답 확인 포인트
- 집계 필드
  - `result.requestedCount`: 요청 라인 수
  - `result.convertedCount`: 성공 건수
  - `result.skippedCount`: 실패/스킵 건수
- 상세 필드
  - `result.items[]`
  - `inventoryId`, `requested`, `converted`, `reason`

성공 항목 예시:
- `reason: "SUCCESS"`
- `requested == converted`

실패 항목 예시:
- `converted: 0`
- `reason`에 실패 사유 문자열 포함

## 5. 실패 사유(주요 유형)
- `유효하지 않은 전환 수량입니다.`
  - `inventoryId`가 null이거나 `convertQuantity <= 0`
- `재고를 찾을 수 없습니다.`
  - 대상 `inventoryId` 미존재
- `순환 재고 후보 상태가 아닙니다.`
  - 대상 재고 상태가 `CIRCULAR_CANDIDATE`가 아님
- `창고 재고만 전환할 수 있습니다.`
  - 창고(`WAREHOUSE`) 재고가 아님
- `전환 가능 재고를 초과했습니다.`
  - 요청 수량이 `availableQuantity`보다 큼

## 6. 전환 제약 및 동작 메모
- 전환 제약
  - 후보 상태여야 전환 가능
  - 창고 재고만 전환 가능
  - 전환 수량은 전환 가능 수량 이내여야 함
- 처리 동작
  - 요청 라인 단위로 성공/실패 판정
  - 실패 라인이 있어도 다른 라인은 계속 처리되어 부분 성공 가능

## 7. 빠른 테스트 세트
1) 후보 조회
- `GET {{baseUrl}}/api/hq/inventories/circular-candidates`

2) 전환 실행
- `POST {{baseUrl}}/api/hq/inventories/circular-candidates/convert`

3) 결과 확인
- `GET {{baseUrl}}/api/hq/inventories/circular`
