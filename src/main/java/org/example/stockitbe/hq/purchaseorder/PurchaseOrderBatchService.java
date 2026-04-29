package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * SYS-001 본사 발주 자동 전환 배치 서비스.
 *
 * 5분 주기 스케줄러({@link PurchaseOrderAutoTransitionScheduler}) 와 시연용 강제 트리거
 * 컨트롤러({@link PurchaseOrderBatchController}) 모두 이 서비스의 {@link #run(boolean)} 을 호출한다.
 *
 * 1건 1트랜잭션 — {@link PurchaseOrderService#approve(String)} / {@link PurchaseOrderService#startShipping(String)}
 * 가 자체 {@code @Transactional} 이므로 이 클래스에는 트랜잭션 어노테이션을 절대 달지 않는다.
 * 한 건 실패는 try/catch 로 격리하고 다음 건을 계속 처리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderBatchService {

    private final PurchaseOrderRepository repo;
    private final PurchaseOrderService service;

    @Value("${purchase-order.batch.wait-minutes:30}")
    private long waitMinutes;

    /**
     * @param force true 면 시간 조건 무시 (시연·QA·장애 대응) — 모든 PENDING/APPROVED/SHIPPING 즉시 처리.
     *              false 면 {@code updatedAt < now - waitMinutes} 인 건만.
     *
     * 거래처 책임 단계 3개 모두 SYS-001 자동 전환 (ADR-013/019).
     * SHIPPING → DELIVERED 도 같은 패턴 — 배송 도착 시점도 거래처 책임이라 자동화.
     * DELIVERED → COMPLETED 는 창고 [입고 확정] 매뉴얼 트리거(WHS-007) — 배치 처리 안 함.
     */
    public BatchResult run(boolean force) {
        int approved  = autoTransition(PurchaseOrderStatus.PENDING,  force, code -> service.approve(code));
        int shipping  = autoTransition(PurchaseOrderStatus.APPROVED, force, code -> service.startShipping(code));
        int delivered = autoTransition(PurchaseOrderStatus.SHIPPING, force, code -> service.deliver(code));
        return new BatchResult(approved, shipping, delivered);
    }

    private int autoTransition(PurchaseOrderStatus from, boolean force, Consumer<String> action) {
        List<PurchaseOrder> targets = force
                ? repo.findAllByStatus(from)
                : repo.findAllByStatusAndUpdatedAtBefore(
                        from,
                        Date.from(Instant.now().minus(Duration.ofMinutes(waitMinutes))));
        int success = 0;
        for (PurchaseOrder po : targets) {
            try {
                action.accept(po.getCode());
                success++;
            } catch (Exception e) {
                log.warn("[SYS-001] 자동 전환 실패 from={} code={}", from, po.getCode(), e);
            }
        }
        return success;
    }

    public record BatchResult(int approved, int shipping, int delivered) {}
}
