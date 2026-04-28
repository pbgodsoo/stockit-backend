package org.example.stockitbe.vendor;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.vendor.model.Vendor;
import org.example.stockitbe.vendor.model.VendorDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Vendor read-only Service.
 * 등록/수정/삭제는 SQL 로 직접 — 컨트롤러도 조회 전용.
 */
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    @Transactional(readOnly = true)
    public List<VendorDto.ListRes> findAll() {
        return vendorRepository.findAll().stream()
                .map(VendorDto.ListRes::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorDto.ListRes findByCode(String code) {
        Vendor vendor = vendorRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));
        return VendorDto.ListRes.from(vendor);
    }
}
