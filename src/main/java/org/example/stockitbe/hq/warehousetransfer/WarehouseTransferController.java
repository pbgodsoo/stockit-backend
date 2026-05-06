package org.example.stockitbe.hq.warehousetransfer;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hq/warehouse-transfers")
@RequiredArgsConstructor
public class WarehouseTransferController {

    private final InventoryService inventoryService;

    @GetMapping("/imbalanced-skus")
    public BaseResponse<List<InventoryDto.ImbalancedSkuRes>> getImbalancedSkus() {
        return BaseResponse.success(inventoryService.findImbalancedSkus());
    }
}
