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
import java.util.UUID;

@RestController
@RequestMapping("/api/hq/store-orders/batch-approve")
@RequiredArgsConstructor
public class StoreOrderBatchApproveController {

    private final StoreOrderBatchApproveService batchApproveService;
    private final JobLauncher jobLauncher;
    private final Job storeOrderBatchApproveJob;

    // 발주 승인 배치 처리 (Spring Batch Job으로 실행 → BATCH_JOB_EXECUTION 이력 자동 기록)
    @PostMapping("/run")
    public BaseResponse<StoreOrderBatchDto.RunRes> run(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody(required = false) StoreOrderBatchDto.RunReq req
    ) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .addString("triggerType", "MANUAL")
                    .toJobParameters();
            JobExecution jobExecution = jobLauncher.run(storeOrderBatchApproveJob, params);

            // Tasklet이 ExecutionContext에 저장한 처리 결과를 HTTP 응답으로 반환
            StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
            ExecutionContext ec = stepExecution.getExecutionContext();

            return BaseResponse.success(StoreOrderBatchDto.RunRes.builder()
                    .runId(ec.getString("runId", UUID.randomUUID().toString()))
                    .triggerType(StoreOrderBatchTriggerType.MANUAL)
                    .scope(StoreOrderBatchScope.ALL)
                    .requestedCount(ec.getInt("requestedCount", 0))
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
