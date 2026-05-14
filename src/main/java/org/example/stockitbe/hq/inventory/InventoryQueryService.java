package org.example.stockitbe.hq.inventory;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.CompanyWideAggregateRow;
import org.example.stockitbe.hq.inventory.model.CompanyWideSkuRow;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.inventory.model.InventoryStatusPolicy;
import org.example.stockitbe.hq.inventory.model.ItemSafetyStockRow;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // 전사 재고(품목 단위) 페이지 목록을 조회한다.
    // native @Query GROUP BY 로 itemCode 단위 페이징 + 카테고리/재고 합계 한 번에,
    // safetyStock 은 페이지 후 itemCodes IN-clause 로 별도 native @Query 한 번 추가 호출.
    // 페이지당 SQL 호출: 메인(1) + safetyStock(1) + locationOptions(1) = 총 3개.
    @Transactional(readOnly = true)
    public InventoryDto.CompanyWidePageRes findCompanyWide(LocationType locationType,
                                                            List<Long> locationIds,
                                                            String parentCategory,
                                                            String childCategory,
                                                            String category,
                                                            InventoryStatus status,
                                                            String keyword,
                                                            Pageable pageable) {
        String locationTypeStr = locationType == null ? null : locationType.name();
        String statusStr = status == null ? null : status.name();
        boolean hasLocationIds = locationIds != null && !locationIds.isEmpty();
        // IN (:locationIds) 가 빈 리스트면 SQL 에러 → 더미 값으로 채움 (hasLocationIds=false 일 땐 어차피 평가 안 됨)
        List<Long> safeLocationIds = hasLocationIds ? locationIds : List.of(-1L);
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String safeParent = (parentCategory == null || parentCategory.isBlank()) ? null : parentCategory.trim();
        String safeChild = (childCategory == null || childCategory.isBlank()) ? null : childCategory.trim();
        String safeCategory = (category == null || category.isBlank()) ? null : category.trim();

        // sort 무시(native @Query 안에 ORDER BY 박혀 있음) — page/size 만 사용
        Pageable safePageable = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.max(pageable.getPageSize(), 1)
        );

        Page<CompanyWideAggregateRow> rows = inventoryRepository.findCompanyWideAggregated(
                locationTypeStr, hasLocationIds, safeLocationIds, statusStr,
                safeParent, safeChild, safeCategory, safeKeyword, safePageable
        );

        List<InventoryDto.LocationOptionRes> locationOptions = buildLocationOptions(locationType);

        if (rows.isEmpty()) {
            return InventoryDto.CompanyWidePageRes.from(rows.map(r -> null), locationOptions);
        }

        // 페이지 안 itemCode 만 추려 safetyStock 한 번에 집계 (DB 단계, sub-query DISTINCT)
        List<String> itemCodes = rows.getContent().stream()
                .map(CompanyWideAggregateRow::getItemCode).toList();
        Map<String, Integer> safetyByItemCode = inventoryRepository
                .aggregateSafetyStockByItemCode(itemCodes, locationTypeStr, hasLocationIds, safeLocationIds, statusStr)
                .stream()
                .collect(Collectors.toMap(ItemSafetyStockRow::getItemCode, r -> n(r.getSafetyStock())));

        Page<InventoryDto.CompanyWideRes> mapped = rows.map(row -> {
            int actual = n(row.getActualStock());
            int available = n(row.getAvailableStock());
            int safety = safetyByItemCode.getOrDefault(row.getItemCode(), 0);
            return InventoryDto.CompanyWideRes.builder()
                    .itemCode(row.getItemCode())
                    .parentCategory(row.getParentCategory() == null ? "" : row.getParentCategory())
                    .childCategory(row.getChildCategory() == null ? "" : row.getChildCategory())
                    .itemName(row.getItemName())
                    .actualStock(actual)
                    .availableStock(available)
                    .safetyStock(safety)
                    .status(toUiStatus(available, safety))
                    .updatedAt(row.getUpdatedAt())
                    .build();
        });

        return InventoryDto.CompanyWidePageRes.from(mapped, locationOptions);
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
    // region(한글 지역명) 필드 포함 — FE 거점 트리 지역 그룹화용.
    private List<InventoryDto.LocationOptionRes> buildLocationOptions(LocationType locationType) {
        return infrastructureRepository.findAll().stream()
                .filter(i -> locationType == null || i.getLocationType() == locationType)
                .sorted(Comparator.comparing(Infrastructure::getName))
                .map(i -> InventoryDto.LocationOptionRes.builder()
                        .id(i.getId())
                        .code(i.getCode())
                        .name(i.getName())
                        .region(i.getRegion())
                        .build())
                .toList();
    }

    // 전사 재고 SKU 단위 페이지 목록을 조회한다 (모드 토글 SKU 모드).
    // 마스터 무관 모든 SKU 한 표 + 색상/사이즈 필터. status 필터는 BE HAVING 미적용,
    //   FE/Service 측 후처리 (페이징 정확성은 약간 양보 — 사이클 후속 정교화).
    // 주의: skuSize 파라미터명 — Spring Pageable 의 size 와 충돌 방지.
    @Transactional(readOnly = true)
    public InventoryDto.CompanyWideSkuPageRes findCompanyWideSkus(LocationType locationType,
                                                                    List<Long> locationIds,
                                                                    String parentCategory,
                                                                    String childCategory,
                                                                    String status,
                                                                    String color,
                                                                    String skuSize,
                                                                    String keyword,
                                                                    Pageable pageable) {
        String locationTypeStr = locationType == null ? null : locationType.name();
        boolean hasLocationIds = locationIds != null && !locationIds.isEmpty();
        List<Long> safeLocationIds = hasLocationIds ? locationIds : List.of(-1L);
        String safeParent = (parentCategory == null || parentCategory.isBlank()) ? null : parentCategory.trim();
        String safeChild = (childCategory == null || childCategory.isBlank()) ? null : childCategory.trim();
        String safeStatus = (status == null || status.isBlank()) ? null : status.trim();
        String safeColor = (color == null || color.isBlank()) ? null : color.trim();
        String safeSkuSize = (skuSize == null || skuSize.isBlank()) ? null : skuSize.trim();
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Pageable safePageable = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.max(pageable.getPageSize(), 1)
        );

        Page<CompanyWideSkuRow> rows = inventoryRepository.findCompanyWideSkus(
                locationTypeStr, hasLocationIds, safeLocationIds,
                safeParent, safeChild, safeColor, safeSkuSize, safeKeyword, safePageable
        );

        Page<InventoryDto.CompanyWideSkuRowRes> mapped = rows.map(row -> InventoryDto.CompanyWideSkuRowRes.builder()
                .skuCode(row.getSkuCode())
                .itemCode(row.getItemCode())
                .itemName(row.getItemName())
                .parentCategory(row.getParentCategory() == null ? "" : row.getParentCategory())
                .childCategory(row.getChildCategory() == null ? "" : row.getChildCategory())
                .color(row.getColor())
                .size(row.getSize())
                .actualStock(n(row.getActualStock()))
                .availableStock(n(row.getAvailableStock()))
                .safetyStock(n(row.getSafetyStock()))
                .status(row.getStatus())
                .build());

        // status 필터는 페이지 후 클라이언트 측 후처리 (페이징 부정확 양해 — TODO: HAVING 정교화)
        if (safeStatus != null) {
            List<InventoryDto.CompanyWideSkuRowRes> filtered = mapped.getContent().stream()
                    .filter(r -> safeStatus.equals(r.getStatus()))
                    .toList();
            Page<InventoryDto.CompanyWideSkuRowRes> filteredPage = new org.springframework.data.domain.PageImpl<>(
                    filtered, mapped.getPageable(), mapped.getTotalElements()
            );
            List<InventoryDto.LocationOptionRes> locationOptions = buildLocationOptions(locationType);
            return InventoryDto.CompanyWideSkuPageRes.from(filteredPage, locationOptions);
        }

        List<InventoryDto.LocationOptionRes> locationOptions = buildLocationOptions(locationType);
        return InventoryDto.CompanyWideSkuPageRes.from(mapped, locationOptions);
    }

    // 전사 재고 SKU 칩 필터용 facets — 거점/카테고리/검색 조건 안의 가능한 색상/사이즈 목록.
    @Transactional(readOnly = true)
    public InventoryDto.CompanyWideSkuFacetsRes findCompanyWideSkuFacets(LocationType locationType,
                                                                          List<Long> locationIds,
                                                                          String parentCategory,
                                                                          String childCategory,
                                                                          String keyword) {
        String locationTypeStr = locationType == null ? null : locationType.name();
        boolean hasLocationIds = locationIds != null && !locationIds.isEmpty();
        List<Long> safeLocationIds = hasLocationIds ? locationIds : List.of(-1L);
        String safeParent = (parentCategory == null || parentCategory.isBlank()) ? null : parentCategory.trim();
        String safeChild = (childCategory == null || childCategory.isBlank()) ? null : childCategory.trim();
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        List<String> colors = inventoryRepository.findCompanyWideSkuColors(
                locationTypeStr, hasLocationIds, safeLocationIds, safeParent, safeChild, safeKeyword);
        List<String> sizes = inventoryRepository.findCompanyWideSkuSizes(
                locationTypeStr, hasLocationIds, safeLocationIds, safeParent, safeChild, safeKeyword);
        return InventoryDto.CompanyWideSkuFacetsRes.builder()
                .colors(colors)
                .sizes(sizes)
                .build();
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
