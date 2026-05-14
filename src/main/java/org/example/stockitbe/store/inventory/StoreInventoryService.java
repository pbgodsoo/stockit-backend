package org.example.stockitbe.store.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.InventoryRepository;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryStatusPolicy;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.store.inventory.model.StoreInventoryDto;
import org.example.stockitbe.store.inventory.model.StoreItemRow;
import org.example.stockitbe.store.inventory.model.StoreSkuRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreInventoryService {

    private final InfrastructureRepository infrastructureRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final CategoryRepository categoryRepository;

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
                n(row.getActualStock()),
                n(row.getAvailableStock()),
                n(row.getSafetyStock()),
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

    // 매장 재고 SKU 목록 조회 (옛 /{itemCode}/skus 라우트 호환용 — FE 라우트 폐기 PR 머지 후 cleanup 예정)
    // 선택 품목(itemCode) 내 SKU 단위 재고를 조회한다.
    @Transactional(readOnly = true)
    public List<StoreInventoryDto.SkuRes> getItemSkus(String locationCode, String itemCode) {
        Infrastructure store = resolveStore(locationCode);
        List<Inventory> inventories = inventoryRepository.findAllByLocationId(store.getId());
        if (inventories.isEmpty()) {
            return List.of();
        }

        Context context = buildContext(inventories);

        Map<String, SkuAccumulator> grouped = new HashMap<>();
        for (Inventory inventory : inventories) {
            if (!InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inventory.getInventoryStatus())) continue;
            StoreInventoryDto.SkuRes row = toSkuRes(inventory, itemCode, context);
            if (row == null) continue;
            SkuAccumulator acc = grouped.computeIfAbsent(row.getSkuCode(), ignored -> new SkuAccumulator(row.getSkuCode(), row.getColor(), row.getSize(), row.getSafetyStock(), row.getUpdatedAt()));
            acc.actualStock += row.getActualStock();
            acc.availableStock += row.getAvailableStock();
            if (acc.updatedAt == null || (row.getUpdatedAt() != null && row.getUpdatedAt().after(acc.updatedAt))) {
                acc.updatedAt = row.getUpdatedAt();
            }
        }

        return grouped.values().stream()
                .map(acc -> StoreInventoryDto.SkuRes.from(
                        acc.skuCode,
                        acc.color,
                        acc.size,
                        acc.actualStock,
                        acc.availableStock,
                        acc.safetyStock,
                        resolveStatus(acc.actualStock, acc.safetyStock),
                        acc.updatedAt
                ))
                .sorted(Comparator
                        .comparing(StoreInventoryDto.SkuRes::getColor, Comparator.nullsLast(String::compareTo))
                        .thenComparing(StoreInventoryDto.SkuRes::getSize, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    // 단일 재고 행을 SKU 응답 DTO로 변환한다.
    // itemCode와 일치하지 않으면 null을 반환한다.
    private StoreInventoryDto.SkuRes toSkuRes(Inventory inventory, String itemCode, Context context) {
        ProductSku sku = context.skuById.get(inventory.getSkuId());
        if (sku == null) return null;

        ProductMaster master = context.masterByCode.get(sku.getProductCode());
        if (master == null) return null;

        if (!Objects.equals(master.getCode(), itemCode)) {
            return null;
        }

        int actual = n(inventory.getQuantity());
        int safety = n(master.getStoreSafetyStock());
        return StoreInventoryDto.SkuRes.from(
                sku.getSkuCode(),
                sku.getColor(),
                sku.getSize(),
                actual,
                n(inventory.getAvailableQuantity()),
                safety,
                resolveStatus(actual, safety),
                inventory.getUpdatedAt()
        );
    }

    // -------- 내부 메서드 --------

    // 조회에 필요한 SKU/상품/카테고리 참조 맵을 구성한다.
    private Context buildContext(List<Inventory> inventories) {
        Set<Long> skuIds = inventories.stream().map(Inventory::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuById = productSkuRepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, sku -> sku));

        Set<String> productCodes = skuById.values().stream().map(ProductSku::getProductCode).collect(Collectors.toSet());
        Map<String, ProductMaster> masterByCode = productMasterRepository.findAllByCodeIn(productCodes).stream()
                .collect(Collectors.toMap(ProductMaster::getCode, master -> master));

        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        Map<Long, Category> categoryById = categories.stream().collect(Collectors.toMap(Category::getId, c -> c));
        Map<String, Category> categoryByCode = categories.stream().collect(Collectors.toMap(Category::getCode, c -> c));

        return new Context(skuById, masterByCode, categoryById, categoryByCode);
    }

    // locationCode로 매장 인프라를 조회한다.
    private Infrastructure resolveStore(String locationCode) {
        return infrastructureRepository.findByCodeAndLocationType(locationCode, LocationType.STORE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_SALE_STORE_NOT_FOUND));
    }

    // 실재고와 안전재고 기준으로 품절/부족/정상 상태를 계산한다.
    private String resolveStatus(int actual, int safety) {
        if (actual == 0) return "품절";
        if (actual <= safety) return "부족";
        return "정상";
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private record Context(
            Map<Long, ProductSku> skuById,
            Map<String, ProductMaster> masterByCode,
            Map<Long, Category> categoryById,
            Map<String, Category> categoryByCode
    ) {
    }

    private static class SkuAccumulator {
        private final String skuCode;
        private final String color;
        private final String size;
        private final int safetyStock;
        private int actualStock = 0;
        private int availableStock = 0;
        private Date updatedAt;

        SkuAccumulator(String skuCode, String color, String size, int safetyStock, Date updatedAt) {
            this.skuCode = skuCode;
            this.color = color;
            this.size = size;
            this.safetyStock = safetyStock;
            this.updatedAt = updatedAt;
        }
    }
}
