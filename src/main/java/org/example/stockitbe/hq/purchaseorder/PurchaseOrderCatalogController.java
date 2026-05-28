package org.example.stockitbe.hq.purchaseorder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "공급처 발주 카탈로그", description = "본사 발주 등록 화면용 SKU 카탈로그 조회 API (ADR-027 페이지네이션)")
@RestController
@RequestMapping("/api/hq/purchase-orders/catalog")
@RequiredArgsConstructor
public class PurchaseOrderCatalogController {

    private final PurchaseOrderCatalogService service;

    @Operation(
            summary = "발주 카탈로그 SKU 목록 조회",
            description = "거래처·키워드·색상·사이즈·창고 필터를 적용해 발주 가능 SKU 를 페이지 단위로 반환한다. shortageOnly=true 시 안전재고 미달 SKU 만 노출."
    )
    @GetMapping
    public BaseResponse<Page<PurchaseOrderCatalogDto.SkuRowRes>> getCatalog(
            @Parameter(description = "거래처 코드 필터", example = "VND-001") @RequestParam(required = false) String vendorCode,
            @Parameter(description = "검색 키워드 (품목명·SKU 코드)", example = "코튼") @RequestParam(required = false) String keyword,
            @Parameter(description = "색상 필터 (BLK/WHT/NVY 등)", example = "BLK") @RequestParam(required = false) String color,
            @Parameter(description = "사이즈 필터 (S/M/L/XL). Pageable.size 와 충돌 방지를 위해 skuSize 로 명명", example = "M") @RequestParam(name = "skuSize", required = false) String skuSize,
            @Parameter(description = "안전재고 미달 SKU 만 노출 (true 시)", example = "false") @RequestParam(required = false, defaultValue = "false") boolean shortageOnly,
            @Parameter(description = "창고 ID 필터", example = "21") @RequestParam(required = false) Long warehouseId,
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return BaseResponse.success(service.getCatalog(
                vendorCode, keyword, color, skuSize, shortageOnly, warehouseId, pageable));
    }

    @Operation(
            summary = "발주 카탈로그 facets 조회",
            description = "현재 거래처·키워드 조건 안에서 선택 가능한 색상/사이즈 distinct 값을 반환. 칩 필터 UI 용."
    )
    @GetMapping("/facets")
    public BaseResponse<PurchaseOrderCatalogDto.FacetsRes> getCatalogFacets(
            @Parameter(description = "거래처 코드 필터", example = "VND-001") @RequestParam(required = false) String vendorCode,
            @Parameter(description = "검색 키워드 (품목명·SKU 코드)", example = "코튼") @RequestParam(required = false) String keyword) {
        return BaseResponse.success(service.getFacets(vendorCode, keyword));
    }
}
