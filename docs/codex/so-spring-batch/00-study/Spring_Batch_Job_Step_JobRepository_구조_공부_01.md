# Spring Batch — Job / Step / JobRepository 전체 구조 공부 01

## 한 문장 요약

```
JobLauncher가 Job을 실행하면,
Job 안의 Step들이 순서대로 실행되고,
모든 실행 결과는 JobRepository가 DB에 기록한다.
```

---

## 전체 구조 지도

```
┌─────────────────────────────────────────────────────┐
│                   Spring Batch                      │
│                                                     │
│  [JobLauncher]  →  [Job]  →  [Step]  →  [Step]      │
│                       │                             │
│                       ↓                             │
│               [JobRepository]                       │
│                  (DB 저장)                           │
│                                                     │
│  DB: BATCH_JOB_INSTANCE                             │
│      BATCH_JOB_EXECUTION                            │
│      BATCH_JOB_EXECUTION_PARAMS                     │
│      BATCH_STEP_EXECUTION                           │
│      BATCH_JOB_EXECUTION_CONTEXT                    │
│      BATCH_STEP_EXECUTION_CONTEXT                   │
└─────────────────────────────────────────────────────┘
```

---

## 등장 개념 7가지

---

### 1. Job — "무엇을 할 것인가"를 정의하는 설계도

Job은 실행될 배치 작업 전체를 정의하는 최상위 단위.  
Job 자체는 실제로 "실행"되지 않는다 — **설계도**일 뿐.

```java
// 이 프로젝트에서 만들 Job
@Bean
public Job storeOrderBatchApproveJob(JobRepository jobRepository, Step approveStep) {
    return new JobBuilder("storeOrderBatchApproveJob", jobRepository)
        .start(approveStep)
        .build();
}
```

**Job이 가진 것:**
- **이름 (String)** — JobRepository가 이 이름으로 이력을 관리
- **Step 목록** — 어떤 Step을 어떤 순서로 실행할지
- **재시작 가능 여부** — FAILED 시 처음부터? 이어서? 설정 가능

---

### 2. JobInstance — "특정 파라미터로 실행한 케이스 식별자"

Job이 설계도라면, JobInstance는 "이 설계도로 특정 날짜에 실행한 케이스".

```
Job: storeOrderBatchApproveJob (설계도, 1개)
  │
  ├─ JobInstance: 2026-05-24 실행분  ← fromDateTime=2026-05-24T00:00:00
  ├─ JobInstance: 2026-05-25 실행분  ← fromDateTime=2026-05-25T00:00:00
  └─ JobInstance: 2026-05-26 실행분  ← fromDateTime=2026-05-26T00:00:00
```

**JobInstance는 JobParameter 조합으로 식별된다.**

```
JobParameter: { fromDateTime: 2026-05-24T00:00:00, runType: MIDNIGHT }
  → 이 조합이 처음이면             → 새 JobInstance 생성
  → 이 조합이 있는데 COMPLETED면   → Spring Batch가 실행 거부
  → 이 조합이 있는데 FAILED면      → 재시작 (기존 JobInstance 이어서)
```

> ⚠️ **MANUAL 배치에 `runId(UUID)`를 넣는 이유**  
> 같은 `fromDateTime + runType` 조합으로 두 번 실행하면 Spring Batch가 거부한다.  
> `runId`를 UUID로 넣으면 매번 새로운 JobInstance로 식별되어 중복 실행이 가능해진다.

---

### 3. JobExecution — "JobInstance를 실제로 실행한 한 번의 시도"

```
JobInstance (2026-05-24 실행분)
  │
  ├─ JobExecution #1: FAILED    ← 1차 시도, 서버 장애로 중단
  └─ JobExecution #2: COMPLETED ← 2차 시도, 성공
```

JobExecution은 **한 번의 실행 시도**를 의미한다.  
JobInstance 1개에 여러 JobExecution이 생길 수 있다 (실패 후 재시도 등).

**JobExecution이 기록하는 것:**

| 컬럼 | 내용 |
|------|------|
| `START_TIME` | 배치 시작 시각 |
| `END_TIME` | 배치 종료 시각 |
| `STATUS` | STARTED / COMPLETED / FAILED / STOPPED |
| `EXIT_CODE` | COMPLETED / FAILED |

→ DB 테이블: `BATCH_JOB_EXECUTION`

---

### 4. Step — "Job 안의 실제 일하는 단위"

Job이 설계도라면, Step은 그 안의 실제 작업 모듈.  
Job 1개는 Step 여러 개로 구성할 수 있다.

```java
// Step이 2개인 Job 예시
new JobBuilder("myJob", jobRepository)
    .start(validateStep)   // Step 1: 검증
    .next(approveStep)     // Step 2: 승인
    .build();
```

**이 프로젝트에서 단계별 Step 구성:**

```
Stage 1: Step 1개 (Tasklet)
  └─ approveTaskletStep: 기존 for-loop 그대로 래핑

Stage 2: Step 1개 (Chunk)
  └─ approveChunkStep: Reader → Processor → Writer

Stage 3: Step 2개 (Partitioned)
  ├─ masterStep: 파티션 분할만 담당 (실제 처리 안 함)
  └─ workerStep: 각 파티션의 실제 Chunk 처리 (11개 병렬)
```

---

### 5. StepExecution — "Step 1회 실행의 처리 기록"

JobExecution과 같은 개념인데 Step 단위.

```
JobExecution #2 (COMPLETED)
  └─ StepExecution: approveStep
        READ_COUNT:   1100    ← 읽은 건수
        WRITE_COUNT:  1100    ← 처리 완료 건수
        SKIP_COUNT:   0       ← 건너뛴 건수
        COMMIT_COUNT: 22      ← chunk size 50 기준 커밋 횟수
        STATUS:       COMPLETED
```

**Stage 3 파티션에서 StepExecution이 12개 생기는 예:**

```
StepExecution: workerStep:partition0  → READ 100 / WRITE 100
StepExecution: workerStep:partition1  → READ 100 / WRITE 100
...
StepExecution: workerStep:partition10 → READ 100 / WRITE 100
StepExecution: masterStep             → READ 0   / WRITE 0  (분배만 함)
```

파티션별로 몇 건 처리했는지 각각 기록된다.

→ DB 테이블: `BATCH_STEP_EXECUTION`

---

### 6. JobRepository — "모든 실행 이력을 DB에 저장하는 관리자"

개발자가 직접 호출하는 게 아니라, Spring Batch 내부에서 자동으로 사용한다.

**JobRepository가 하는 일:**

| 시점 | 동작 |
|------|------|
| Job 시작 전 | JobInstance 존재 여부 확인 (중복 실행 방지) |
| Job 시작 시 | `BATCH_JOB_EXECUTION` INSERT (STATUS=STARTED) |
| Step 시작 시 | `BATCH_STEP_EXECUTION` INSERT |
| Chunk 완료마다 | `BATCH_STEP_EXECUTION` UPDATE (READ_COUNT, WRITE_COUNT 누적) |
| Step 완료 시 | `BATCH_STEP_EXECUTION` UPDATE (STATUS=COMPLETED) |
| Job 완료 시 | `BATCH_JOB_EXECUTION` UPDATE (STATUS=COMPLETED) |
| 실패 시 | STATUS=FAILED 기록 → 나중에 재시작 가능 |

**JobRepository 설정:**

```yaml
# application.yml
spring:
  batch:
    jdbc:
      initialize-schema: always  # BATCH_* 테이블 자동 생성
    job:
      enabled: false             # 서버 시작 시 자동 실행 방지 ← 반드시 추가
```

---

### 7. JobLauncher — "Job을 실행시키는 방아쇠"

```java
// 스케줄러에서 실행 (MIDNIGHT 배치)
@Scheduled(cron = "0 0 0 * * *")
@SchedulerLock(name = "storeOrderBatchApproveJob", lockAtMostFor = "PT30M")
public void runMidnightBatch() {
    JobParameters params = new JobParametersBuilder()
        .addLocalDateTime("fromDateTime", LocalDate.now().minusDays(1).atStartOfDay())
        .addLocalDateTime("toDateTime",   LocalDate.now().minusDays(1).atTime(23, 59, 59))
        .addString("runType", "MIDNIGHT")
        .toJobParameters();

    jobLauncher.run(storeOrderBatchApproveJob, params);
}

// 수동 API에서 실행 (MANUAL 배치)
@PostMapping("/batch-approve/run")
public ResponseEntity<Void> runManualBatch(@RequestBody BatchRunRequest request) {
    JobParameters params = new JobParametersBuilder()
        .addLocalDateTime("fromDateTime", request.getFromDateTime())
        .addLocalDateTime("toDateTime",   request.getToDateTime())
        .addString("runType", "MANUAL")
        .addString("runId", UUID.randomUUID().toString())  // JobInstance 구분용
        .toJobParameters();

    jobLauncher.run(storeOrderBatchApproveJob, params);
    return ResponseEntity.ok().build();
}
```

---

## 전체 실행 흐름

```
@Scheduled 또는 API 호출
    │
    ▼
JobLauncher.run(job, params)
    │
    ├─ JobRepository에 물어봄: "이 params로 실행한 JobInstance 있어?"
    │       없으면             → 새 JobInstance 생성
    │       있는데 COMPLETED면 → 실행 거부 (JobInstanceAlreadyCompleteException)
    │       있는데 FAILED면    → 재시작 (기존 JobInstance 이어서)
    │
    ├─ BATCH_JOB_EXECUTION INSERT (STATUS=STARTED)
    │
    ▼
Job.execute()
    │
    ▼
Step 실행 (approveStep)
    │
    ├─ BATCH_STEP_EXECUTION INSERT
    │
    ├─ [Tasklet 방식 — Stage 1]
    │      tasklet.execute() 호출 → 기존 for-loop 로직 실행
    │
    ├─ [Chunk 방식 — Stage 2~]
    │      loop {
    │        Reader.read()       × chunkSize건  → 읽기
    │        Processor.process() × 각 건        → 검증
    │        Writer.write()      한 번에         → 저장
    │        BATCH_STEP_EXECUTION: COMMIT_COUNT++ UPDATE
    │      } until (Reader가 null 반환 = 더 읽을 게 없음)
    │
    └─ BATCH_STEP_EXECUTION UPDATE (STATUS=COMPLETED)
    │
    ▼
BATCH_JOB_EXECUTION UPDATE (STATUS=COMPLETED)
```

---

## BATCH_* 테이블 — 영구 이력 테이블

### 특성

- **영구 테이블**: 서버 재시작, 배포와 무관하게 데이터가 유지됨
- **자동 삭제 없음**: Spring Batch는 오래된 이력을 자동으로 지우지 않음
- **누적**: 배치 실행할 때마다 새 행이 쌓임

### 이 프로젝트 기준 1년 누적량

| 테이블 | 1회 실행 | 1년 누적 |
|--------|---------|---------|
| `BATCH_JOB_INSTANCE` | 1행 | ~365행 |
| `BATCH_JOB_EXECUTION` | 1행 | ~365행 (실패 포함 시 더) |
| `BATCH_JOB_EXECUTION_PARAMS` | 4행 (파라미터 4개) | ~1,460행 |
| `BATCH_STEP_EXECUTION` | Stage 3: 12행 (파티션11+마스터1) | ~4,380행 |
| `BATCH_JOB_EXECUTION_CONTEXT` | 1행 | ~365행 |
| `BATCH_STEP_EXECUTION_CONTEXT` | Stage 3: 12행 | ~4,380행 |

→ **전체 합산 1년: 약 1~2만 행** — 이 프로젝트 규모(일 1회)에서는 수년이 지나도 용량 문제 없음.

### 실제로 조회해보는 명령어

```sql
-- 최근 5회 실행 이력
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY START_TIME DESC LIMIT 5;

-- 특정 실행의 Step별 처리 결과
SELECT STEP_NAME, STATUS, READ_COUNT, WRITE_COUNT, SKIP_COUNT, COMMIT_COUNT
FROM BATCH_STEP_EXECUTION
WHERE JOB_EXECUTION_ID = 1;

-- 실패한 배치 목록
SELECT * FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED';
```

### 이력 정리가 필요할 때 (수년 후)

```sql
-- 6개월 지난 이력 정리 (외래키 관계상 아래→위 순서로 삭제)
DELETE FROM BATCH_STEP_EXECUTION_CONTEXT
WHERE STEP_EXECUTION_ID IN (
    SELECT STEP_EXECUTION_ID FROM BATCH_STEP_EXECUTION
    WHERE JOB_EXECUTION_ID IN (
        SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION
        WHERE START_TIME < NOW() - INTERVAL 6 MONTH
    )
);

DELETE FROM BATCH_STEP_EXECUTION
WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION
    WHERE START_TIME < NOW() - INTERVAL 6 MONTH
);

DELETE FROM BATCH_JOB_EXECUTION_CONTEXT
WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION
    WHERE START_TIME < NOW() - INTERVAL 6 MONTH
);

DELETE FROM BATCH_JOB_EXECUTION_PARAMS
WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION
    WHERE START_TIME < NOW() - INTERVAL 6 MONTH
);

DELETE FROM BATCH_JOB_EXECUTION
WHERE START_TIME < NOW() - INTERVAL 6 MONTH;

-- JobInstance는 관련 JobExecution이 모두 삭제된 것만 제거
DELETE FROM BATCH_JOB_INSTANCE
WHERE JOB_INSTANCE_ID NOT IN (
    SELECT JOB_INSTANCE_ID FROM BATCH_JOB_EXECUTION
);
```

---

## 핵심 요약

| 개념 | 비유 | 역할 |
|------|------|------|
| **Job** | 레시피 | 무엇을 어떻게 할지 설계도 |
| **JobInstance** | 오늘의 요리 케이스 | 특정 파라미터 조합의 실행 케이스 |
| **JobExecution** | 요리 시도 기록 | 1번의 실행 시도 (실패해도 기록) |
| **Step** | 요리 단계 | 실제 일하는 단위 (Tasklet or Chunk) |
| **StepExecution** | 단계별 처리 기록 | 처리 건수 / 상태 / 소요 시간 |
| **JobRepository** | 주방 일지 | 모든 실행 이력을 DB에 자동 기록 |
| **JobLauncher** | 요리 시작 버튼 | Job 실행 트리거 (스케줄러 or API) |
