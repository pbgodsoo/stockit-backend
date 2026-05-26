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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
            // KST 기준 전일 범위를 계산해 Reader에 전달한다.
            // fromDateTime/toDateTime이 JobParameter에 포함되어 날짜가 달라지면
            // 실행 인스턴스가 자동으로 구분되므로 runAt 타임스탬프가 불필요하다.
            ZoneId kst = ZoneId.of("Asia/Seoul");
            LocalDate prevDay = LocalDate.now(kst).minusDays(1);
            LocalDateTime from = prevDay.atStartOfDay();
            LocalDateTime to   = prevDay.plusDays(1).atStartOfDay().minusNanos(1);

            JobParameters params = new JobParametersBuilder()
                    .addString("runType", "MIDNIGHT")
                    .addLocalDateTime("fromDateTime", from)
                    .addLocalDateTime("toDateTime", to)
                    .toJobParameters();
            jobLauncher.run(storeOrderBatchApproveJob, params);
        } catch (Exception e) {
            log.error("[STORE-ORDER-BATCH] job launch failed", e);
            throw new RuntimeException(e);
        }
    }
}
