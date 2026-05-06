package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SYS-001 강제 트리거 — 시연·QA·장애 대응용.
 *
 * 시간 조건(wait-minutes)을 무시하고 모든 PENDING/APPROVED 발주를 즉시 다음 단계로 전환한다.
 * 운영 환경에서는 {@link PurchaseOrderAutoTransitionScheduler} 가 5분마다 자동 호출.
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
