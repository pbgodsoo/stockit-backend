package org.example.stockitbe.hq.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/hq/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/company-wide")
    public BaseResponse<InventoryDto.CompanyWidePageRes> getCompanyWide(
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) List<Long> locationIds,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(inventoryService.findCompanyWide(locationType, locationIds, parentCategory, childCategory, status, keyword));
    }

    @GetMapping("/company-wide/{itemCode}/skus")
    public BaseResponse<List<InventoryDto.CompanyWideSkuDetailRes>> getCompanyWideSkus(
            @PathVariable String itemCode,
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) List<Long> locationIds,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(inventoryService.findCompanyWideSkuDetails(itemCode, locationType, locationIds, parentCategory, childCategory, status, keyword));
    }

    @PostMapping("/circular-candidates/refresh")
    public BaseResponse<InventoryDto.CircularCandidateRefreshRes> refreshCircularCandidates() {
        return BaseResponse.success(inventoryService.refreshCircularCandidates());
    }

    @GetMapping("/circular-candidates")
    public BaseResponse<List<InventoryDto.CircularCandidateRes>> getCircularCandidates() {
        return BaseResponse.success(inventoryService.findCircularCandidates());
    }

    @GetMapping("/circular")
    public BaseResponse<List<InventoryDto.CircularInventoryRes>> getCircularInventories() {
        return BaseResponse.success(inventoryService.findCircularInventories());
    }

    @PostMapping("/circular-candidates/convert")
    public BaseResponse<InventoryDto.CircularCandidateConvertRes> convertCircularCandidates(
            @RequestBody @Valid List<InventoryDto.CircularCandidateConvertItemReq> requests
    ) {
        return BaseResponse.success(inventoryService.convertCircularCandidates(requests));
    }
}
