package org.example.stockitbe.hq.purchaseorder;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderCatalogDto;
import org.example.stockitbe.hq.purchaseorder.model.SkuRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

// 진입점: ProductSku — SKU 평탄 row 가 page 단위라 진입점도 SKU.
//   vendor_product / vendor / product_master 는 JOIN 으로 결합 (자연 키 매핑이라 JPA 관계 매핑 X → 네이티브 SQL).
//   availableQty 는 서브쿼리 1회로 SQL 단계에서 SUM (페이지 단위 N+1 없음).
//   facet 별도 엔드포인트 — 페이지만 넘기는 흔한 케이스 SQL 절약.
//   정렬 화이트리스트로 정제 후 Pageable 재구성 (자유 sort 키 그대로 받으면 인덱스 부재 컬럼 정렬·잠재 공격면).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderCatalogService {

    private final PurchaseOrderCatalogRepository catalogRepository;

    private static final Set<String> ALLOWED_SORT_KEYS = Set.of(
            "vendorName", "productName", "unitPrice", "availableQty", "id"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "id");

    public Page<PurchaseOrderCatalogDto.SkuRowRes> getCatalog(
            String vendorCode, String keyword, String color, String size,
            boolean shortageOnly, Long warehouseId, Pageable pageable) {

        Pageable safePageable = sanitizePageable(pageable);
        Page<SkuRowProjection> page = catalogRepository.findCatalogPage(
                blankToNull(vendorCode),
                blankToNull(keyword),
                blankToNull(color),
                blankToNull(size),
                shortageOnly,
                warehouseId,
                safePageable);
        return page.map(PurchaseOrderCatalogDto.SkuRowRes::from);
    }

    public PurchaseOrderCatalogDto.FacetsRes getFacets(String vendorCode, String keyword) {
        String safeVendor = blankToNull(vendorCode);
        String safeKeyword = blankToNull(keyword);
        List<String> colors = catalogRepository.findDistinctColors(safeVendor, safeKeyword);
        List<String> sizes = catalogRepository.findDistinctSizes(safeVendor, safeKeyword);
        return PurchaseOrderCatalogDto.FacetsRes.builder()
                .colors(colors)
                .sizes(sizes)
                .build();
    }

    private Pageable sanitizePageable(Pageable pageable) {
        Sort sanitized = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            if (ALLOWED_SORT_KEYS.contains(order.getProperty())) {
                sanitized = sanitized.and(Sort.by(order.getDirection(), order.getProperty()));
            }
        }
        if (sanitized.isUnsorted()) {
            sanitized = DEFAULT_SORT;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sanitized);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
