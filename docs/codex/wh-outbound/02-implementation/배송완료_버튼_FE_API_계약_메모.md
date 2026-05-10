# 배송완료 버튼 FE-API 계약 메모

## 1) 목적
- 시연/운영에서 창고 출고의 `IN_TRANSIT -> ARRIVED` 전이를 사용자가 명시적으로 수행할 수 있도록 프론트 버튼 계약을 정의한다.

## 2) 버튼 노출 조건
- 화면: 창고 출고 목록/상세
- 버튼명: `배송완료`
- 노출 조건:
  - 출고 상태가 `IN_TRANSIT`일 때만 노출
  - 로그인 사용자가 해당 출고 창고 범위 권한을 가질 때만 활성

## 3) 호출 API
- Method/Path: `POST /api/warehouse/outbound/{outboundNo}/arrive`
- Request:
  - Path: `outboundNo`
  - Body(옵션): `reason` (없으면 백엔드 기본값 사용)
- Response:
  - `BaseResponse<WhOutboundDto.DetailRes>`
  - 성공 시 `status=ARRIVED`, `arrivedAt` 반영

## 4) 실패 처리 규칙 (FE)
- `OUTBOUND_INVALID_STATUS_TRANSITION(4901)`
  - 메시지: 이미 배송완료 처리되었거나 현재 상태에서 배송완료가 불가
  - UI: 경고 토스트 + 상태 재조회
- `OUTBOUND_SCOPE_FORBIDDEN(4902)`
  - 메시지: 권한 범위를 벗어난 출고건
  - UI: 에러 토스트 + 버튼 비활성
- `OUTBOUND_NOT_FOUND(4900)`
  - 메시지: 존재하지 않는 출고건
  - UI: 목록 새로고침 유도

## 5) 호출 후 화면 반영
- 성공 시:
  - 해당 행/상세 상태를 즉시 `ARRIVED`로 갱신
  - `배송완료` 버튼 숨김
  - 상태 이력 탭/영역에 `ARRIVED` 이력 추가 반영
- 실패 시:
  - 서버 메시지 노출
  - 낙관적 업데이트를 했다면 롤백

## 6) 입고확정과의 연계 규칙
- 매장 입고 확정 API는 연계 출고 상태가 `ARRIVED`일 때만 성공
- 따라서 프론트 흐름 권장 순서:
  1. 창고 출고확정(`confirm`)
  2. 배송완료(`arrive`)
  3. 매장 입고확정(`confirm inbound`)
