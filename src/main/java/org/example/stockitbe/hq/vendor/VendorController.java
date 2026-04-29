package org.example.stockitbe.hq.vendor;

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
 * 거래처 계약 표(E 안)는 vendorProductService.findContractRows 로 mainVendorCode 매칭 ProductMaster + VendorProduct join.
 */
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService service;
    private final VendorProductService vendorProductService;

    @GetMapping
    public BaseResponse<List<VendorDto.ListRes>> list() {
        return BaseResponse.success(service.findAll());
    }

    @GetMapping("/{code}")
    public BaseResponse<VendorDto.ListRes> detail(@PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    /**
     * 거래처 계약 표 — ProductMaster(mainVendorCode 매칭) 행 + VendorProduct 매칭 join.
     * VendorProduct 가 없으면 contracted=false (미정) 행으로 반환.
     */
    @GetMapping("/{code}/contracts")
    public BaseResponse<List<VendorProductDto.ContractRowRes>> contracts(@PathVariable String code) {
        return BaseResponse.success(vendorProductService.findContractRows(code));
    }
}
