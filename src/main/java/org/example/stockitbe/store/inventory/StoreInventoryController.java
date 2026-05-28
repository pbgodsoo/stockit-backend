package org.example.stockitbe.store.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.store.inventory.model.StoreInventoryDto;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매장 재고", description = "로그인 매장 기준 품목·SKU 단위 재고 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store/inventories")
// 매장 재고 조회 컨트롤러
// 로그인 사용자의 매장 기준 품목/SKU 재고 조회 API를 제공한다.
public class StoreInventoryController {

    private final StoreInventoryService service;

    @Operation(summary = "매장 재고 품목 목록 조회", description = "로그인 매장의 재고를 품목(상품코드) 단위로 집계해 페이지 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    // 매장 재고 품목 페이지 조회 API
    // category 단일 파라미터: 부모 또는 자식 한글 이름 (FE 한 줄 dropdown 호환).
    @GetMapping
    public BaseResponse<StoreInventoryDto.ItemPageRes> getStoreInventories(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "카테고리 이름 (메인 카테고리 또는 서브 카테고리 한글명)", example = "상의") @RequestParam(required = false) String category,
            @Parameter(description = "재고 상태 (정상 / 부족 / 품절)", example = "정상") @RequestParam(required = false) String status,
            @Parameter(description = "상품명·상품코드 검색 키워드", example = "코튼") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(service.getItems(
                me.getLocationCode(), category, status, keyword, pageable
        ));
    }

    @Operation(
            summary = "매장 재고 SKU 단위 페이지 조회",
            description = "마스터 무관 모든 SKU 를 한 표로 조회. 색상·사이즈 칩 필터 + 한국어 라벨 상태 필터(정상/부족/품절) 지원."
    )
    // 매장 재고 SKU 단위 페이지 조회 API (모드 토글 SKU 모드 — 마스터 무관 모든 SKU 한 표).
    // status: 한국어 라벨("정상"/"부족"/"품절") — SQL HAVING 으로 필터.
    // category: 부모 또는 자식 한글 이름 단일 파라미터.
    // skuSize: SKU 사이즈 (M/L/XL 등) — Pageable 의 size 와 이름 충돌 방지 위해 query param 명 분리.
    @GetMapping("/skus")
    public BaseResponse<StoreInventoryDto.SkuPageRes> getSkus(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "카테고리 이름 (부모 또는 자식 한글명)", example = "상의") @RequestParam(required = false) String category,
            @Parameter(description = "재고 상태 (정상 / 부족 / 품절)", example = "정상") @RequestParam(required = false) String status,
            @Parameter(description = "색상 필터", example = "블랙") @RequestParam(required = false) String color,
            @Parameter(description = "사이즈 필터 (Pageable size 파라미터와 충돌 방지를 위해 skuSize 사용)", example = "M") @RequestParam(value = "skuSize", required = false) String skuSize,
            @Parameter(description = "상품명·SKU 코드 검색 키워드", example = "코튼") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(service.findSkus(
                me.getLocationCode(), category, status, color, skuSize, keyword, pageable
        ));
    }

    @Operation(
            summary = "매장 재고 SKU facets 조회",
            description = "현재 카테고리·키워드 조건 안에서 선택 가능한 색상/사이즈 distinct 값 반환. 칩 필터 UI 용."
    )
    // 매장 재고 SKU 칩 필터용 facets API — 거점/카테고리/검색 조건 안의 가능한 색상/사이즈 distinct.
    @GetMapping("/skus/facets")
    public BaseResponse<StoreInventoryDto.SkuFacetsRes> getSkuFacets(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "카테고리 이름 (부모 또는 자식 한글명)", example = "상의") @RequestParam(required = false) String category,
            @Parameter(description = "상품명·SKU 코드 검색 키워드", example = "코튼") @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(service.findSkuFacets(
                me.getLocationCode(), category, keyword
        ));
    }

}
