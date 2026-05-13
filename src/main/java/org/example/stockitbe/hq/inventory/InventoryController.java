package org.example.stockitbe.hq.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/hq/inventories")
@RequiredArgsConstructor
// 본사 관리자 재고 조회/순환재고 관리 컨트롤러
// 전사 재고 조회, 순환재고 후보 관리, 순환재고 조회 API를 제공한다.
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryQueryService inventoryQueryService;

    // 전사 재고(품목 단위) 목록 조회 API
    @GetMapping("/company-wide")
    public BaseResponse<InventoryDto.CompanyWidePageRes> getCompanyWide(
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) List<Long> locationIds,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(inventoryQueryService.findCompanyWide(locationType, locationIds, parentCategory, childCategory, status, keyword, pageable));
    }

    // 전사 재고 SKU 상세 조회 API
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
        return BaseResponse.success(inventoryQueryService.findCompanyWideSkuDetails(itemCode, locationType, locationIds, parentCategory, childCategory, status, keyword));
    }

    // 순환재고 후보 리프레시 API
    @PostMapping("/circular-candidates/refresh")
    public BaseResponse<InventoryDto.CircularCandidateRefreshRes> refreshCircularCandidates() {
        return BaseResponse.success(inventoryService.refreshCircularCandidates());
    }

    // 순환재고 후보 목록 조회 API
    @GetMapping("/circular-candidates")
    public BaseResponse<InventoryDto.CircularCandidatePageRes> getCircularCandidates(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "convertibleStock,desc") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) List<String> warehouseCodes,
            @RequestParam(required = false) List<Integer> conditionCodes
    ) {
        return BaseResponse.success(inventoryService.findCircularCandidates(
                page,
                size,
                sort,
                keyword,
                parentCategory,
                childCategory,
                warehouseCodes,
                conditionCodes
        ));
    }

    // 순환재고 목록 조회 API
    @GetMapping("/circular")
    public BaseResponse<InventoryDto.CircularInventoryPageRes> getCircularInventories(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "skuCode,asc") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> warehouseCodes,
            @RequestParam(required = false) String materialGroup,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) Integer minRatio
    ) {
        return BaseResponse.success(inventoryService.findCircularInventories(
                page,
                size,
                sort,
                keyword,
                warehouseCodes,
                materialGroup,
                materialName,
                minRatio
        ));
    }

    // 순환재고 후보 전환 API
    // 요청한 후보 수량을 순환재고 상태로 전환한다.
    @PostMapping("/circular-candidates/convert")
    public BaseResponse<InventoryDto.CircularCandidateConvertRes> convertCircularCandidates(
            @RequestBody @Valid List<InventoryDto.CircularCandidateConvertItemReq> requests
    ) {
        return BaseResponse.success(inventoryService.convertCircularCandidates(requests));
    }
}
