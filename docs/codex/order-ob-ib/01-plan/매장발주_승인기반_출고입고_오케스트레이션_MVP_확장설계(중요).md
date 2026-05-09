# 매장발주 승인 기반 출고/입고 오케스트레이션 MVP 확장 설계

## 1. 문서 목적
- 매장 발주 승인 시 출고/입고를 자동 생성하는 오케스트레이션을 MVP 범위에서 안정적으로 구현하기 위한 확정안을 정리한다.
- 동기 트랜잭션 기반 처리 원칙, 재고 정책, 분할 배송(1순위/2순위 창고), 상태/조회 계약을 함께 정의한다.

## 2. 현재 코드/구조 점검 결과
### 2.1 이미 구현된 내용
- 발주 배치 승인 진입점 존재
  - `StoreOrderBatchApproveService -> StoreOrderService.approveByBatch(...)`
- 출고/입고 도메인 엔티티/리포지토리/상태 이력 테이블 존재
  - 출고: `wh_outbound_header/item/status_history`
  - 입고: `store_inbound_header/item/status_history`
- 매장-창고 매핑 모델에 `PRIMARY/BACKUP` 역할 존재
  - `StoreWarehouseRole.PRIMARY`, `StoreWarehouseRole.BACKUP`

### 2.2 미구현/문제점
- 발주 승인 시 출고/입고 자동 생성 오케스트레이션 미구현
- 출고/입고 최초 상태 이력(`READY_TO_SHIP`, `PENDING_RECEIPT`) 누락
- 발주 승인 로직에 매장 재고 증가가 포함되어 도메인 시점 불일치
  - 매장 재고 증가는 입고 확정(`RECEIVED`) 시점이 맞음
- 2순위 창고 폴백(재고 부족 시 BACKUP 사용) 로직 미구현
- 현 제약으로는 분할 출고/분할 입고 표현 한계

## 3. 처리 방식 확정
### 3.1 트랜잭션/오케스트레이션
- MVP는 **동기 오케스트레이션 + 단일 `@Transactional` 경계**로 구현한다.
- 승인/출고생성/입고생성/각 상태이력 중 하나라도 실패하면 전체 롤백한다.
- 비동기(Kafka/Outbox) 전환은 차기 단계로 분리한다.

### 3.2 상태 모델
- 발주 상태 enum은 유지한다.
  - `REQUESTED -> APPROVED -> COMPLETED` (`CANCELLED` 별도)
- 출고 상태 유지
  - `READY_TO_SHIP -> IN_TRANSIT -> ARRIVED`
- 입고 상태 유지
  - `PENDING_RECEIPT -> RECEIVED`
- 발주 조회용 파생 상태(`inboundProgress`)를 추가한다. (DB 저장 아님)
  - `NOT_STARTED`, `PARTIAL`, `FULL`

### 3.3 번호 생성 규칙
- 출고번호: `WOB-YYYYMMDD-00001`
- 입고번호: `SIB-YYYYMMDD-00001`

## 4. 재고 정책 확정
## 4.1 용어/필드 매핑
- `quantity`: 실물 재고(on-hand)
- `reserved_quantity`: 출고 예정 예약 재고
- `in_transit_quantity`: 이동 중 재고
- `available_quantity`: 가용 재고

## 4.2 시점별 재고 반영 정책
- 발주 승인 시: 출고지 창고 재고를 `reserved`로 잠금
- 출고 확정(`IN_TRANSIT`) 시: `reserved -> in_transit` 전이 및 실차감 정책 반영
- 입고 확정(`RECEIVED`) 시: 매장 `quantity` 증가

참고:
- 현재 코드의 `increaseAvailable` 기반 처리(승인 시 매장 가용 증가)는 본 정책과 불일치하므로 정리 대상이다.

## 5. 재고 부족/폴백 출고 정책
### 5.1 기본 정책
- 1차: `PRIMARY` 창고 할당 시도
- 2차: 부족 수량은 `BACKUP` 창고로 폴백 할당
- 1+2순위 합산으로 전량 충족 가능하면 승인 성공
- 1+2순위 합산으로도 부족하면 승인 실패(반려) + 사유 기록, 전체 롤백

### 5.2 부분 승인 정책
- MVP에서는 부분 승인(일부만 승인)은 미적용
- 정책 단순화를 위해 전량 충족 시 승인, 미충족 시 실패 원칙 적용

## 6. 분할 배송/입고 확정 정책
### 6.1 채택안
- **출고건별 입고 헤더 분리**를 채택한다.
  - 예: 기본창고 출고 1건 + 백업창고 출고 1건이면 입고도 2건 생성
  - 각 입고건은 도착 시점마다 독립적으로 `RECEIVED` 처리

### 6.2 채택 이유
- 도착 시점 불일치 대응 용이
- 추적/감사/CS 대응에 유리
- 부분입고 전용 모델(입고 1건 + 수량 분할)보다 MVP 복잡도 낮음

## 7. 발주 완료 조건 및 조회 계약
### 7.1 완료 조건
- 발주에 연결된 모든 입고건이 `RECEIVED`일 때 `COMPLETED`

### 7.2 조회 파생값
- `totalInboundCount`: 발주 연계 입고 총 건수
- `receivedInboundCount`: `RECEIVED` 입고 건수
- `inboundProgress`:
  - `NOT_STARTED`: 0/N
  - `PARTIAL`: 1~N-1/N
  - `FULL`: N/N

### 7.3 FE 노출 권장
- 발주 목록: `입고 n/m` 또는 `NOT_STARTED/PARTIAL/FULL` 뱃지
- 발주 상세: 입고건 목록(건별 상태, 출고창고, 예정도착, 입고확정 액션)

## 8. 테이블 변경 설계
아래는 분할 출고/분할 입고를 위한 최소 변경안이다.

| 테이블 | 변경 유형 | 대상 | 현재 | 변경안 | 목적 |
|---|---|---|---|---|---|
| `wh_outbound_header` | 제약 변경 | `uk_wh_outbound_source_ref (source_type, source_ref_no)` | 유니크 | 제거 | 발주 1건에 출고 N건 허용 |
| `wh_outbound_header` | 컬럼 추가 | `source_ref_seq` | 없음 | `INT NOT NULL DEFAULT 1` | source 내 출고 순번 |
| `wh_outbound_header` | 제약 추가 | `uk_wh_outbound_source_ref_seq` | 없음 | `(source_type, source_ref_no, source_ref_seq)` 유니크 | 출고건 유일성 보장 |
| `store_inbound_header` | 제약 변경 | `uk_store_inbound_source_ref (source_ref_no)` | 유니크 | 제거 | 발주 1건에 입고 N건 허용 |
| `store_inbound_header` | 제약 추가 | `outbound_no` | 일반 인덱스 | 유니크(또는 FK+유니크) | 입고-출고 1:1 연결 보장 |
| `store_inbound_header` | 선택 컬럼 | `delivery_group_no` | 없음 | `VARCHAR` 선택 추가 | 분할배송 그룹 조회 편의 |

주의:
- `inboundProgress`는 저장 컬럼이 아니라 조회 시 계산 파생값으로 처리한다.

## 9. 오류/예외 정책
- 재고 부족(1+2순위 합산 불가): 승인 실패 예외 반환
- 상태 이력 저장 실패: 전체 롤백
- 출고/입고 중복 생성 충돌: 멱등 정책에 따라 방어
  - 사전 조회 + DB 유니크 제약 2중 보호

## 10. 구현 우선순위
1. 승인 오케스트레이션 서비스 추가 (동기/단일 트랜잭션)
2. 발주 승인 시 출고/입고/각 최초 이력 생성 연동
3. 1순위/2순위 재고 할당 및 분할 출고/입고 생성
4. 발주 완료 조건을 "연계 입고 전건 RECEIVED"로 반영
5. 발주 조회 응답에 `inboundProgress` 집계 필드 추가
6. 제약/컬럼 변경 SQL 반영 및 데이터 정합성 점검

## 11. 비동기 전환 가이드(차기)
- MVP는 동기 처리 유지
- 차기 전환 시 Outbox + 이벤트 소비 멱등키 + 재시도/DLQ + 보상 트랜잭션(Saga) 도입
- 이때는 단일 전역 롤백이 아니라 단계별 보상 모델로 운영

