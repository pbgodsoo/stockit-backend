package org.example.stockitbe.hq.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.CircularMaterialPricePolicy;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryCandidateCondition;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.inventory.model.InventoryStatusPolicy;
import org.example.stockitbe.hq.product.MaterialRepository;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaterialComposition;
import org.example.stockitbe.hq.product.model.Material;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductMaterialType;
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
    private final InventoryCandidateConditionRepository inventoryCandidateConditionRepository;
    private final CircularMaterialPricePolicyRepository circularMaterialPricePolicyRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final MaterialRepository materialRepository;
    private final CategoryRepository categoryRepository;
    private final InfrastructureRepository infrastructureRepository;

    private static final int CONDITION_LONG_NO_MOVEMENT = 1;
    private static final int CONDITION_LOW_PERFORMANCE = 2;
    private static final int CONDITION_SIZE_COLOR_BIAS = 3;
    private static final double SAFETY_STOCK_MULTIPLIER = 2.5d;
    private static final String LABEL_LONG_NO_MOVEMENT = "최근 24개월 이상 판매 이력이 없는 SKU";
    private static final String LABEL_LOW_PERFORMANCE = "안전재고 대비 초과 누적 SKU";
    private static final String LABEL_SIZE_COLOR_BIAS = "극단 사이즈 재고 또는 특정 컬러 재고에 편중된 SKU";
    private static final double SIZE_COLOR_BIAS_THRESHOLD = 0.60d;
    private static final String MAT_BLEND = "BLEND";
    private static final double DEFAULT_UNIT_WEIGHT_KG = 0.300d;
    private static final Map<String, Double> CATEGORY_UNIT_WEIGHT_KG = Map.ofEntries(
            Map.entry("반팔", 0.180d), Map.entry("긴팔", 0.220d), Map.entry("셔츠", 0.230d),
            Map.entry("니트", 0.350d), Map.entry("후드티", 0.600d), Map.entry("청바지", 0.700d),
            Map.entry("반바지", 0.250d), Map.entry("긴바지", 0.400d), Map.entry("츄리닝", 0.450d),
            Map.entry("미니스커트", 0.200d), Map.entry("롱스커트", 0.350d), Map.entry("패딩", 0.800d),
            Map.entry("후드집업", 0.455d), Map.entry("자켓", 0.490d), Map.entry("가디건", 0.300d)
    );
    private static final Map<String, String> MATERIAL_NAME_KO_MAP = Map.of(
            "COTTON", "면",
            "WOOL", "울",
            "CASHMERE", "캐시미어",
            "SILK", "실크",
            "LINEN", "린넨",
            "POLYESTER", "폴리에스터",
            "ACRYLIC", "아크릴",
            "POLYAMIDE", "나일론",
            "ELASTANE", "스판덱스",
            MAT_BLEND, "혼방"
    );

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

    @Transactional(readOnly = true)
    public List<InventoryDto.ImbalancedSkuRes> findImbalancedSkus() {
        List<Inventory> inventories = inventoryRepository.findAll();

        Map<Long, Infrastructure> warehouseById = infrastructureRepository.findAll().stream()
                .filter(infra -> infra.getLocationType() == LocationType.WAREHOUSE)
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
        if (warehouseById.isEmpty()) return List.of();

        Set<Long> skuIds = inventories.stream()
                .filter(inv -> warehouseById.containsKey(inv.getLocationId()))
                .filter(inv -> InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inv.getInventoryStatus()))
                .map(Inventory::getSkuId)
                .collect(Collectors.toSet());
        if (skuIds.isEmpty()) return List.of();

        Map<Long, ProductSku> skuById = productSkuRepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        if (skuById.isEmpty()) return List.of();

        Set<String> productCodes = skuById.values().stream()
                .map(ProductSku::getProductCode)
                .collect(Collectors.toSet());
        Map<String, ProductMaster> masterByCode = productMasterRepository.findAllByCodeIn(productCodes).stream()
                .collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));

        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        Map<String, Category> categoryByCode = categories.stream().collect(Collectors.toMap(Category::getCode, Function.identity()));
        Map<Long, Category> categoryById = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        Map<String, WarehouseSkuStock> stockByWarehouseSkuKey = new HashMap<>();
        for (Inventory inventory : inventories) {
            if (!warehouseById.containsKey(inventory.getLocationId())) continue;
            if (!InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inventory.getInventoryStatus())) continue;
            String key = inventory.getSkuId() + ":" + inventory.getLocationId();
            WarehouseSkuStock stock = stockByWarehouseSkuKey.computeIfAbsent(key, ignored -> new WarehouseSkuStock(inventory.getSkuId()));
            stock.totalOnHand += Math.max(0, n(inventory.getQuantity()));
            stock.totalAvailable += Math.max(0, n(inventory.getAvailableQuantity()));
        }

        Map<Long, ImbalancedSkuAggregate> aggregateBySkuId = new HashMap<>();
        for (ProductSku sku : skuById.values()) {
            ProductMaster master = masterByCode.get(sku.getProductCode());
            if (master == null) continue;

            ImbalancedSkuAggregate agg = new ImbalancedSkuAggregate(sku, master);
            int safetyStock = Math.max(0, n(master.getWarehouseSafetyStock()));

            for (Long warehouseId : warehouseById.keySet()) {
                String key = sku.getId() + ":" + warehouseId;
                WarehouseSkuStock stock = stockByWarehouseSkuKey.get(key);
                int onHand = stock == null ? 0 : stock.totalOnHand;
                int available = stock == null ? 0 : stock.totalAvailable;

                agg.totalOnHand += onHand;
                agg.totalAvailable += available;

                if (available < safetyStock) {
                    agg.shortageWarehouseCount += 1;
                    agg.totalShortageQty += (safetyStock - available);
                }
            }
            aggregateBySkuId.put(sku.getId(), agg);
        }

        return aggregateBySkuId.values().stream()
                .map(agg -> {
                    Category child = categoryByCode.get(agg.master.getCategoryCode());
                    Category parent = child == null || child.getParentId() == null
                            ? child
                            : categoryById.get(child.getParentId());
                    String categoryLabel = child == null
                            ? ""
                            : (parent == null ? child.getName() : parent.getName() + " > " + child.getName());

                    return InventoryDto.ImbalancedSkuRes.builder()
                            .skuCode(agg.sku.getSkuCode())
                            .itemCode(agg.master.getCode())
                            .itemName(agg.master.getName())
                            .color(agg.sku.getColor())
                            .size(agg.sku.getSize())
                            .category(categoryLabel)
                            .totalOnHand(agg.totalOnHand)
                            .totalAvailable(agg.totalAvailable)
                            .shortageWarehouseCount(agg.shortageWarehouseCount)
                            .totalShortageQty(agg.totalShortageQty)
                            .build();
                })
                .sorted(
                        Comparator.comparing(InventoryDto.ImbalancedSkuRes::getShortageWarehouseCount, Comparator.reverseOrder())
                                .thenComparing(InventoryDto.ImbalancedSkuRes::getTotalShortageQty, Comparator.reverseOrder())
                                .thenComparing(InventoryDto.ImbalancedSkuRes::getSkuCode, Comparator.nullsLast(String::compareTo))
                )
                .toList();
    }

    /**
     * 발주 IN_TRANSIT 진입 시 해당 창고 SKU 의 가용재고 증가 (이슈 #169 — 발주 ↔ 인벤토리 연결 룰).
     * row 부재 시 신규 INSERT 분기. 동일 트랜잭션에서 호출자(PurchaseOrderService.startInTransit)
     * 가 실패하면 함께 롤백된다.
     */
    @Transactional
    public void increaseAvailable(Long locationId, String skuCode, int quantity) {
        if (quantity <= 0) return;
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));

        // 발주 hook 은 NORMAL 재고에만 반영 — (sku, location) 에 status 3행이 있을 수 있어 단일 보장 위해 status 까지 좁힌다.
        inventoryRepository.findBySkuIdAndLocationIdAndInventoryStatus(sku.getId(), locationId, InventoryStatus.NORMAL)
                .ifPresentOrElse(
                        inv -> inv.increaseAvailable(quantity),
                        () -> inventoryRepository.save(Inventory.builder()
                                .skuId(sku.getId())
                                .locationId(locationId)
                                .inventoryStatus(InventoryStatus.NORMAL)
                                .quantity(0)
                                .reservedQuantity(0)
                                .inTransitQuantity(0)
                                .availableQuantity(quantity)
                                .build()));
    }

    /**
     * 발주 COMPLETED (입고 확정) 진입 시 가용재고를 실재고로 이동.
     * row 부재 시 신규 INSERT (가용재고 0, 실재고 quantity — SHIPPING 누락된 비정상 케이스 방어).
     */
    @Transactional
    public void markPhysical(Long locationId, String skuCode, int quantity) {
        if (quantity <= 0) return;
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));

        // 입고 확정 hook 도 NORMAL 한정 — increaseAvailable 과 동일한 status 분리 룰.
        inventoryRepository.findBySkuIdAndLocationIdAndInventoryStatus(sku.getId(), locationId, InventoryStatus.NORMAL)
                .ifPresentOrElse(
                        inv -> inv.moveAvailableToPhysical(quantity),
                        () -> inventoryRepository.save(Inventory.builder()
                                .skuId(sku.getId())
                                .locationId(locationId)
                                .inventoryStatus(InventoryStatus.NORMAL)
                                .quantity(quantity)
                                .reservedQuantity(0)
                                .inTransitQuantity(0)
                                .availableQuantity(0)
                                .build()));
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
        private final Set<String> safetyKeys = new HashSet<>();

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
        private int safetyStock = 0;
        private Date updatedAt;
        private final Set<String> safetyKeys = new HashSet<>();

        private SkuAggregate(ProductSku sku) {
            this.sku = sku;
        }
    }

    private static class ImbalancedSkuAggregate {
        private final ProductSku sku;
        private final ProductMaster master;
        private int totalOnHand = 0;
        private int totalAvailable = 0;
        private int shortageWarehouseCount = 0;
        private int totalShortageQty = 0;

        private ImbalancedSkuAggregate(ProductSku sku, ProductMaster master) {
            this.sku = sku;
            this.master = master;
        }
    }

    private static class WarehouseSkuStock {
        private final Long skuId;
        private int totalOnHand = 0;
        private int totalAvailable = 0;

        private WarehouseSkuStock(Long skuId) {
            this.skuId = skuId;
        }
    }

    @Transactional
    public InventoryDto.CircularCandidateRefreshRes refreshCircularCandidates() {
        Map<Long, Infrastructure> warehouseById = infrastructureRepository.findAll().stream()
                .filter(i -> i.getLocationType() == LocationType.WAREHOUSE)
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
        if (warehouseById.isEmpty()) {
            return InventoryDto.CircularCandidateRefreshRes.builder()
                    .scannedCount(0)
                    .convertedCount(0)
                    .build();
        }

        List<Inventory> normalInventories = inventoryRepository.findAllByInventoryStatus(InventoryStatus.NORMAL).stream()
                .filter(inv -> warehouseById.containsKey(inv.getLocationId()))
                .toList();
        if (normalInventories.isEmpty()) {
            return InventoryDto.CircularCandidateRefreshRes.builder()
                    .scannedCount(0)
                    .convertedCount(0)
                    .build();
        }

        Set<Long> skuIds = normalInventories.stream().map(Inventory::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuById = productSkuRepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Set<String> productCodes = skuById.values().stream().map(ProductSku::getProductCode).collect(Collectors.toSet());
        Map<String, ProductMaster> productMasterByCode = productMasterRepository.findAllByCodeIn(productCodes).stream()
                .collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));

        Map<String, GroupAvailabilityAggregate> groupAvailability = buildGroupAvailability(normalInventories, skuById);

        Date now = new Date();
        int convertedCount = 0;
        Map<Long, Set<Integer>> matchedByFinalInventoryId = new HashMap<>();

        for (Inventory snapshot : normalInventories) {
            ProductSku sku = skuById.get(snapshot.getSkuId());
            if (sku == null) continue;
            ProductMaster productMaster = productMasterByCode.get(sku.getProductCode());
            List<Integer> matchedCodes = evaluateCandidateConditions(snapshot, sku, productMaster, groupAvailability);
            if (matchedCodes.isEmpty()) continue;

            Inventory source = inventoryRepository.findWithLockById(snapshot.getId()).orElse(null);
            if (source == null || source.getInventoryStatus() != InventoryStatus.NORMAL) continue;

            Inventory target = inventoryRepository
                    .findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                            source.getSkuId(),
                            source.getLocationId(),
                            InventoryStatus.CIRCULAR_CANDIDATE
                    )
                    .orElse(null);

            Long finalInventoryId;
            if (target != null) {
                target.absorbAsCircularCandidate(source, now);
                inventoryRepository.save(target);
                inventoryCandidateConditionRepository.deleteByInventoryIdIn(List.of(source.getId()));
                inventoryRepository.delete(source);
                finalInventoryId = target.getId();
            } else {
                source.markCircularCandidate(now);
                inventoryRepository.save(source);
                finalInventoryId = source.getId();
            }

            matchedByFinalInventoryId
                    .computeIfAbsent(finalInventoryId, ignored -> new HashSet<>())
                    .addAll(matchedCodes);
            convertedCount += 1;
        }

        if (matchedByFinalInventoryId.isEmpty()) {
            return InventoryDto.CircularCandidateRefreshRes.builder()
                    .scannedCount(normalInventories.size())
                    .convertedCount(0)
                    .build();
        }

        List<Long> convertedIds = new ArrayList<>(matchedByFinalInventoryId.keySet());
        inventoryCandidateConditionRepository.deleteByInventoryIdIn(convertedIds);

        List<InventoryCandidateCondition> conditions = new ArrayList<>();
        for (Map.Entry<Long, Set<Integer>> entry : matchedByFinalInventoryId.entrySet()) {
            Long inventoryId = entry.getKey();
            for (Integer code : entry.getValue()) {
                conditions.add(InventoryCandidateCondition.builder()
                        .inventoryId(inventoryId)
                        .conditionCode(code)
                        .conditionLabel(conditionLabel(code))
                        .matchedAt(now)
                        .build());
            }
        }
        inventoryCandidateConditionRepository.saveAll(conditions);

        return InventoryDto.CircularCandidateRefreshRes.builder()
                .scannedCount(normalInventories.size())
                .convertedCount(convertedCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.CircularCandidateRes> findCircularCandidates() {
        List<Inventory> candidates = inventoryRepository.findAllByInventoryStatus(InventoryStatus.CIRCULAR_CANDIDATE);
        if (candidates.isEmpty()) return List.of();

        Map<Long, Infrastructure> warehouseById = infrastructureRepository.findAll().stream()
                .filter(i -> i.getLocationType() == LocationType.WAREHOUSE)
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
        List<Inventory> warehouseCandidates = candidates.stream()
                .filter(inv -> warehouseById.containsKey(inv.getLocationId()))
                .toList();
        if (warehouseCandidates.isEmpty()) return List.of();

        Set<Long> skuIds = warehouseCandidates.stream().map(Inventory::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuById = productSkuRepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));

        Set<String> productCodes = skuById.values().stream().map(ProductSku::getProductCode).collect(Collectors.toSet());
        Map<String, ProductMaster> productByCode = productMasterRepository.findAllByCodeIn(productCodes).stream()
                .collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));

        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        Map<String, Category> categoryByCode = categories.stream().collect(Collectors.toMap(Category::getCode, Function.identity()));
        Map<Long, Category> categoryById = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        Map<Long, List<Integer>> conditionCodesByInventoryId = inventoryCandidateConditionRepository
                .findAllByInventoryIdIn(warehouseCandidates.stream().map(Inventory::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        InventoryCandidateCondition::getInventoryId,
                        Collectors.mapping(InventoryCandidateCondition::getConditionCode, Collectors.toList())
                ));

        return warehouseCandidates.stream()
                .map(inv -> {
                    ProductSku sku = skuById.get(inv.getSkuId());
                    if (sku == null) return null;
                    ProductMaster master = productByCode.get(sku.getProductCode());
                    if (master == null) return null;
                    Infrastructure warehouse = warehouseById.get(inv.getLocationId());
                    if (warehouse == null) return null;

                    Category child = categoryByCode.get(master.getCategoryCode());
                    if (child == null) return null;
                    Category parent = child.getParentId() == null ? child : categoryById.get(child.getParentId());

                    List<Integer> matchedCodes = conditionCodesByInventoryId.getOrDefault(inv.getId(), List.of());
                    int convertibleStock = calculateConvertibleStock(inv);

                    return InventoryDto.CircularCandidateRes.builder()
                            .inventoryId(inv.getId())
                            .skuCode(sku.getSkuCode())
                            .itemCode(master.getCode())
                            .parentCategory(parent != null ? parent.getName() : "")
                            .childCategory(child.getName())
                            .itemName(master.getName())
                            .warehouseCode(warehouse.getCode())
                            .warehouseName(warehouse.getName())
                            .color(sku.getColor())
                            .size(sku.getSize())
                            .actualStock(n(inv.getQuantity()))
                            .availableStock(n(inv.getAvailableQuantity()))
                            .convertibleStock(convertibleStock)
                            .updatedAt(inv.getUpdatedAt())
                            .matchedConditionCodes(matchedCodes.stream().sorted().toList())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(InventoryDto.CircularCandidateRes::getSkuCode))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.CircularInventoryRes> findCircularInventories() {
        List<Inventory> circularInventories = inventoryRepository.findAllByInventoryStatus(InventoryStatus.CIRCULAR);
        if (circularInventories.isEmpty()) return List.of();

        Map<Long, Infrastructure> warehouseById = infrastructureRepository.findAll().stream()
                .filter(i -> i.getLocationType() == LocationType.WAREHOUSE)
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
        List<Inventory> warehouseCirculars = circularInventories.stream()
                .filter(inv -> warehouseById.containsKey(inv.getLocationId()))
                .toList();
        if (warehouseCirculars.isEmpty()) return List.of();

        Set<Long> skuIds = warehouseCirculars.stream().map(Inventory::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuById = productSkuRepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Set<String> productCodes = skuById.values().stream().map(ProductSku::getProductCode).collect(Collectors.toSet());
        Map<String, ProductMaster> masterByCode = productMasterRepository.findAllByCodeIn(productCodes).stream()
                .collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));
        Map<String, Material> materialByCode = materialRepository.findAllByActiveTrueOrderByCodeAsc().stream()
                .collect(Collectors.toMap(Material::getCode, Function.identity()));

        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        Map<String, Category> categoryByCode = categories.stream().collect(Collectors.toMap(Category::getCode, Function.identity()));
        Map<Long, Category> categoryById = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<String, Integer> materialPriceByCode = loadActiveMaterialPriceByCode();

        return warehouseCirculars.stream()
                .map(inv -> {
                    ProductSku sku = skuById.get(inv.getSkuId());
                    if (sku == null) return null;
                    ProductMaster master = masterByCode.get(sku.getProductCode());
                    if (master == null) return null;
                    Infrastructure warehouse = warehouseById.get(inv.getLocationId());
                    if (warehouse == null) return null;
                    Category child = categoryByCode.get(master.getCategoryCode());
                    if (child == null) return null;
                    Category parent = child.getParentId() == null ? child : categoryById.get(child.getParentId());

                    int availableQuantity = Math.max(0, n(inv.getAvailableQuantity()));
                    double unitWeightKg = resolveCategoryUnitWeightKg(child.getName());
                    double totalWeightKg = round3(availableQuantity * unitWeightKg);

                    List<ProductMaterialComposition> materialCompositions = master.getMaterialCompositions();
                    String materialType = resolveMaterialTypeLabel(materialCompositions, materialByCode);
                    List<InventoryDto.MaterialCompositionRes> compositions = toMaterialCompositionRes(materialCompositions, materialByCode);
                    int materialKgPrice = resolveMaterialKgPrice(materialCompositions, materialByCode, materialPriceByCode);
                    long circularSalePrice = Math.round(totalWeightKg * materialKgPrice);

                    return InventoryDto.CircularInventoryRes.builder()
                            .inventoryId(inv.getId())
                            .skuCode(sku.getSkuCode())
                            .itemCode(master.getCode())
                            .itemName(master.getName())
                            .warehouseCode(warehouse.getCode())
                            .warehouseName(warehouse.getName())
                            .parentCategory(parent == null ? "" : parent.getName())
                            .childCategory(child.getName())
                            .color(sku.getColor())
                            .size(sku.getSize())
                            .availableQuantity(availableQuantity)
                            .materialType(materialType)
                            .materialCompositions(compositions)
                            .materialKgPrice(materialKgPrice)
                            .unitWeightKg(unitWeightKg)
                            .totalWeightKg(totalWeightKg)
                            .circularSalePrice(circularSalePrice)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(InventoryDto.CircularInventoryRes::getSkuCode))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.CircularMaterialPriceRes> findCircularMaterialPrices() {
        return circularMaterialPricePolicyRepository.findAll().stream()
                .sorted(Comparator.comparing(CircularMaterialPricePolicy::getMaterialCode))
                .map(this::toCircularMaterialPriceRes)
                .toList();
    }

    @Transactional
    public InventoryDto.CircularMaterialPriceRes updateCircularMaterialPrice(
            String materialCode,
            InventoryDto.CircularMaterialPriceUpdateReq request
    ) {
        String key = materialCode == null ? "" : materialCode.trim().toUpperCase(Locale.ROOT);
        CircularMaterialPricePolicy policy = circularMaterialPricePolicyRepository.findById(key)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOT_FOUND_DATA));
        policy.updatePrice(request.getPricePerKg());
        return toCircularMaterialPriceRes(policy);
    }

    @Transactional
    public InventoryDto.CircularCandidateConvertRes convertCircularCandidates(List<InventoryDto.CircularCandidateConvertItemReq> requests) {
        if (requests == null || requests.isEmpty()) {
            return InventoryDto.CircularCandidateConvertRes.builder()
                    .requestedCount(0)
                    .convertedCount(0)
                    .skippedCount(0)
                    .items(List.of())
                    .build();
        }

        Map<Long, Infrastructure> warehouseById = infrastructureRepository.findAll().stream()
                .filter(i -> i.getLocationType() == LocationType.WAREHOUSE)
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));

        Date now = new Date();
        int convertedCount = 0;
        List<InventoryDto.CircularCandidateConvertItemRes> results = new ArrayList<>();

        for (InventoryDto.CircularCandidateConvertItemReq request : requests) {
            Long inventoryId = request.getInventoryId();
            int requested = request.getConvertQuantity() == null ? 0 : request.getConvertQuantity();

            if (inventoryId == null || requested <= 0) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.builder()
                        .inventoryId(inventoryId)
                        .requested(requested)
                        .converted(0)
                        .reason("유효하지 않은 전환 수량입니다.")
                        .build());
                continue;
            }

            Optional<Inventory> sourceOpt = inventoryRepository.findWithLockById(inventoryId);
            if (sourceOpt.isEmpty()) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.builder()
                        .inventoryId(inventoryId)
                        .requested(requested)
                        .converted(0)
                        .reason("재고를 찾을 수 없습니다.")
                        .build());
                continue;
            }

            Inventory source = sourceOpt.get();
            if (source.getInventoryStatus() != InventoryStatus.CIRCULAR_CANDIDATE) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.builder()
                        .inventoryId(inventoryId)
                        .requested(requested)
                        .converted(0)
                        .reason("순환 재고 후보 상태가 아닙니다.")
                        .build());
                continue;
            }
            if (!warehouseById.containsKey(source.getLocationId())) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.builder()
                        .inventoryId(inventoryId)
                        .requested(requested)
                        .converted(0)
                        .reason("창고 재고만 전환할 수 있습니다.")
                        .build());
                continue;
            }

            int available = Math.max(0, n(source.getAvailableQuantity()));
            if (requested > available) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.builder()
                        .inventoryId(inventoryId)
                        .requested(requested)
                        .converted(0)
                        .reason("전환 가능 재고를 초과했습니다.")
                        .build());
                continue;
            }

            Inventory target = inventoryRepository
                    .findWithLockBySkuIdAndLocationIdAndInventoryStatus(source.getSkuId(), source.getLocationId(), InventoryStatus.CIRCULAR)
                    .orElse(null);
            if (target == null) {
                target = Inventory.builder()
                        .skuId(source.getSkuId())
                        .locationId(source.getLocationId())
                        .inventoryStatus(InventoryStatus.CIRCULAR)
                        .quantity(0)
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .inTransitQuantity(0)
                        .statusChangedAt(now)
                        .lastMovementAt(source.getLastMovementAt())
                        .build();
                target.markCircular(now);
            } else {
                target.markCircular(now);
            }

            source.decreaseForConversion(requested);
            target.increaseForConversion(requested);

            if (source.isEmptyStock()) {
                inventoryCandidateConditionRepository.deleteByInventoryIdIn(List.of(source.getId()));
                inventoryRepository.delete(source);
            } else {
                inventoryRepository.save(source);
            }
            inventoryRepository.save(target);

            convertedCount += 1;
            results.add(InventoryDto.CircularCandidateConvertItemRes.builder()
                    .inventoryId(inventoryId)
                    .requested(requested)
                    .converted(requested)
                    .reason("SUCCESS")
                    .build());
        }

        int requestedCount = requests.size();
        int skippedCount = requestedCount - convertedCount;
        return InventoryDto.CircularCandidateConvertRes.builder()
                .requestedCount(requestedCount)
                .convertedCount(convertedCount)
                .skippedCount(skippedCount)
                .items(results)
                .build();
    }

    private List<Integer> evaluateCandidateConditions(Inventory inventory, ProductSku sku,
                                                      ProductMaster master,
                                                      Map<String, GroupAvailabilityAggregate> groupAvailability) {
        List<Integer> matchedCodes = new ArrayList<>();

        long daysSinceMovement = daysSince(inventory.getLastMovementAt());
        int hashSeed = Math.abs(Objects.hash(sku.getSkuCode(), inventory.getId()));
        if (daysSinceMovement >= 730 || hashSeed % 13 == 0) {
            matchedCodes.add(CONDITION_LONG_NO_MOVEMENT);
        }

        int available = Math.max(0, n(inventory.getAvailableQuantity()));
        int warehouseSafetyStock = master == null ? 0 : Math.max(0, n(master.getWarehouseSafetyStock()));
        if (warehouseSafetyStock > 0 && available > (warehouseSafetyStock * SAFETY_STOCK_MULTIPLIER)) {
            matchedCodes.add(CONDITION_LOW_PERFORMANCE);
        }

        double sizeShare = calculateSizeShare(inventory, sku, groupAvailability);
        double colorShare = calculateColorShare(inventory, sku, groupAvailability);
        if (sizeShare >= SIZE_COLOR_BIAS_THRESHOLD || colorShare >= SIZE_COLOR_BIAS_THRESHOLD) {
            matchedCodes.add(CONDITION_SIZE_COLOR_BIAS);
        }

        return matchedCodes;
    }

    private Map<String, GroupAvailabilityAggregate> buildGroupAvailability(List<Inventory> inventories,
                                                                           Map<Long, ProductSku> skuById) {
        Map<String, GroupAvailabilityAggregate> grouped = new HashMap<>();

        for (Inventory inventory : inventories) {
            ProductSku sku = skuById.get(inventory.getSkuId());
            if (sku == null) continue;
            String groupKey = buildGroupKey(sku.getProductCode(), inventory.getLocationId());
            GroupAvailabilityAggregate aggregate = grouped.computeIfAbsent(groupKey, key -> new GroupAvailabilityAggregate());
            int available = Math.max(0, n(inventory.getAvailableQuantity()));

            aggregate.totalAvailable += available;
            aggregate.availableBySize.merge(normalizeToken(sku.getSize()), available, Integer::sum);
            aggregate.availableByColor.merge(normalizeToken(sku.getColor()), available, Integer::sum);
        }

        return grouped;
    }

    private double calculateSizeShare(Inventory inventory, ProductSku sku,
                                      Map<String, GroupAvailabilityAggregate> groupAvailability) {
        String groupKey = buildGroupKey(sku.getProductCode(), inventory.getLocationId());
        GroupAvailabilityAggregate aggregate = groupAvailability.get(groupKey);
        if (aggregate == null || aggregate.totalAvailable <= 0) return 0d;

        int matched = aggregate.availableBySize.getOrDefault(normalizeToken(sku.getSize()), 0);
        return (double) matched / aggregate.totalAvailable;
    }

    private double calculateColorShare(Inventory inventory, ProductSku sku,
                                       Map<String, GroupAvailabilityAggregate> groupAvailability) {
        String groupKey = buildGroupKey(sku.getProductCode(), inventory.getLocationId());
        GroupAvailabilityAggregate aggregate = groupAvailability.get(groupKey);
        if (aggregate == null || aggregate.totalAvailable <= 0) return 0d;

        int matched = aggregate.availableByColor.getOrDefault(normalizeToken(sku.getColor()), 0);
        return (double) matched / aggregate.totalAvailable;
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String buildGroupKey(String productCode, Long locationId) {
        return (productCode == null ? "" : productCode.trim()) + "::" + (locationId == null ? 0L : locationId);
    }

    private int calculateConvertibleStock(Inventory inventory) {
        return Math.max(0, n(inventory.getAvailableQuantity()));
    }

    private long daysSince(Date date) {
        if (date == null) return Long.MAX_VALUE;
        long millis = System.currentTimeMillis() - date.getTime();
        return millis < 0 ? 0 : millis / (1000L * 60 * 60 * 24);
    }

    private String conditionLabel(Integer code) {
        if (code == null) return "";
        return switch (code) {
            case CONDITION_LONG_NO_MOVEMENT -> LABEL_LONG_NO_MOVEMENT;
            case CONDITION_LOW_PERFORMANCE -> LABEL_LOW_PERFORMANCE;
            case CONDITION_SIZE_COLOR_BIAS -> LABEL_SIZE_COLOR_BIAS;
            default -> "";
        };
    }

    private Map<String, Integer> loadActiveMaterialPriceByCode() {
        return circularMaterialPricePolicyRepository.findAllByActiveTrueOrderByMaterialCodeAsc().stream()
                .collect(Collectors.toMap(CircularMaterialPricePolicy::getMaterialCode, CircularMaterialPricePolicy::getPricePerKg));
    }

    private InventoryDto.CircularMaterialPriceRes toCircularMaterialPriceRes(CircularMaterialPricePolicy policy) {
        return InventoryDto.CircularMaterialPriceRes.builder()
                .materialCode(policy.getMaterialCode())
                .materialNameKo(policy.getMaterialNameKo())
                .materialGroup(policy.getMaterialGroup())
                .pricePerKg(policy.getPricePerKg())
                .active(policy.getActive())
                .build();
    }

    private double resolveCategoryUnitWeightKg(String childCategoryName) {
        if (childCategoryName == null) return DEFAULT_UNIT_WEIGHT_KG;
        return CATEGORY_UNIT_WEIGHT_KG.getOrDefault(childCategoryName.trim(), DEFAULT_UNIT_WEIGHT_KG);
    }

    private String resolveMaterialTypeLabel(List<ProductMaterialComposition> compositions, Map<String, Material> materialByCode) {
        ProductMaterialType type = deriveMaterialType(compositions, materialByCode);
        if (type == ProductMaterialType.NATURAL_SINGLE) return "천연 단일 섬유";
        if (type == ProductMaterialType.SYNTHETIC) return "합성 섬유";
        return "혼방";
    }

    private List<InventoryDto.MaterialCompositionRes> toMaterialCompositionRes(List<ProductMaterialComposition> compositions,
                                                                                Map<String, Material> materialByCode) {
        if (compositions == null) return List.of();
        return compositions.stream()
                .filter(comp -> comp.getMaterial() != null && comp.getMaterial().getCode() != null)
                .map(comp -> {
                    String code = comp.getMaterial().getCode().trim().toUpperCase(Locale.ROOT);
                    return InventoryDto.MaterialCompositionRes.builder()
                            .materialCode(code)
                            .materialNameKo(resolveMaterialName(code, materialByCode))
                            .ratio(comp.getRatio() == null ? 0 : comp.getRatio())
                            .build();
                })
                .toList();
    }

    private int resolveMaterialKgPrice(List<ProductMaterialComposition> compositions,
                                       Map<String, Material> materialByCode,
                                       Map<String, Integer> materialPriceByCode) {
        ProductMaterialType materialType = deriveMaterialType(compositions, materialByCode);
        if (materialType == ProductMaterialType.BLEND) {
            return materialPriceByCode.getOrDefault(MAT_BLEND, 1000);
        }
        if (compositions == null || compositions.isEmpty()) {
            return materialPriceByCode.getOrDefault(MAT_BLEND, 1000);
        }
        ProductMaterialComposition single = compositions.get(0);
        String code = single.getMaterial() == null ? "" : single.getMaterial().getCode().trim().toUpperCase(Locale.ROOT);
        return materialPriceByCode.getOrDefault(code, materialPriceByCode.getOrDefault(MAT_BLEND, 1000));
    }

    private ProductMaterialType deriveMaterialType(List<ProductMaterialComposition> compositions, Map<String, Material> materialByCode) {
        if (compositions == null || compositions.isEmpty()) return ProductMaterialType.BLEND;
        if (compositions.size() >= 2) return ProductMaterialType.BLEND;

        ProductMaterialComposition single = compositions.get(0);
        if (single.getMaterial() == null || single.getMaterial().getCode() == null) return ProductMaterialType.BLEND;
        String code = single.getMaterial().getCode().trim().toUpperCase(Locale.ROOT);
        Material material = materialByCode.get(code);
        if (material == null || material.getMaterialGroup() == null) return ProductMaterialType.BLEND;

        String group = material.getMaterialGroup().trim().toUpperCase(Locale.ROOT);
        if ("NATURAL".equals(group)) return ProductMaterialType.NATURAL_SINGLE;
        if ("SYNTHETIC".equals(group)) return ProductMaterialType.SYNTHETIC;
        return ProductMaterialType.BLEND;
    }

    private String resolveMaterialName(String code, Map<String, Material> materialByCode) {
        Material material = materialByCode.get(code);
        if (material == null || material.getNameKo() == null || material.getNameKo().isBlank()) {
            return MATERIAL_NAME_KO_MAP.getOrDefault(code, code);
        }
        return material.getNameKo();
    }

    private double round3(double value) {
        return Math.round(value * 1000d) / 1000d;
    }

    private static class GroupAvailabilityAggregate {
        private int totalAvailable = 0;
        private final Map<String, Integer> availableBySize = new HashMap<>();
        private final Map<String, Integer> availableByColor = new HashMap<>();
    }
}
