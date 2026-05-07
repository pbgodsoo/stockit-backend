package org.example.stockitbe.warehouse.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.inventory.model.WarehouseInventoryDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse/inventories")
public class WarehouseInventoryController {

    private final WarehouseInventoryService service;

    @GetMapping
    public BaseResponse<List<WarehouseInventoryDto.ItemRes>> getWarehouseInventories(
            @AuthenticationPrincipal AuthUserDetails me
    ) {
        return BaseResponse.success(service.getItems(me.getLocationCode()));
    }

    @GetMapping("/{itemCode}/skus")
    public BaseResponse<List<WarehouseInventoryDto.SkuRes>> getWarehouseInventorySkus(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String itemCode
    ) {
        return BaseResponse.success(service.getItemSkus(me.getLocationCode(), itemCode));
    }
}
