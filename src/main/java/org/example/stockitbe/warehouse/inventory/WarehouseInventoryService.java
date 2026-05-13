package org.example.stockitbe.warehouse.inventory;

import lombok.Builder;
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
import org.example.stockitbe.warehouse.inventory.model.WarehouseInventoryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseInventoryService {
    private static final List<String> MAIN_CATEGORY_ORDER = List.of("상의", "바지", "치마", "아우터");

    private final InfrastructureRepository infrastructureRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<WarehouseInventoryDto.ItemRes> getItems(String locationCode) {
        Infrastructure warehouse = resolveWarehouse(locationCode);
        List<Inventory> inventories = inventoryRepository.findAllByLocationId(warehouse.getId());
        if (inventories.isEmpty()) {
            return List.of();
        }

        Context context = buildContext(inventories);
        Map<String, ItemAccumulator> grouped = new HashMap<>();

        for (Inventory inventory : inventories) {
            if (!InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inventory.getInventoryStatus())) continue;
            ProductSku sku = context.skuById.get(inventory.getSkuId());
            if (sku == null) continue;

            ProductMaster master = context.masterByCode.get(sku.getProductCode());
            if (master == null) continue;

            Category child = context.categoryByCode.get(master.getCategoryCode());
            if (child == null) continue;

            Category parent = child.getParentId() == null ? child : context.categoryById.get(child.getParentId());
            String parentCategory = parent == null ? child.getName() : parent.getName();
            String childCategory = child.getName();
            String itemCode = master.getCode();

            ItemAccumulator acc = grouped.computeIfAbsent(itemCode, key -> ItemAccumulator.builder()
                    .itemCode(itemCode)
                    .parentCategory(parentCategory)
                    .childCategory(childCategory)
                    .itemName(master.getName())
                    .actualStock(0)
                    .availableStock(0)
                    .safetyStock(0)
                    .updatedAt(inventory.getUpdatedAt())
                    .build());

            acc.actualStock += n(inventory.getQuantity());
            acc.availableStock += n(inventory.getAvailableQuantity());
            acc.skuIds.add(sku.getId());
            if (acc.updatedAt == null || (inventory.getUpdatedAt() != null && inventory.getUpdatedAt().after(acc.updatedAt))) {
                acc.updatedAt = inventory.getUpdatedAt();
            }
        }

        grouped.values().forEach(acc -> acc.safetyStock = acc.skuIds.size() * n(context.masterByCode.get(acc.itemCode).getWarehouseSafetyStock()));

        return grouped.values().stream()
                .map(acc -> WarehouseInventoryDto.ItemRes.from(
                        acc.itemCode,
                        acc.parentCategory,
                        acc.childCategory,
                        acc.itemName,
                        acc.actualStock,
                        acc.availableStock,
                        acc.safetyStock,
                        resolveStatus(acc.availableStock, acc.safetyStock),
                        acc.updatedAt
                ))
                .sorted(Comparator
                        .comparing(WarehouseInventoryDto.ItemRes::getParentCategory, this::compareMainCategory)
                        .thenComparing(WarehouseInventoryDto.ItemRes::getChildCategory, Comparator.nullsLast(String::compareTo))
                        .thenComparing(WarehouseInventoryDto.ItemRes::getItemName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseInventoryDto.SkuRes> getItemSkus(String locationCode, String itemCode) {
        Infrastructure warehouse = resolveWarehouse(locationCode);
        List<Inventory> inventories = inventoryRepository.findAllByLocationId(warehouse.getId());
        if (inventories.isEmpty()) {
            return List.of();
        }

        Context context = buildContext(inventories);

        Map<String, SkuAccumulator> grouped = new HashMap<>();
        for (Inventory inventory : inventories) {
            if (!InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inventory.getInventoryStatus())) continue;
            WarehouseInventoryDto.SkuRes row = toSkuRes(inventory, itemCode, context);
            if (row == null) continue;
            SkuAccumulator acc = grouped.computeIfAbsent(row.getSkuCode(), ignored -> new SkuAccumulator(row.getSkuCode(), row.getColor(), row.getSize(), row.getSafetyStock(), row.getUpdatedAt()));
            acc.actualStock += row.getActualStock();
            acc.availableStock += row.getAvailableStock();
            if (acc.updatedAt == null || (row.getUpdatedAt() != null && row.getUpdatedAt().after(acc.updatedAt))) {
                acc.updatedAt = row.getUpdatedAt();
            }
        }

        return grouped.values().stream()
                .map(acc -> WarehouseInventoryDto.SkuRes.from(
                        acc.skuCode,
                        acc.color,
                        acc.size,
                        acc.actualStock,
                        acc.availableStock,
                        acc.safetyStock,
                        resolveStatus(acc.availableStock, acc.safetyStock),
                        acc.updatedAt
                ))
                .sorted(Comparator
                        .comparing(WarehouseInventoryDto.SkuRes::getColor, Comparator.nullsLast(String::compareTo))
                        .thenComparing(WarehouseInventoryDto.SkuRes::getSize, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private WarehouseInventoryDto.SkuRes toSkuRes(Inventory inventory, String itemCode, Context context) {
        ProductSku sku = context.skuById.get(inventory.getSkuId());
        if (sku == null) return null;

        ProductMaster master = context.masterByCode.get(sku.getProductCode());
        if (master == null) return null;

        if (!Objects.equals(master.getCode(), itemCode)) {
            return null;
        }

        int available = n(inventory.getAvailableQuantity());
        int safety = n(master.getWarehouseSafetyStock());
        return WarehouseInventoryDto.SkuRes.from(
                sku.getSkuCode(),
                sku.getColor(),
                sku.getSize(),
                n(inventory.getQuantity()),
                available,
                safety,
                resolveStatus(available, safety),
                inventory.getUpdatedAt()
        );
    }

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

    private Infrastructure resolveWarehouse(String locationCode) {
        return infrastructureRepository.findByCodeAndLocationType(locationCode, LocationType.WAREHOUSE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND));
    }

    private String resolveStatus(int available, int safety) {
        if (available <= 0) return "품절";
        if (available < safety) return "부족";
        return "정상";
    }

    private int compareMainCategory(String a, String b) {
        int ai = MAIN_CATEGORY_ORDER.indexOf(a);
        int bi = MAIN_CATEGORY_ORDER.indexOf(b);
        ai = ai < 0 ? MAIN_CATEGORY_ORDER.size() : ai;
        bi = bi < 0 ? MAIN_CATEGORY_ORDER.size() : bi;
        if (ai != bi) return Integer.compare(ai, bi);
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private record Context(
            Map<Long, ProductSku> skuById,
            Map<String, ProductMaster> masterByCode,
            Map<Long, Category> categoryById,
            Map<String, Category> categoryByCode
    ) {
    }

    @Builder
    private static class ItemAccumulator {
        private String itemCode;
        private String parentCategory;
        private String childCategory;
        private String itemName;
        private int actualStock;
        private int availableStock;
        private int safetyStock;
        private Date updatedAt;
        @Builder.Default
        private Set<Long> skuIds = new HashSet<>();
    }

    private static class SkuAccumulator {
        private final String skuCode;
        private final String color;
        private final String size;
        private final int safetyStock;
        private int actualStock = 0;
        private int availableStock = 0;
        private Date updatedAt;

        private SkuAccumulator(String skuCode, String color, String size, int safetyStock, Date updatedAt) {
            this.skuCode = skuCode;
            this.color = color;
            this.size = size;
            this.safetyStock = safetyStock;
            this.updatedAt = updatedAt;
        }
    }
}
