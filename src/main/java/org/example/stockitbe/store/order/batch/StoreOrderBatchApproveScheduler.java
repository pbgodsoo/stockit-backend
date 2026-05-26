package org.example.stockitbe.store.order.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreOrderBatchApproveScheduler {

    private final JobLauncher jobLauncher;
    private final Job storeOrderBatchApproveJob;

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
        try {
            // Spring Batch는 동일한 파라미터로 성공한 Job의 재실행을 거부함.
            // runAt 타임스탬프로 매 실행을 고유하게 구분.
            // triggerType: Reader SQL 분기 기준 (AUTO → 전일 날짜 범위 필터 적용).
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .addString("triggerType", "AUTO")
                    .toJobParameters();
            jobLauncher.run(storeOrderBatchApproveJob, params);
        } catch (Exception e) {
            log.error("[STORE-ORDER-BATCH] job launch failed", e);
            throw new RuntimeException(e);
        }
    }
}
