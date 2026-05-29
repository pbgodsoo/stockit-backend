package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SYS-001 강제 트리거 — 시연·QA·장애 대응용.
 *
 * 시간 조건(wait-minutes)을 무시하고, 호출 1회당 각 발주를 한 단계씩 다음으로 전환한다
 * (REQUESTED→APPROVED→READY_TO_SHIP→IN_TRANSIT→ARRIVED). 버튼을 누를 때마다 한 단계 진행되며,
 * ARRIVED 이후(→COMPLETED)는 창고 [입고 확정] 매뉴얼 트리거(WHS-007)로만 처리된다.
 * 운영 환경에서는 {@link PurchaseOrderAutoTransitionScheduler} 가 주기적으로 자동 호출.
 */
@RestController
@RequestMapping("/api/hq/purchase-orders/batch")
@RequiredArgsConstructor
public class PurchaseOrderBatchController {

    private final PurchaseOrderBatchService batchService;

    @PostMapping("/run")
    public BaseResponse<PurchaseOrderBatchService.BatchResult> run() {
        return BaseResponse.success(batchService.run(true));
    }
}
