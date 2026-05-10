package org.example.stockitbe.store.order;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.store.order.model.dto.StoreOrderDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreOrderBatchApproveItemService {

    private final StoreOrderService storeOrderService;

    /**
     * Approve one order in an isolated transaction so failures do not mark the outer batch flow rollback-only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StoreOrderDto.ApproveRes approveOne(String orderNo, String actorId, String actorName, String reason) {
        return storeOrderService.approveByBatch(orderNo, actorId, actorName, reason);
    }
}

