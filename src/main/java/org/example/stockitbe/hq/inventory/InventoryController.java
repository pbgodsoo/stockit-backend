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
    // category 단일 파라미터: 부모 또는 자식 한글 이름 (FE 한 줄 dropdown 호환). 기존 parent/child 와 공존.
    @GetMapping("/company-wide")
    public BaseResponse<InventoryDto.CompanyWidePageRes> getCompanyWide(
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) List<Long> locationIds,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(inventoryQueryService.findCompanyWide(locationType, locationIds, parentCategory, childCategory, category, status, keyword, pageable));
    }

    // 전사 재고 SKU 상세 조회 API (마스터 itemCode 한정 — 옛 라우트 호환, FE 라우트 폐기 후 cleanup 예정)
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

    // 전사 재고 SKU 단위 페이지 조회 API (모드 토글 SKU 모드 — 마스터 무관 모든 SKU 한 표).
    // status: 한국어 라벨("정상"/"부족"/"품절") — service 측 페이지 후처리 필터.
    // category: 부모 또는 자식 한글 이름 단일 파라미터.
    // skuSize: SKU 사이즈 (M/L/XL 등) — Pageable 의 size 와 이름 충돌 방지 위해 query param 명 분리.
    @GetMapping("/company-wide/skus")
    public BaseResponse<InventoryDto.CompanyWideSkuPageRes> getCompanyWideSkusPaged(
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) List<Long> locationIds,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String color,
            @RequestParam(value = "skuSize", required = false) String skuSize,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(inventoryQueryService.findCompanyWideSkus(
                locationType, locationIds, parentCategory, childCategory, status, color, skuSize, keyword, pageable
        ));
    }

    // 전사 재고 SKU 칩 필터용 facets API — 거점/카테고리/검색 조건 안의 가능한 색상/사이즈 distinct.
    @GetMapping("/company-wide/skus/facets")
    public BaseResponse<InventoryDto.CompanyWideSkuFacetsRes> getCompanyWideSkuFacets(
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) List<Long> locationIds,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(inventoryQueryService.findCompanyWideSkuFacets(
                locationType, locationIds, parentCategory, childCategory, keyword
        ));
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
