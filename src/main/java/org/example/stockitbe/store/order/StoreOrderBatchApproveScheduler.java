package org.example.stockitbe.store.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.store.order.model.dto.StoreOrderBatchDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreOrderBatchApproveScheduler {

    private final StoreOrderBatchApproveService batchApproveService;

    // 발주 자동 배치 스케줄러 추가 (매일 18:00, 00:00에 실행)
    @Scheduled(
            cron = "${store-order.batch.cron:0 0 18,0 * * *}",
            zone = "${store-order.batch.zone:Asia/Seoul}"
    )
    public void runAutoBatch() {
        StoreOrderBatchDto.RunRes result = batchApproveService.runAutoDaily();
        log.info("[STORE-ORDER-BATCH] scheduler done runId={} requested={} success={} fail={}",
                result.getRunId(), result.getRequestedCount(), result.getSuccessCount(), result.getFailCount());
    }
}

