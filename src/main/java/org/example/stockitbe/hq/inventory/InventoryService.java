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
// Phase 2 알림 트리거 — 순환재고 후보 + 매장/창고 재고 부족·품절 알림 발행
import org.example.stockitbe.notification.event.NotificationEvent;
import org.example.stockitbe.notification.model.entity.NotificationSeverity;
import org.example.stockitbe.notification.model.entity.NotificationType;
import org.example.stockitbe.user.model.entity.UserRole;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// 본사 재고 관리 서비스
// 재고 전이, 순환재고 후보/확정 처리, 순환재고 조회 및 소재 단가 정책을 관리한다.
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryCandidateConditionRepository inventoryCandidateConditionRepository;
    private final CircularMaterialPricePolicyRepository circularMaterialPricePolicyRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final MaterialRepository materialRepository;
    private final CategoryRepository categoryRepository;
    private final InfrastructureRepository infrastructureRepository;
    // Phase 2 — Spring ApplicationEvent 발행자. 알림 도메인이 직접 의존하지 않도록 이벤트 패턴 사용
    private final ApplicationEventPublisher eventPublisher;

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

    // 전사 재고 불균형 SKU를 계산해 반환한다.
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

                    return InventoryDto.ImbalancedSkuRes.from(
                            agg.sku,
                            agg.master,
                            categoryLabel,
                            agg.totalOnHand,
                            agg.totalAvailable,
                            agg.shortageWarehouseCount,
                            agg.totalShortageQty
                    );
                })
                .sorted(
                        Comparator.comparing(InventoryDto.ImbalancedSkuRes::getShortageWarehouseCount, Comparator.reverseOrder())
                                .thenComparing(InventoryDto.ImbalancedSkuRes::getTotalShortageQty, Comparator.reverseOrder())
                                .thenComparing(InventoryDto.ImbalancedSkuRes::getSkuCode, Comparator.nullsLast(String::compareTo))
                )
                .toList();
    }

    // -------- 재고 전이/후킹 --------

    /** 특정 창고 SKU를 락으로 조회해 요청 수량만큼 출고 예약한다. */
    @Transactional
    public int reserveForOutboundUpTo(Long locationId, Long skuId, int requestedQuantity) {
        if (locationId == null || skuId == null || requestedQuantity <= 0) return 0;
        return inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                        skuId, locationId, InventoryStatus.NORMAL
                )
                .map(inv -> inv.reserveUpTo(requestedQuantity))
                .orElse(0);
    }

    // 출고 확정 시점에 특정 창고/상품 재고를 reserved -> inTransit으로 옮기는 역할
    // 반환값은 "실제로 이동된 수량"이며, 호출부에서 요청수량과 일치하는지 검증해 예외 처리한다.
    @Transactional
        public int moveReservedToInTransit(Long locationId, Long skuId, int requestedQuantity) {
        // 1. 필수 파라미터가 없거나 요청 수량이 0 이하이면 처리하지 않는다.
        if (locationId == null || skuId == null || requestedQuantity <= 0) return 0;

        // 2. NORMAL 재고 row를 락으로 조회해 동시성 충돌 없이 전이 처리한다.
        // 3. row가 있으면 reserved -> inTransit 전이를 수행하고, 없으면 0을 반환한다.
        return inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                        skuId, locationId, InventoryStatus.NORMAL
                )
                .map(inv -> inv.moveReservedToInTransit(requestedQuantity))
                .orElse(0);
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
     * 창고간 이동 입고 확정 시 송신 창고 inTransit 재고 차감.
     * row 부재 시 무시 (정상 흐름에선 moveReservedToInTransit 가 이미 row 를 만들어둠).
     * NORMAL 한정 — increaseAvailable/markPhysical 과 동일한 status 분리 룰.
     */
    @Transactional
    public void reduceInTransit(Long locationId, String skuCode, int quantity) {
        if (quantity <= 0) return;
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));

        inventoryRepository.findBySkuIdAndLocationIdAndInventoryStatus(sku.getId(), locationId, InventoryStatus.NORMAL)
                .ifPresent(inv -> inv.reduceInTransit(quantity));
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

    // 입고 수량을 실제로 재고에 반영 : 입고 확정 시 매장 NORMAL 재고를 늘리는 함수
    @Transactional
    public void increaseOnHandAndAvailable(Long locationId, Long skuId, int quantity) {
        if (locationId == null || skuId == null || quantity <= 0) return;

        inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                        skuId, locationId, InventoryStatus.NORMAL
                )
                .ifPresentOrElse(
                        inv -> inv.increaseOnHandAndAvailable(quantity),
                        () -> inventoryRepository.save(Inventory.builder()
                                .skuId(skuId)
                                .locationId(locationId)
                                .inventoryStatus(InventoryStatus.NORMAL)
                                .quantity(quantity)
                                .reservedQuantity(0)
                                .inTransitQuantity(0)
                                .availableQuantity(quantity)
                                .build())
                );
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    // -------- Phase 2 알림 — 재고 부족/품절 평가 helper --------

    // 매장 판매 직후 호출되는 진입점.
    // 사용하는 메서드: StoreSaleService.create() — 판매로 인한 재고 차감 직후 호출
    // 현재 가용재고 < ProductMaster.storeSafetyStock 이면 매장 + 본사 두 채널에 알림 발행
    // <=0 이면 품절(CRITICAL), 그 미만이면 부족(WARNING)
    public void evaluateStoreStockAndAlert(Long storeLocationId, String storeCode, Long skuId, String skuCode) {
        if (storeLocationId == null || skuId == null) return;
        // 매장 inventory 는 status=NORMAL row 한 개만 사용 (status 분리 룰)
        inventoryRepository.findBySkuIdAndLocationIdAndInventoryStatus(skuId, storeLocationId, InventoryStatus.NORMAL)
                .ifPresent(inv -> {
                    ProductSku sku = productSkuRepository.findById(skuId).orElse(null);
                    if (sku == null) return;
                    ProductMaster master = productMasterRepository.findByCode(sku.getProductCode()).orElse(null);
                    if (master == null) return;
                    int available = Math.max(0, n(inv.getAvailableQuantity()));
                    int safetyStock = Math.max(0, n(master.getStoreSafetyStock()));   // 매장용 안전재고
                    publishStockAlertIfNeeded(available, safetyStock, UserRole.STORE, storeCode, skuCode);
                });
    }

    // 창고 출고 확정 직후 호출되는 진입점.
    // 사용하는 메서드: WhOutboundService.confirm() — moveReservedToInTransit 직후 호출
    // 매장과 동일 로직이나 안전재고는 ProductMaster.warehouseSafetyStock 사용
    public void evaluateWarehouseStockAndAlert(Long warehouseLocationId, String warehouseCode, Long skuId, String skuCode) {
        if (warehouseLocationId == null || skuId == null) return;
        inventoryRepository.findBySkuIdAndLocationIdAndInventoryStatus(skuId, warehouseLocationId, InventoryStatus.NORMAL)
                .ifPresent(inv -> {
                    ProductSku sku = productSkuRepository.findById(skuId).orElse(null);
                    if (sku == null) return;
                    ProductMaster master = productMasterRepository.findByCode(sku.getProductCode()).orElse(null);
                    if (master == null) return;
                    int available = Math.max(0, n(inv.getAvailableQuantity()));
                    int safetyStock = Math.max(0, n(master.getWarehouseSafetyStock())); // 창고용 안전재고
                    publishStockAlertIfNeeded(available, safetyStock, UserRole.WAREHOUSE, warehouseCode, skuCode);
                });
    }

    // 사용하는 메서드: evaluateStoreStockAndAlert, evaluateWarehouseStockAndAlert
    // 가용재고 vs 안전재고 비교 후 임계 초과 시 지점 + 본사 2채널에 알림 발행
    private void publishStockAlertIfNeeded(int available, int safetyStock,
                                            UserRole role, String locationCode, String skuCode) {
        // (a) 품절 — 가용재고 0 이하
        if (available <= 0) {
            String title = role == UserRole.STORE ? "매장 재고 품절" : "창고 재고 품절";
            // 본인 지점용 — 권한+locationCode 매칭으로 본인만 수신
            publishStockAlert(NotificationType.INVENTORY_OUT_OF_STOCK, NotificationSeverity.CRITICAL,
                    title, skuCode + " 재고가 품절되었습니다.",
                    role, locationCode, skuCode);
            // 본사 전체용 — locationCode=null 이면 모든 HQ 수신
            publishStockAlert(NotificationType.INVENTORY_OUT_OF_STOCK, NotificationSeverity.CRITICAL,
                    title, "[" + locationCode + "] " + skuCode + " 품절",
                    UserRole.HQ, null, skuCode);
            return;
        }
        // (b) 부족 — 가용재고가 안전재고 미만 (안전재고가 0 인 SKU 는 알림 발생 X)
        if (safetyStock > 0 && available < safetyStock) {
            String title = role == UserRole.STORE ? "매장 재고 부족" : "창고 재고 부족";
            publishStockAlert(NotificationType.INVENTORY_SHORTAGE, NotificationSeverity.WARNING,
                    title,
                    skuCode + " 재고가 안전재고(" + safetyStock + ") 미만입니다. 현재 가용 " + available,
                    role, locationCode, skuCode);
            publishStockAlert(NotificationType.INVENTORY_SHORTAGE, NotificationSeverity.WARNING,
                    title,
                    "[" + locationCode + "] " + skuCode + " 안전재고 미만 (가용 " + available + "/" + safetyStock + ")",
                    UserRole.HQ, null, skuCode);
        }
    }

    // 사용하는 메서드: publishStockAlertIfNeeded
    // NotificationEvent 빌드 + publishEvent 호출. refType=INVENTORY 로 통일해 알림→원천 SKU 추적
    private void publishStockAlert(NotificationType type, NotificationSeverity severity,
                                    String title, String message,
                                    UserRole role, String locationCode, String skuCode) {
        eventPublisher.publishEvent(NotificationEvent.builder()
                .type(type)
                .severity(severity)
                .title(title)
                .message(message)
                .targetRole(role)
                .targetLocationCode(locationCode)
                .refType("INVENTORY")
                .refId(skuCode)
                .build());
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

    // -------- 순환재고 후보 리프레시 --------

    // 순환재고 후보 조건을 재평가하고 대상 재고를 후보 상태로 전환한다.
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

        // Phase 2 — 새로 전환된 후보 1건 이상이면 본사에 요약 1건 알림 (계획서 §4-10 ③)
        //          개별 SKU 마다 알림 X (스팸 방지). convertedCount 만 한 줄로 보고
        if (convertedCount > 0) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .type(NotificationType.CIRCULAR_CANDIDATE)
                    .severity(NotificationSeverity.INFO)
                    .title("순환재고 후보 신규 발생")
                    .message("순환재고 후보 " + convertedCount + "건이 새로 등록되었습니다.")
                    .targetRole(UserRole.HQ)              // 본사 전체
                    .refType("INVENTORY")
                    .refId("CIRCULAR_REFRESH")            // 식별용 더미 id (배치 단위)
                    .build());
        }

        return InventoryDto.CircularCandidateRefreshRes.builder()
                .scannedCount(normalInventories.size())
                .convertedCount(convertedCount)
                .build();
    }

    // -------- 순환재고 조회 --------

    // 순환재고 후보 목록을 필터/정렬/페이지 조건으로 조회한다.
    @Transactional(readOnly = true)
    public InventoryDto.CircularCandidatePageRes findCircularCandidates(Integer page,
                                                                         Integer size,
                                                                         String sort,
                                                                         String keyword,
                                                                         String parentCategory,
                                                                         String childCategory,
                                                                         List<String> warehouseCodes,
                                                                         List<Integer> conditionCodes) {
        int safePage = Math.max(0, page == null ? 0 : page);
        int safeSize = normalizePageSize(size);
        Sort sortSpec = parseCandidateSort(sort);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String safeParentCategory = parentCategory == null ? "" : parentCategory.trim();
        String safeChildCategory = childCategory == null ? "" : childCategory.trim();
        Set<String> warehouseCodeSet = (warehouseCodes == null ? List.<String>of() : warehouseCodes).stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .collect(Collectors.toSet());
        Set<Integer> conditionCodeSet = (conditionCodes == null ? List.<Integer>of() : conditionCodes).stream()
                .filter(Objects::nonNull)
                .filter(code -> code >= 1 && code <= 3)
                .collect(Collectors.toSet());

        List<Inventory> candidates = inventoryRepository.findAllByInventoryStatus(InventoryStatus.CIRCULAR_CANDIDATE);
        if (candidates.isEmpty()) {
            return buildCircularCandidatePage(List.of(), safePage, safeSize, 0);
        }

        Map<Long, Infrastructure> warehouseById = infrastructureRepository.findAll().stream()
                .filter(i -> i.getLocationType() == LocationType.WAREHOUSE)
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
        List<Inventory> warehouseCandidates = candidates.stream()
                .filter(inv -> warehouseById.containsKey(inv.getLocationId()))
                .toList();
        if (warehouseCandidates.isEmpty()) {
            return buildCircularCandidatePage(List.of(), safePage, safeSize, 0);
        }

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

        List<InventoryDto.CircularCandidateRes> rows = warehouseCandidates.stream()
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

                    return InventoryDto.CircularCandidateRes.from(
                            inv.getId(),
                            sku,
                            master,
                            parent != null ? parent.getName() : "",
                            child.getName(),
                            warehouse.getCode(),
                            warehouse.getName(),
                            n(inv.getQuantity()),
                            n(inv.getAvailableQuantity()),
                            convertibleStock,
                            inv.getUpdatedAt(),
                            matchedCodes.stream().sorted().toList()
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        List<InventoryDto.CircularCandidateRes> filtered = rows.stream()
                .filter(row -> matchesCandidateKeyword(row, safeKeyword))
                .filter(row -> matchesCandidateCategory(row, safeParentCategory, safeChildCategory))
                .filter(row -> matchesCandidateWarehouse(row, warehouseCodeSet))
                .filter(row -> matchesCandidateConditionCodes(row, conditionCodeSet))
                .sorted(buildCandidateComparator(sortSpec))
                .toList();

        return buildCircularCandidatePage(filtered, safePage, safeSize, filtered.size());
    }

    // 순환재고 확정 목록을 필터/정렬/페이지 조건으로 조회한다.
    @Transactional(readOnly = true)
    public InventoryDto.CircularInventoryPageRes findCircularInventories(Integer page,
                                                                         Integer size,
                                                                         String sort,
                                                                         String keyword,
                                                                         List<String> warehouseCodes,
                                                                         String materialGroup,
                                                                         String materialName,
                                                                         Integer minRatio) {
        int safePage = Math.max(0, page == null ? 0 : page);
        int safeSize = normalizePageSize(size);
        Sort sortSpec = parseCircularSort(sort);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Set<String> warehouseCodeSet = (warehouseCodes == null ? List.<String>of() : warehouseCodes).stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        String safeMaterialGroup = materialGroup == null ? "" : materialGroup.trim();
        String safeMaterialName = materialName == null ? "" : normalizeMaterialName(materialName.trim());
        int safeMinRatio = Math.max(0, minRatio == null ? 0 : minRatio);

        List<Inventory> circularInventories = inventoryRepository.findAllByInventoryStatus(InventoryStatus.CIRCULAR);
        if (circularInventories.isEmpty()) {
            return buildCircularInventoryPage(List.of(), safePage, safeSize, 0);
        }

        Map<Long, Infrastructure> warehouseById = infrastructureRepository.findAll().stream()
                .filter(i -> i.getLocationType() == LocationType.WAREHOUSE)
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
        List<Inventory> warehouseCirculars = circularInventories.stream()
                .filter(inv -> warehouseById.containsKey(inv.getLocationId()))
                .toList();
        if (warehouseCirculars.isEmpty()) {
            return buildCircularInventoryPage(List.of(), safePage, safeSize, 0);
        }

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

        List<InventoryDto.CircularInventoryRes> rows = warehouseCirculars.stream()
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

                    return InventoryDto.CircularInventoryRes.from(
                            inv.getId(),
                            sku,
                            master,
                            warehouse.getCode(),
                            warehouse.getName(),
                            parent == null ? "" : parent.getName(),
                            child.getName(),
                            availableQuantity,
                            materialType,
                            compositions,
                            materialKgPrice,
                            unitWeightKg,
                            totalWeightKg,
                            circularSalePrice
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        List<InventoryDto.CircularInventoryRes> filtered = rows.stream()
                .filter(row -> matchesCircularKeyword(row, safeKeyword))
                .filter(row -> matchesWarehouseCodes(row, warehouseCodeSet))
                .filter(row -> matchesMaterialFilter(row, safeMaterialGroup, safeMaterialName, safeMinRatio))
                .sorted(buildCircularComparator(sortSpec))
                .toList();

        return buildCircularInventoryPage(filtered, safePage, safeSize, filtered.size());
    }

    // 순환재고 페이지 응답 객체를 생성한다.
    private InventoryDto.CircularInventoryPageRes buildCircularInventoryPage(List<InventoryDto.CircularInventoryRes> rows,
                                                                             int page,
                                                                             int size,
                                                                             int totalElements) {
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);
        List<InventoryDto.CircularInventoryRes> content = rows.subList(from, to);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        PageImpl<InventoryDto.CircularInventoryRes> result = new PageImpl<>(content, pageable, totalElements);
        return InventoryDto.CircularInventoryPageRes.from(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    // 순환재고 후보 페이지 응답 객체를 생성한다.
    private InventoryDto.CircularCandidatePageRes buildCircularCandidatePage(List<InventoryDto.CircularCandidateRes> rows,
                                                                              int page,
                                                                              int size,
                                                                              int totalElements) {
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);
        List<InventoryDto.CircularCandidateRes> content = rows.subList(from, to);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        PageImpl<InventoryDto.CircularCandidateRes> result = new PageImpl<>(content, pageable, totalElements);
        return InventoryDto.CircularCandidatePageRes.from(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    // 요청 페이지 크기를 허용 값(20/50/100)으로 정규화한다.
    private int normalizePageSize(Integer size) {
        int requested = size == null ? 20 : size;
        if (requested == 50 || requested == 100) return requested;
        return 20;
    }

    // 순환재고 정렬 파라미터를 Sort 스펙으로 변환한다.
    private Sort parseCircularSort(String sort) {
        String normalized = sort == null ? "" : sort.trim();
        if (normalized.isBlank()) return Sort.by(Sort.Order.asc("skuCode"));
        String[] split = normalized.split(",");
        String field = split.length > 0 ? split[0].trim() : "skuCode";
        String direction = split.length > 1 ? split[1].trim().toLowerCase(Locale.ROOT) : "asc";
        Sort.Direction dir = "desc".equals(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        if (!Set.of("skuCode", "quantity", "materialKgPrice", "circularSalePrice", "weight").contains(field)) {
            field = "skuCode";
            dir = Sort.Direction.ASC;
        }
        return Sort.by(new Sort.Order(dir, field));
    }

    // 순환재고 후보 정렬 파라미터를 Sort 스펙으로 변환한다.
    private Sort parseCandidateSort(String sort) {
        String normalized = sort == null ? "" : sort.trim();
        if (normalized.isBlank()) return Sort.by(Sort.Order.desc("convertibleStock"));
        String[] split = normalized.split(",");
        String field = split.length > 0 ? split[0].trim() : "convertibleStock";
        String direction = split.length > 1 ? split[1].trim().toLowerCase(Locale.ROOT) : "desc";
        Sort.Direction dir = "asc".equals(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (!Set.of("skuCode", "availableStock", "convertibleStock", "updatedAt").contains(field)) {
            field = "convertibleStock";
            dir = Sort.Direction.DESC;
        }
        return Sort.by(new Sort.Order(dir, field));
    }

    // 순환재고 목록 정렬 Comparator를 생성한다.
    private Comparator<InventoryDto.CircularInventoryRes> buildCircularComparator(Sort sortSpec) {
        Sort.Order order = sortSpec.stream().findFirst().orElse(Sort.Order.asc("skuCode"));
        Comparator<InventoryDto.CircularInventoryRes> comparator;
        String property = order.getProperty();
        switch (property) {
            case "quantity":
                comparator = Comparator.comparing(row -> n(row.getAvailableQuantity()));
                break;
            case "materialKgPrice":
                comparator = Comparator.comparing(row -> n(row.getMaterialKgPrice()));
                break;
            case "circularSalePrice":
                comparator = Comparator.comparing(row -> row.getCircularSalePrice() == null ? 0L : row.getCircularSalePrice());
                break;
            case "weight":
                comparator = Comparator.comparing(row -> row.getTotalWeightKg() == null ? 0d : row.getTotalWeightKg());
                break;
            case "skuCode":
            default:
                comparator = Comparator.comparing(row -> row.getSkuCode() == null ? "" : row.getSkuCode(), Comparator.naturalOrder());
                break;
        }
        if (order.getDirection() == Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(row -> row.getSkuCode() == null ? "" : row.getSkuCode());
    }

    // 순환재고 후보 목록 정렬 Comparator를 생성한다.
    private Comparator<InventoryDto.CircularCandidateRes> buildCandidateComparator(Sort sortSpec) {
        Sort.Order order = sortSpec.stream().findFirst().orElse(Sort.Order.desc("convertibleStock"));
        Comparator<InventoryDto.CircularCandidateRes> comparator;
        switch (order.getProperty()) {
            case "skuCode":
                comparator = Comparator.comparing(row -> row.getSkuCode() == null ? "" : row.getSkuCode());
                break;
            case "availableStock":
                comparator = Comparator.comparing(row -> n(row.getAvailableStock()));
                break;
            case "updatedAt":
                comparator = Comparator.comparing(row -> row.getUpdatedAt() == null ? new Date(0) : row.getUpdatedAt());
                break;
            case "convertibleStock":
            default:
                comparator = Comparator.comparing(row -> n(row.getConvertibleStock()));
                break;
        }
        if (order.getDirection() == Sort.Direction.DESC) comparator = comparator.reversed();
        return comparator.thenComparing(row -> row.getSkuCode() == null ? "" : row.getSkuCode());
    }

    // 순환재고 키워드 검색 조건 일치 여부를 판단한다.
    private boolean matchesCircularKeyword(InventoryDto.CircularInventoryRes row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String materialDetail = row.getMaterialCompositions() == null
                ? ""
                : row.getMaterialCompositions().stream()
                .map(comp -> safeText(comp.getMaterialNameKo()) + " " + n(comp.getRatio()) + "%")
                .collect(Collectors.joining(" + "));
        String searchable = String.join(" ", safeText(row.getItemCode()), safeText(row.getItemName()), materialDetail)
                .toLowerCase(Locale.ROOT);
        return searchable.contains(keyword);
    }

    // 순환재고 창고코드 필터 일치 여부를 판단한다.
    private boolean matchesWarehouseCodes(InventoryDto.CircularInventoryRes row, Set<String> warehouseCodes) {
        if (warehouseCodes == null || warehouseCodes.isEmpty()) return true;
        return warehouseCodes.contains(safeText(row.getWarehouseCode()).toUpperCase(Locale.ROOT));
    }

    // 후보 키워드 검색 조건 일치 여부를 판단한다.
    private boolean matchesCandidateKeyword(InventoryDto.CircularCandidateRes row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String searchable = String.join(" ", safeText(row.getSkuCode()), safeText(row.getItemCode()), safeText(row.getItemName()))
                .toLowerCase(Locale.ROOT);
        return searchable.contains(keyword);
    }

    // 후보 카테고리 필터 일치 여부를 판단한다.
    private boolean matchesCandidateCategory(InventoryDto.CircularCandidateRes row, String parentCategory, String childCategory) {
        if (parentCategory != null && !parentCategory.isBlank() && !parentCategory.equals(safeText(row.getParentCategory()))) {
            return false;
        }
        if (childCategory != null && !childCategory.isBlank() && !childCategory.equals(safeText(row.getChildCategory()))) {
            return false;
        }
        return true;
    }

    // 후보 창고 필터 일치 여부를 판단한다.
    private boolean matchesCandidateWarehouse(InventoryDto.CircularCandidateRes row, Set<String> warehouseCodes) {
        if (warehouseCodes == null || warehouseCodes.isEmpty()) return true;
        return warehouseCodes.contains(safeText(row.getWarehouseCode()).toUpperCase(Locale.ROOT));
    }

    // 후보 조건코드 필터 일치 여부를 판단한다.
    private boolean matchesCandidateConditionCodes(InventoryDto.CircularCandidateRes row, Set<Integer> conditionCodes) {
        if (conditionCodes == null || conditionCodes.isEmpty()) return true;
        List<Integer> matched = row.getMatchedConditionCodes() == null ? List.of() : row.getMatchedConditionCodes();
        return conditionCodes.stream().allMatch(matched::contains);
    }

    // 순환재고 소재 필터 일치 여부를 판단한다.
    private boolean matchesMaterialFilter(InventoryDto.CircularInventoryRes row,
                                          String materialGroup,
                                          String materialName,
                                          int minRatio) {
        if (materialGroup == null || materialGroup.isBlank()) return true;
        if (!materialGroup.equals(safeText(row.getMaterialType()))) return false;
        if (materialName == null || materialName.isBlank()) return true;
        if (row.getMaterialCompositions() == null || row.getMaterialCompositions().isEmpty()) return false;

        return row.getMaterialCompositions().stream().anyMatch(comp ->
                materialName.equals(normalizeMaterialName(safeText(comp.getMaterialNameKo())))
                        && (minRatio <= 0 || n(comp.getRatio()) >= minRatio)
        );
    }

    // null-safe 문자열 변환
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    // 소재명 별칭을 표준 한글명으로 정규화한다.
    private String normalizeMaterialName(String value) {
        String normalized = value == null ? "" : value.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "코튼", "cotton" -> "면";
            case "폴리", "polyester" -> "폴리에스터";
            case "acrylic" -> "아크릴";
            case "polyamide", "nylon" -> "나일론";
            case "elastane", "스판", "spandex" -> "스판덱스";
            case "wool" -> "울";
            case "cashmere" -> "캐시미어";
            case "silk" -> "실크";
            case "linen" -> "리넨";
            default -> normalized;
        };
    }

    // -------- 순환재고 단가 정책 --------

    // 소재 단가 정책 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<InventoryDto.CircularMaterialPriceRes> findCircularMaterialPrices() {
        return circularMaterialPricePolicyRepository.findAll().stream()
                .sorted(Comparator.comparing(CircularMaterialPricePolicy::getMaterialCode))
                .map(InventoryDto.CircularMaterialPriceRes::from)
                .toList();
    }

    // 소재 코드 기준 단가 정책을 수정한다.
    @Transactional
    public InventoryDto.CircularMaterialPriceRes updateCircularMaterialPrice(
            String materialCode,
            InventoryDto.CircularMaterialPriceUpdateReq request
    ) {
        String key = materialCode == null ? "" : materialCode.trim().toUpperCase(Locale.ROOT);
        CircularMaterialPricePolicy policy = circularMaterialPricePolicyRepository.findById(key)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOT_FOUND_DATA));
        policy.updatePrice(request.getPricePerKg());
        return InventoryDto.CircularMaterialPriceRes.from(policy);
    }

    // -------- 후보 -> 순환재고 전환 --------

    // 순환재고 후보를 요청 수량만큼 순환재고로 전환한다.
    @Transactional
    public InventoryDto.CircularCandidateConvertRes convertCircularCandidates(List<InventoryDto.CircularCandidateConvertItemReq> requests) {
        if (requests == null || requests.isEmpty()) {
            return InventoryDto.CircularCandidateConvertRes.from(0, 0, List.of());
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
                results.add(InventoryDto.CircularCandidateConvertItemRes.from(inventoryId, requested, 0, "유효하지 않은 전환 수량입니다."));
                continue;
            }

            Optional<Inventory> sourceOpt = inventoryRepository.findWithLockById(inventoryId);
            if (sourceOpt.isEmpty()) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.from(inventoryId, requested, 0, "재고를 찾을 수 없습니다."));
                continue;
            }

            Inventory source = sourceOpt.get();
            if (source.getInventoryStatus() != InventoryStatus.CIRCULAR_CANDIDATE) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.from(inventoryId, requested, 0, "순환 재고 후보 상태가 아닙니다."));
                continue;
            }
            if (!warehouseById.containsKey(source.getLocationId())) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.from(inventoryId, requested, 0, "창고 재고만 전환할 수 있습니다."));
                continue;
            }

            int available = Math.max(0, n(source.getAvailableQuantity()));
            if (requested > available) {
                results.add(InventoryDto.CircularCandidateConvertItemRes.from(inventoryId, requested, 0, "전환 가능 재고를 초과했습니다."));
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
            results.add(InventoryDto.CircularCandidateConvertItemRes.from(inventoryId, requested, requested, "SUCCESS"));
        }

        return InventoryDto.CircularCandidateConvertRes.from(requests.size(), convertedCount, results);
    }

    // -------- 후보 판정/소재 계산 보조 메서드 --------

    // 후보 판정 조건 코드를 계산한다.
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

    // 상품+위치 단위 가용재고 집계를 생성한다.
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

    // 동일 상품/위치 내 사이즈 점유율을 계산한다.
    private double calculateSizeShare(Inventory inventory, ProductSku sku,
                                      Map<String, GroupAvailabilityAggregate> groupAvailability) {
        String groupKey = buildGroupKey(sku.getProductCode(), inventory.getLocationId());
        GroupAvailabilityAggregate aggregate = groupAvailability.get(groupKey);
        if (aggregate == null || aggregate.totalAvailable <= 0) return 0d;

        int matched = aggregate.availableBySize.getOrDefault(normalizeToken(sku.getSize()), 0);
        return (double) matched / aggregate.totalAvailable;
    }

    // 동일 상품/위치 내 컬러 점유율을 계산한다.
    private double calculateColorShare(Inventory inventory, ProductSku sku,
                                       Map<String, GroupAvailabilityAggregate> groupAvailability) {
        String groupKey = buildGroupKey(sku.getProductCode(), inventory.getLocationId());
        GroupAvailabilityAggregate aggregate = groupAvailability.get(groupKey);
        if (aggregate == null || aggregate.totalAvailable <= 0) return 0d;

        int matched = aggregate.availableByColor.getOrDefault(normalizeToken(sku.getColor()), 0);
        return (double) matched / aggregate.totalAvailable;
    }

    // null-safe 토큰 정규화
    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    // 상품코드+위치 기반 그룹 키를 생성한다.
    private String buildGroupKey(String productCode, Long locationId) {
        return (productCode == null ? "" : productCode.trim()) + "::" + (locationId == null ? 0L : locationId);
    }

    // 전환 가능한 재고 수량을 계산한다.
    private int calculateConvertibleStock(Inventory inventory) {
        return Math.max(0, n(inventory.getAvailableQuantity()));
    }

    // 기준일로부터 경과 일수를 계산한다.
    private long daysSince(Date date) {
        if (date == null) return Long.MAX_VALUE;
        long millis = System.currentTimeMillis() - date.getTime();
        return millis < 0 ? 0 : millis / (1000L * 60 * 60 * 24);
    }

    // 조건 코드의 표시 라벨을 반환한다.
    private String conditionLabel(Integer code) {
        if (code == null) return "";
        return switch (code) {
            case CONDITION_LONG_NO_MOVEMENT -> LABEL_LONG_NO_MOVEMENT;
            case CONDITION_LOW_PERFORMANCE -> LABEL_LOW_PERFORMANCE;
            case CONDITION_SIZE_COLOR_BIAS -> LABEL_SIZE_COLOR_BIAS;
            default -> "";
        };
    }

    // 활성 소재 단가 맵을 로딩한다.
    private Map<String, Integer> loadActiveMaterialPriceByCode() {
        return circularMaterialPricePolicyRepository.findAllByActiveTrueOrderByMaterialCodeAsc().stream()
                .collect(Collectors.toMap(CircularMaterialPricePolicy::getMaterialCode, CircularMaterialPricePolicy::getPricePerKg));
    }

    // 카테고리별 단위 중량(kg)을 해석한다.
    private double resolveCategoryUnitWeightKg(String childCategoryName) {
        if (childCategoryName == null) return DEFAULT_UNIT_WEIGHT_KG;
        return CATEGORY_UNIT_WEIGHT_KG.getOrDefault(childCategoryName.trim(), DEFAULT_UNIT_WEIGHT_KG);
    }

    // 소재 구성을 기반으로 소재 유형 라벨을 결정한다.
    private String resolveMaterialTypeLabel(List<ProductMaterialComposition> compositions, Map<String, Material> materialByCode) {
        ProductMaterialType type = deriveMaterialType(compositions, materialByCode);
        if (type == ProductMaterialType.NATURAL_SINGLE) return "천연 단일 섬유";
        if (type == ProductMaterialType.SYNTHETIC) return "합성 섬유";
        return "혼방";
    }

    // 소재 구성 엔티티를 응답 DTO로 변환한다.
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

    // 소재 구성에 따른 kg 단가를 계산한다.
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

    // 소재 구성으로 상품 소재 유형을 판정한다.
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

    // 소재 코드의 한글명을 조회한다.
    private String resolveMaterialName(String code, Map<String, Material> materialByCode) {
        Material material = materialByCode.get(code);
        if (material == null || material.getNameKo() == null || material.getNameKo().isBlank()) {
            return MATERIAL_NAME_KO_MAP.getOrDefault(code, code);
        }
        return material.getNameKo();
    }

    // 소수점 셋째 자리 반올림
    private double round3(double value) {
        return Math.round(value * 1000d) / 1000d;
    }

    private static class GroupAvailabilityAggregate {
        private int totalAvailable = 0;
        private final Map<String, Integer> availableBySize = new HashMap<>();
        private final Map<String, Integer> availableByColor = new HashMap<>();
    }
}
