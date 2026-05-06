package org.example.stockitbe.store.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.store.inventory.model.StoreInventoryDto;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store/inventories")
public class StoreInventoryController {

    private final StoreInventoryService service;

    @GetMapping
    public BaseResponse<List<StoreInventoryDto.ItemRes>> getStoreInventories(
            @AuthenticationPrincipal AuthUserDetails me
    ) {
        return BaseResponse.success(service.getItems(me.getLocationCode()));
    }

    @GetMapping("/{itemCode}/skus")
    public BaseResponse<List<StoreInventoryDto.SkuRes>> getStoreInventorySkus(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String itemCode
    ) {
        return BaseResponse.success(service.getItemSkus(me.getLocationCode(), itemCode));
    }
}
