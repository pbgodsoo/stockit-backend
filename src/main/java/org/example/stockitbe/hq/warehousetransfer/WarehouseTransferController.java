package org.example.stockitbe.hq.warehousetransfer;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferDto;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hq/warehouse-transfers")
@RequiredArgsConstructor
public class WarehouseTransferController {

    private final InventoryService inventoryService;
    private final WarehouseTransferService warehouseTransferService;

    @GetMapping("/imbalanced-skus")
    public BaseResponse<List<InventoryDto.ImbalancedSkuRes>> getImbalancedSkus() {
        return BaseResponse.success(inventoryService.findImbalancedSkus());
    }

    @PostMapping("/execute")
    public BaseResponse<WarehouseTransferDto.ExecuteRes> execute(
            @RequestBody @Valid WarehouseTransferDto.ExecuteReq request
    ) {
        return BaseResponse.success(warehouseTransferService.execute(request));
    }

    @GetMapping
    public BaseResponse<List<WarehouseTransferDto.TransferListItemRes>> getTransfers(
            @RequestParam(required = false) WarehouseTransferStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(warehouseTransferService.findTransfers(status, fromDate, toDate, keyword));
    }

    @GetMapping("/{transferNo}")
    public BaseResponse<WarehouseTransferDto.TransferDetailRes> getTransferDetail(@PathVariable String transferNo) {
        return BaseResponse.success(warehouseTransferService.findTransferDetail(transferNo));
    }

    @GetMapping("/sku-distribution")
    public BaseResponse<List<WarehouseTransferDto.WarehouseSkuDistributionRes>> getSkuDistribution(
            @RequestParam String skuCode
    ) {
        return BaseResponse.success(warehouseTransferService.findSkuDistribution(skuCode));
    }
}
