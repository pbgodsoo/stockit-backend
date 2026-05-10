# 재고이동 출고연계 오케스트레이션 BE 구현보고서

## 1. 작업 개요
- 작업 일자: 2026-05-11
- 작업 목적:
  - 재고이동 실행 시 `WH_TRANSFER` 출고를 즉시 생성하도록 오케스트레이션 연계
  - 재고이동 상태를 출고 상태 흐름(`READY_TO_SHIP -> IN_TRANSIT -> ARRIVED`)과 정합화
  - 부분성공/멱등/중복방지 정책 반영
  - DTO 변환 책임을 DTO(`toEntity/from`)로 정리

## 2. 구현 범위
- 포함
  - 재고이동 실행(`execute`) 시 transfer -> outbound 생성 오케스트레이션
  - 출고 상태 이력(`READY_TO_SHIP`) 생성
  - 출고 상태 전이(`confirm/arrive`) 시 transfer 상태 동기화
  - 부분성공 응답(`failedTransfers`) 확장
  - 이동번호 prefix 변경 `STF` -> `WTF`
- 제외
  - 입고 확정(`RECEIVED`) 오케스트레이션
  - FE 상태 라벨/화면 정비

## 3. 변경 파일
- `src/main/java/org/example/stockitbe/hq/warehousetransfer/WarehouseTransferService.java`
- `src/main/java/org/example/stockitbe/hq/warehousetransfer/model/WarehouseTransferDto.java`
- `src/main/java/org/example/stockitbe/hq/warehousetransfer/model/WarehouseTransferHeader.java`
- `src/main/java/org/example/stockitbe/hq/warehousetransfer/model/WarehouseTransferStatus.java`
- `src/main/java/org/example/stockitbe/warehouse/outbound/WhOutboundService.java`
- `docs/codex/whTransfer-ob-ib/01-plan/재고이동_실행기반_WH_TRANSFER_출고연계_BE_설계보고서.md`

## 4. 핵심 구현 내용
### 4.1 재고이동 실행 -> 출고 생성 오케스트레이션
- `WarehouseTransferService.execute`를 라우트(`fromWarehouseCode -> toWarehouseCode`) 단위로 그룹 처리
- 라우트 단위 `REQUIRES_NEW` 트랜잭션 적용:
  - 성공 라우트 커밋
  - 실패 라우트 롤백 + 실패사유 수집
- 라우트 성공 처리 순서:
  1. transfer header 생성
  2. transfer item 생성
  3. outbound header 생성(멱등 upsert)
  4. outbound item 생성
  5. outbound status history(`READY_TO_SHIP`) 생성
  6. transfer 상태 `READY_TO_SHIP` 동기화

### 4.2 WH_TRANSFER 출고 생성 규칙
- `sourceType = WAREHOUSE_TRANSFER`
- `sourceRefNo = transferNo`
- `sourceRefId = transferHeader.id`
- `sourceRefSeq = 1`
- `warehouseId = fromWarehouseId`
- `destinationType = WAREHOUSE`
- `destinationId = toWarehouseId`
- `status = READY_TO_SHIP`
- `outboundNo = WOB-YYYYMMDD-00001`

### 4.3 멱등/중복 방지
- 1차: `(sourceType, sourceRefNo, sourceRefSeq)` 사전 조회
- 2차: 유니크 충돌(`DataIntegrityViolationException`) 시 재조회 후 기존건 사용
- 결과: 동일 transfer 재처리 시 outbound 중복 생성 방지

### 4.4 상태 동기화
- `WarehouseTransferStatus`를 3상태로 정리:
  - `READY_TO_SHIP`, `IN_TRANSIT`, `ARRIVED`
- `WhOutboundService.confirm` 시 transfer `IN_TRANSIT` 반영
- `WhOutboundService.arrive` 시 transfer `ARRIVED` 반영

### 4.5 DTO 변환 리팩터링
- `Req DTO`에서 `toEntity(...)` 적용
  - `ExecuteReq.toEntity(ExecuteHeaderContext)`
  - `ExecuteLineReq.toEntity(ExecuteLineContext)`
- `Res DTO`에서 `from(...)` 적용
  - `ExecuteRes/ExecuteLineResultRes/ExecuteTransferRes`
  - `ExecuteFailedRouteRes/ExecuteLineFailureRes`
  - `TransferListItemRes/TransferDetailRes/TransferLineRes`
  - `WarehouseSkuDistributionRes`
- 서비스의 수동 `builder()` 조립 코드를 DTO 변환 호출 중심으로 정리

### 4.6 이동번호 정책 변경
- transferNo 생성 prefix: `STF-` -> `WTF-`
- 최종 정책: `WTF-YYYYMMDD-00001`

## 5. 응답 계약 변경
- `WarehouseTransferDto.ExecuteRes` 확장:
  - 유지: `requestedCount`, `successCount`, `lineResults`, `createdTransfers`
  - 추가: `failureCount`, `failedTransfers`
- `failedTransfers` 구조:
  - 라우트 식별: `fromWarehouseCode`, `toWarehouseCode`
  - 실패 원인: `errorCode`, `errorMessage`
  - 라인 상세: `failedLines[]`

## 6. 데이터베이스 정합 이슈 및 조치
- 이슈:
  - 기존 `warehouse_transfer_header.status` enum이 구값(`REQUESTED/IN_PROGRESS/COMPLETED/CANCELED`) 기반
  - 신규 상태 저장 시 truncation 오류 발생 가능
- 조치:
  - 테이블 재생성 후 enum을 아래로 정합화
    - `ARRIVED`, `IN_TRANSIT`, `READY_TO_SHIP`
- 확인 결과:
  - `SHOW CREATE TABLE warehouse_transfer_header` 기준 신규 enum 반영 확인

## 7. 검증 결과
### 7.1 완료된 검증
- 코드 레벨
  - 오케스트레이션 경로(생성/상태동기화/부분성공/멱등) 반영 확인
  - DTO 변환 구조(`toEntity/from`) 반영 확인
  - 이동번호 정책(`WTF`) 반영 확인

### 7.2 미완료 검증
- 실행 컴파일/테스트
  - 현재 작업 환경에서 `JAVA_HOME` 경로 이슈로 `gradlew compileJava` 미수행

## 8. 남은 작업(TODO)
1. 실행 검증
  - 재고이동 실행 -> 출고 생성/이력 생성
  - 출고 `confirm/arrive` -> transfer 상태 동기화
  - 다중 라우트 부분성공 응답 검증
2. FE 연동 확인
  - 재고이동 내역 상태 라벨(`READY_TO_SHIP/IN_TRANSIT/ARRIVED`) 정합
  - 출고 목록 타입 필터(`WH_TRANSFER`) 노출 확인
3. 입고 담당 후속
  - `ARRIVED -> RECEIVED` 입고 확정 오케스트레이션 연계

## 9. 결론
- BE 기준 재고이동-출고 연계 오케스트레이션 1차 구현은 완료되었다.
- 현재 상태는 “입고 오케스트레이션 제외, 출고 연계 및 상태 동기화 완료” 단계이며,
  FE 정합 및 실행 검증 완료 후 운영 반영 가능 상태로 판단한다.
