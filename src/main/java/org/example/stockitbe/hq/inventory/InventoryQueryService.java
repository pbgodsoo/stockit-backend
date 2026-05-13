package org.example.stockitbe.hq.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.inventory.model.InventoryStatusPolicy;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// 본사 재고 조회 전용 서비스
// 전사 재고 목록/상세 조회와 위치 옵션 조회 데이터를 조합한다.
public class InventoryQueryService {

    private final InventoryRepository inventoryRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final CategoryRepository categoryRepository;
    private final InfrastructureRepository infrastructureRepository;

    // 전사 재고(품목 단위) 목록을 조회한다.
    @Transactional(readOnly = true)
    public InventoryDto.CompanyWidePageRes findCompanyWide(LocationType locationType,
                                                            List<Long> locationIds,
                                                            String parentCategory,
                                                            String childCategory,
                                                            InventoryStatus status,
                                                            String keyword) {
        List<Inventory> inventories = inventoryRepository.findAll();
        List<ProductSku> skus = productSkuRepository.findAll();
        if (inventories.isEmpty() || skus.isEmpty()) {
            return InventoryDto.CompanyWidePageRes.builder()
                    .items(List.of())
                    .locationOptions(buildLocationOptions(locationType))
                    .build();
        }

        Map<Long, ProductSku> skuById = skus.stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Set<String> productCodes = skus.stream().map(ProductSku::getProductCode).collect(Collectors.toSet());
        Map<String, ProductMaster> productByCode = productMasterRepository.findAllByCodeIn(productCodes).stream()
                .collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));

        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        Map<String, Category> categoryByCode = categories.stream().collect(Collectors.toMap(Category::getCode, Function.identity()));
        Map<Long, Category> categoryById = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        Map<Long, Infrastructure> locationById = infrastructureRepository.findAll().stream()
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));

        String safeParent = parentCategory == null ? "" : parentCategory.trim();
        String safeChild = childCategory == null ? "" : childCategory.trim();
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Set<Long> locationIdSet = locationIds == null ? Set.of() : new HashSet<>(locationIds);

        Map<String, Aggregate> aggregateMap = new LinkedHashMap<>();

        for (Inventory inv : inventories) {
            if (!InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inv.getInventoryStatus())) continue;
            ProductSku sku = skuById.get(inv.getSkuId());
            if (sku == null) continue;
            ProductMaster master = productByCode.get(sku.getProductCode());
            if (master == null) continue;
            Infrastructure location = locationById.get(inv.getLocationId());
            if (location == null) continue;
            if (locationType != null && location.getLocationType() != locationType) continue;
            if (!locationIdSet.isEmpty() && !locationIdSet.contains(location.getId())) continue;
            if (status != null && inv.getInventoryStatus() != status) continue;

            Category child = categoryByCode.get(master.getCategoryCode());
            if (child == null) continue;
            Category parent = child.getParentId() == null ? child : categoryById.get(child.getParentId());

            String parentName = parent != null ? parent.getName() : "";
            String childName = child.getName();

            if (!safeParent.isBlank() && !safeParent.equals(parentName)) continue;
            if (!safeChild.isBlank() && !safeChild.equals(childName)) continue;

            if (!safeKeyword.isBlank()) {
                String searchable = String.join(" ",
                        master.getCode(),
                        master.getName(),
                        sku.getSkuCode(),
                        location.getCode(),
                        location.getName()
                ).toLowerCase(Locale.ROOT);
                if (!searchable.contains(safeKeyword)) continue;
            }

            Aggregate agg = aggregateMap.computeIfAbsent(master.getCode(), k -> new Aggregate(master, parentName, childName));
            agg.actualStock += n(inv.getQuantity());
            agg.availableStock += n(inv.getAvailableQuantity());
            String safetyKey = inv.getSkuId() + ":" + inv.getLocationId();
            if (agg.safetyKeys.add(safetyKey)) {
                agg.safetyStock += location.getLocationType() == LocationType.WAREHOUSE
                        ? n(master.getWarehouseSafetyStock())
                        : n(master.getStoreSafetyStock());
            }
            if (agg.updatedAt == null || inv.getUpdatedAt().after(agg.updatedAt)) {
                agg.updatedAt = inv.getUpdatedAt();
            }
            agg.statuses.add(inv.getInventoryStatus());
        }

        List<InventoryDto.CompanyWideRes> items = aggregateMap.values().stream()
                .map(agg -> InventoryDto.CompanyWideRes.builder()
                        .itemCode(agg.master.getCode())
                        .parentCategory(agg.parentCategory)
                        .childCategory(agg.childCategory)
                        .itemName(agg.master.getName())
                        .actualStock(agg.actualStock)
                        .availableStock(agg.availableStock)
                        .safetyStock(agg.safetyStock)
                        .status(toUiStatus(agg.availableStock, agg.safetyStock))
                        .updatedAt(agg.updatedAt)
                        .build())
                .sorted(Comparator.comparing(InventoryDto.CompanyWideRes::getItemCode))
                .toList();

        return InventoryDto.CompanyWidePageRes.builder()
                .items(items)
                .locationOptions(buildLocationOptions(locationType))
                .build();
    }

    // 전사 재고 SKU 상세를 조회한다.
    @Transactional(readOnly = true)
    public List<InventoryDto.CompanyWideSkuDetailRes> findCompanyWideSkuDetails(String itemCode,
                                                                                 LocationType locationType,
                                                                                 List<Long> locationIds,
                                                                                 String parentCategory,
                                                                                 String childCategory,
                                                                                 InventoryStatus status,
                                                                                 String keyword) {
        if (itemCode == null || itemCode.isBlank()) return List.of();

        List<ProductSku> productSkus = productSkuRepository.findByProductCodeOrderByIdDesc(itemCode.trim());
        if (productSkus.isEmpty()) return List.of();

        Set<Long> skuIds = productSkus.stream().map(ProductSku::getId).collect(Collectors.toSet());
        List<Inventory> inventories = inventoryRepository.findAllBySkuIdIn(skuIds);

        Map<Long, Infrastructure> locationById = infrastructureRepository.findAll().stream()
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
        Set<Long> locationIdSet = locationIds == null ? Set.of() : new HashSet<>(locationIds);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        ProductMaster master = productMasterRepository.findByCode(itemCode.trim()).orElse(null);
        int warehouseSafety = master == null ? 0 : n(master.getWarehouseSafetyStock());
        int storeSafety = master == null ? 0 : n(master.getStoreSafetyStock());

        Map<Long, SkuAggregate> skuMap = new LinkedHashMap<>();
        for (ProductSku sku : productSkus) {
            skuMap.put(sku.getId(), new SkuAggregate(sku));
        }

        for (Inventory inv : inventories) {
            if (!InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inv.getInventoryStatus())) continue;
            Infrastructure location = locationById.get(inv.getLocationId());
            if (location == null) continue;
            if (locationType != null && location.getLocationType() != locationType) continue;
            if (!locationIdSet.isEmpty() && !locationIdSet.contains(location.getId())) continue;
            if (status != null && inv.getInventoryStatus() != status) continue;

            if (!safeKeyword.isBlank()) {
                ProductSku sku = skuMap.get(inv.getSkuId()).sku;
                String searchable = String.join(" ", sku.getSkuCode(), sku.getColor(), sku.getSize(), location.getName())
                        .toLowerCase(Locale.ROOT);
                if (!searchable.contains(safeKeyword)) continue;
            }

            SkuAggregate agg = skuMap.get(inv.getSkuId());
            if (agg == null) continue;
            agg.actualStock += n(inv.getQuantity());
            agg.availableStock += n(inv.getAvailableQuantity());
            String safetyKey = inv.getSkuId() + ":" + inv.getLocationId();
            if (agg.safetyKeys.add(safetyKey)) {
                agg.safetyStock += location.getLocationType() == LocationType.WAREHOUSE
                        ? warehouseSafety
                        : storeSafety;
            }
            if (agg.updatedAt == null || inv.getUpdatedAt().after(agg.updatedAt)) {
                agg.updatedAt = inv.getUpdatedAt();
            }
        }

        return skuMap.values().stream()
                .filter(agg -> agg.actualStock > 0 || agg.availableStock > 0)
                .map(agg -> {
                    return InventoryDto.CompanyWideSkuDetailRes.builder()
                            .skuCode(agg.sku.getSkuCode())
                            .color(agg.sku.getColor())
                            .size(agg.sku.getSize())
                            .unitPrice(agg.sku.getUnitPrice())
                            .actualStock(agg.actualStock)
                            .availableStock(agg.availableStock)
                            .safetyStock(agg.safetyStock)
                            .status(toUiStatus(agg.availableStock, agg.safetyStock))
                            .updatedAt(agg.updatedAt)
                            .build();
                })
                .sorted(Comparator.comparing(InventoryDto.CompanyWideSkuDetailRes::getSkuCode))
                .toList();
    }

    // locationType 조건에 맞는 위치 옵션 목록을 생성한다.
    private List<InventoryDto.LocationOptionRes> buildLocationOptions(LocationType locationType) {
        return infrastructureRepository.findAll().stream()
                .filter(i -> locationType == null || i.getLocationType() == locationType)
                .sorted(Comparator.comparing(Infrastructure::getName))
                .map(i -> InventoryDto.LocationOptionRes.builder()
                        .id(i.getId())
                        .code(i.getCode())
                        .name(i.getName())
                        .build())
                .toList();
    }

    // null-safe 정수 변환
    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    // 가용재고/안전재고 기준 UI 상태 라벨 계산
    private String toUiStatus(int available, int safety) {
        if (available <= 0) return "품절";
        if (available < safety) return "부족";
        return "정상";
    }

    // 품목 단위 재고 집계 보조 객체
    private static class Aggregate {
        private final ProductMaster master;
        private final String parentCategory;
        private final String childCategory;
        private int actualStock = 0;
        private int availableStock = 0;
        private int safetyStock = 0;
        private Date updatedAt;
        private final Set<InventoryStatus> statuses = new HashSet<>();
        private final Set<String> safetyKeys = new HashSet<>();

        private Aggregate(ProductMaster master, String parentCategory, String childCategory) {
            this.master = master;
            this.parentCategory = parentCategory;
            this.childCategory = childCategory;
        }
    }

    // SKU 단위 재고 집계 보조 객체
    private static class SkuAggregate {
        private final ProductSku sku;
        private int actualStock = 0;
        private int availableStock = 0;
        private int safetyStock = 0;
        private Date updatedAt;
        private final Set<String> safetyKeys = new HashSet<>();

        private SkuAggregate(ProductSku sku) {
            this.sku = sku;
        }
    }
}
