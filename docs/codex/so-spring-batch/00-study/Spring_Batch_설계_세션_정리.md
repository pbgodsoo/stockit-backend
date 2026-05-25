# Spring Batch 전환 — 세션 전체 정리

> 작성일: 2026-05-25  
> 범위: 학습 내용 / 설계 완료 / 설계 미완료 / 주요 논의 사항

---

## 1. 현재 시스템 (AS-IS)

```
@Scheduled(cron = "0 0 0 * * *")  ← 하루 1회 00:00 실행 (18시 배치 제거됨)
StoreOrderBatchApproveService.runAutoDaily()
  └── for each 발주 (REQUESTED):
        approveOne()  [단일 스레드 순차]
          ├── inventory.lock() [PESSIMISTIC_WRITE]
          ├── status → APPROVED
          ├── WhOutboundHeader/Item 생성
          └── StoreInboundHeader/Item 생성
```

### 확인된 7가지 문제

| 번호 | 문제 | 해결 Stage |
|------|------|-----------|
| 1 | 실행 이력 없음 (로그만 존재) | Stage 1 |
| 2 | 서버 중단 시 재처리 불가 | Stage 4 |
| 3 | 다중 인스턴스 중복 실행 | Stage 1 |
| 4 | 배치 간 충돌 가능성 | Stage 1 |
| 5 | 처리 범위 코드 하드코딩 | Stage 2 |
| 6 | 순차 처리, 병렬화 불가 | Stage 3 |
| 7 | 스킵/재시도 정책 없음 (수동 try-catch) | Stage 4 |

---

## 2. 전환 목표 (TO-BE)

```
K8s CronJob (00:00)
  └── stockit-batch Pod (Stage 5) 또는 stockit-be API 트리거 (Stage 1~4)
        └── Spring Batch Job
              └── Partitioned Step (지역 11개 파티션, ThreadPool)
                    ├── Partition 1: Region1 [W1, W2]
                    ├── Partition 2: Region2 [W3, W4]
                    └── ... Partition 11: Region11 [W21, W22]
                          각 파티션: Read → Process → Write (Chunk 방식)
```

**이번 전환 범위:** Spring Batch 전환만. Kafka / Outbound MSA 도입 없음.

---

## 3. 인프라 환경 정보

### RDS

| 항목 | 현재값 | 변경 예정 |
|------|--------|---------|
| 인스턴스 | db.t4g.small | 유지 |
| vCPU | 2개 | 유지 |
| RAM | 2GB | 유지 |
| max_connections | 68 | **150** (파라미터 그룹 수정) |
| 스토리지 | gp3 50GiB, 3000 IOPS | 유지 |
| 비용 변동 | - | **없음** (인스턴스 변경 아님) |

### RDS 파라미터 그룹 변경 절차

```
1. RDS 콘솔 → 데이터베이스 → 인스턴스 → 구성 탭 → 엔진 버전 확인
   (예: MariaDB 10.6.x → 패밀리: mariadb10.6)

2. RDS 콘솔 → 파라미터 그룹 → 파라미터 그룹 생성
   - 파라미터 그룹 패밀리: mariadb10.6 (버전 맞춰서)
   - 유형: DB Parameter Group
   - 그룹 이름: stockit-mariadb-custom
   - 설명: Stockit MariaDB custom params

3. 생성된 파라미터 그룹 → 편집 → max_connections = 150 → 저장

4. RDS 인스턴스 → 수정 → DB 파라미터 그룹: stockit-mariadb-custom → 즉시 적용

5. 인스턴스 재부팅 (파라미터 적용에 필요)

6. 확인: SELECT @@max_connections; → 150 출력 확인
   (현재 스크린샷 기준 아직 68 — 위 절차 미완료 상태)
```

> 커스텀 파라미터 그룹은 기본 그룹의 복사본으로 생성됨.
> max_connections만 수정하면 나머지 파라미터는 기존과 동일. 재입력 불필요.

### HikariCP (application.yml)

```yaml
datasource:
  hikari:
    maximum-pool-size: ${DB_POOL_MAX:10}   # Pod당 커넥션 수
    minimum-idle: ${DB_POOL_MIN_IDLE:1}
    connection-timeout: ${DB_CONNECTION_TIMEOUT_MS:15000}
    leak-detection-threshold: ${DB_LEAK_DETECTION_THRESHOLD_MS:20000}
```

### Stage별 HikariCP 변경 계획

| Stage | stockit-be Pod당 | 이유 |
|-------|----------------|------|
| 1~2 | 10 (현재 유지) | 단일 스레드, 추가 커넥션 불필요 |
| 3~4 | **30** | 파티션 11개 × 2 + 여유 = 24개 필요 |
| 5 (MSA 분리 후) | 10 (복귀) | API 전용 |
| stockit-batch | 30 | 배치 전용 |

### 커넥션 수 계산

```
파티션 N개 동시 실행 시:
  Reader용 (Step 내내 점유): N개
  Writer 트랜잭션용 (청크마다 점유): 최대 N개
  MasterStep/JobRepository: ~2개
  최대 합계: 2N + 2개

파티션 11개: 2×11 + 2 = 24개

Stage 3~4 전체 커넥션:
  stockit-be Pod1: 30개 (배치 실행 Pod)
  stockit-be Pod2: 30개 (API 전용)
  합계: 60개 / RDS 150개 → 여유 90개 ✅

Stage 5 이후:
  stockit-be Pod1: 10개
  stockit-be Pod2: 10개
  stockit-batch Pod: 30개
  합계: 50개 / RDS 150개 → 여유 100개 ✅
```

---

## 4. 도메인 구조 (설계 기준)

```
매장: 110개
창고: 22개
지역: 11개

지역 1개 = 창고 2개 (서로 백업 관계)
  Region1: W1 ↔ W2 (서로의 백업)
  Region2: W3 ↔ W4
  ...
  Region11: W21 ↔ W22

매장 1개 → PRIMARY 창고 1개 (store_warehouse_map role='PRIMARY')
창고 1개 → 담당 매장 약 5개
```

---

## 5. Spring Batch 핵심 개념 정리

### 5-1. Job / Step 구조

```
Job = 하나의 배치 작업 전체
  └── Step = 작업 안의 단계
        └── Tasklet (단순 실행) 또는 Chunk (Read-Process-Write)
```

### 5-2. JobRepository (실행 이력 자동 저장)

자동 생성되는 6개 테이블:

| 테이블 | 저장 내용 |
|--------|---------|
| BATCH_JOB_INSTANCE | Job명 + Parameter 조합 |
| BATCH_JOB_EXECUTION | 실행 기록 (시작/종료/상태) |
| BATCH_JOB_EXECUTION_PARAMS | 실행 시 사용된 파라미터 |
| BATCH_JOB_EXECUTION_CONTEXT | Job 레벨 중간 상태 |
| BATCH_STEP_EXECUTION | Step별 기록 (readCount, writeCount, skipCount) |
| BATCH_STEP_EXECUTION_CONTEXT | Step 레벨 중간 상태 |

```yaml
# 반드시 추가
spring:
  batch:
    job:
      enabled: false          # 서버 시작 시 자동 실행 방지 (누락 시 운영 데이터 오염)
    jdbc:
      initialize-schema: always  # 6개 테이블 자동 생성
```

### 5-3. Tasklet vs Chunk

```
Tasklet: 기존 로직을 Spring Batch 껍데기로 감싸기 (Stage 1)
  → 코드 변경 없이 이력 저장 기능만 추가

Chunk: N건씩 나눠서 Read → Process → Write → Commit (Stage 2~)
  → chunkSize = 트랜잭션 1회당 처리 건수
  → 실패 시 해당 청크(N건)만 롤백
```

### 5-4. ItemReader 선택

```
PagingItemReader  → 사용 금지 (이 프로젝트)
  이유: 처리 완료 건이 status=APPROVED로 변경되면
        OFFSET이 밀려 일부 건 누락 (조용한 버그)

JdbcCursorItemReader  → 사용 ✅
  이유: DB 커서로 결과셋을 한 번 확보 → 중간 변경 영향 없음
  단점: 커넥션을 Step 내내 점유

pageSize (참고):
  PagingItemReader 사용 시 DB 한 번 쿼리당 가져오는 행 수
  반드시 chunkSize와 동일하게 설정 (다르면 N+1 발생)
  → JdbcCursorItemReader 사용 시 직접 해당 없음
```

### 5-5. Partitioned Step (병렬 처리)

```
[Pod와 Thread의 차이]
Pod    = 독립된 JVM 프로세스 (서버 1대)
Thread = 하나의 JVM 안에서 동시에 실행되는 작업 단위

Spring Batch 파티셔닝 = 로컬 파티셔닝 (Thread 기반)
  CronJob → Pod 1개 생성 (JVM 1개)
                └── ThreadPoolTaskExecutor
                      ├── Thread 1 → Partition 1
                      ├── Thread 2 → Partition 2
                      └── ... (corePoolSize 수만큼 동시 실행)
```

**파티션 기준: 지역(Region)**

```
올바른 설계 (지역 기준, 11개 파티션):
  Partition 1 → Region1: [W1, W2] (서로 백업)
  Partition 2 → Region2: [W3, W4]
  ...
  → W1과 W2는 같은 파티션(스레드)이 처리
  → Processor에서 W1 부족 → W2 백업 확인해도 같은 스레드 → DeadLock 없음 ✅

잘못된 설계 (매장/창고 단위):
  → 서로 다른 파티션이 같은 창고 재고를 동시에 잠그려 함 → DeadLock
```

**@StepScope 필수:**

```java
// 없으면: 모든 스레드가 같은 Reader 인스턴스 공유 → 데이터 뒤섞임
// 있으면: 파티션마다 독립적인 Reader 인스턴스 생성
@Bean
@StepScope
public JdbcCursorItemReader<StoreOrderHeader> partitionReader(
    @Value("#{stepExecutionContext['warehouseIds']}") List<Long> warehouseIds) { ... }
```

**ThreadPoolTaskExecutor 주의:**

```java
executor.setCorePoolSize(4);
executor.setMaxPoolSize(8);
executor.setQueueCapacity(gridSize); // 반드시 gridSize 이상

// queueCapacity=0 설정 시:
// 파티션 수 > corePoolSize 이면 초과 파티션 즉시 거절
// → 배치가 빠르게 완료된 것처럼 보이지만 실제로는 처리 누락!
```

### 5-6. faultTolerant / Retry / Skip

```java
.faultTolerant()
.retryLimit(3)
.retry(TransientDataAccessException.class)       // DB 일시 오류 → 재시도
.retry(DeadlockLoserDataAccessException.class)   // 데드락 패배 → 재시도
.noRetry(BaseException.class)                    // 비즈니스 예외 → 재시도 안 함
.skipLimit(20)                                   // 1,100건 기준 약 2%
.skip(BaseException.class)                       // 비즈니스 예외 → 스킵
.noSkip(DataIntegrityViolationException.class)   // DB 무결성 위반 → 스킵 안 함
```

### 5-7. Spring Batch 5.x 주요 API 변경 (Boot 3.x 필수)

| Batch 4.x (인터넷 예제) | Batch 5.x (이 프로젝트) |
|----------------------|----------------------|
| `new JobBuilderFactory()` | `new JobBuilder("name", jobRepository)` |
| `new StepBuilderFactory()` | `new StepBuilder("name", jobRepository)` |
| `new JobParameter("값")` | `new JobParameter<>("값", String.class)` |
| `.addString("fromDate", "값")` | `.addLocalDateTime("fromDate", LocalDateTime)` |

---

## 6. 설계 확정 사항 ✅

### 6-1. 전환 단계 (Stage)

| Stage | 내용 | 해결 문제 |
|-------|------|---------|
| 1 | Spring Batch 기본 + 이력 저장 + ShedLock | 문제 1, 3 |
| 2 | Chunk 방식 전환 + JobParameter 날짜 외부화 | 문제 5 |
| 3 | Partitioned Step (지역 단위 병렬) | 문제 6 |
| 4 | faultTolerant + retry/skip | 문제 2, 7 |
| 5 | MSA 분리 (stockit-batch 서버) | 독립 배포 |

### 6-2. 파티셔닝

| 항목 | 결정값 |
|------|--------|
| 파티션 기준 | 지역(Region) |
| 파티션 수 | 11개 (지역 수) |
| 파티션당 창고 | 2개 (서로 백업 관계) |
| Reader 범위 | 두 창고를 primary로 하는 매장들의 REQUESTED 발주 |

### 6-3. Reader

| 항목 | 결정값 |
|------|--------|
| Reader 방식 | JdbcCursorItemReader |
| PagingItemReader | 사용 금지 (OFFSET 밀림 버그) |

### 6-4. Processor 설계

```
판단 흐름:
  1. status == REQUESTED 확인
     → 아니면: AlreadyProcessedException (Skip 대상)

  2. primary 창고 재고 확인
     → 충분: primary 창고로 확정

  3. backup 창고 재고 확인 (primary 부족 시)
     → 충분: backup 창고로 확정 (같은 파티션 스레드 내 → 안전)
     → 부족: InsufficientStockException (Skip 대상)

  4. 정상 → Writer로 전달
```

```
예외 분류:
  재시도 대상 (일시적 오류):
    TransientDataAccessException
    DeadlockLoserDataAccessException
    OptimisticLockingFailureException

  스킵 대상 (비즈니스 오류):
    InsufficientStockException
    AlreadyProcessedException
    BaseException 하위 비즈니스 예외

  스킵 금지:
    DataIntegrityViolationException
```

### 6-5. 트리거 방식

| Stage | 방식 | ShedLock |
|-------|------|---------|
| 1~4 | @Scheduled + ShedLock | 필요 |
| 5 (MSA) | K8s CronJob | 불필요 |

**CronJob에서 ShedLock 불필요한 이유:**
- @Scheduled: 모든 Pod에서 동시 발동 → ShedLock으로 1개만 실행
- CronJob: Pod 1개만 생성 → 물리적으로 중복 불가
- `concurrencyPolicy: Forbid` → 이전 Pod 실행 중이면 새 Pod 생성 차단

### 6-6. JobParameter 설계

| 파라미터 | 타입 | 자동 배치 | 수동 배치 |
|---------|------|---------|---------|
| fromDateTime | LocalDateTime | 자동 계산 | 직접 지정 |
| toDateTime | LocalDateTime | 자동 계산 | 직접 지정 |
| runType | String | MIDNIGHT | MANUAL |
| runId | String | 없음 | UUID (항상 포함) |

**runType 코드: MIDNIGHT \| MANUAL (EVENING 코드 유지 — 미래 확장 대비)**

**18시 배치 현황:** 제거됨. 00시 배치(MIDNIGHT)만 운영.

**JobInstance 유일성 규칙:**

```
MIDNIGHT 배치:
  runId 없음 → 같은 파라미터 = 동일 JobInstance
  COMPLETED 후 재실행 → 자동 차단 (중복 방지) ✅
  FAILED 후 재실행 → 허용 (재시작) ✅

MANUAL 배치:
  runId 있음 (UUID) → 항상 새 JobInstance
  이전 실행 결과 무관하게 항상 실행 허용 ✅
  중복 발주 걱정 없음 → Reader가 REQUESTED만 조회하므로 APPROVED 건 자동 제외
```

### 6-7. 수동 배치 중복 실행 방지 (JobExplorer 활용)

```java
// MANUAL 배치 API 진입 시 체크
Set<JobExecution> running =
    jobExplorer.findRunningJobExecutions("storeOrderBatchApproveJob");

// 차단 조건 1: MIDNIGHT 배치 실행 중
boolean midnightRunning = running.stream()
    .anyMatch(je -> "MIDNIGHT".equals(je.getJobParameters().getString("runType")));

// 차단 조건 2: 날짜 범위 겹치는 MANUAL 배치 실행 중
boolean overlapRunning = running.stream()
    .anyMatch(je -> isDateRangeOverlap(je.getJobParameters(), fromDateTime, toDateTime));

// 둘 다 아니면 → 정상 실행
```

**차단/허용 정책:**

| 상황 | 처리 |
|------|------|
| MIDNIGHT 실행 중 → MANUAL 요청 | 409 차단 |
| 같은 날짜 범위 MANUAL 실행 중 → 재요청 | 409 차단 |
| MIDNIGHT 완료 후 → MANUAL 요청 | 허용 |
| 다른 날짜 범위 MANUAL 동시 요청 | 허용 |

### 6-8. CronJob YAML (Stage 5 기준)

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: store-order-batch-midnight
spec:
  schedule: "0 0 * * *"
  timeZone: "Asia/Seoul"
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 7
  failedJobsHistoryLimit: 7
  jobTemplate:
    spec:
      backoffLimit: 0          # 실패 시 자동 재시도 안 함 (Spring Batch가 관리)
      template:
        spec:
          restartPolicy: Never
          containers:
          - name: batch
            env:
            - name: BATCH_RUN_TYPE
              value: "MIDNIGHT"
            - name: DB_POOL_MAX
              value: "30"
```

---

## 7. 설계 미완료 (측정 후 결정) 🟡

| 항목 | 시작값 | 결정 시점 |
|------|--------|---------|
| chunkSize | 10 | Stage 2 k6 측정 후 (10/50/100/200 비교) |
| 파티션 수 | 11 | Stage 3 k6 측정 후 (4/8/11 비교) |
| HikariCP pool size | 30 (Stage 3~4) | 파티션 수 결정 후 역산 |
| ThreadPool corePoolSize | 4 | Stage 3 측정 후 |
| ThreadPool maxPoolSize | 8 | corePoolSize × 2 |
| skipLimit | 20 | 운영 보며 조정 |

**공식:**
```
HikariCP pool size ≥ (파티션 수 × 2) + 2 + API 여유분
corePoolSize = 파티션 수 (동시 실행 파티션 수와 동일)
```

---

## 8. 예상 이슈 우선순위

| 우선순위 | 이슈 | 단계 | 위험도 |
|--------|------|------|------|
| 🔴 1 | DeadLock (파티션 설계 실수) | Stage 3 | 높음 |
| 🔴 2 | Boot 3.x = Batch 5.x API 변경 | Stage 1~ | 높음 |
| 🔴 3 | Job 자동 실행 (enabled 누락) | Stage 1 | 높음 |
| 🟡 4 | JpaCursorItemReader + REQUIRES_NEW 충돌 | Stage 2 | 중간 |
| 🟡 5 | PagingItemReader OFFSET 밀림 | Stage 2 | 중간 |
| 🟡 6 | queueCapacity=0 조기 종료 | Stage 3 | 중간 |
| 🟡 7 | @StepScope 누락 (스레드 상태 공유) | Stage 3 | 중간 |
| 🟡 8 | HikariCP 부족 (파티션 수 증가 시) | Stage 3 | 중간 |
| 🟢 9 | skipLimit 초과 시 Job 전체 FAILED | Stage 4 | 낮음 |
| 🟢 10 | Entity 중복 관리 (MSA 분리 후) | Stage 5 | 낮음 |

---

## 9. 단계별 성능 측정 계획

| Stage | 조건 | 기록 항목 |
|-------|------|---------|
| 0 (AS-IS) | 1,100건 / 2,200건 | 처리 시간, 힙 메모리 |
| 1 | 1,100건 / 2,200건 | 처리 시간 (변화 없어야 함), BATCH_JOB_EXECUTION 확인 |
| 2 | 1,100건, chunk 10/50/100/200 | 처리 시간, 힙 메모리 피크 |
| 3 | 1,100건, partition 4/8/11 | 처리 시간, DB 커넥션 수 |
| 4 | 일부 실패 inject | skip 건수, retry 로그 |
| 5 | 1,100건 (MSA 후) | 처리 시간, 네트워크 오버헤드 |

```bash
# k6 성능 측정
k6 run --env BASE_URL=https://www.stockit.kro.kr --env MODE=baseline before.js

# 처리 건수 확인
SELECT status, COUNT(*) FROM store_order_header GROUP BY status;

# Spring Batch 이력 확인 (Stage 1 이후)
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY START_TIME DESC LIMIT 5;
```

---

## 10. 즉시 할 일 체크리스트

```
[ ] RDS 파라미터 그룹 생성 및 max_connections=150 적용
    → 재부팅 후 SELECT @@max_connections; = 150 확인

[ ] Spring Batch 5.x API 변경점 숙지
    (JobBuilderFactory → JobBuilder, JobParameter 타입 등)

[ ] application.yml에 spring.batch.job.enabled=false 추가 확인

Stage 1 구현 시작 전:
[ ] build.gradle에 spring-boot-starter-batch, shedlock 의존성 추가
[ ] ShedLock 테이블 DDL 실행
[ ] BatchConfig.java 생성 (Tasklet 방식, 기존 로직 래핑)

Stage 2 구현 전:
[ ] JdbcCursorItemReader 방식 확정 (JPA 아님)
[ ] JobParameter 타입 확정 (LocalDateTime, Batch 5.x)

Stage 3 구현 전:
[ ] store_warehouse_map 기반 Region-Warehouse 매핑 데이터 확인
[ ] HikariCP pool size 30으로 변경 (DB_POOL_MAX=30)
[ ] Partitioner StepExecutionContext 설계 (warehouseIds 전달 구조)
[ ] @StepScope 모든 Reader/Writer에 적용 확인
```

---

## 11. 관련 파일 목록

| 파일 | 역할 |
|------|------|
| `StoreOrderBatchApproveService.java` | AS-IS 배치 로직 (→ Job/Step으로 이관 대상) |
| `StoreOrderBatchApproveItemService.java` | 건별 REQUIRES_NEW 트랜잭션 (Stage 4까지 재사용) |
| `StoreOrderBatchApproveScheduler.java` | @Scheduled → Stage 1에서 @SchedulerLock 추가 |
| `StoreOrderBatchApproveController.java` | 수동 API (→ JobLauncher 호출 + 중복 실행 차단 로직) |
| `build.gradle` | spring-boot-starter-batch, shedlock 추가 |
| `StoreOrderHeaderRepository.java` | Stage 2에서 Cursor 기반 쿼리 추가 필요 |

---

*이 문서는 세션에서 논의된 내용을 기반으로 작성되었습니다.*
*설계 미완료 항목은 각 Stage k6 측정 결과 후 업데이트 필요.*
