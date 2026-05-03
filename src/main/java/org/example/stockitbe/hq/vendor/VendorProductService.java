package org.example.stockitbe.hq.vendor;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
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
    private final ProductMasterRepository productMasterRepository;

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
     * 전체 공급처의 제품 일괄 조회 (CEN-035 발주 작성 페이지 카탈로그용).
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

        // ProductMaster 정합 검증 — 마스터에 없는 productCode 차단
        ProductMaster product = productMasterRepository.findByCode(req.getProductCode())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));

        // 한 제품 = 한 공급처 검증 — ProductMaster.mainVendorCode 와 요청 vendor 일치해야 함
        if (!vendor.getCode().equals(product.getMainVendorCode())) {
            throw BaseException.from(BaseResponseStatus.VENDOR_PRODUCT_VENDOR_MISMATCH);
        }

        boolean duplicate = vendorProductRepository
                .existsByVendorIdAndProductCodeAndStatusNot(vendor.getId(), req.getProductCode(), VendorProductStatus.DELETED);
        if (duplicate) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_VENDOR_PRODUCT_CODE);
        }

        String code = generateCode(vendor);
        VendorProduct entity = req.toEntity(vendor, code, product.getName());
        VendorProduct saved = vendorProductRepository.save(entity);
        return VendorProductDto.DetailRes.from(saved, vendor);
    }

    @Transactional
    public VendorProductDto.DetailRes update(String code, VendorProductDto.UpdateReq req) {
        VendorProduct vp = lookupVendorProduct(code);
        vp.updateContract(
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

    /**
     * 공급처 계약 표 (E 안 — UX 친화 inline edit).
     * mainVendorCode 매칭 ProductMaster 전체를 행으로 보여주고, 매칭되는 VendorProduct 가 있으면 계약 디테일 채움.
     * VendorProduct 0건이면 모두 contracted=false ("미정") 상태로 반환.
     */
    @Transactional(readOnly = true)
    public List<VendorProductDto.ContractRowRes> findContractRows(String vendorCode) {
        Vendor vendor = lookupVendor(vendorCode);

        // 이 공급처를 메인으로 둔 ProductMaster (in-memory 필터 — ProductMasterRepository 메소드 추가 회피, 팀원 코드 보호)
        List<ProductMaster> products = productMasterRepository.findAllByOrderByIdDesc().stream()
                .filter(p -> vendor.getCode().equals(p.getMainVendorCode()))
                .toList();
        if (products.isEmpty()) {
            return List.of();
        }

        // 해당 공급처의 active VendorProduct (DELETED 제외)
        List<VendorProduct> vps = vendorProductRepository
                .findAllByVendorIdAndStatusNotOrderByIdDesc(vendor.getId(), VendorProductStatus.DELETED);
        Map<String, VendorProduct> vpByProductCode = vps.stream()
                .collect(Collectors.toMap(VendorProduct::getProductCode, vp -> vp, (a, b) -> a));

        return products.stream()
                .map(pm -> VendorProductDto.ContractRowRes.from(pm, vpByProductCode.get(pm.getCode())))
                .toList();
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
