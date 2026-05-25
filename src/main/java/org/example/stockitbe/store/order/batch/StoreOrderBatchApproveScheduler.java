package org.example.stockitbe.store.order.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.stockitbe.store.order.batch.model.dto.StoreOrderBatchDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreOrderBatchApproveScheduler {

    private final StoreOrderBatchApproveService batchApproveService;

    // 매장 발주 자동 승인 배치 (매일 00:00 실행)
    // @SchedulerLock: 다중 Pod 중 하나만 실행되도록 분산 락 적용
    //   - name: shedlock 테이블의 락 식별자 (스케줄러마다 고유해야 함)
    //   - lockAtMostFor: Pod 비정상 종료 시에도 30분 후 락 자동 해제
    @Scheduled(
            cron = "${store-order.batch.cron:0 0 0 * * *}",
            zone = "${store-order.batch.zone:Asia/Seoul}"
    )
    @SchedulerLock(name = "storeOrderBatchApproveJob", lockAtMostFor = "PT30M")
    public void runAutoBatch() {
        StoreOrderBatchDto.RunRes result = batchApproveService.runAutoDaily();
        log.info("[STORE-ORDER-BATCH] scheduler done runId={} requested={} success={} fail={}",
                result.getRunId(), result.getRequestedCount(), result.getSuccessCount(), result.getFailCount());
    }
}
