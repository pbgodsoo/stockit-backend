package org.example.stockitbe.hq.vendor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.vendor.model.VendorProductDto;
import org.example.stockitbe.hq.vendor.model.VendorProductStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "본사 - 공급처 제품(VendorProduct)", description = "공급처-제품 매핑 CRUD API. 공급처 계약 단위 등록·수정·상태 전환·삭제")
@RestController
@RequestMapping("/api/vendor-products")
@RequiredArgsConstructor
public class VendorProductController {

    private final VendorProductService service;

    @Operation(
            summary = "공급처 제품 목록 조회",
            description = "vendorCode 가 있으면 해당 공급처의 제품 매핑만, 없으면 전체. status 필터로 활성/비활성 매핑 분리 조회 가능."
    )
    @GetMapping
    public BaseResponse<List<VendorProductDto.ListRes>> list(
            @Parameter(description = "공급처 코드 필터 — 지정 시 해당 공급처 매핑만") @RequestParam(required = false) String vendorCode,
            @Parameter(description = "매핑 상태 필터 (ACTIVE/INACTIVE)") @RequestParam(required = false) VendorProductStatus status) {
        if (vendorCode != null && !vendorCode.isBlank()) {
            return BaseResponse.success(service.findByVendor(vendorCode));
        }
        return BaseResponse.success(service.findAll(status));
    }

    @Operation(summary = "공급처 제품 단건 상세 조회", description = "vendor-product 코드로 단건 상세 반환.")
    @GetMapping("/{code}")
    public BaseResponse<VendorProductDto.DetailRes> detail(
            @Parameter(description = "공급처 제품 코드") @PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @Operation(summary = "공급처 제품 등록", description = "공급처 ↔ 품목 매핑 1건 생성. 단가·MOQ·리드타임 등 계약 조건 포함.")
    @PostMapping
    public BaseResponse<VendorProductDto.DetailRes> create(@Valid @RequestBody VendorProductDto.CreateReq req) {
        return BaseResponse.success(service.create(req));
    }

    @Operation(summary = "공급처 제품 수정", description = "매핑 코드로 계약 조건(단가·MOQ·리드타임 등) 수정.")
    @PatchMapping("/{code}")
    public BaseResponse<VendorProductDto.DetailRes> update(
            @Parameter(description = "공급처 제품 코드") @PathVariable String code,
            @Valid @RequestBody VendorProductDto.UpdateReq req) {
        return BaseResponse.success(service.update(code, req));
    }

    @Operation(summary = "공급처 제품 상태 전환", description = "매핑의 활성/비활성 상태(ACTIVE/INACTIVE) 토글.")
    @PatchMapping("/{code}/status")
    public BaseResponse<VendorProductDto.DetailRes> updateStatus(
            @Parameter(description = "공급처 제품 코드") @PathVariable String code,
            @Valid @RequestBody VendorProductDto.StatusUpdateReq req) {
        return BaseResponse.success(service.updateStatus(code, req));
    }

    @Operation(summary = "공급처 제품 삭제", description = "매핑 1건 삭제. 발주 이력이 있으면 INACTIVE 전환 권장.")
    @DeleteMapping("/{code}")
    public BaseResponse<Void> delete(
            @Parameter(description = "공급처 제품 코드") @PathVariable String code) {
        service.delete(code);
        return BaseResponse.success(null);
    }
}
