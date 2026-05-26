package org.example.stockitbe.store.order.batch;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.store.order.batch.model.dto.StoreOrderBatchDto;
import org.example.stockitbe.store.order.batch.model.enums.StoreOrderBatchScope;
import org.example.stockitbe.store.order.batch.model.enums.StoreOrderBatchTriggerType;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hq/store-orders/batch-approve")
@RequiredArgsConstructor
public class StoreOrderBatchApproveController {

    private final StoreOrderBatchApproveService batchApproveService;
    private final JobLauncher jobLauncher;
    private final Job storeOrderBatchApproveJob;

    // 발주 승인 배치 수동 실행 (Spring Batch Job → BATCH_JOB_EXECUTION 이력 자동 기록)
    @PostMapping("/run")
    public BaseResponse<StoreOrderBatchDto.RunRes> run(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody(required = false) StoreOrderBatchDto.RunReq req
    ) {
        try {
            // triggerType=MANUAL: Reader가 날짜 필터 없이 전체 REQUESTED를 읽도록 분기.
            // runAt 타임스탬프: 동일 파라미터 재실행 거부를 피하기 위한 고유화 키.
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .addString("triggerType", "MANUAL")
                    .toJobParameters();
            JobExecution jobExecution = jobLauncher.run(storeOrderBatchApproveJob, params);

            StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
            ExecutionContext ec = stepExecution.getExecutionContext();

            // runId: BATCH_JOB_EXECUTION.job_execution_id를 사용해 DB 이력과 직접 대응.
            // requestedCount: Chunk 방식에서는 Spring Batch가 read_count를 자동 집계하므로 EC 대신 사용.
            // successCount/failCount: Writer가 per-item 처리 후 chunk마다 EC에 누적 저장.
            return BaseResponse.success(StoreOrderBatchDto.RunRes.builder()
                    .runId(String.valueOf(jobExecution.getId()))
                    .triggerType(StoreOrderBatchTriggerType.MANUAL)
                    .scope(StoreOrderBatchScope.ALL)
                    .requestedCount((int) stepExecution.getReadCount())
                    .successCount(ec.getInt("successCount", 0))
                    .failCount(ec.getInt("failCount", 0))
                    .results(List.of())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 승인 대기 발주건이 있는 매장 조회
    @GetMapping("/pending-stores")
    public BaseResponse<List<StoreOrderBatchDto.PendingStoreRes>> pendingStores(
            @AuthenticationPrincipal AuthUserDetails me
    ) {
        return BaseResponse.success(batchApproveService.listPendingStores());
    }
}
