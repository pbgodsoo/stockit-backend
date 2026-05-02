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
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final CategoryRepository categoryRepository;
    private final InfrastructureRepository infrastructureRepository;

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
            agg.safetyStock += location.getLocationType() == LocationType.WAREHOUSE
                    ? n(master.getWarehouseSafetyStock())
                    : n(master.getStoreSafetyStock());
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
            if (agg.locationType == null) agg.locationType = location.getLocationType();
            if (agg.updatedAt == null || inv.getUpdatedAt().after(agg.updatedAt)) {
                agg.updatedAt = inv.getUpdatedAt();
            }
        }

        return skuMap.values().stream()
                .filter(agg -> agg.actualStock > 0 || agg.availableStock > 0)
                .map(agg -> {
                    int safetyStock = agg.locationType == LocationType.WAREHOUSE ? warehouseSafety : storeSafety;
                    return InventoryDto.CompanyWideSkuDetailRes.builder()
                            .skuCode(agg.sku.getSkuCode())
                            .color(agg.sku.getColor())
                            .size(agg.sku.getSize())
                            .actualStock(agg.actualStock)
                            .availableStock(agg.availableStock)
                            .safetyStock(safetyStock)
                            .status(toUiStatus(agg.availableStock, safetyStock))
                            .updatedAt(agg.updatedAt)
                            .build();
                })
                .sorted(Comparator.comparing(InventoryDto.CompanyWideSkuDetailRes::getSkuCode))
                .toList();
    }

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

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private String toUiStatus(int available, int safety) {
        if (available <= 0) return "품절";
        if (available < safety) return "부족";
        return "정상";
    }

    private static class Aggregate {
        private final ProductMaster master;
        private final String parentCategory;
        private final String childCategory;
        private int actualStock = 0;
        private int availableStock = 0;
        private int safetyStock = 0;
        private Date updatedAt;
        private final Set<InventoryStatus> statuses = new HashSet<>();

        private Aggregate(ProductMaster master, String parentCategory, String childCategory) {
            this.master = master;
            this.parentCategory = parentCategory;
            this.childCategory = childCategory;
        }
    }

    private static class SkuAggregate {
        private final ProductSku sku;
        private int actualStock = 0;
        private int availableStock = 0;
        private Date updatedAt;
        private LocationType locationType;

        private SkuAggregate(ProductSku sku) {
            this.sku = sku;
        }
    }
}
