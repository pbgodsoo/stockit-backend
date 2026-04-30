package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderCatalogDto;
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
    public BaseResponse<PurchaseOrderCatalogDto.CatalogRes> getCatalog(
            @RequestParam(required = false) String vendorCode,
            @RequestParam(required = false) Long warehouseId) {
        return BaseResponse.success(service.getCatalog(vendorCode, warehouseId));
    }
}
