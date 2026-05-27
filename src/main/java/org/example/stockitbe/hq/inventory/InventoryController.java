package org.example.stockitbe.hq.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/inventories")
@RequiredArgsConstructor
@Tag(name = "순환재고", description = "순환재고 후보 발굴, 후보 전환, 순환재고 목록 조회 API")
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
    @Operation(
            summary = "순환재고 후보 리프레시",
            description = "전사 재고를 스캔해 순환재고 후보 조건을 다시 계산하고 후보 상태를 갱신한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "후보 리프레시 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/circular-candidates/refresh")
    public BaseResponse<InventoryDto.CircularCandidateRefreshRes> refreshCircularCandidates() {
        return BaseResponse.success(inventoryService.refreshCircularCandidates());
    }

    // 순환재고 후보 목록 조회 API
    @Operation(
            summary = "순환재고 후보 목록 조회",
            description = "순환재고로 전환 가능한 후보 재고를 페이지 단위로 조회한다. 정렬 허용값: skuCode, availableStock, convertibleStock, updatedAt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "후보 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/circular-candidates")
    public BaseResponse<InventoryDto.CircularCandidatePageRes> getCircularCandidates(
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "정렬 조건. 허용 필드: skuCode, availableStock, convertibleStock, updatedAt", example = "convertibleStock,desc")
            @RequestParam(defaultValue = "convertibleStock,desc") String sort,
            @Parameter(description = "SKU 코드, 품목명 등 검색어", example = "자켓")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "상위 카테고리명", example = "상의")
            @RequestParam(required = false) String parentCategory,
            @Parameter(description = "하위 카테고리명", example = "자켓")
            @RequestParam(required = false) String childCategory,
            @Parameter(description = "창고 코드 목록", example = "WH-001")
            @RequestParam(required = false) List<String> warehouseCodes,
            @Parameter(description = "후보 조건 코드 목록. 전달한 조건 코드를 모두 만족하는 후보만 조회한다.", example = "1")
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
    @Operation(
            summary = "순환재고 목록 조회",
            description = "순환재고 상태인 재고를 소재, 창고, 검색어 조건으로 조회한다. 정렬 허용값: skuCode, quantity, materialKgPrice, circularSalePrice, weight."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "순환재고 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/circular")
    public BaseResponse<InventoryDto.CircularInventoryPageRes> getCircularInventories(
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "정렬 조건. 허용 필드: skuCode, quantity, materialKgPrice, circularSalePrice, weight", example = "skuCode,asc")
            @RequestParam(defaultValue = "skuCode,asc") String sort,
            @Parameter(description = "SKU 코드, 품목명 등 검색어", example = "폴리에스터")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "창고 코드 목록", example = "WH-001")
            @RequestParam(required = false) List<String> warehouseCodes,
            @Parameter(description = "소재 그룹", example = "synthetic")
            @RequestParam(required = false) String materialGroup,
            @Parameter(description = "소재명", example = "폴리에스터")
            @RequestParam(required = false) String materialName,
            @Parameter(description = "최소 소재 구성 비율", example = "50")
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
    @Operation(
            summary = "순환재고 후보 전환",
            description = "요청한 후보 재고 수량을 순환재고 상태로 전환한다. 항목별 성공/실패 결과를 함께 반환한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "후보 전환 처리 완료"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/circular-candidates/convert")
    public BaseResponse<InventoryDto.CircularCandidateConvertRes> convertCircularCandidates(
            @RequestBody @Valid List<InventoryDto.CircularCandidateConvertItemReq> requests
    ) {
        return BaseResponse.success(inventoryService.convertCircularCandidates(requests));
    }
}
