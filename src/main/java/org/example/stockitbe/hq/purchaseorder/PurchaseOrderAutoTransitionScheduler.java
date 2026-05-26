package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SYS-001 본사 발주 자동 전환 스케줄러.
 *
 * fixedDelay 주기로 {@link PurchaseOrderBatchService#run(boolean)} 을 시간 조건 모드(force=false)로 호출.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderAutoTransitionScheduler {

    private final PurchaseOrderBatchService batchService;

    @Scheduled(fixedDelayString = "${purchase-order.batch.delay-ms:300000}")
    @SchedulerLock(
            name = "purchaseOrderBatch",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m"
    )
    public void run() {
        log.info("[SYS-001] 배치 시작");
        PurchaseOrderBatchService.BatchResult result = batchService.run(false);
        log.info("[SYS-001] 배치 완료 — approved={}, readyToShip={}, inTransit={}, arrived={}",
                result.approved(), result.readyToShip(), result.inTransit(), result.arrived());
    }
}
