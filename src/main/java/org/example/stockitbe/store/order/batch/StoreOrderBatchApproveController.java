package org.example.stockitbe.store.order.batch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "발주 배치 승인", description = "매장 발주 일괄 승인 배치 수동 실행 및 대기 매장 조회 API (HQ 전용)")
@RestController
@RequestMapping("/api/hq/store-orders/batch-approve")
@RequiredArgsConstructor
public class StoreOrderBatchApproveController {

    private final StoreOrderBatchApproveService batchApproveService;
    private final JobLauncher jobLauncher;
    private final Job storeOrderBatchApproveJob;
    private final InfrastructureRepository infrastructureRepository;

    // ── R : Read ─────────────────────────────────────────────────────────────
    @Operation(summary = "승인 대기 매장 목록 조회", description = "REQUESTED 상태의 발주가 존재하는 매장 목록과 건수를 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/pending-stores")
    public BaseResponse<List<StoreOrderBatchDto.PendingStoreRes>> pendingStores(
            @AuthenticationPrincipal AuthUserDetails me
    ) {
        return BaseResponse.success(batchApproveService.listPendingStores());
    }

    // ── Action : Run ──────────────────────────────────────────────────────────
    @Operation(summary = "발주 배치 승인 수동 실행", description = "지정 기간 내 REQUESTED 상태 발주를 일괄 승인하는 Spring Batch Job을 수동으로 실행한다. mode=STORE이면 storeCode 필수.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배치 실행 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터 (mode=STORE인데 storeCode 누락 등)")
    })
    @PostMapping("/run")
    public BaseResponse<StoreOrderBatchDto.RunRes> run(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody StoreOrderBatchDto.RunReq req
    ) {
        try {
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
}
