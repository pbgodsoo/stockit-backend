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
public class StoreInventoryController {

    private final StoreInventoryService service;

    @Operation(summary = "매장 재고 품목 목록 조회", description = "로그인 매장의 재고를 품목(상품코드) 단위로 집계해 페이지 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public BaseResponse<StoreInventoryDto.ItemPageRes> getStoreInventories(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "카테고리 이름 (메인 카테고리 또는 서브 카테고리 한글명)", example = "상의") @RequestParam(required = false) String category,
            @Parameter(description = "재고 상태 (정상 / 부족 / 품절)") @RequestParam(required = false) String status,
            @Parameter(description = "상품명·상품코드 검색 키워드") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(service.getItems(
                me.getLocationCode(), category, status, keyword, pageable
        ));
    }

    @Operation(summary = "매장 재고 SKU 목록 조회 (SKU 모드)", description = "마스터 무관 모든 SKU를 한 표로 조회한다. status는 HAVING 필터로 동작한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/skus")
    public BaseResponse<StoreInventoryDto.SkuPageRes> getSkus(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "카테고리 이름 (부모 또는 자식 한글명)", example = "상의") @RequestParam(required = false) String category,
            @Parameter(description = "재고 상태 (정상 / 부족 / 품절)") @RequestParam(required = false) String status,
            @Parameter(description = "색상 필터", example = "블랙") @RequestParam(required = false) String color,
            @Parameter(description = "사이즈 필터 (Pageable size 파라미터와 충돌 방지를 위해 skuSize 사용)", example = "M") @RequestParam(value = "skuSize", required = false) String skuSize,
            @Parameter(description = "상품명·SKU 코드 검색 키워드") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(service.findSkus(
                me.getLocationCode(), category, status, color, skuSize, keyword, pageable
        ));
    }

    @Operation(summary = "SKU 필터 Facets 조회", description = "현재 조건(매장·카테고리·키워드) 안에서 선택 가능한 색상·사이즈 목록을 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/skus/facets")
    public BaseResponse<StoreInventoryDto.SkuFacetsRes> getSkuFacets(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "카테고리 이름 (부모 또는 자식 한글명)") @RequestParam(required = false) String category,
            @Parameter(description = "상품명·SKU 코드 검색 키워드") @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(service.findSkuFacets(
                me.getLocationCode(), category, keyword
        ));
    }

}
