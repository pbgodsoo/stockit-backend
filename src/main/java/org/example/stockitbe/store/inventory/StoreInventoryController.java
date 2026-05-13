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
// 매장 재고 조회 컨트롤러
// 로그인 사용자의 매장 기준 품목/SKU 재고 조회 API를 제공한다.
public class StoreInventoryController {

    private final StoreInventoryService service;

    // 매장 재고 품목 목록 조회 API
    @GetMapping
    public BaseResponse<List<StoreInventoryDto.ItemRes>> getStoreInventories(
            @AuthenticationPrincipal AuthUserDetails me
    ) {
        return BaseResponse.success(service.getItems(me.getLocationCode()));
    }

    // 매장 재고 SKU 목록 조회 API
    // 지정 품목(itemCode) 내 SKU 단위 재고를 반환한다.
    @GetMapping("/{itemCode}/skus")
    public BaseResponse<List<StoreInventoryDto.SkuRes>> getStoreInventorySkus(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String itemCode
    ) {
        return BaseResponse.success(service.getItemSkus(me.getLocationCode(), itemCode));
    }
}
