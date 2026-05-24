package org.example.stockitbe.store.sale;

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
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.product.model.ProductStatus;
import org.example.stockitbe.store.sale.model.dto.StoreSaleDto;
import org.example.stockitbe.store.sale.model.entity.StoreSaleHeader;
import org.example.stockitbe.store.sale.model.entity.StoreSaleItem;
import org.example.stockitbe.store.sale.repository.StoreSaleHeaderRepository;
import org.example.stockitbe.store.sale.repository.StoreSaleItemRepository;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreSaleService {

    private static final DateTimeFormatter SALE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StoreSaleHeaderRepository headerRepository;
    private final StoreSaleItemRepository itemRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    // Phase 2 — 판매로 재고 차감 후 안전재고/품절 평가 위임. 알림 발행은 InventoryService 내부에서 ApplicationEvent 로 처리
    private final InventoryService inventoryService;

    // 판매
    @Transactional
    public StoreSaleDto.SaleRes create(StoreSaleDto.SaleReq dto, AuthUserDetails me) {
        // 1) 요청 데이터(판매 SKU 존재 여부)를 검증한다.
        validateCreateRequest(dto);

        // 2) 매장/카테고리 기준 데이터를 조회한다.
        Infrastructure store = resolveStore(me);
        CategoryLookup categoryLookup = buildCategoryLookup();

        // 3) 라인별로 SKU/재고/상품 정보를 조회하고 재고 락 기반 검증을 수행한다.
        List<LineContext> contexts = new ArrayList<>();
        for (StoreSaleDto.SaleLineReq line : dto.getItems()) {
            contexts.add(buildLineContext(line, store, categoryLookup));
        }

        // 4) 모든 검증 통과 후 재고를 차감한다.
        applyInventoryDeductions(contexts);

        // 4-1) Phase 2 — 차감 후 가용재고가 안전재고 미만이거나 0 이하면 매장 + 본사에 알림 발행
        //      InventoryService 의 helper 가 ProductMaster.storeSafetyStock 과 비교 후 이벤트 발행
        //      AFTER_COMMIT 이라 본 트랜잭션 롤백 시 알림도 안 나감
        for (LineContext context : contexts) {
            inventoryService.evaluateStoreStockAndAlert(
                    store.getId(),
                    store.getCode(),
                    context.sku.getId(),
                    context.sku.getSkuCode()
            );
        }

        // 5) 판매 헤더/아이템을 저장하고 판매번호를 부여한다.
        SalePersistResult persisted = persistSale(dto, store, contexts);

        // 6) 저장 결과를 응답 DTO로 변환해 반환한다.
        return StoreSaleDto.SaleRes.from(
                persisted.header,
                store.getCode(),
                persisted.items.stream().map(StoreSaleDto.SaleLineRes::from).toList()
        );
    }

    // 판매 내역 목록 조회
    @Transactional(readOnly = true)
    public List<StoreSaleDto.SaleListRes> findAll(AuthUserDetails me, LocalDate from, LocalDate to, String keyword) {
        // 1) 조회 필터(매장/기간/키워드)를 표준 형태로 정리한다.
        Infrastructure store = resolveStore(me);
        Long storeIdFilter = store.getId();
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Date fromDate = from == null ? null : Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDateExclusive = to == null ? null : Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 2) 판매 헤더와 연관 조회용 맵(매장/아이템)을 로드한다.
        List<StoreSaleHeader> headers = headerRepository.findAllByOrderBySoldAtDescIdDesc();
        if (headers.isEmpty()) {
            return List.of();
        }
        Map<Long, Infrastructure> storeById = loadStoreMap(headers);
        Map<Long, List<StoreSaleItem>> itemsByHeaderId = loadItemsByHeaderId(headers);

        // 3) 조건 필터링 후 목록 응답 DTO로 변환한다.
        List<StoreSaleDto.SaleListRes> result = new ArrayList<>();
        for (StoreSaleHeader header : headers) {
            if (!matchesHeaderFilter(header, storeIdFilter, fromDate, toDateExclusive)) {
                continue;
            }
            List<StoreSaleItem> saleItems = itemsByHeaderId.getOrDefault(header.getId(), List.of());
            if (!matchesKeyword(header, saleItems, safeKeyword)) {
                continue;
            }

            Infrastructure headerStore = storeById.get(header.getStoreId());
            result.add(StoreSaleDto.SaleListRes.from(
                    header,
                    headerStore == null ? "" : headerStore.getCode(),
                    buildHeadline(saleItems)
            ));
        }
        return result;
    }

    // 판매 내역 상세 조회
    @Transactional(readOnly = true)
    public StoreSaleDto.SaleDetailRes findDetail(String saleNo, AuthUserDetails me) {
        // 1) 판매번호로 판매 헤더를 조회한다.
        StoreSaleHeader header = headerRepository.findBySaleNo(saleNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_SALE_NOT_FOUND));
        Infrastructure store = resolveStore(me);
        assertOwnedByStore(header.getStoreId(), store.getId());

        // 2) 판매 아이템과 매장 코드를 조회한다.
        List<StoreSaleItem> items = itemRepository.findAllBySaleHeaderIdOrderByIdAsc(header.getId());
        String storeCode = infrastructureRepository.findById(header.getStoreId())
                .filter(i -> i.getLocationType() == LocationType.STORE)
                .map(Infrastructure::getCode)
                .orElse("");

        // 3) 상세 응답 DTO로 변환해 반환한다.
        return StoreSaleDto.SaleDetailRes.from(
                header,
                storeCode,
                items.stream().map(StoreSaleDto.SaleLineRes::from).toList()
        );
    }

    // ----------------------- 내부 메서드 --------------------------------

    // 사용하는 메서드: create
    // 판매 생성 요청에 판매 아이템이 비어 있는지 검증한다.
    private void validateCreateRequest(StoreSaleDto.SaleReq dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw BaseException.from(BaseResponseStatus.STORE_SALE_EMPTY_ITEMS);
        }
    }

    // 사용하는 메서드: create, findAll
    // 매장 코드로 매장 정보를 조회한다(STORE 타입만 허용).
    private Infrastructure lookupStore(String storeCode) {
        return infrastructureRepository.findByCodeAndLocationType(storeCode, LocationType.STORE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_SALE_STORE_NOT_FOUND));
    }

    // 사용하는 메서드: create, findAll, findDetail
    // 로그인 사용자 컨텍스트로 매장을 조회한다.
    private Infrastructure resolveStore(AuthUserDetails me) {
        String locationCode = me == null ? null : me.getLocationCode();
        if (locationCode == null || locationCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.STORE_SALE_STORE_NOT_FOUND);
        }
        return lookupStore(locationCode);
    }

    // 사용하는 메서드: findDetail
    // 판매 데이터의 소유 매장과 로그인 매장의 일치 여부를 검증한다.
    private void assertOwnedByStore(Long targetStoreId, Long actorStoreId) {
        if (!Objects.equals(targetStoreId, actorStoreId)) {
            throw BaseException.from(BaseResponseStatus.STORE_SALE_SCOPE_FORBIDDEN);
        }
    }

    // 사용하는 메서드: create
    // 카테고리 탐색 성능을 위해 id/code 기반 조회 맵을 구성한다.
    private CategoryLookup buildCategoryLookup() {
        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        Map<Long, Category> categoryById = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<String, Category> categoryByCode = categories.stream()
                .collect(Collectors.toMap(Category::getCode, Function.identity()));
        return new CategoryLookup(categoryById, categoryByCode);
    }

    // 사용하는 메서드: create
    // 단일 판매 라인의 수량 검증, SKU/재고/상품/카테고리 조회, 금액 계산을 수행한다.
    private LineContext buildLineContext(StoreSaleDto.SaleLineReq line, Infrastructure store, CategoryLookup categoryLookup) {
        validateLineQuantity(line);
        ProductSku sku = lookupActiveSku(line.getSkuCode());

        Inventory inventory = inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                        sku.getId(), store.getId(), InventoryStatus.NORMAL)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_SALE_INSUFFICIENT_STOCK));
        if (!inventory.canSell(line.getQuantity())) {
            throw BaseException.from(BaseResponseStatus.STORE_SALE_INSUFFICIENT_STOCK);
        }

        ProductMaster product = productMasterRepository.findByCode(sku.getProductCode())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        Category child = categoryLookup.categoryByCode.get(product.getCategoryCode());
        if (child == null) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_NOT_FOUND);
        }

        Category parent = child.getParentId() == null ? child : categoryLookup.categoryById.get(child.getParentId());
        String mainCategory = parent == null ? child.getName() : parent.getName();
        String subCategory = child.getName();
        int quantity = line.getQuantity();
        long lineAmount = sku.getUnitPrice() * quantity;

        return LineContext.builder()
                .requestLine(line)
                .sku(sku)
                .inventory(inventory)
                .product(product)
                .mainCategory(mainCategory)
                .subCategory(subCategory)
                .quantity(quantity)
                .lineAmount(lineAmount)
                .build();
    }

    // 사용하는 메서드: create
    // 판매 수량이 1 이상인지 검증한다.
    private void validateLineQuantity(StoreSaleDto.SaleLineReq line) {
        if (line.getQuantity() == null || line.getQuantity() <= 0) {
            throw BaseException.from(BaseResponseStatus.STORE_SALE_INVALID_QUANTITY);
        }
    }
    // 사용하는 메서드: create
    // SKU 코드로 활성 SKU를 조회한다(비활성 SKU는 판매 불가).
    private ProductSku lookupActiveSku(String skuCode) {
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_SALE_SKU_NOT_FOUND));
        if (sku.getStatus() != ProductStatus.ACTIVE) {
            throw BaseException.from(BaseResponseStatus.STORE_SALE_SKU_NOT_FOUND);
        }
        return sku;
    }

    // 사용하는 메서드: create
    // 검증이 완료된 라인 컨텍스트를 기준으로 재고를 차감한다.
    private void applyInventoryDeductions(List<LineContext> contexts) {
        for (LineContext context : contexts) {
            context.inventory.applySale(context.quantity);
        }
    }

    // 사용하는 메서드: create
    // 판매 헤더/아이템 저장과 판매번호 부여를 처리한다.
    private SalePersistResult persistSale(StoreSaleDto.SaleReq dto, Infrastructure store, List<LineContext> contexts) {
        int totalQuantity = contexts.stream().mapToInt(c -> c.quantity).sum();
        long totalAmount = contexts.stream().mapToLong(c -> c.lineAmount).sum();
        Date soldAt = new Date();

        StoreSaleHeader header = dto.toEntity(
                StoreSaleDto.SaleHeaderContext.builder()
                        .storeId(store.getId())
                        .soldAt(soldAt)
                        .totalQuantity(totalQuantity)
                        .totalAmount(totalAmount)
                        .temporarySaleNo("TEMP-" + UUID.randomUUID())
                        .build()
        );

        StoreSaleHeader savedHeader = headerRepository.save(header);
        savedHeader.assignSaleNo(generateSaleNo(savedHeader.getId(), soldAt));

        List<StoreSaleItem> items = contexts.stream()
                .map(context -> context.requestLine.toEntity(
                        StoreSaleDto.SaleLineContext.builder()
                                .saleHeaderId(savedHeader.getId())
                                .skuId(context.sku.getId())
                                .productCode(context.product.getCode())
                                .productName(context.product.getName())
                                .mainCategory(context.mainCategory)
                                .subCategory(context.subCategory)
                                .color(context.sku.getColor())
                                .size(context.sku.getSize())
                                .unitPrice(context.sku.getUnitPrice())
                                .build()
                ))
                .toList();
        itemRepository.saveAll(items);

        return new SalePersistResult(savedHeader, items);
    }

    // 사용하는 메서드: create
    // 판매 시각과 PK 기반으로 판매번호(SALE-yyyyMMdd-xxxxx)를 생성한다. (persistSale 내부에서 사용)
    private String generateSaleNo(Long id, Date soldAt) {
        LocalDate day = soldAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String date = day.format(SALE_DATE_FORMAT);
        String seq = String.format("%05d", id);
        return "SALE-" + date + "-" + seq;
    }

    // 사용하는 메서드: findAll
    // 목록 조회에 필요한 매장 맵(storeId -> 매장 정보)을 구성한다.
    private Map<Long, Infrastructure> loadStoreMap(List<StoreSaleHeader> headers) {
        Set<Long> storeIds = headers.stream().map(StoreSaleHeader::getStoreId).collect(Collectors.toSet());
        return infrastructureRepository.findAllById(storeIds).stream()
                .filter(i -> i.getLocationType() == LocationType.STORE)
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
    }

    // 사용하는 메서드: findAll
    // 목록 조회에 필요한 판매 아이템 맵(headerId -> 판매 아이템 목록)을 구성한다.
    private Map<Long, List<StoreSaleItem>> loadItemsByHeaderId(List<StoreSaleHeader> headers) {
        Set<Long> headerIds = headers.stream().map(StoreSaleHeader::getId).collect(Collectors.toSet());
        return itemRepository.findAllBySaleHeaderIdIn(headerIds).stream()
                .collect(Collectors.groupingBy(StoreSaleItem::getSaleHeaderId));
    }

    // 사용하는 메서드: findAll
    // 헤더가 매장/기간 필터 조건에 부합하는지 판별한다.
    private boolean matchesHeaderFilter(StoreSaleHeader header, Long storeIdFilter, Date fromDate, Date toDateExclusive) {
        if (storeIdFilter != null && !storeIdFilter.equals(header.getStoreId())) {
            return false;
        }
        if (fromDate != null && header.getSoldAt().before(fromDate)) {
            return false;
        }
        return toDateExclusive == null || header.getSoldAt().before(toDateExclusive);
    }

    // 사용하는 메서드: findAll
    // 판매번호/상품명 기준으로 키워드 검색 조건 일치 여부를 판별한다.
    private boolean matchesKeyword(StoreSaleHeader header, List<StoreSaleItem> saleItems, String safeKeyword) {
        if (safeKeyword.isBlank()) {
            return true;
        }
        String joined = (header.getSaleNo() + " " + saleItems.stream()
                .map(StoreSaleItem::getProductName)
                .collect(Collectors.joining(" "))).toLowerCase(Locale.ROOT);
        return joined.contains(safeKeyword);
    }

    // 사용하는 메서드: findAll
    // 목록 노출용 대표 문구(첫 상품명 + 나머지 개수)를 생성한다.
    private String buildHeadline(List<StoreSaleItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0).getProductName();
        }
        return items.get(0).getProductName() + " 외 " + (items.size() - 1) + "건";
    }

@Builder
    private static class LineContext {
        private StoreSaleDto.SaleLineReq requestLine;
        private ProductSku sku;
        private Inventory inventory;
        private ProductMaster product;
        private String mainCategory;
        private String subCategory;
        private Integer quantity;
        private Long lineAmount;
    }

    private record CategoryLookup(Map<Long, Category> categoryById, Map<String, Category> categoryByCode) {
    }

    private record SalePersistResult(StoreSaleHeader header, List<StoreSaleItem> items) {
    }
}
