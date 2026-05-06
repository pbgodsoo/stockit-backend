# 매장 발주 MVP 구현계획 및 적용내역

## 1. 목표
- 매장 발주 기능을 FE 목업(Pinia local state)에서 BE+DB 기반으로 전환한다.
- 범위: 발주 요청/수정/취소/내역/상세/분석, 상태 이력 저장/조회, FE API 연동.

## 2. 상태 전략
- 주문 상태: `REQUESTED`, `APPROVED`, `COMPLETED`, `CANCELLED`
- 이행 상태: `fulfillment_status` 단일 컬럼 사용
  - `READY_TO_SHIP`, `IN_TRANSIT`, `ARRIVED`, `RECEIVED`
- 이번 단계는 이행 상태 저장/조회 중심 최소 구현.

## 3. DB 설계 반영
- 대상 테이블:
  - `store_order_header`
  - `store_order_item`
  - `store_order_status_history`
- 제약:
  - `order_no UNIQUE`
  - `UNIQUE(order_header_id, sku_id)`
  - `CHECK(total_sku_count >= 0)`
  - `CHECK(total_requested_quantity >= 0)`
  - `CHECK(requested_quantity > 0)`
  - `CHECK(unit_price >= 0)`
- 인덱스:
  - header: `(store_id, requested_at desc)`, `(status, requested_at desc)`
  - item: `(order_header_id)`, `(sku_id)`
  - history: `(order_header_id, changed_at desc)`
- 마이그레이션 파일:
  - `docs/table/V20260506_01__store_orders.sql`

## 4. 백엔드 구현 반영
- 패키지: `org.example.stockitbe.store.order`
- 구성:
  - `StoreOrderController`
  - `StoreOrderService`
  - `StoreOrderHeaderRepository`
  - `StoreOrderItemRepository`
  - `StoreOrderStatusHistoryRepository`
  - `StoreOrderDto`
  - `StoreOrderStatus`, `StoreOrderFulfillmentStatus`, `StoreOrderHistoryType`
  - `StoreOrderHeader`, `StoreOrderItem`, `StoreOrderStatusHistory`
- API:
  - `POST /api/store/orders`
  - `PUT /api/store/orders/{orderNo}`
  - `PATCH /api/store/orders/{orderNo}/cancel`
  - `GET /api/store/orders`
  - `GET /api/store/orders/{orderNo}`
  - `GET /api/store/orders/analytics`
- 에러코드:
  - `BaseResponseStatus` 4600번대 추가 (`STORE_ORDER_*`)

## 5. 프론트엔드 구현 반영
- API 파일 추가:
  - `src/api/store/orders.js`
- 구현 상태:
  - 백엔드 API 호출 모듈 추가 완료
  - 기존 `storeOrder` 스토어/주문 화면의 완전 전환은 후속 패치에서 단계적으로 적용 예정

## 6. 검증 결과
- FE 문법 검증:
  - `node --check src/api/store/orders.js` 통과
- BE 컴파일:
  - 로컬 환경 `JAVA_HOME` 미설정으로 `./gradlew :compileJava` 실행 불가

## 7. COLLAB_RULES 준수
- 파일 수정은 `apply_patch` 중심으로 수행
- PowerShell 인코딩 강제 변환/전체 덮어쓰기 방식 사용하지 않음
- JS 수정 후 `node --check` 수행

