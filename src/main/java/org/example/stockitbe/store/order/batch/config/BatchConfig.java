package org.example.stockitbe.store.order.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.store.order.batch.StoreOrderBatchApproveService;
import org.example.stockitbe.store.order.batch.model.dto.StoreOrderBatchDto;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final StoreOrderBatchApproveService batchApproveService;

    // 배치 작업 전체를 대표하는 단위
    // 실행될 때마다 BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION 테이블에 이력이 자동 기록됨
    @Bean
    public Job storeOrderBatchApproveJob(Step storeOrderBatchApproveStep) {
        return new JobBuilder("storeOrderBatchApproveJob", jobRepository)
                .start(storeOrderBatchApproveStep)
                .build();
    }

    // Job 안에서 실제로 실행되는 단계
    // 현재는 단계가 하나지만 필요 시 전처리 → 처리 → 후처리 형태로 Step 추가 가능
    @Bean
    public Step storeOrderBatchApproveStep() {
        return new StepBuilder("storeOrderBatchApproveStep", jobRepository)
                .tasklet(storeOrderBatchApproveTasklet(), transactionManager)
                .build();
    }

    // Step이 실행할 실제 코드 조각
    // triggerType 파라미터로 수동(MANUAL) / 자동(AUTO) 실행 경로를 분기
    @Bean
    public Tasklet storeOrderBatchApproveTasklet() {
        return (contribution, chunkContext) -> {
            String triggerType = chunkContext.getStepContext()
                    .getStepExecution().getJobParameters()
                    .getString("triggerType", "AUTO");

            StoreOrderBatchDto.RunRes result;
            if ("MANUAL".equals(triggerType)) {
                // 수동 실행: 날짜 필터 없이 전체 REQUESTED 처리
                result = batchApproveService.runManual(null, null);
            } else {
                // 자동 실행(스케줄러): 전일 날짜 범위의 REQUESTED만 처리
                result = batchApproveService.runAutoDaily();
            }

            // Controller가 HTTP 응답으로 반환할 수 있도록 ExecutionContext에 결과 저장
            ExecutionContext ec = chunkContext.getStepContext()
                    .getStepExecution().getExecutionContext();
            ec.putString("runId", result.getRunId());
            ec.putInt("requestedCount", result.getRequestedCount());
            ec.putInt("successCount", result.getSuccessCount());
            ec.putInt("failCount", result.getFailCount());

            log.info("[STORE-ORDER-BATCH] job done trigger={} runId={} requested={} success={} fail={}",
                    triggerType, result.getRunId(), result.getRequestedCount(),
                    result.getSuccessCount(), result.getFailCount());
            return RepeatStatus.FINISHED;
        };
    }
}
