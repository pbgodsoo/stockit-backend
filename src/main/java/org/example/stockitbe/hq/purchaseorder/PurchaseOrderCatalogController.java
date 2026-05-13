package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderCatalogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/purchase-orders/catalog")
@RequiredArgsConstructor
public class PurchaseOrderCatalogController {

    private final PurchaseOrderCatalogService service;

    @GetMapping
    public BaseResponse<Page<PurchaseOrderCatalogDto.SkuRowRes>> getCatalog(
            @RequestParam(required = false) String vendorCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String color,
            @RequestParam(name = "skuSize", required = false) String skuSize,
            @RequestParam(required = false, defaultValue = "false") boolean shortageOnly,
            @RequestParam(required = false) Long warehouseId,
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return BaseResponse.success(service.getCatalog(
                vendorCode, keyword, color, skuSize, shortageOnly, warehouseId, pageable));
    }

    @GetMapping("/facets")
    public BaseResponse<PurchaseOrderCatalogDto.FacetsRes> getCatalogFacets(
            @RequestParam(required = false) String vendorCode,
            @RequestParam(required = false) String keyword) {
        return BaseResponse.success(service.getFacets(vendorCode, keyword));
    }
}
