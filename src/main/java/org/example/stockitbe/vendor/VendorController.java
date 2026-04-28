package org.example.stockitbe.vendor;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.vendor.model.VendorDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vendor read-only Controller — 등록/수정/삭제는 SQL 로 직접 처리.
 */
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService service;

    @GetMapping
    public BaseResponse<List<VendorDto.ListRes>> list() {
        return BaseResponse.success(service.findAll());
    }

    @GetMapping("/{code}")
    public BaseResponse<VendorDto.ListRes> detail(@PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }
}
