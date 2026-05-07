package org.example.stockitbe.store.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.store.order.model.dto.StoreOrderBatchDto;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hq/store-orders/batch-approve")
@RequiredArgsConstructor
public class StoreOrderBatchApproveController {

    private final StoreOrderBatchApproveService batchApproveService;

    // 발주 승인 배치 처리
    @PostMapping("/run")
    public BaseResponse<StoreOrderBatchDto.RunRes> run(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody(required = false) StoreOrderBatchDto.RunReq req
    ) {
        return BaseResponse.success(batchApproveService.runManual(req, me));
    }

    // 승인 대기 발주건이 있는 매장 조회
    @GetMapping("/pending-stores")
    public BaseResponse<List<StoreOrderBatchDto.PendingStoreRes>> pendingStores(
            @AuthenticationPrincipal AuthUserDetails me
    ) {
        return BaseResponse.success(batchApproveService.listPendingStores());
    }
}
