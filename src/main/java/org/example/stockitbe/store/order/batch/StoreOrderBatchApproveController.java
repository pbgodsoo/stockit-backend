package org.example.stockitbe.store.order.batch;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
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
    // storeCode → storeId 변환에 사용. BATCH-6에서 Service의 runManual()이 제거되면서
    // 동일 변환 로직을 Controller가 Repository를 직접 참조해 수행한다.
    private final InfrastructureRepository infrastructureRepository;

    // 발주 승인 배치 수동 실행 (Spring Batch Job → BATCH_JOB_EXECUTION 이력 자동 기록)
    @PostMapping("/run")
    public BaseResponse<StoreOrderBatchDto.RunRes> run(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody StoreOrderBatchDto.RunReq req
    ) {
        try {
            // runId: JobParameter에 포함해 동일 요청 재실행을 구분하는 고유 키.
            // MIDNIGHT 스케줄러는 날짜가 고유 키 역할을 하므로 runId가 불필요하지만,
            // 수동 실행은 같은 날 여러 번 호출될 수 있어 UUID로 고유화한다.
            String runId = UUID.randomUUID().toString();

            JobParametersBuilder builder = new JobParametersBuilder()
                    .addString("runType", "MANUAL")
                    .addString("runId", runId);
            if (req.getFromDateTime() != null) {
                builder.addLocalDateTime("fromDateTime", req.getFromDateTime());
            }
            if (req.getToDateTime() != null) {
                builder.addLocalDateTime("toDateTime", req.getToDateTime());
            }

            // mode=STORE일 때만 storeCode → storeId 변환 후 파라미터에 추가.
            // Reader는 storeId=0을 "전체 매장" sentinel로 해석하므로 미전달 시 전체 처리.
            if (req.getMode() == StoreOrderBatchScope.STORE) {
                Long storeId = infrastructureRepository
                        .findByCodeAndLocationType(req.getStoreCode(), LocationType.STORE)
                        .map(Infrastructure::getId)
                        .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_STORE_NOT_FOUND));
                builder.addLong("storeId", storeId);
            }

            JobParameters params = builder.toJobParameters();
            JobExecution jobExecution = jobLauncher.run(storeOrderBatchApproveJob, params);

            StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
            ExecutionContext ec = stepExecution.getExecutionContext();

            // runId: BATCH_JOB_EXECUTION.job_execution_id를 사용해 DB 이력과 직접 대응.
            // requestedCount: Chunk 방식에서 Spring Batch가 read_count를 자동 집계.
            // successCount/failCount: Writer가 per-item 처리 후 chunk마다 EC에 누적 저장.
            return BaseResponse.success(StoreOrderBatchDto.RunRes.builder()
                    .runId(String.valueOf(jobExecution.getId()))
                    .triggerType(StoreOrderBatchTriggerType.MANUAL)
                    .scope(req.getMode())
                    .storeCode(req.getStoreCode())
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
