# 매장 입고(Store Inbound) MVP 설계 계획서

## 1. 목적
- 매장 발주 승인 이후 생성되는 `store inbound` 문서를 매장 사용자가 조회/확정할 수 있도록 백엔드 기능을 정의한다.
- 입고 확정 시 재고 반영, 입고 상태 이력, 발주 완료 전이를 하나의 트랜잭션으로 처리한다.

## 2. 범위
- 포함
  - 매장 입고 목록 조회
  - 매장 입고 상세 조회
  - 매장 입고 확정
  - 입고 확정 시 매장 재고 증가
  - 연계 입고 전건 수령 시 발주 `COMPLETED` 전이
- 제외
  - 부분입고 상태 enum 추가
  - 입고 전용 신규 테이블 추가
  - FE 화면 변경

## 3. 도메인/상태 정책
- 입고 상태: `PENDING_RECEIPT -> RECEIVED`
- 발주 상태: `REQUESTED -> APPROVED -> COMPLETED` (기존 유지)
- 출고 상태: `READY_TO_SHIP -> IN_TRANSIT -> ARRIVED` (기존 유지)
- 전이 규칙
  - 입고 확정은 `PENDING_RECEIPT` 상태에서만 허용
  - 같은 발주(`sourceRefNo`)의 연계 입고가 모두 `RECEIVED`이면 발주를 `COMPLETED`로 전이

## 4. API 설계
- `GET /api/store/inbound`
  - 설명: 로그인 매장 기준 입고 목록 조회
  - 필터: `status`, `from`, `to`, `keyword`
- `GET /api/store/inbound/{inboundNo}`
  - 설명: 입고 상세 조회
  - 응답: 헤더 + 아이템 + 상태이력 + 연계 출고 요약
- `POST /api/store/inbound/{inboundNo}/confirm`
  - 설명: 입고 확정 처리
  - 파라미터: `reason`(optional)

## 5. 트랜잭션/멱등/롤백 정책
- 트랜잭션 경계
  - `confirm` 메서드 단일 `@Transactional`로 처리
- 처리 순서
  - 소유/상태 검증
  - 재고 반영
  - 입고 상태 전이
  - 입고 이력 저장
  - 발주 완료 전이/이력 저장
- 롤백 정책
  - 위 단계 중 하나라도 실패 시 전체 롤백
- 멱등 정책
  - 이미 `RECEIVED` 상태인 입고에 재확정 요청 시 상태 전이 예외 반환

## 6. 권한/범위 정책
- 로그인 사용자 `locationCode`를 `STORE` 인프라로 해석
- 입고 헤더의 `storeId`와 로그인 매장 ID가 다르면 접근 차단

## 7. 재고 반영 정책
- 입고 확정 시 매장 `NORMAL` 재고 증가
  - `quantity` 증가
  - `availableQuantity` 증가
- 대상 재고 row가 없으면 신규 생성 후 증가값 반영

## 8. 예외 코드 정책
- 현재 구현 기준
  - `INBOUND_NOT_FOUND`
  - `STORE_ORDER_SCOPE_FORBIDDEN`
  - `INVALID_INBOUND_STATUS_TRANSITION`
- 개선 계획
  - 추후 `STORE_INBOUND_*` 전용 코드로 분리 가능

## 9. 검증 시나리오
- 정상
  - 단건 입고 확정 성공
  - 분할 입고 일부 확정 시 발주 `APPROVED` 유지
  - 분할 입고 전건 확정 시 발주 `COMPLETED` 전이
- 실패
  - 이미 `RECEIVED` 건 재확정 요청
  - 타 매장 입고 접근/확정 요청
  - 재고 반영 중 오류 발생 시 전체 롤백
