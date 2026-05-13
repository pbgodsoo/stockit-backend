package org.example.stockitbe.warehouse.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.inventory.model.WarehouseInventoryDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse/inventories")
// 창고 재고 조회 컨트롤러
// 로그인 사용자의 창고 기준 품목/SKU 재고 조회 API를 제공한다.
public class WarehouseInventoryController {

    private final WarehouseInventoryService service;

    // 창고 재고 품목 목록 조회 API
    @GetMapping
    public BaseResponse<WarehouseInventoryDto.ItemPageRes> getWarehouseInventories(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(service.getItems(
                me.getLocationCode(), parentCategory, childCategory, status, keyword, pageable
        ));
    }

    // 창고 재고 SKU 목록 조회 API
    // 지정 품목(itemCode) 내 SKU 단위 재고를 반환한다.
    @GetMapping("/{itemCode}/skus")
    public BaseResponse<List<WarehouseInventoryDto.SkuRes>> getWarehouseInventorySkus(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String itemCode
    ) {
        return BaseResponse.success(service.getItemSkus(me.getLocationCode(), itemCode));
    }
}
