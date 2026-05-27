package org.example.stockitbe.store.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.InventoryRepository;
import org.example.stockitbe.store.inventory.model.StoreInventoryDto;
import org.example.stockitbe.store.inventory.model.StoreItemRow;
import org.example.stockitbe.store.inventory.model.StoreSkuRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreInventoryService {

    private final InfrastructureRepository infrastructureRepository;
    private final InventoryRepository inventoryRepository;

    // 매장 재고 품목 페이지 목록 조회
    // 품목 단위로 실재고/가용재고/안전재고를 native @Query GROUP BY 로 집계 + LIMIT/OFFSET.
    @Transactional(readOnly = true)
    public StoreInventoryDto.ItemPageRes getItems(String locationCode,
                                                   String category,
                                                   String status,
                                                   String keyword,
                                                   Pageable pageable) {
        Infrastructure store = resolveStore(locationCode);
        String safeCategory = blankToNull(category);
        String safeStatus = blankToNull(status);
        String safeKeyword = blankToNull(keyword);

        // sort 무시(native @Query 안에 ORDER BY 박혀 있음) — page/size 만 사용
        Pageable safePageable = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.max(pageable.getPageSize(), 1)
        );

        Page<StoreItemRow> rows = inventoryRepository.findStoreAggregated(
                store.getId(), safeCategory, safeStatus, safeKeyword, safePageable
        );

        Page<StoreInventoryDto.ItemRes> mapped = rows.map(row -> StoreInventoryDto.ItemRes.from(
                row.getItemCode(),
                row.getParentCategory(),
                row.getChildCategory(),
                row.getItemName(),
                n(row.getActualStock()),
                n(row.getAvailableStock()),
                n(row.getSafetyStock()),
                row.getStatus()
        ));

        return StoreInventoryDto.ItemPageRes.from(mapped);
    }

    // 매장 재고 SKU 단위 페이지 조회 (모드 토글 SKU 모드 — 마스터 무관 모든 SKU 한 표).
    // GROUP BY ps.id 로 SKU 단위 합산, status HAVING 으로 필터.
    @Transactional(readOnly = true)
    public StoreInventoryDto.SkuPageRes findSkus(String locationCode,
                                                  String category,
                                                  String status,
                                                  String color,
                                                  String skuSize,
                                                  String keyword,
                                                  Pageable pageable) {
        Infrastructure store = resolveStore(locationCode);
        Pageable safePageable = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.max(pageable.getPageSize(), 1)
        );

        Page<StoreSkuRow> rows = inventoryRepository.findStoreSkus(
                store.getId(),
                blankToNull(category),
                blankToNull(status),
                blankToNull(color),
                blankToNull(skuSize),
                blankToNull(keyword),
                safePageable
        );

        Page<StoreInventoryDto.SkuRowRes> mapped = rows.map(row -> StoreInventoryDto.SkuRowRes.from(
                row.getSkuCode(),
                row.getItemCode(),
                row.getItemName(),
                row.getParentCategory(),
                row.getChildCategory(),
                row.getColor(),
                row.getSize(),
                l(row.getUnitPrice()),
                n(row.getActualStock()),
                n(row.getAvailableStock()),
                n(row.getSafetyStock()),
                n(row.getInboundExpectedQuantity()),
                row.getStatus()
        ));

        return StoreInventoryDto.SkuPageRes.from(mapped);
    }

    // 매장 재고 SKU 칩 필터 facets — 같은 거점/카테고리/검색 조건 안의 가능한 색상/사이즈 distinct.
    @Transactional(readOnly = true)
    public StoreInventoryDto.SkuFacetsRes findSkuFacets(String locationCode,
                                                         String category,
                                                         String keyword) {
        Infrastructure store = resolveStore(locationCode);
        String safeCategory = blankToNull(category);
        String safeKeyword = blankToNull(keyword);
        List<String> colors = inventoryRepository.findStoreSkuColors(store.getId(), safeCategory, safeKeyword);
        List<String> sizes = inventoryRepository.findStoreSkuSizes(store.getId(), safeCategory, safeKeyword);
        return new StoreInventoryDto.SkuFacetsRes(colors, sizes);
    }

    // -------- 내부 메서드 --------

    // locationCode로 매장 인프라를 조회한다.
    private Infrastructure resolveStore(String locationCode) {
        return infrastructureRepository.findByCodeAndLocationType(locationCode, LocationType.STORE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_SALE_STORE_NOT_FOUND));
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private long l(Long value) {
        return value == null ? 0L : value;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

}
