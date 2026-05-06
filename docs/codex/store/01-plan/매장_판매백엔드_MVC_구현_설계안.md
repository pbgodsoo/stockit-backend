# 매장 판매백엔드 MVC 구현 설계안 (v1)

## 1. 목표 요약
- 범위: 매장 `판매 등록`, `판매 내역` 백엔드 구현
- 제외: 판매 분석, 취소/환불, 결제수단/고객정보
- 아키텍처: `Controller + Service + Repository + Entity + Dto(inner class)`
- 동시성: `PESSIMISTIC_WRITE` 락 + 단일 트랜잭션으로 재고 oversell 방지

## 2. DB/DDL 설계
적용 파일:
- `src/main/resources/sql/migration/V20260505_01__store_sales.sql`

핵심 테이블:
- `store_sale_header`
  - `sale_no UNIQUE`
  - `store_id` (FK -> infrastructure.id)
  - `sold_at`, `total_quantity`, `total_amount`, `status(COMPLETED)`
  - `CHECK(total_quantity >= 0, total_amount >= 0)`
- `store_sale_item`
  - `sale_header_id` (FK -> store_sale_header.id)
  - `sku_id` (FK -> product_sku.id)
  - 스냅샷: `sku_code`, `product_code`, `product_name`, `main_category`, `sub_category`, `color`, `size`
  - `quantity`, `unit_price`, `line_amount`
  - `CHECK(quantity > 0, unit_price >= 0, line_amount >= 0)`

인덱스:
- `idx_store_sale_header_store_soldat (store_id, sold_at DESC)`
- `idx_store_sale_item_sale_header_id (sale_header_id)`
- `idx_store_sale_item_sku_id (sku_id)`

## 3. 판매번호 정책
- 규칙: `SALE-{yyyyMMdd}-{id 최소 5자리 zero-pad}`
- 예:
  - `id=1 -> SALE-20260505-00001`
  - `id=123456 -> SALE-20260505-123456`
- 방식:
  1. 헤더를 먼저 저장해서 PK `id` 확보
  2. `sold_at` 날짜 + `id`로 `sale_no` 생성
  3. 헤더에 `sale_no` 반영

장점:
- `max+1` 계산이 없어 동시성 충돌 없음
- `sale_no UNIQUE`로 DB 레벨 무결성 보강

## 4. API 명세 (v1)
베이스 경로:
- `/api/store/sales`

### 4.1 판매 확정
- `POST /api/store/sales`
- Request
```json
{
  "storeCode": "STORE-GANGNAM-01",
  "items": [
    { "skuCode": "SKU-TOP-SS-001-BLK-S", "quantity": 2 },
    { "skuCode": "SKU-PNT-DN-001-IND-30", "quantity": 1 }
  ]
}
```
- Response(result)
```json
{
  "saleNo": "SALE-20260505-00001",
  "storeCode": "STORE-GANGNAM-01",
  "soldAt": "2026-05-05T08:55:00.000+00:00",
  "totalQuantity": 3,
  "totalAmount": 127000,
  "items": [
    {
      "skuCode": "SKU-TOP-SS-001-BLK-S",
      "productCode": "PRD-TOP-SS-001",
      "productName": "베이직 코튼 반팔 티셔츠",
      "mainCategory": "상의",
      "subCategory": "반팔",
      "color": "블랙",
      "size": "S",
      "quantity": 2,
      "unitPrice": 29000,
      "lineAmount": 58000
    }
  ]
}
```

### 4.2 판매 목록
- `GET /api/store/sales?storeCode=&from=&to=&keyword=`
- 파라미터
  - `storeCode` optional
  - `from`, `to` optional (`yyyy-MM-dd`)
  - `keyword` optional (`saleNo`, 상품명 대상)

### 4.3 판매 상세
- `GET /api/store/sales/{saleNo}`
- 판매 헤더 + 아이템 라인 전체 반환

## 5. 레이어별 책임
- Controller
  - 요청/응답 바인딩, 검증 트리거, `BaseResponse` 래핑
- Service
  - 판매 규칙 검증
  - 재고 락/차감
  - 판매 저장/조회 로직
- Repository
  - 헤더/아이템 조회 및 저장
  - 재고 락 조회 (`findBySkuIdAndLocationIdForUpdate`)
- Entity
  - 판매 헤더/라인, 상태 모델
  - 재고 엔티티 내부 판매 차감 메서드
- Dto
  - `StoreSaleDto` 단일 파일 + `static inner class`

## 6. 동시성/트랜잭션 설계
- `createSale`는 단일 트랜잭션으로 처리
- SKU별 재고 조회 시 `PESSIMISTIC_WRITE` 락 사용
- 처리 순서:
  1. SKU/수량/매장 유효성 검증
  2. 재고 행 락 획득
  3. 재고 충분성 검증
  4. 재고 차감
  5. 판매 헤더/아이템 저장
- 실패 시 전체 롤백(부분 차감/부분 저장 금지)

## 7. 테스트 기준
- 정상
  - 다중 SKU 판매 시 헤더 1건 + 아이템 N건 + 재고 차감 일치
  - `sale_no` 형식 검증
- 실패
  - 빈 항목, 수량 0 이하, 존재하지 않는 SKU
  - 재고 부족 시 전체 롤백
- 동시성
  - 동일 SKU 동시 판매 요청에서 oversell 미발생
- 조회
  - 목록 필터/검색 정합성
  - 상세 합계(`sum(lineAmount) == totalAmount`) 정합성

