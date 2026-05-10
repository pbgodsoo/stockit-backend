# 재고이동 실행기반 WH_TRANSFER 출고연계 BE 설계보고서

## 1. 목적
- 재고이동 실행 시점에 물류 출고 문서를 즉시 생성해 창고 출고 리스트와 연동한다.
- 기존 매장발주 기반 출고 흐름과 충돌 없이 `WH_TRANSFER` 원천을 공존시킨다.
- 재고이동 상태를 출고 상태와 동일한 3단계로 통일해 운영/조회 혼선을 제거한다.

## 2. 범위
- 포함
  - 재고이동 실행 API 내부 오케스트레이션(Transfer -> Outbound)
  - `sourceType=WAREHOUSE_TRANSFER` 출고 생성 규칙
  - 멱등/중복 방지(사전조회 + 유니크 충돌 재조회)
  - 출고 상태 전이 시 재고이동 상태 동기화
  - 부분성공 응답 확장(실패 라우트 사유 분리)
- 제외
  - 입고 확정 오케스트레이션(후속 담당 연계)
  - FE 화면 상세 개선(이번 문서는 BE 중심)

## 3. 상태 정책
- 재고이동 상태는 아래 3개만 사용한다.
  - `READY_TO_SHIP`
  - `IN_TRANSIT`
  - `ARRIVED`
- 미사용 상태(`REQUESTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELED`)는 제거한다.
- 상태 동기화 규칙
  - 재고이동 실행 + 출고 생성 완료: `READY_TO_SHIP`
  - 출고확정(`confirm`): `IN_TRANSIT`
  - 배송완료(`arrive`): `ARRIVED`

## 4. 오케스트레이션 설계
### 4.1 실행 단위
- 요청 라인은 `fromWarehouseCode -> toWarehouseCode` 라우트 단위로 그룹화한다.
- 각 라우트는 독립 트랜잭션(`REQUIRES_NEW`)으로 처리한다.

### 4.2 라우트 처리 순서
1. 라우트 검증(창고 유효성, SKU 유효성, 가용수량)
2. 재고이동 헤더/아이템 생성
3. WH_TRANSFER 출고 헤더 생성
4. WH_TRANSFER 출고 아이템 생성
5. 출고 상태이력 `READY_TO_SHIP` 생성
6. 재고이동 상태 `READY_TO_SHIP` 반영

### 4.3 부분성공 정책
- 성공 라우트는 커밋한다.
- 실패 라우트는 롤백하고 실패 사유를 응답에 적재한다.
- 배치 전체는 중단하지 않는다.

## 5. 출고 생성 규칙
- 헤더
  - `sourceType = WAREHOUSE_TRANSFER`
  - `sourceRefNo = transferNo`
  - `sourceRefId = transferHeader.id`
  - `sourceRefSeq = 1`
  - `warehouseId = fromWarehouseId`
  - `destinationType = WAREHOUSE`
  - `destinationId = toWarehouseId`
  - `status = READY_TO_SHIP`
  - `outboundNo = WOB-YYYYMMDD-00001`
- 아이템
  - 원천: `warehouse_transfer_item`
  - 매핑: `skuId/quantity/reason/memo`
  - 스냅샷: `productSku/productMaster` 조회로 `skuCode/productCode/productName/color/size/unitPrice` 구성

## 6. 멱등/중복 방지
- 1차 방어: `(sourceType, sourceRefNo, sourceRefSeq)` 사전 조회
- 2차 방어: DB 유니크 충돌(`DataIntegrityViolationException`) 시 재조회 후 기존건 재사용
- 목표: 동일 transfer 재처리 또는 동시 요청에도 출고 중복 미생성

## 7. 응답 계약
- 기존 유지
  - `lineResults`
  - `createdTransfers`
- 확장 추가
  - `failedTransfers[]`
    - `fromWarehouseCode`, `toWarehouseCode`
    - `errorCode`, `errorMessage`
    - `failedLines[]` (`lineId`, `skuCode`, `qty`, `reason`)

## 8. 검증 시나리오
1. 정상
  - 단일 라우트 실행 시 transfer 1건 + outbound 1건 + 상태이력 1건 생성
2. 부분성공
  - 다중 라우트 중 일부 실패 시 성공 라우트만 커밋, 실패 라우트만 사유 반환
3. 멱등
  - 동일 transfer 재처리 시 outbound 중복 미생성
4. 상태동기화
  - outbound confirm 시 transfer `IN_TRANSIT`
  - outbound arrive 시 transfer `ARRIVED`
5. 회귀
  - STORE_ORDER 출고/입고 오케스트레이션 영향 없음

## 9. 후속 연계(TODO)
- 입고 담당자 연계 시 `ARRIVED -> RECEIVED` 연결
- 필요 시 transfer 상세 응답에 연계 outboundNo 추가
- FE 히스토리 화면 상태 라벨(`IN_PROGRESS` 기준) 정비
