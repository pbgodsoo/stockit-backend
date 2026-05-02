package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.product.model.ProductStatus;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderCatalogDto;
import org.example.stockitbe.hq.vendor.VendorProductRepository;
import org.example.stockitbe.hq.vendor.VendorRepository;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorProduct;
import org.example.stockitbe.hq.vendor.model.VendorProductStatus;
import org.example.stockitbe.hq.vendor.model.VendorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 새 발주 페이지 카탈로그 read-only 집계 서비스.
 *
 * vendor_product → ProductMaster → ProductSku 를 묶어서 한 번에 응답.
 * FE 가 마스터 N건마다 SKU 추가 fetch 하는 N+1 회피 목적.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderCatalogService {

    private final VendorProductRepository vendorProductRepository;
    private final VendorRepository vendorRepository;
    private final ProductMasterRepository productMasterRepository;
    private final ProductSkuRepository productSkuRepository;

    /**
     * vendorCode 미지정 → 모든 ACTIVE 거래처의 ACTIVE 계약 펼침.
     * 지정 시 그 거래처만.
     *
     * warehouseId 는 본 사이클에선 사용 X (인벤토리 합류 후 stock 필터링용 placeholder).
     *
     * 응답 룰:
     *  - vendor.status != ACTIVE 거래처는 제외
     *  - vendor_product.status != ACTIVE 계약은 제외
     *  - product_master.status != ACTIVE 마스터는 제외
     *  - product_sku.status != ACTIVE SKU 는 제외
     *  - SKU 0건 마스터는 결과에서 제외 (발주 불가가 자연)
     */
    public PurchaseOrderCatalogDto.CatalogRes getCatalog(String vendorCode, Long warehouseId) {
        // 1) VendorProduct 후보 조회 (ACTIVE 만)
        List<VendorProduct> vendorProducts = loadActiveVendorProducts(vendorCode);
        if (vendorProducts.isEmpty()) {
            return PurchaseOrderCatalogDto.CatalogRes.builder()
                    .masters(List.of())
                    .optionFacets(List.of())
                    .build();
        }

        // 2) Vendor 일괄 조회 + ACTIVE 만 통과
        Set<Long> vendorIds = vendorProducts.stream()
                .map(VendorProduct::getVendorId)
                .collect(Collectors.toSet());
        Map<Long, Vendor> vendorMap = vendorRepository.findAllById(vendorIds).stream()
                .filter(v -> v.getStatus() == VendorStatus.ACTIVE)
                .collect(Collectors.toMap(Vendor::getId, v -> v));

        // 3) ProductMaster 일괄 조회 + ACTIVE 만 통과 (productCode key)
        Set<String> productCodes = vendorProducts.stream()
                .map(VendorProduct::getProductCode)
                .collect(Collectors.toSet());
        Map<String, ProductMaster> productMap = productMasterRepository.findAllByCodeIn(productCodes).stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .collect(Collectors.toMap(ProductMaster::getCode, p -> p));

        // 4) ProductSku 일괄 조회 + ACTIVE 만 통과 (productCode 별 group)
        Map<String, List<ProductSku>> skusByProductCode = productSkuRepository
                .findAllByProductCodeInOrderByIdAsc(productCodes).stream()
                .filter(s -> s.getStatus() == ProductStatus.ACTIVE)
                .collect(Collectors.groupingBy(ProductSku::getProductCode));

        // 5) MasterRes 빌드 (SKU 0건 마스터 제외)
        List<PurchaseOrderCatalogDto.MasterRes> masters = new ArrayList<>();
        for (VendorProduct vp : vendorProducts) {
            Vendor vendor = vendorMap.get(vp.getVendorId());
            if (vendor == null) continue; // vendor 비ACTIVE
            ProductMaster master = productMap.get(vp.getProductCode());
            if (master == null) continue; // master 비ACTIVE 또는 없음
            List<ProductSku> skus = skusByProductCode.getOrDefault(vp.getProductCode(), List.of());
            if (skus.isEmpty()) continue; // SKU 0건 마스터 제외

            List<PurchaseOrderCatalogDto.SkuRes> skuResList = skus.stream()
                    .map(s -> PurchaseOrderCatalogDto.SkuRes.builder()
                            .skuCode(s.getSkuCode())
                            .color(s.getColor())
                            .size(s.getSize())
                            .displayOption(s.getColor() + "/" + s.getSize())
                            .unitPrice(s.getUnitPrice())
                            .build())
                    .toList();
            long minPrice = skus.stream().mapToLong(ProductSku::getUnitPrice).min().orElse(0L);
            long maxPrice = skus.stream().mapToLong(ProductSku::getUnitPrice).max().orElse(0L);

            masters.add(PurchaseOrderCatalogDto.MasterRes.builder()
                    .vendorCode(vendor.getCode())
                    .vendorName(vendor.getName())
                    .vendorProductCode(vp.getCode())
                    .productCode(master.getCode())
                    .productName(master.getName())
                    .contractUnitPrice(vp.getUnitPrice())
                    .minSkuUnitPrice(minPrice)
                    .maxSkuUnitPrice(maxPrice)
                    .skus(skuResList)
                    .build());
        }

        // 6) optionFacets 빌드 — color/size 분리
        Map<String, Set<String>> facetMap = new LinkedHashMap<>();
        for (PurchaseOrderCatalogDto.MasterRes m : masters) {
            for (PurchaseOrderCatalogDto.SkuRes s : m.getSkus()) {
                if (s.getColor() != null && !s.getColor().isBlank()) {
                    facetMap.computeIfAbsent("색상", k -> new TreeSet<>()).add(s.getColor().trim());
                }
                if (s.getSize() != null && !s.getSize().isBlank()) {
                    facetMap.computeIfAbsent("사이즈", k -> new TreeSet<>()).add(s.getSize().trim());
                }
            }
        }
        List<PurchaseOrderCatalogDto.FacetRes> facets = facetMap.entrySet().stream()
                .map(e -> PurchaseOrderCatalogDto.FacetRes.builder()
                        .name(e.getKey())
                        .values(new ArrayList<>(e.getValue()))
                        .build())
                .sorted(Comparator.comparing(PurchaseOrderCatalogDto.FacetRes::getName))
                .toList();

        return PurchaseOrderCatalogDto.CatalogRes.builder()
                .masters(masters)
                .optionFacets(facets)
                .build();
    }

    private List<VendorProduct> loadActiveVendorProducts(String vendorCode) {
        if (vendorCode != null && !vendorCode.isBlank()) {
            Vendor vendor = vendorRepository.findByCode(vendorCode)
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));
            return vendorProductRepository
                    .findAllByVendorIdAndStatusNotOrderByIdDesc(vendor.getId(), VendorProductStatus.DELETED).stream()
                    .filter(vp -> vp.getStatus() == VendorProductStatus.ACTIVE)
                    .toList();
        }
        return vendorProductRepository.findAllByStatusOrderByIdDesc(VendorProductStatus.ACTIVE);
    }
}
