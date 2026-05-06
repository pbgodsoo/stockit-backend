# 매장 발주/출고/입고 도메인 분리 MVP 설계 보고서

## 1. 작성 목적
- 현재 매장 발주 흐름에서 승인 이후 출고/입고까지 연결되는 업무를 MVP 범위로 안정적으로 구현하기 위한 설계안을 정리한다.
- 발주 도메인과 물류(출고), 매장(입고) 도메인의 책임을 분리하면서도 상태 정합성을 유지하는 구현 방향을 확정한다.
- 향후 물류창고간 재고이동, 순환재고 판매 기능 추가 시에도 확장 가능한 공통 출고 구조를 선제적으로 반영한다.

## 2. 배경 및 핵심 요구사항
- 발주 상태는 4단계로 관리한다.
  - `REQUESTED`(승인대기), `APPROVED`(승인완료), `COMPLETED`(종료), `CANCELLED`(취소)
- 출고/입고 상태는 별도 흐름으로 관리한다.
  - 출고: `READY_TO_SHIP -> IN_TRANSIT -> ARRIVED`
  - 입고: `PENDING_RECEIPT -> RECEIVED`
- 발주 승인 완료 시점에 다음이 즉시 생성되어야 한다.
  - 물류창고 출고 리스트 건
  - 매장 입고 리스트 건
- 매장 입고 조회 내역에서는 입고 상태뿐 아니라 출고 상태(출고준비/배송중/배송완료)도 함께 표시해야 한다.
- MVP 단계에서는 Kafka 등 메시징 미적용이며, 현재 DB/트랜잭션 기반으로 동작해야 한다.

## 3. 도메인 경계(책임 분리)
### 3.1 발주 도메인 (`store-order`)
- 책임
  - 발주 생성/수정/취소/승인/종료
  - 발주 헤더/라인/발주상태 이력 관리
- 상태 책임
  - `REQUESTED`, `APPROVED`, `COMPLETED`, `CANCELLED`
- 비고
  - 출고/입고 세부 전이는 직접 소유하지 않는다.
  - 단, 발주 상세/목록에서 필요한 진행 상태는 조회 조합으로 제공한다.

### 3.2 출고 도메인 (`warehouse-shipping`)
- 책임
  - 출고 문서 생성, 출고 진행 상태 전이, 재고 차감
  - 출고 이력 관리
- 상태 책임
  - `READY_TO_SHIP`, `IN_TRANSIT`, `ARRIVED`
- 확장 포인트
  - 원천(source) 3종을 단일 모델로 수용
    - 매장 발주
    - 물류창고간 재고이동
    - 순환재고 판매

### 3.3 입고 도메인 (`store-receiving`)
- 책임
  - 입고 문서 생성, 입고 확정, 매장 재고 반영
  - 입고 이력 관리
- 상태 책임
  - `PENDING_RECEIPT`, `RECEIVED`
- 비고
  - MVP에서는 부분입고/검수 미포함

## 4. 테이블 설계 방향
## 4.1 기존 유지 테이블(발주)
- `store_order_header`
- `store_order_item`
- `store_order_status_history`

## 4.2 신규 권장 테이블(출고)
- `wh_outbound_header`
- `wh_outbound_item`
- `wh_outbound_status_history`

### 출고 공통 헤더 핵심 컬럼 예시
- `outbound_no` (유니크)
- `source_type` (`STORE_ORDER`, `WAREHOUSE_TRANSFER`, `CIRCULAR_SALE`)
- `source_ref_no` (원천 문서 번호)
- `warehouse_id`, `destination_type`, `destination_id`
- `status` (`READY_TO_SHIP`, `IN_TRANSIT`, `ARRIVED`)
- `created_at`, `confirmed_at`, `departed_at`, `arrived_at`
- `created_by`, `updated_by`

### 출고 1:1 보장 권장 제약
- `UNIQUE(source_type, source_ref_no)`
  - 원천 문서 당 출고 헤더 중복 생성 방지

## 4.3 신규 권장 테이블(입고)
- `store_inbound_header`
- `store_inbound_item`
- `store_inbound_status_history`

### 입고 헤더 핵심 컬럼 예시
- `inbound_no` (유니크)
- `source_type` (MVP: `STORE_ORDER`)
- `source_ref_no` (발주 번호)
- `outbound_no` (연결 출고 문서)
- `store_id`
- `status` (`PENDING_RECEIPT`, `RECEIVED`)
- `created_at`, `received_at`
- `created_by`, `received_by`

### 입고 생성 시점
- 발주 승인 완료와 동일 트랜잭션에서 `PENDING_RECEIPT` 상태로 생성

## 4.4 최소 테이블 수 결론
- 기존 3개(발주) + 신규 6개(출고/입고) = 총 9개 권장
- 이유
  - 상태 책임 분리
  - 이력 누락 방지
  - 원천 3종 통합 출고 구조 확보
  - 추후 기능 확장 시 재설계 비용 최소화

## 5. 상태 전이 및 업무 시나리오
### 5.1 정상 시나리오
1. 매장 발주 생성: `REQUESTED`
2. 발주 배치 승인: 발주 `APPROVED`
3. 승인 트랜잭션에서 출고/입고 문서 즉시 생성
   - 출고: `READY_TO_SHIP`
   - 입고: `PENDING_RECEIPT`
4. 물류창고 출고 확정(재고 차감): 출고 `IN_TRANSIT`
5. 배송 완료 처리: 출고 `ARRIVED`
6. 매장 입고 확정(재고 반영): 입고 `RECEIVED`
7. 입고 확정 완료 후 발주 `COMPLETED`

### 5.2 입고 조회에서 출고 상태 표시
- 입고 목록/상세 조회 API에서 `store_inbound_header`와 `wh_outbound_header`를 조인해 제공
- 입고 도메인은 입고 상태를 소유하고, 출고 상태는 조회용 필드로 포함

## 6. MVP에서의 이벤트 기반 구현 방식 (메시징 미사용)
## 6.1 가능 여부
- 가능하다.
- Kafka 없이도 "이벤트 기반 스타일" 구현이 가능하다.

## 6.2 적용 방식
- 발주 승인 서비스에서 도메인 이벤트 객체(`OrderApproved`)를 발행
- 동기 핸들러(또는 동일 서비스 오케스트레이션)에서
  - 출고 헤더/라인 생성
  - 입고 헤더/라인 생성
- 같은 DB 트랜잭션에서 처리하여 즉시 가시성 확보

## 6.3 향후 전환성
- 이벤트 타입/페이로드 계약을 먼저 고정하면
  - 추후 Outbox + Kafka 구조로 무리 없이 전환 가능


## 6.4 Kafka 도입 시 기대 성능효과와 전제조건
- 이벤트 스타일로 선구현하면, 이후 Kafka 도입 시 성능 개선 체감을 얻을 가능성이 크다.
- 성능 이점의 본질은 Kafka 자체보다 동기 결합된 후속 처리를 비동기 분리하는 데 있다.
- 특히 다음 조건에서 개선 폭이 커진다.
  - 승인/출고/입고 이후 후속 작업이 많아 API 응답이 길어지는 경우
  - 피크 트래픽에서 DB 락 대기, 재시도, 타임아웃이 잦은 경우
  - 알림, 집계, 감사로그 등 부가 처리량이 증가하는 경우
- 운영 측면에서는 다음 전제를 반드시 반영한다.
  - 소비자 멱등성 보장(중복 이벤트 안전 처리)
  - 재처리/실패 복구 전략(DLQ 또는 보상 재시도)
  - 이벤트 순서/지연 허용 범위에 대한 업무 합의
- 결론적으로 MVP에서는 동기 트랜잭션 기반 이벤트 스타일을 유지하고, 트래픽/후처리 복잡도가 증가하는 시점에 Outbox + Kafka로 단계 전환하는 전략을 권장한다.

## 7. 구현 순서(권장)
1. 출고 도메인 테이블 3종 DDL + 엔티티/리포지토리
2. 발주 승인 배치 API 구현 또는 확장 (`REQUESTED -> APPROVED`)
3. 승인 시 출고/입고 생성 오케스트레이션 연동
4. 출고 상태 전이 API 구현 (`READY_TO_SHIP -> IN_TRANSIT -> ARRIVED`)
5. 입고 도메인 테이블 3종 DDL + 엔티티/리포지토리
6. 입고 확정 API 구현 (`PENDING_RECEIPT -> RECEIVED`) 및 매장 재고 반영
7. 입고 확정 시 발주 `COMPLETED` 반영
8. 조회 API 보강
   - 발주 조회: 출고/입고 진행상태 요약 포함
   - 입고 조회: 출고 상태 포함

## 7.1 출고 테이블 상세 설계(1단계 확정안)
- 이번 1단계에서는 통합 출고 공통 모델을 먼저 확정한다.
- 테이블은 `wh_outbound_header / wh_outbound_item / wh_outbound_status_history` 3종으로 구성한다.

### Header(wh_outbound_header)
- 문서 식별: `outbound_no` (유니크)
- 원천 추적: `source_type` + `source_ref_no` (유니크), `source_ref_id`
- 출고 주체: `warehouse_id`
- 도착지: `destination_type` + `destination_id`
- 상태: `READY_TO_SHIP / IN_TRANSIT / ARRIVED`
- 시점: `requested_at / confirmed_at / departed_at / arrived_at`
- 생성/갱신 감사: `create_date / update_date`

### Item(wh_outbound_item)
- 헤더 참조: `outbound_header_id`
- 원천 라인 추적: `source_line_ref_id`
- 상품 스냅샷: `sku_id, sku_code, product_code, product_name, main/sub category, color, size, unit_price`
- 수량: `requested_quantity`, `confirmed_quantity`

### Status History(wh_outbound_status_history)
- 헤더 참조: `outbound_header_id`
- 상태 이력: `status, changed_at, changed_by_member_id, changed_by_name, reason`
- 정렬 인덱스: `(outbound_header_id, changed_at, id)`

### 원천(Source) 표준
- `STORE_ORDER`: 매장 발주 승인으로 생성
- `WAREHOUSE_TRANSFER`: 물류창고간 이동 요청으로 생성
- `CIRCULAR_SALE`: 순환재고 판매 확정으로 생성

### 목적지(Destination) 표준
- `STORE`, `WAREHOUSE`, `CIRCULAR_BUYER`

## 8. 예외/검증 기준 (MVP)
- 상태 전이 불일치 시 공통 예외 코드 반환
  - 예: 이미 `IN_TRANSIT`인데 출고 확정 재요청
  - 예: `ARRIVED` 전 입고 확정 요청
- 원천 문서-출고 1:1 제약 위반 시 중복 생성 차단
- 발주 취소 가능 조건은 기존 규칙 유지(승인 전 중심)

## 9. 요약 결론
- 출고/입고를 발주에서 분리하는 방향은 타당하다.
- 다만 발주 상태는 출고/입고 결과에 영향받으므로, 도메인 간 연계 규칙을 명확히 정의해야 한다.
- MVP에서는 동기 트랜잭션 기반 이벤트 스타일로 구현하고, 향후 성능개선 시 메시징으로 전환하는 2단계 전략이 가장 안전하다.
