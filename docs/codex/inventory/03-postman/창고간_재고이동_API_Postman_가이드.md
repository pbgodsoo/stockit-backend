# 창고간 재고 이동 API Postman 가이드

## 1. 개요
- 대상 API (전체 이동 흐름)
  - `GET /api/hq/warehouse-transfers/imbalanced-skus` (불균형 SKU 조회)
  - `GET /api/hq/warehouse-transfers/sku-distribution?skuCode=...` (SKU 창고 분포 조회)
  - `POST /api/hq/warehouse-transfers/execute` (이동 실행)
  - `GET /api/hq/warehouse-transfers` (이동 이력 조회)
  - `GET /api/hq/warehouse-transfers/{transferNo}` (이동 상세 조회)
- 공통 응답 형식: `BaseResponse`

## 2. 사전 준비
- Base URL 예시: `http://localhost:8080`
- 인증/권한
  - `Authorization: Bearer {{hqAccessToken}}`
  - `/api/hq/**`는 `ROLE_HQ` 권한 필요
- 재고/창고/상품 데이터 준비 필요

## 3. 권장 실행 순서
1. 불균형 SKU 확인
2. SKU 분포 확인
3. 이동 실행
4. 이동 이력 조회
5. 이동 상세 조회

## 4. API별 사용법

### 4.1 불균형 SKU 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/warehouse-transfers/imbalanced-skus`

성공 시 확인:
- `result[]`
- `skuCode`, `itemCode`, `itemName`, `color`, `size`, `category`
- `totalOnHand`, `totalAvailable`, `shortageWarehouseCount`, `totalShortageQty`

### 4.2 SKU 창고 분포 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/warehouse-transfers/sku-distribution`
- Query Params
  - `skuCode` (필수)

요청 예시:
- `{{baseUrl}}/api/hq/warehouse-transfers/sku-distribution?skuCode=SKU-00001`

성공 시 확인:
- `result[]`
- `warehouseCode`, `warehouseName`, `location`
- `onHandStock`, `reservedStock`, `availableStock`, `safetyStock`
- `status` (`정상` | `부족` | `품절`)
- `updatedAt`

### 4.3 이동 실행
- Method: `POST`
- URL: `{{baseUrl}}/api/hq/warehouse-transfers/execute`
- Request Body
  - `requestedBy` (선택, 미입력 시 서버 기본값 사용)
  - `lines[]` (필수, 1건 이상)

라인 스키마:
- `lineId` (선택)
- `skuCode` (필수)
- `fromWarehouseCode` (필수)
- `toWarehouseCode` (필수)
- `qty` (필수, `>= 1`)
- `reason` (선택)
- `memo` (선택)

요청 예시:
```json
{
  "requestedBy": "hq-admin",
  "lines": [
    {
      "lineId": "L1",
      "skuCode": "SKU-00001",
      "fromWarehouseCode": "WH-SU-0001",
      "toWarehouseCode": "WH-SU-0002",
      "qty": 10,
      "reason": "권역 수요 불균형",
      "memo": "긴급 이동"
    },
    {
      "lineId": "L2",
      "skuCode": "SKU-00002",
      "fromWarehouseCode": "WH-SU-0001",
      "toWarehouseCode": "WH-SU-0002",
      "qty": 4,
      "reason": "권역 수요 불균형"
    }
  ]
}
```

실행 규칙 메모:
- 서버는 `fromWarehouseCode + toWarehouseCode` 기준으로 라인을 그룹 처리
- 같은 route 그룹마다 `transferNo` 1건 생성

성공 시 확인:
- `result.requestedCount`, `successCount`, `failureCount`
- `result.lineResults[]`: 라인별 성공 여부와 `transferNo`
- `result.createdTransfers[]`: 생성된 이동 문서 요약

### 4.4 이동 이력 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/warehouse-transfers`
- Query Params (모두 선택)
  - `status`: `REQUESTED` | `IN_PROGRESS` | `COMPLETED` | `CANCELED`
  - `fromDate`: `yyyy-MM-dd`
  - `toDate`: `yyyy-MM-dd`
  - `keyword`: 이동번호/창고코드/창고명/SKU/품목/사유/메모 검색

요청 예시:
- `{{baseUrl}}/api/hq/warehouse-transfers`
- `{{baseUrl}}/api/hq/warehouse-transfers?status=IN_PROGRESS`
- `{{baseUrl}}/api/hq/warehouse-transfers?fromDate=2026-05-01&toDate=2026-05-09&keyword=WH-SU-0001`

성공 시 확인:
- `result[]`
- 헤더: `transferNo`, `fromWarehouseCode`, `toWarehouseCode`, `requestedBy`, `requestedAt`, `status`
- 집계: `skuCount`, `totalQty`, `reasonCount`, `memoCount`
- 상세 라인: `lines[]` (`skuCode`, `qty`, `reason`, `memo`, `fromStockBefore/After`, `toStockBefore/After`)

### 4.5 이동 상세 조회
- Method: `GET`
- URL: `{{baseUrl}}/api/hq/warehouse-transfers/{transferNo}`

요청 예시:
- `{{baseUrl}}/api/hq/warehouse-transfers/STF-20260509-00001`

성공 시 확인:
- 이력 조회와 동일한 구조의 단건 상세
- `lines[]`에서 SKU별 이동 수량/사유/메모 및 전후 가용재고 확인

## 5. 주요 검증/실패 포인트

### 5.1 요청 형식 오류
- 원인
  - `lines` 비어 있음, `qty < 1`, 필수 코드 누락
- 점검
  - JSON 구조와 필수 필드 재확인

### 5.2 동일 출발/도착 창고
- 원인
  - `fromWarehouseCode == toWarehouseCode`
- 점검
  - 출발/도착 창고를 서로 다르게 지정

### 5.3 가용재고 초과 이동
- 원인
  - `qty`가 출발 창고 가용재고보다 큼
- 점검
  - `sku-distribution` 조회로 가용재고 확인 후 수량 조정

### 5.4 SKU/창고 코드 불일치
- 원인
  - 미존재 SKU 코드, 미존재 창고 코드
- 점검
  - SKU/창고 마스터 데이터 확인 후 재시도

## 6. 빠른 테스트 세트
1) 불균형 SKU 조회
- `GET {{baseUrl}}/api/hq/warehouse-transfers/imbalanced-skus`

2) SKU 분포 조회
- `GET {{baseUrl}}/api/hq/warehouse-transfers/sku-distribution?skuCode=SKU-00001`

3) 이동 실행
- `POST {{baseUrl}}/api/hq/warehouse-transfers/execute`

4) 이동 이력 조회
- `GET {{baseUrl}}/api/hq/warehouse-transfers?status=IN_PROGRESS`

5) 이동 상세 조회
- `GET {{baseUrl}}/api/hq/warehouse-transfers/{transferNo}`
