package org.example.stockitbe.hq.vendor;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorProduct;
import org.example.stockitbe.hq.vendor.model.VendorProductDto;
import org.example.stockitbe.hq.vendor.model.VendorProductStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorProductService {

    private final VendorRepository vendorRepository;
    private final VendorProductRepository vendorProductRepository;

    @Transactional(readOnly = true)
    public List<VendorProductDto.ListRes> findByVendor(String vendorCode) {
        Vendor vendor = lookupVendor(vendorCode);
        List<VendorProduct> list = vendorProductRepository
                .findAllByVendorIdAndStatusNotOrderByIdDesc(vendor.getId(), VendorProductStatus.DELETED);
        return list.stream()
                .map(vp -> VendorProductDto.ListRes.from(vp, vendor))
                .toList();
    }

    /**
     * 전체 거래처의 제품 일괄 조회 (CEN-035 발주 작성 페이지 카탈로그용).
     * statusFilter == null → DELETED 만 제외한 전체.
     * statusFilter != null → 정확히 그 status 만 (DELETED 도 호출자가 의도하면 그대로 통과).
     */
    @Transactional(readOnly = true)
    public List<VendorProductDto.ListRes> findAll(VendorProductStatus statusFilter) {
        List<VendorProduct> list = (statusFilter == null)
                ? vendorProductRepository.findAllByStatusNotOrderByIdDesc(VendorProductStatus.DELETED)
                : vendorProductRepository.findAllByStatusOrderByIdDesc(statusFilter);
        if (list.isEmpty()) {
            return List.of();
        }

        // N+1 방지 — vendorIds 일괄 lookup
        Set<Long> vendorIds = list.stream().map(VendorProduct::getVendorId).collect(Collectors.toSet());
        Map<Long, Vendor> vendorMap = vendorRepository.findAllById(vendorIds).stream()
                .collect(Collectors.toMap(Vendor::getId, v -> v));

        return list.stream()
                .map(vp -> {
                    Vendor vendor = vendorMap.get(vp.getVendorId());
                    if (vendor == null) {
                        throw BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND);
                    }
                    return VendorProductDto.ListRes.from(vp, vendor);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorProductDto.DetailRes findByCode(String code) {
        VendorProduct vp = lookupVendorProduct(code);
        Vendor vendor = vendorRepository.findById(vp.getVendorId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));
        return VendorProductDto.DetailRes.from(vp, vendor);
    }

    @Transactional
    public VendorProductDto.DetailRes create(VendorProductDto.CreateReq req) {
        Vendor vendor = lookupVendor(req.getVendorCode());

        boolean duplicate = vendorProductRepository
                .existsByVendorIdAndProductCodeAndStatusNot(vendor.getId(), req.getProductCode(), VendorProductStatus.DELETED);
        if (duplicate) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_VENDOR_PRODUCT_CODE);
        }

        String code = generateCode(vendor);
        VendorProduct entity = req.toEntity(vendor, code);
        VendorProduct saved = vendorProductRepository.save(entity);
        return VendorProductDto.DetailRes.from(saved, vendor);
    }

    @Transactional
    public VendorProductDto.DetailRes update(String code, VendorProductDto.UpdateReq req) {
        VendorProduct vp = lookupVendorProduct(code);
        vp.updateContract(
                req.getProductName(),
                req.getUnitPrice(),
                req.getMoq(),
                req.getLeadTimeDays(),
                req.getContractStart(),
                req.getContractEnd()
        );
        Vendor vendor = vendorRepository.findById(vp.getVendorId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));
        return VendorProductDto.DetailRes.from(vp, vendor);
    }

    @Transactional
    public VendorProductDto.DetailRes updateStatus(String code, VendorProductDto.StatusUpdateReq req) {
        VendorProduct vp = lookupVendorProduct(code);
        vp.changeStatus(req.getStatus());
        Vendor vendor = vendorRepository.findById(vp.getVendorId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));
        return VendorProductDto.DetailRes.from(vp, vendor);
    }

    @Transactional
    public void delete(String code) {
        VendorProduct vp = lookupVendorProduct(code);
        vp.softDelete();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Vendor lookupVendor(String vendorCode) {
        return vendorRepository.findByCode(vendorCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));
    }

    private VendorProduct lookupVendorProduct(String code) {
        return vendorProductRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_PRODUCT_NOT_FOUND));
    }

    /**
     * 비즈니스 코드 자동 생성 — VP-{vendorCode}-{seq:3자리}
     * seq 는 해당 vendor 의 product 총 개수 + 1 (DELETED 포함, 충돌 방지).
     */
    private String generateCode(Vendor vendor) {
        long seq = vendorProductRepository.countByVendorId(vendor.getId()) + 1;
        return String.format("VP-%s-%03d", vendor.getCode(), seq);
    }
}
