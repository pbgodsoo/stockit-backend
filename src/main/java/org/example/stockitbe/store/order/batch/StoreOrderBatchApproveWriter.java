package org.example.stockitbe.store.order.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.store.order.batch.model.dto.StoreOrderBatchItem;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class StoreOrderBatchApproveWriter implements ItemWriter<StoreOrderBatchItem> {

    private final StoreOrderBatchApproveItemService itemService;

    // jobParameters는 Step 실행 시점에 확정되므로 생성자가 아닌 @Value SpEL로 주입.
    // 기본값 'MIDNIGHT'은 runType 파라미터 누락 시의 안전망.
    @Value("#{jobParameters['runType'] ?: 'MIDNIGHT'}")
    private String runType;

    // @BeforeStep: Step 실행 직전에 Spring Batch가 호출해 live StepExecution을 주입.
    // write() 내에서 카운트를 누적하고 EC에 저장하기 위해 참조를 보관.
    private StepExecution stepExecution;
    private int successCount;
    private int failCount;

    // ExecutionContext에 초기값 0을 명시적으로 써두지 않으면,
    // Job이 FAILED 후 재시작될 때 이전 실행의 EC 값이 복원되어 카운트가 오염될 수 있다.
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        ExecutionContext ec = stepExecution.getExecutionContext();
        ec.putInt("successCount", 0);
        ec.putInt("failCount", 0);
        this.successCount = 0;
        this.failCount = 0;
    }

    @Override
    public void write(Chunk<? extends StoreOrderBatchItem> chunk) throws Exception {
        // runType별 감사 주체 분기.
        // MANUAL: 관리자가 HTTP로 직접 실행 → actorId는 비워두고 이름만 SYSTEM으로 기록.
        // MIDNIGHT: 스케줄러 자동 실행 → actorId와 actorName 모두 SYSTEM_BATCH로 통일.
        String actorId;
        String actorName;
        String reason;
        if ("MANUAL".equals(runType)) {
            actorId = "";
            actorName = "SYSTEM";
            reason = "본사 수동 배치 처리";
        } else {
            actorId = "SYSTEM_BATCH";
            actorName = "SYSTEM_BATCH";
            reason = "MIDNIGHT_BATCH_APPROVE";
        }

        // per-item try-catch: 한 건 실패가 chunk 전체를 롤백시키지 않도록 예외를 삼킨다.
        // approveOne()은 REQUIRES_NEW 트랜잭션이므로 실패해도 이미 커밋된 건에 영향 없음.
        for (StoreOrderBatchItem item : chunk) {
            try {
                itemService.approveOne(item.getOrderNo(), actorId, actorName, reason);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("[STORE-ORDER-BATCH] fail orderNo={} runType={}", item.getOrderNo(), runType, e);
            }
        }

        // chunk 완료마다 EC에 누적값을 저장해 Job 비정상 종료 시에도 중간 집계가 보존된다.
        ExecutionContext ec = stepExecution.getExecutionContext();
        ec.putInt("successCount", successCount);
        ec.putInt("failCount", failCount);
    }
}
