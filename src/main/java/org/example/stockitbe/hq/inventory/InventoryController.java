package org.example.stockitbe.hq.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Tag(name = "본사 - 전사 재고 조회", description = "본사 관리자 전사 재고 조회 API (CEN-001) — Phase 2 ES + CQRS 적용")
    @Operation(
            summary = "전사 재고 품목 단위 페이지 조회",
            description = "거점 타입·거점 ID 리스트·카테고리·재고 상태·검색 키워드로 필터링한 품목 단위 재고 목록을 페이지로 반환. ES `inventory-master` 인덱스 질의."
    )
    @GetMapping("/company-wide")
    public BaseResponse<InventoryDto.CompanyWidePageRes> getCompanyWide(
            @Parameter(description = "거점 타입 (STORE/WAREHOUSE). 미지정 시 전체") @RequestParam(required = false) LocationType locationType,
            @Parameter(description = "거점 ID 리스트 (locationType 안 추가 필터)") @RequestParam(required = false) List<Long> locationIds,
            @Parameter(description = "상위 카테고리 한글 이름") @RequestParam(required = false) String parentCategory,
            @Parameter(description = "하위 카테고리 한글 이름") @RequestParam(required = false) String childCategory,
            @Parameter(description = "단일 카테고리 파라미터 — 부모 또는 자식 한글 이름. FE 단일 dropdown 호환용") @RequestParam(required = false) String category,
            @Parameter(description = "재고 상태 (NORMAL/CIRCULAR_CANDIDATE/CIRCULAR)") @RequestParam(required = false) InventoryStatus status,
            @Parameter(description = "검색 키워드 (품목명·품목 코드 nori + edge n-gram)") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(inventoryQueryService.findCompanyWide(locationType, locationIds, parentCategory, childCategory, category, status, keyword, pageable));
    }

    // 전사 재고 SKU 상세 조회 API (마스터 itemCode 한정 — 옛 라우트 호환, FE 라우트 폐기 후 cleanup 예정)
    @Tag(name = "본사 - 전사 재고 조회")
    @Operation(
            summary = "전사 재고 — 특정 품목 산하 SKU 상세 조회",
            description = "지정한 품목 코드 하위 SKU 들의 거점별 재고 상세 리스트. 옛 라우트 호환용 — FE 라우트 폐기 후 cleanup 예정."
    )
    @GetMapping("/company-wide/{itemCode}/skus")
    public BaseResponse<List<InventoryDto.CompanyWideSkuDetailRes>> getCompanyWideSkus(
            @Parameter(description = "품목 코드 (PRD-{L1}-{L2}-{seq})") @PathVariable String itemCode,
            @Parameter(description = "거점 타입 필터") @RequestParam(required = false) LocationType locationType,
            @Parameter(description = "거점 ID 리스트") @RequestParam(required = false) List<Long> locationIds,
            @Parameter(description = "상위 카테고리") @RequestParam(required = false) String parentCategory,
            @Parameter(description = "하위 카테고리") @RequestParam(required = false) String childCategory,
            @Parameter(description = "재고 상태") @RequestParam(required = false) InventoryStatus status,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(inventoryQueryService.findCompanyWideSkuDetails(itemCode, locationType, locationIds, parentCategory, childCategory, status, keyword));
    }

    // 전사 재고 SKU 단위 페이지 조회 API (모드 토글 SKU 모드 — 마스터 무관 모든 SKU 한 표).
    // status: 한국어 라벨("정상"/"부족"/"품절") — service 측 페이지 후처리 필터.
    // category: 부모 또는 자식 한글 이름 단일 파라미터.
    // skuSize: SKU 사이즈 (M/L/XL 등) — Pageable 의 size 와 이름 충돌 방지 위해 query param 명 분리.
    @Tag(name = "본사 - 전사 재고 조회")
    @Operation(
            summary = "전사 재고 SKU 단위 페이지 조회",
            description = "마스터 무관 모든 SKU 를 한 표로 페이지 조회. ES `inventory-sku` 인덱스 질의. 색상·사이즈 칩 필터 + 한국어 라벨 상태 필터(정상/부족/품절) 지원."
    )
    @GetMapping("/company-wide/skus")
    public BaseResponse<InventoryDto.CompanyWideSkuPageRes> getCompanyWideSkusPaged(
            @Parameter(description = "거점 타입 (STORE/WAREHOUSE)") @RequestParam(required = false) LocationType locationType,
            @Parameter(description = "거점 ID 리스트") @RequestParam(required = false) List<Long> locationIds,
            @Parameter(description = "상위 카테고리") @RequestParam(required = false) String parentCategory,
            @Parameter(description = "하위 카테고리") @RequestParam(required = false) String childCategory,
            @Parameter(description = "재고 상태 한국어 라벨 (정상/부족/품절)") @RequestParam(required = false) String status,
            @Parameter(description = "색상 필터 (BLK/WHT/NVY 등)") @RequestParam(required = false) String color,
            @Parameter(description = "사이즈 필터 (S/M/L/XL). Pageable.size 와 충돌 방지를 위해 skuSize 명명") @RequestParam(value = "skuSize", required = false) String skuSize,
            @Parameter(description = "검색 키워드 (품목명·SKU 코드 nori + edge n-gram)") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(inventoryQueryService.findCompanyWideSkus(
                locationType, locationIds, parentCategory, childCategory, status, color, skuSize, keyword, pageable
        ));
    }

    // 전사 재고 SKU 칩 필터용 facets API — 거점/카테고리/검색 조건 안의 가능한 색상/사이즈 distinct.
    @Tag(name = "본사 - 전사 재고 조회")
    @Operation(
            summary = "전사 재고 SKU facets 조회",
            description = "현재 거점·카테고리·키워드 조건 안에서 선택 가능한 색상/사이즈 distinct 값 반환. 칩 필터 UI 용."
    )
    @GetMapping("/company-wide/skus/facets")
    public BaseResponse<InventoryDto.CompanyWideSkuFacetsRes> getCompanyWideSkuFacets(
            @Parameter(description = "거점 타입") @RequestParam(required = false) LocationType locationType,
            @Parameter(description = "거점 ID 리스트") @RequestParam(required = false) List<Long> locationIds,
            @Parameter(description = "상위 카테고리") @RequestParam(required = false) String parentCategory,
            @Parameter(description = "하위 카테고리") @RequestParam(required = false) String childCategory,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword
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
