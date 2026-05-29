package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.warehouse.inbound.WhInboundService;
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
 * 1건 1트랜잭션 — PurchaseOrderService 의 각 단계 메소드가 자체 {@code @Transactional} 이므로
 * 이 클래스에는 트랜잭션 어노테이션을 절대 달지 않는다.
 * 한 건 실패는 try/catch 로 격리하고 다음 건을 계속 처리한다.
 *
 * 거래처 책임 4단계 자동 전환:
 *   REQUESTED → APPROVED → READY_TO_SHIP → IN_TRANSIT → ARRIVED
 * ARRIVED → COMPLETED 는 창고 [입고 확정] 매뉴얼 트리거(WHS-007) — 배치 처리 안 함.
 *
 * 한 번의 {@link #run(boolean)} 호출에서 각 발주는 정확히 한 단계만 전진한다 (단계 역순 처리, 아래 참조).
 * 따라서 강제 트리거는 버튼을 누를 때마다 발주가 한 단계씩 진행된다 (REQUESTED→APPROVED→…→ARRIVED).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderBatchService {

    private final PurchaseOrderRepository repo;
    private final PurchaseOrderService service;
    private final WhInboundService whInboundService;

    @Value("${purchase-order.batch.wait-minutes:30}")
    private long waitMinutes;

    /**
     * @param force true 면 시간 조건 무시 (시연·QA·장애 대응) — 발주를 한 단계씩 즉시 처리.
     *              false 면 {@code updatedAt < now - waitMinutes} 인 건만.
     *
     * 단계는 진행 역순(ARRIVED ← REQUESTED)으로 처리한다. 그래야 한 번의 호출에서 방금 전진한 발주가
     * 같은 실행의 다음 단계 처리에 다시 잡히지 않아, 발주당 정확히 한 단계만 전진한다.
     * (정방향이면 REQUESTED→APPROVED 직후 같은 건이 APPROVED→READY_TO_SHIP 에 다시 잡혀 끝까지 연쇄됨)
     *  - force=true(강제 트리거): 호출 1회 = 발주당 1단계. 버튼을 누를 때마다 다음 단계로 진행.
     *  - force=false(스케줄러): wait-minutes 시간 조건이 이미 연쇄를 막으므로 동작 동일 (방금 전진 시 updatedAt 갱신).
     *
     * inbound mirror — APPROVED→READY_TO_SHIP 시점에 inbound row INSERT (createFromPurchaseOrder),
     * READY_TO_SHIP→IN_TRANSIT/IN_TRANSIT→ARRIVED 시점에 inbound mirror UPDATE.
     * 인벤토리 hook 은 inbound 도메인 안에서 처리 (PR #173 위치 이동).
     */
    public BatchResult run(boolean force) {
        // ERP 표준 — inbound mirror 는 INSERT 한 곳만 (READY_TO_SHIP 시점). 진행 단계는 PO 가 진실 원천.
        int arrived      = autoTransition(PurchaseOrderStatus.IN_TRANSIT,    force,
                code -> service.arrive(code), null);
        int inTransit    = autoTransition(PurchaseOrderStatus.READY_TO_SHIP, force,
                code -> service.startInTransit(code), null);
        int readyToShip  = autoTransition(PurchaseOrderStatus.APPROVED,      force,
                code -> service.readyToShip(code), whInboundService::createFromPurchaseOrder);
        int approved     = autoTransition(PurchaseOrderStatus.REQUESTED,     force,
                code -> service.approve(code), null);
        return new BatchResult(approved, readyToShip, inTransit, arrived);
    }

    private int autoTransition(PurchaseOrderStatus from, boolean force,
                                Consumer<String> action,
                                Consumer<PurchaseOrder> mirror) {
        List<PurchaseOrder> targets = force
                ? repo.findAllByStatus(from)
                : repo.findAllByStatusAndUpdatedAtBefore(
                        from,
                        Date.from(Instant.now().minus(Duration.ofMinutes(waitMinutes))));
        int success = 0;
        for (PurchaseOrder po : targets) {
            try {
                action.accept(po.getCode());
                if (mirror != null) {
                    mirror.accept(po);
                }
                success++;
            } catch (Exception e) {
                log.warn("[SYS-001] 자동 전환 실패 from={} code={}", from, po.getCode(), e);
            }
        }
        return success;
    }

    public record BatchResult(int approved, int readyToShip, int inTransit, int arrived) {}
}
