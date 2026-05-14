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
    // category 단일 파라미터: 부모 또는 자식 한글 이름 (FE 한 줄 dropdown 호환). 기존 parent/child 와 공존.
    @GetMapping
    public BaseResponse<WarehouseInventoryDto.ItemPageRes> getWarehouseInventories(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) String parentCategory,
            @RequestParam(required = false) String childCategory,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(service.getItems(
                me.getLocationCode(), parentCategory, childCategory, category, status, keyword, pageable
        ));
    }

    // 창고 재고 SKU 단위 페이지 조회 API (모드 토글 SKU 모드 — 마스터 무관 모든 SKU 한 표).
    // status: 한국어 라벨("정상"/"부족"/"품절") — SQL HAVING 으로 필터.
    // category: 부모 또는 자식 한글 이름 단일 파라미터.
    // skuSize: SKU 사이즈 (M/L/XL 등) — Pageable 의 size 와 이름 충돌 방지 위해 query param 명 분리.
    @GetMapping("/skus")
    public BaseResponse<WarehouseInventoryDto.SkuPageRes> getSkus(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String color,
            @RequestParam(value = "skuSize", required = false) String skuSize,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return BaseResponse.success(service.findSkus(
                me.getLocationCode(), category, status, color, skuSize, keyword, pageable
        ));
    }

    // 창고 재고 SKU 칩 필터용 facets API — 거점/카테고리/검색 조건 안의 가능한 색상/사이즈 distinct.
    @GetMapping("/skus/facets")
    public BaseResponse<WarehouseInventoryDto.SkuFacetsRes> getSkuFacets(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(service.findSkuFacets(
                me.getLocationCode(), category, keyword
        ));
    }

    // 창고 재고 SKU 목록 조회 API (옛 라우트 — FE 라우트 폐기 후 cleanup 예정)
    // 지정 품목(itemCode) 내 SKU 단위 재고를 반환한다.
    @GetMapping("/{itemCode}/skus")
    public BaseResponse<List<WarehouseInventoryDto.SkuRes>> getWarehouseInventorySkus(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String itemCode
    ) {
        return BaseResponse.success(service.getItemSkus(me.getLocationCode(), itemCode));
    }
}
