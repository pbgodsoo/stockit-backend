package org.example.stockitbe.hq.circularbuyer.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.example.stockitbe.hq.circularbuyer.sync.model.EsSyncOperationType;
import org.example.stockitbe.hq.circularbuyer.sync.model.EsSyncOutbox;
import org.example.stockitbe.hq.circularbuyer.sync.model.EsSyncOutboxStatus;
import org.example.stockitbe.hq.circularbuyer.sync.repository.EsSyncOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CircularBuyerEsSyncRetryScheduler {

    private final EsSyncOutboxRepository outboxRepository;
    private final CircularBuyerRepository circularBuyerRepository;
    private final CircularBuyerEsSyncService syncService;

    @Value("${stockit.elasticsearch.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${stockit.elasticsearch.sync.retry.max-attempts:8}")
    private int maxAttempts;

    @Value("${stockit.elasticsearch.sync.retry.base-delay-ms:30000}")
    private long baseDelayMs;

    @Value("${stockit.elasticsearch.sync.retry.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${stockit.elasticsearch.sync.retry.interval-ms:30000}")
    @Transactional
    public void retryPending() {
        if (!syncEnabled) return;

        List<EsSyncOutbox> pendings = outboxRepository.findByStatusAndNextRetryAtLessThanEqualOrderByIdAsc(
                EsSyncOutboxStatus.PENDING,
                Instant.now(),
                PageRequest.of(0, Math.max(1, batchSize))
        );

        int success = 0;
        int failed = 0;

        for (EsSyncOutbox item : pendings) {
            item.markProcessing();
            try {
                replay(item);
                item.markDone();
                success++;
            } catch (Exception e) {
                failed++;
                if (item.getRetryCount() + 1 >= Math.max(1, maxAttempts)) {
                    item.markDead(trimError(e.getMessage()));
                } else {
                    long nextDelay = computeBackoffDelay(item.getRetryCount() + 1);
                    item.markRetry(Instant.now().plusMillis(nextDelay), trimError(e.getMessage()));
                }
            }
        }

        if (!pendings.isEmpty()) {
            log.info("[ES-SYNC-RETRY] processed={} success={} failed={}", pendings.size(), success, failed);
        }
    }

    private void replay(EsSyncOutbox item) throws Exception {
        if (item.getOpType() == EsSyncOperationType.DELETE) {
            syncService.doDelete(item.getEntityKey());
            return;
        }

        CircularBuyer buyer = circularBuyerRepository.findByCode(item.getEntityKey())
                .orElseThrow(() -> new IllegalStateException("RDB buyer not found: " + item.getEntityKey()));
        syncService.doUpsert(buyer);
    }

    private long computeBackoffDelay(int attempt) {
        int cappedAttempt = Math.min(Math.max(1, attempt), 10);
        long factor = 1L << (cappedAttempt - 1);
        long delay = baseDelayMs * factor;
        long max = 30L * 60L * 1000L;
        return Math.min(delay, max);
    }

    private static String trimError(String reason) {
        if (reason == null) return "unknown";
        return reason.length() > 1500 ? reason.substring(0, 1500) : reason;
    }
}
