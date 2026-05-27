package org.example.stockitbe.hq.vendor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.vendor.model.VendorDto;
import org.example.stockitbe.hq.vendor.model.VendorProductDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vendor read-only Controller — 등록/수정/삭제는 SQL 로 직접 처리.
 * 공급처 계약 표(E 안)는 vendorProductService.findContractRows 로 mainVendorCode 매칭 ProductMaster + VendorProduct join.
 */
@Tag(name = "본사 - 공급처(Vendor)", description = "본사 공급처 조회 + 공급처 계약 표 조회 API. 등록/수정/삭제는 SQL 로 직접 처리")
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService service;
    private final VendorProductService vendorProductService;

    @Operation(summary = "공급처 목록 조회", description = "전체 공급처 리스트 반환.")
    @GetMapping
    public BaseResponse<List<VendorDto.ListRes>> list() {
        return BaseResponse.success(service.findAll());
    }

    @Operation(summary = "공급처 단건 상세 조회", description = "공급처 코드로 단건 상세 반환.")
    @GetMapping("/{code}")
    public BaseResponse<VendorDto.ListRes> detail(
            @Parameter(description = "공급처 코드") @PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    /**
     * 공급처 계약 표 — ProductMaster(mainVendorCode 매칭) 행 + VendorProduct 매칭 join.
     * VendorProduct 가 없으면 contracted=false (미정) 행으로 반환.
     */
    @Operation(
            summary = "공급처 계약 표 조회",
            description = "ProductMaster(mainVendorCode 매칭) 행 + VendorProduct 매칭 join. VendorProduct 가 없는 품목은 contracted=false (미정) 행으로 반환."
    )
    @GetMapping("/{code}/contracts")
    public BaseResponse<List<VendorProductDto.ContractRowRes>> contracts(
            @Parameter(description = "공급처 코드") @PathVariable String code) {
        return BaseResponse.success(vendorProductService.findContractRows(code));
    }
}
