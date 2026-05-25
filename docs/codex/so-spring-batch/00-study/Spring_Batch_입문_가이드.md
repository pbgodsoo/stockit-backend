# Spring Batch 입문 가이드 — Stockit 매장 발주 배치 전환 기준

## 0. 먼저 이해해야 할 맥락

지금 시스템은 이렇게 생겼습니다:

```
@Scheduled(cron = "0 0 0 * * *")   ← 매일 자정 자동 실행
StoreOrderBatchApproveService.runAutoDaily()
  └── for each 발주:
        approveOne()  ← 1건씩 순차 처리
```

이게 동작은 하지만 **7가지 문제**가 있고, Spring Batch로 전환하면 모두 해결됩니다.

---

## 개념 목록 (학습 순서)

| # | 개념 | 해결하는 문제 | 도입 단계 |
|---|------|------------|---------|
| 1 | Spring Batch가 뭔가? | 전체 배경 이해 | - |
| 2 | Job과 Step | 구조 파악 | Stage 1 |
| 3 | JobRepository (실행 이력) | 문제 1, 2 | Stage 1 |
| 4 | Tasklet | Stage 1 진입점 | Stage 1 |
| 5 | Chunk (Reader/Processor/Writer) | 메모리, 트랜잭션 | Stage 2 |
| 6 | JobParameter | 문제 5 (하드코딩 제거) | Stage 2 |
| 7 | Partitioned Step | 문제 6 (병렬 처리) | Stage 3 |
| 8 | faultTolerant / Retry / Skip | 문제 7 (자동 재시도) | Stage 4 |
| 9 | ShedLock | 문제 3 (중복 실행 방지) | Stage 1 |
| 10 | Spring Batch 5.x 주의사항 | 이 프로젝트 필수 | 전 단계 |

---

## 개념 1 — Spring Batch가 뭔가?

### 한 줄 정의

> 대량 데이터를 안정적으로 처리하기 위해 만들어진 스프링 공식 프레임워크.
> **"어디까지 처리했는지 DB에 기록하면서" 배치를 실행한다.**

### 기존 방식(@Scheduled)과 뭐가 다른가?

| 항목 | 기존 @Scheduled | Spring Batch |
|------|----------------|-------------|
| 실행 이력 | 없음 (로그뿐) | DB 테이블에 자동 저장 |
| 서버 중단 후 재시작 | 처음부터 다시 | 중단된 지점부터 재개 |
| 중복 실행 방지 | 없음 | ShedLock으로 방지 |
| 처리 범위 | 코드에 하드코딩 | 외부 파라미터로 주입 |
| 병렬 처리 | 불가 | Partitioned Step |
| 실패 처리 | 수동 try-catch | 자동 retry/skip |

### 왜 지금 이 전환이 필요한가?

`문제 2`가 핵심입니다:

```
18:00 배치 → 300건 중 150건 처리 중
→ K8s Pod 재스케줄링 → 서버 재시작
→ 어디서 중단됐는지 기록 없음
→ 다음 배치(00:00)까지 150건 방치
→ 매장 민원 발생
```

Spring Batch는 처리 진행 상황을 DB에 계속 기록하기 때문에, 서버가 죽어도 어디서 멈췄는지 알고 거기서 재개할 수 있습니다.

---

## 개념 2 — Job과 Step

### 구조 비유

```
Job = 하나의 배치 작업 전체 (예: "18:00 발주 승인 배치")
  └── Step = 작업 안의 단계 (예: "발주 조회 → 승인 처리")
        └── 실제 처리 로직 (Tasklet 또는 Chunk)
```

### 이 프로젝트 기준

```
storeOrderBatchApproveJob (Job)
  └── approveStep (Step)
        └── 기존 runAutoDaily() 로직
```

### 핵심 규칙

- Job은 이름으로 식별됩니다. 같은 이름의 Job이라도 **JobParameter가 다르면 다른 실행**으로 인식합니다.
- Step은 Job 안에 여러 개 둘 수 있고, 순서가 보장됩니다.
- Step은 독립적으로 성공/실패 상태를 가집니다.

---

## 개념 3 — JobRepository (실행 이력)

### 뭘 하는 건가?

Spring Batch가 자동으로 생성하는 6개 테이블에 모든 실행 기록을 저장합니다.

```yaml
# application.yml — 이 설정 추가 시 6개 테이블 자동 생성
spring:
  batch:
    jdbc:
      initialize-schema: always
```

### 자동 생성되는 6개 테이블

| 테이블 | 저장 내용 |
|--------|---------|
| `BATCH_JOB_INSTANCE` | Job의 논리적 인스턴스 (Job이름 + Parameter 조합) |
| `BATCH_JOB_EXECUTION` | 실제 실행 기록 (시작시각, 종료시각, 상태) |
| `BATCH_JOB_EXECUTION_PARAMS` | 해당 실행에 사용된 JobParameter |
| `BATCH_JOB_EXECUTION_CONTEXT` | Job 레벨 중간 상태 저장 |
| `BATCH_STEP_EXECUTION` | Step별 실행 기록 (읽은 건수, 처리 건수, 실패 건수) |
| `BATCH_STEP_EXECUTION_CONTEXT` | Step 레벨 중간 상태 저장 |

### 왜 중요한가?

- `BATCH_JOB_EXECUTION`을 보면 "배치 실행됐나요?", "몇 건 성공했나요?" 바로 알 수 있음
- `BATCH_STEP_EXECUTION`의 `skipCount`, `readCount`, `writeCount`로 세부 통계 확인
- 서버가 중단돼도 재시작 시 어디서 멈췄는지 이 테이블에서 파악해서 재개

### 주의: 자동 실행 방지 필수

```yaml
# application.yml — 반드시 추가
spring:
  batch:
    job:
      enabled: false  # 이게 없으면 서버 시작할 때마다 배치가 자동 실행됨!
```

---

## 개념 4 — Tasklet

### 뭔가?

> Step의 가장 단순한 실행 방식. "이 Step이 실행될 때 이 코드를 그냥 실행해라"는 구조.

### Stage 1에서 왜 Tasklet을 먼저 쓰는가?

기존 `runAutoDaily()` 로직을 **코드 변경 없이** Spring Batch 틀에 끼워 넣기 위해서입니다.

```
Job: storeOrderBatchApproveJob
  └── Step: approveStep (Tasklet)
        └── 기존 runAutoDaily() 그대로 호출
```

Tasklet은 "래퍼(wrapper)"입니다. 기존 for-loop 로직은 건드리지 않고, Spring Batch의 이력 기록 기능만 추가로 얻는 것이 Stage 1의 목표입니다.

### Tasklet의 한계

- 여전히 한 번에 전부 메모리에 올림 (발주 300건 전체)
- 중간에 실패하면 처음부터 다시
- 이래서 Stage 2에서 Chunk 방식으로 전환함

---

## 개념 5 — Chunk (Reader / Processor / Writer)

### 핵심 아이디어

> "전체를 한꺼번에 처리하지 말고, N건씩 나눠서 처리하자."

```
전체 발주 300건
  → 10건씩 읽어서 (Read)
  → 10건 처리하고 (Process)
  → 10건 저장하고 (Write)
  → 커밋
  → 다음 10건...
```

이 N을 **chunk size** (청크 크기)라고 합니다. 이 프로젝트에서는 10으로 설정 예정입니다.

### 3요소 역할

| 역할 | 이 프로젝트에서 하는 일 |
|------|-------------------|
| ItemReader | REQUESTED 상태 발주를 DB에서 읽어옴 |
| ItemProcessor | 승인 가능 여부 검증 (status == REQUESTED 확인) |
| ItemWriter | approveOne() 호출 (승인 처리) |

### 트랜잭션이 청크 단위로 걸린다

```
청크 1: 발주 1~10건 읽기 → 처리 → 커밋 ✅
청크 2: 발주 11~20건 읽기 → 처리 → 커밋 ✅
청크 3: 발주 21~30건 읽기 → 처리 → 실패! → 롤백 (21~30건만)
청크 4: 다시 21~30건부터...
```

### PagingItemReader는 이 프로젝트에서 사용 금지

조용한 버그가 생깁니다:

```
OFFSET 0: [발주A, 발주B, 발주C, 발주D, 발주E] → A,B,C 처리 후 status=APPROVED로 변경
OFFSET 3: [발주F, G, H, ...] → D,E가 앞으로 당겨졌으므로 D,E 누락!
```

대신 **JdbcCursorItemReader**를 사용합니다.
커서 방식은 한 번 열어놓은 결과셋을 끝까지 순서대로 읽기 때문에 이 문제가 없습니다.

### chunk size와 pageSize는 반드시 같게

```java
chunk(10).reader(pageItemReader())  // pageSize도 반드시 10!
// 다르면 N+1 문제 발생
```

---

## 개념 6 — JobParameter

### 뭔가?

> 배치 실행 시 외부에서 값을 주입하는 파라미터.
> 배치가 처리할 날짜 범위를 코드 밖에서 결정하게 해줍니다.

### 현재 문제 (코드에 하드코딩)

```java
LocalTime now = LocalTime.now();
if (now.getHour() == 18) {
    from = 전날 00:00;
    to = 당일 17:59;
} else {
    from = 당일 18:00;
    to = 당일 23:59;
}
```

배치 시각이 바뀌면 코드를 수정해야 하고, 특정 날짜 재처리가 불가합니다.

### JobParameter 적용 후

```
18:00 배치 실행 시 → fromDate=2026-05-24T00:00, toDate=2026-05-24T17:59 주입
00:00 배치 실행 시 → fromDate=2026-05-24T18:00, toDate=2026-05-24T23:59 주입
수동 재처리 시     → fromDate=원하는날짜, toDate=원하는날짜 직접 지정 가능
```

### Spring Batch 5.x 주의

```java
// ❌ 인터넷 예제 (Batch 4.x) — 컴파일 에러
new JobParametersBuilder()
    .addString("fromDate", "2026-05-24")

// ✅ 이 프로젝트 (Batch 5.x)
new JobParametersBuilder()
    .addLocalDate("fromDate", LocalDate.of(2026, 5, 24))
    .addLocalDate("toDate", LocalDate.of(2026, 5, 24))
```

---

## 개념 7 — Partitioned Step (병렬 처리)

### 뭔가?

> 처리 대상을 여러 파티션으로 나눠서 동시에 병렬로 처리하는 방식.

### 현재 순차 처리의 한계

```
창고A 발주들 처리 (완료 후)
창고B 발주들 처리 (완료 후)
창고C 발주들 처리 (완료 후)
...창고 22개 × 처리시간 = 전체 처리 시간
```

### 병렬 처리로 바뀌면

```
창고A 발주들 처리 ────────────────────┐
창고B 발주들 처리 ──────────────┐     │ 동시!
창고C 발주들 처리 ──────────┐   │     │
                             └───┴─────┘
총 처리 시간 = 가장 오래 걸리는 창고 기준
```

예상 개선: 순차 30초+ → 병렬 3초 (최대 10배 단축)

### Master-Worker 구조

```
MasterStep (Partitioner)
  → 창고 22개를 N개 파티션으로 나눔
  → 각 파티션에 "담당 warehouseId 목록" 전달

WorkerStep × N (각각 별도 스레드)
  ├── Reader    : 담당 창고의 REQUESTED 발주만 조회
  ├── Processor : 승인 검증
  └── Writer    : approveOne() 호출
```

### 왜 "매장 기준"이 아닌 "창고 기준"으로 나누는가?

재고 락(`PESSIMISTIC_WRITE`)이 **창고 단위**로 걸립니다.

```
매장 기준 (잘못된 설계) → DeadLock 발생:
  파티션1 → 매장A → 창고W1 재고 락 시도
  파티션2 → 매장B → 창고W1 재고 락 시도  ← 같은 창고!
  → 두 스레드가 동일한 창고 재고를 동시에 잠그려 함 → DeadLock!

창고 기준 (올바른 설계) → DeadLock 없음:
  파티션1 → 창고W1 담당 매장들(A,B,C) 처리
  파티션2 → 창고W2 담당 매장들(D,E,F) 처리
  → W1 재고는 파티션1만, W2 재고는 파티션2만 접근 → 충돌 없음
```

**핵심 규칙: 창고 1개는 반드시 파티션 1개에만 속해야 합니다.**

### @StepScope — 반드시 붙여야 하는 어노테이션

파티션의 각 Worker Step에서 Reader/Writer에 반드시 `@StepScope`를 붙여야 합니다.

```java
// ❌ 없으면: 모든 스레드가 같은 Reader 인스턴스를 공유
//           → 창고1 데이터와 창고2 데이터가 뒤섞임

// ✅ 있으면: 각 스레드가 자신만의 Reader 인스턴스를 사용
@Bean
@StepScope
public JdbcCursorItemReader<StoreOrderHeader> partitionReader(
    @Value("#{stepExecutionContext['storeIds']}") List<Long> storeIds) {
    // 이 Reader는 해당 파티션의 storeIds만 처리
}
```

### ThreadPoolTaskExecutor 주의사항

```java
executor.setCorePoolSize(4);
executor.setQueueCapacity(gridSize); // 파티션 수만큼 큐 확보

// ❌ queueCapacity=0 설정 시:
// 파티션 수 > corePoolSize 이면 초과 파티션이 즉시 거절됨
// → 배치가 빠르게 완료된 것처럼 보이지만 실제로는 처리 누락!
```

---

## 개념 8 — faultTolerant / Retry / Skip

### 현재 수동 try-catch 방식의 문제

```java
for (StoreOrderHeader order : orders) {
    try {
        approveOne(order);
        successCount++;
    } catch (Exception e) {
        log.error("실패: orderId={}", order.getId(), e);
        failCount++;
        // 끝. 다음 배치까지 이 건 방치.
    }
}
```

DB가 순간적으로 바빠서 실패한 건도, 데이터 오류로 영원히 실패할 건도, 같은 방식으로 처리합니다.

### Spring Batch의 자동화된 실패 처리

```
faultTolerant() 활성화
  │
  ├── Retry: 일시적 오류 → 자동 재시도
  │     TransientDataAccessException 발생
  │     → 1초 대기 후 재시도
  │     → 2초 대기 후 재시도
  │     → 4초 대기 후 재시도 (3회)
  │     → 3회 안에 성공 → 정상 처리
  │
  └── Skip: 재시도도 실패한 건 → 건너뛰고 계속
        → SkipListener 호출
        → store_order_batch_skip_log 테이블에 기록
        → 나머지 정상 건은 계속 처리
```

### 이 프로젝트 설정

```java
.faultTolerant()
.retryLimit(3)
.retry(TransientDataAccessException.class)       // DB 일시 오류는 재시도
.retry(DeadlockLoserDataAccessException.class)   // 데드락 패배자도 재시도
.noRetry(BaseException.class)                    // 비즈니스 예외는 재시도 안 함
.skipLimit(20)                                   // 1,100건 기준 약 2% 수준
.skip(BaseException.class)                       // 비즈니스 예외는 스킵 후 계속
.noSkip(DataIntegrityViolationException.class)   // 데이터 무결성 위반은 스킵 안 함
```

### skipLimit 설정 주의

skipLimit을 너무 작게 설정하면, 임계치 초과 시 `SkipLimitExceededException`으로 **Job 전체가 FAILED**됩니다.
발주 건수 기준 1~2% 수준으로 설정하는 것을 권장합니다.

---

## 개념 9 — ShedLock (중복 실행 방지)

### 문제 상황

K8s Pod가 2개 떠 있을 때:

```
18:00 도달
  → Pod A: @Scheduled 발동 → 배치 시작
  → Pod B: @Scheduled 발동 → 배치 시작  ← 동일 발주 이중 처리!
```

### ShedLock 해결 방법

DB의 `shedlock` 테이블을 분산 락(distributed lock)으로 사용합니다.

```sql
-- 수동으로 생성해야 하는 테이블
CREATE TABLE shedlock(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
```

```java
// 스케줄러에 붙이는 어노테이션
@SchedulerLock(name = "storeOrderBatchApproveJob", lockAtMostFor = "PT30M")
```

### 동작 원리

```
18:00 도달
  → Pod A: shedlock 테이블에 락 INSERT 성공 → 배치 실행
  → Pod B: shedlock 테이블에 락 INSERT 실패 (이미 있음) → 즉시 종료
```

Spring Batch와 별개로 도입하는 외부 라이브러리입니다. Stage 1에서 가장 먼저 적용합니다.

---

## 개념 10 — Spring Batch 5.x 주의사항

이 프로젝트는 Spring Boot 3.x = Spring Batch **5.x**입니다.
인터넷 예제 대부분이 4.x 기준이라 복붙하면 컴파일 에러가 납니다.

### 주요 API 변경점

| Batch 4.x (인터넷 예제) | Batch 5.x (이 프로젝트) |
|----------------------|----------------------|
| `new JobBuilderFactory()` | `new JobBuilder("name", jobRepository)` |
| `new StepBuilderFactory()` | `new StepBuilder("name", jobRepository)` |
| `new JobParameter("값")` | `new JobParameter<>("값", String.class)` |
| `addString("key", "value")` | `addLocalDate()`, `addLocalDateTime()` |

### JPA 트랜잭션 매니저 충돌 주의

```java
// BatchConfig에서 트랜잭션 매니저를 명시적으로 지정해야 함
@Bean
public Step approveStep(JobRepository jobRepository,
                         PlatformTransactionManager transactionManager) {
    return new StepBuilder("approveStep", jobRepository)
        .tasklet(approveTasklet(), transactionManager)  // 명시 지정 필수
        .build();
}
```

---

## 예상 이슈 우선순위 요약

| 우선순위 | 이슈 | 발생 단계 | 위험도 |
|--------|------|---------|------|
| 🔴 1 | DeadLock (매장별 파티션 설계 시) | Stage 3 | 높음 — 처리 중단 |
| 🔴 2 | Boot 3.x = Batch 5.x API 변경 | Stage 1~ | 높음 — 컴파일 불가 |
| 🔴 3 | Job 자동 실행 (enabled 설정 누락) | Stage 1 | 높음 — 운영 데이터 오염 |
| 🟡 4 | JpaCursorItemReader + REQUIRES_NEW 충돌 | Stage 2 | 중간 — 런타임 오류 |
| 🟡 5 | PagingItemReader OFFSET 밀림 | Stage 2 | 중간 — 조용한 데이터 누락 |
| 🟡 6 | queueCapacity=0 조기 종료 | Stage 3 | 중간 — 처리 누락 |
| 🟡 7 | @StepScope 누락으로 스레드 상태 공유 | Stage 3 | 중간 — 데이터 꼬임 |
| 🟢 8 | skipLimit 초과로 Job 전체 FAILED | Stage 4 | 낮음 — 설정으로 해결 |
| 🟢 9 | Entity 중복 관리 (MSA 분리 후) | Stage 5 | 낮음 — 유지보수 부담 |

---

## 학습 로드맵

```
지금 바로        → 개념 1~3  (Spring Batch 전체 그림, Job/Step, JobRepository)

Stage 1 구현 전  → 개념 4, 9, 10  (Tasklet, ShedLock, 5.x 주의사항)
Stage 2 구현 전  → 개념 5, 6      (Chunk, JobParameter)
Stage 3 구현 전  → 개념 7         (Partitioned Step) — 가장 복잡, 꼼꼼히
Stage 4 구현 전  → 개념 8         (faultTolerant / Retry / Skip)
```

---

## 단계별 성능 측정 계획

| Stage | 측정 조건 | 기록 항목 |
|-------|---------|---------|
| 0 (AS-IS) | 1,100건 / 2,200건 | 처리 시간, 힙 메모리 |
| 1 | 1,100건 / 2,200건 | 처리 시간 (변화 없어야 함), 이력 DB 확인 |
| 2 | 1,100건, chunk size 10/50/100/200 | 처리 시간, 힙 메모리 피크 |
| 3 | 1,100건, partition 4/8/11/22 | 처리 시간, DB 커넥션 수 |
| 4 | 일부 실패 inject | skip 건수, retry 로그 |
| 5 | 1,100건 (MSA 분리 후) | 처리 시간, 네트워크 오버헤드 |

---

## 각 단계 완료 후 검증 명령

```bash
# k6 성능 측정
k6 run --env BASE_URL=https://www.stockit.kro.kr --env MODE=baseline before.js

# 처리 건수 확인
SELECT status, COUNT(*) FROM store_order_header GROUP BY status;

# Spring Batch 이력 확인 (Stage 1 이후)
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY START_TIME DESC LIMIT 5;
```
