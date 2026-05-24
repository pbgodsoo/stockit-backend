package org.example.stockitbe.store.order;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.StoreWarehouseMapRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.infrastructure.model.StoreWarehouseRole;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.product.model.ProductStatus;
import org.example.stockitbe.store.order.model.StoreOrderHistoryType;
import org.example.stockitbe.store.order.model.StoreOrderStatus;
import org.example.stockitbe.store.order.model.dto.StoreOrderDto;
import org.example.stockitbe.store.order.model.entity.StoreOrderHeader;
import org.example.stockitbe.store.order.model.entity.StoreOrderItem;
import org.example.stockitbe.store.order.model.entity.StoreOrderStatusHistory;
import org.example.stockitbe.store.inbound.repository.StoreInboundHeaderRepository;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundHeader;
import org.example.stockitbe.store.order.repository.StoreOrderHeaderRepository;
import org.example.stockitbe.store.order.repository.StoreOrderItemRepository;
import org.example.stockitbe.store.order.repository.StoreOrderStatusHistoryRepository;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreOrderService {

    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<String> CATEGORY_ORDER = List.of("상의", "바지", "치마", "아우터");

    private final StoreOrderHeaderRepository headerRepository;
    private final StoreOrderItemRepository itemRepository;
    private final StoreOrderStatusHistoryRepository historyRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final StoreWarehouseMapRepository storeWarehouseMapRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final CategoryRepository categoryRepository;
    private final StoreInboundHeaderRepository storeInboundHeaderRepository;
    private final StoreOrderApprovalOrchestrationService approvalOrchestrationService;

    // 매장 발주 요청 생성 (Create)
    // 매장 발주를 신규 생성한다.
    // 라인 검증, 매장/창고 조회, 헤더·라인 저장, 상태 이력 기록을 순차적으로 수행한다.
    @Transactional
    public StoreOrderDto.CreateRes create(StoreOrderDto.CreateReq dto, AuthUserDetails me) {
        // 1) 요청 라인의 수량/필수값을 검증한다.
        validateCreateItems(dto.getItems());
        // 2) 로그인 사용자 기준 매장과 기본 출고 창고를 해석한다.
        Infrastructure store = resolveStore(me);
        Infrastructure warehouse = lookupWarehouseFromStore(store);
        Date now = new Date();

        // 3) 발주 헤더를 저장하고 PK 기반 주문번호를 부여한다.
        StoreOrderHeader header = createHeader(dto, store, warehouse, now);
        StoreOrderHeader saved = headerRepository.save(header);
        saved.assignOrderNo(generateOrderNo(saved.getId(), now));
        // 4) 발주 라인을 저장하고 초기 상태 이력(REQUESTED)을 기록한다.
        saveCreateOrderItems(saved.getId(), dto.getItems(), buildCategoryLookup());
        appendOrderStatusHistory(saved.getId(), StoreOrderStatus.REQUESTED.name(), now,
                safe(dto.getRequestedByMemberId()), dto.getRequestedByName(), null);

        // 5) 생성 결과를 상세 응답 DTO로 조합해 반환한다.
        return buildCreateRes(saved);
    }

    // 매장 발주 요청 수정 (Update)
    // 수정 가능한 발주를 업데이트한다.
    // 헤더 합계를 재계산하고 상세 라인을 전체 교체한 뒤 이력을 남긴다.
    @Transactional
    public StoreOrderDto.UpdateRes update(String orderNo, StoreOrderDto.UpdateReq dto, AuthUserDetails me) {
        // 1) 수정 요청 라인의 수량/필수값을 검증한다.
        validateUpdateItems(dto.getItems());
        // 2) 로그인 매장 범위 내 발주인지 검증하고 대상 발주를 조회한다.
        Infrastructure store = resolveStore(me);
        StoreOrderHeader header = getOwnedOrderByOrderNo(orderNo, store.getId());
        Date now = new Date();

        // 3) 헤더 집계값/메모를 갱신한다.
        header.updateRequested(
                now,
                dto.getItems().size(),
                dto.getItems().stream().mapToInt(StoreOrderDto.UpdateLineReq::getRequestedQuantity).sum(),
                trimToNull(dto.getMemo())
        );
        // 4) 기존 라인을 삭제하고 새 라인으로 전체 교체한다.
        itemRepository.deleteAllByOrderHeaderId(header.getId());
        saveUpdateOrderItems(header.getId(), dto.getItems(), buildCategoryLookup());
        // 5) 상태 이력을 적재하고 응답을 반환한다.
        appendOrderStatusHistory(header.getId(), StoreOrderStatus.REQUESTED.name(), now,
                header.getRequestedByMemberId(), header.getRequestedByName(), "매장 발주 요청 수정");

        return StoreOrderDto.UpdateRes.from(buildCreateRes(header));
    }

    // 매장 발주 요청 취소
    @Transactional
    // 발주를 취소한다.
    // 추적을 위해 취소 사유는 필수로 검증한다.
    public StoreOrderDto.CancelRes cancel(String orderNo, StoreOrderDto.CancelReq dto, AuthUserDetails me) {
        // 1) 로그인 매장 범위 내 발주인지 검증하고 대상 발주를 조회한다.
        Infrastructure store = resolveStore(me);
        StoreOrderHeader header = getOwnedOrderByOrderNo(orderNo, store.getId());
        // 2) 취소 사유를 필수 검증한다.
        String cancelReason = trimToNull(dto.getCancelReason());
        if (cancelReason == null) throw BaseException.from(BaseResponseStatus.STORE_ORDER_CANCEL_REASON_REQUIRED);

        // 3) 상태를 CANCELLED로 전이하고 이력을 적재한다.
        header.markCancelled(cancelReason);
        appendOrderStatusHistory(header.getId(), StoreOrderStatus.CANCELLED.name(), new Date(),
                safe(dto.getCancelledByMemberId()), blankTo(dto.getCancelledByName(), header.getRequestedByName()),
                cancelReason);
        // 4) 취소 반영 결과를 반환한다.
        return StoreOrderDto.CancelRes.from(buildCreateRes(header));
    }

    // 발주 승인
    @Transactional
    public StoreOrderDto.ApproveRes approve(String orderNo, StoreOrderDto.ApproveReq dto, AuthUserDetails me) {
        // 1) 로그인 매장 범위 내 발주인지 검증한다.
        Infrastructure store = resolveStore(me);
        StoreOrderHeader header = getOwnedOrderByOrderNo(orderNo, store.getId());
        // 2) 공통 승인 내부 로직으로 위임한다.
        return approveInternal(
                header,
                safe(dto == null ? null : dto.getApprovedByMemberId()),
                blankTo(dto == null ? null : dto.getApprovedByName(), "SYSTEM"),
                "STORE_ORDER_APPROVE"
        );
    }

    // 배치 발주 승인
    @Transactional
    public StoreOrderDto.ApproveRes approveByBatch(String orderNo, String actorMemberId, String actorName, String reason) {
        // 1) 배치 대상 발주를 조회한다.
        StoreOrderHeader header = getOrderByOrderNo(orderNo);
        // 2) 공통 승인 내부 로직으로 위임한다.
        return approveInternal(header, safe(actorMemberId), blankTo(actorName, "SYSTEM"), blankTo(reason, "AUTO_BATCH_APPROVE"));
    }

    // 매장 발주 내역 목록 조회
    @Transactional(readOnly = true)
    // 발주 목록을 조회한다.
    // 매장/상태/기간/키워드 필터를 적용한다.
    public List<StoreOrderDto.ListRes> list(String status, LocalDate from, LocalDate to, String keyword, AuthUserDetails me) {
        // 1) 로그인 매장 범위를 해석하고 필터 값을 정규화한다.
        Infrastructure store = resolveStore(me);
        Long storeIdFilter = store.getId();
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Date fromDate = from == null ? null : Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDateExclusive = to == null ? null : Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        StoreOrderStatus statusFilter = parseStatus(status);

        List<StoreOrderHeader> headers = headerRepository.findAllByOrderByRequestedAtDescIdDesc();
        if (headers.isEmpty()) return List.of();

        // 2) 매장/라인 연관 데이터를 일괄 조회한다.
        Map<Long, Infrastructure> storeById = loadStoreMap(headers);
        Map<Long, List<StoreOrderItem>> itemsByHeader = loadItemsByHeader(headers);

        // 3) 필터를 적용하고 목록 응답 DTO를 구성한다.
        List<StoreOrderDto.ListRes> result = new ArrayList<>();
        for (StoreOrderHeader header : headers) {
            if (!matchesHeaderFilter(header, storeIdFilter, statusFilter, fromDate, toDateExclusive)) continue;
            List<StoreOrderItem> items = itemsByHeader.getOrDefault(header.getId(), List.of());
            if (!matchesKeyword(header, items, safeKeyword)) continue;
            Infrastructure headerStore = storeById.get(header.getStoreId());
            result.add(StoreOrderDto.ListRes.from(
                    header,
                    headerStore == null ? "" : headerStore.getCode(),
                    headerStore == null ? "" : headerStore.getName(),
                    buildHeadline(items),
                    totalInboundCount(header.getOrderNo()),
                    receivedInboundCount(header.getOrderNo()),
                    inboundProgress(header.getOrderNo())
            ));
        }
        return result;
    }

    // 매장 발주 상세 조회
    @Transactional(readOnly = true)
    // 발주 상세를 조회한다.
    // 헤더, 아이템, 상태 이력을 결합해 응답한다.
    public StoreOrderDto.DetailRes detail(String orderNo, AuthUserDetails me) {
        // 1) 로그인 매장 범위를 해석한다.
        Infrastructure store = resolveStore(me);
        // 2) 소유 발주 상세 응답을 조합해 반환한다.
        return StoreOrderDto.DetailRes.from(buildCreateRes(getOwnedOrderByOrderNo(orderNo, store.getId())));
    }

    // 매장 발주 분석 조회
    @Transactional(readOnly = true)
    // 발주 분석 데이터를 조회한다.
    // 상태별 건수, 상위 SKU, 카테고리 분포를 집계한다.
    public StoreOrderDto.AnalyticsRes analytics(LocalDate from, LocalDate to, AuthUserDetails me) {
        // 1) 기간 조건 기준 발주와 라인 데이터를 조회한다.
        List<StoreOrderDto.ListRes> orders = list(null, from, to, null, me);
        List<StoreOrderHeader> headers = orders.stream().map(o -> getOrderByOrderNo(o.getOrderId())).toList();
        Map<Long, List<StoreOrderItem>> itemsByHeader = loadItemsByHeader(headers);

        // 2) 상태별 건수 집계를 계산한다.
        int requestedCount = 0;
        int approvedCount = 0;
        int completedCount = 0;
        int cancelledCount = 0;
        for (StoreOrderHeader h : headers) {
            if (h.getStatus() == StoreOrderStatus.REQUESTED) requestedCount++;
            else if (h.getStatus() == StoreOrderStatus.APPROVED) approvedCount++;
            else if (h.getStatus() == StoreOrderStatus.COMPLETED) completedCount++;
            else if (h.getStatus() == StoreOrderStatus.CANCELLED) cancelledCount++;
        }

        Map<String, SkuAgg> skuAgg = new HashMap<>();
        Map<String, CategoryAgg> categoryAgg = new HashMap<>();
        // 3) SKU/카테고리별 요청수량 집계를 계산한다.
        for (StoreOrderHeader header : headers) {
            for (StoreOrderItem item : itemsByHeader.getOrDefault(header.getId(), List.of())) {
                String skuKey = item.getSkuCode();
                SkuAgg s = skuAgg.getOrDefault(skuKey, new SkuAgg(item.getSkuCode(), item.getProductName(),
                        item.getMainCategory() + " > " + item.getSubCategory(), 0, 0));
                s.requestedQuantity += item.getRequestedQuantity();
                s.orderCount += 1;
                skuAgg.put(skuKey, s);

                String categoryKey = item.getMainCategory() + "|" + item.getSubCategory();
                CategoryAgg c = categoryAgg.getOrDefault(categoryKey,
                        new CategoryAgg(item.getMainCategory(), item.getSubCategory(),
                                item.getMainCategory() + " > " + item.getSubCategory(), 0));
                c.requestedQuantity += item.getRequestedQuantity();
                categoryAgg.put(categoryKey, c);
            }
        }

        // 4) 분석 응답 DTO를 구성해 반환한다.
        return StoreOrderDto.AnalyticsRes.builder()
                .totalOrders(headers.size())
                .totalRequestedQuantity(headers.stream().mapToInt(StoreOrderHeader::getTotalRequestedQuantity).sum())
                .requestedCount(requestedCount)
                .approvedCount(approvedCount)
                .completedCount(completedCount)
                .cancelledCount(cancelledCount)
                .topSkus(
                        skuAgg.values().stream()
                                .sorted((a, b) -> Integer.compare(b.requestedQuantity, a.requestedQuantity))
                                .limit(5)
                                .map(s -> StoreOrderDto.AnalyticsSkuRes.builder()
                                        .skuCode(s.skuCode)
                                        .productName(s.productName)
                                        .categoryLabel(s.categoryLabel)
                                        .requestedQuantity(s.requestedQuantity)
                                        .orderCount(s.orderCount)
                                        .build())
                                .toList()
                )
                .categoryBreakdown(
                        categoryAgg.values().stream()
                                .sorted((a, b) -> compareCategory(a.mainCategory, b.mainCategory))
                                .map(c -> StoreOrderDto.AnalyticsCategoryRes.builder()
                                        .mainCategory(c.mainCategory)
                                        .subCategory(c.subCategory)
                                        .label(c.label)
                                        .requestedQuantity(c.requestedQuantity)
                                        .build())
                                .toList()
                )
                .build();
    }

    // ---------------------------- 내부 메서드 --------------------------------

    // 사용하는 메서드: create
    // 생성 요청 품목 유효성을 검증한다.
    private void validateCreateItems(List<StoreOrderDto.CreateLineReq> items) {
        if (items == null || items.isEmpty()) throw BaseException.from(BaseResponseStatus.STORE_ORDER_EMPTY_ITEMS);
        for (StoreOrderDto.CreateLineReq item : items) {
            if (item.getRequestedQuantity() == null || item.getRequestedQuantity() <= 0) {
                throw BaseException.from(BaseResponseStatus.STORE_ORDER_INVALID_QUANTITY);
            }
        }
    }

    // 사용하는 메서드: update
    // 수정 요청 품목 유효성을 검증한다.
    private void validateUpdateItems(List<StoreOrderDto.UpdateLineReq> items) {
        if (items == null || items.isEmpty()) throw BaseException.from(BaseResponseStatus.STORE_ORDER_EMPTY_ITEMS);
        for (StoreOrderDto.UpdateLineReq item : items) {
            if (item.getRequestedQuantity() == null || item.getRequestedQuantity() <= 0) {
                throw BaseException.from(BaseResponseStatus.STORE_ORDER_INVALID_QUANTITY);
            }
        }
    }

    // 사용하는 메서드: create, list
    // 매장 코드를 기준으로 STORE 인프라를 조회한다.
    private Infrastructure lookupStore(String storeCode) {
        return infrastructureRepository.findByCodeAndLocationType(storeCode, LocationType.STORE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_STORE_NOT_FOUND));
    }

    // 사용하는 메서드: create, update, cancel, approve, list, detail, analytics
    // 로그인 사용자 컨텍스트로 매장을 조회한다.
    private Infrastructure resolveStore(AuthUserDetails me) {
        String locationCode = me == null ? null : me.getLocationCode();
        if (locationCode == null || locationCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.STORE_ORDER_STORE_NOT_FOUND);
        }
        return lookupStore(locationCode);
    }

    // 사용하는 메서드: create
    // 매장에 매핑된 WAREHOUSE 인프라를 조회한다.
    private Infrastructure lookupWarehouseFromStore(Infrastructure store) {
        return storeWarehouseMapRepository.findByStoreAndRole(store, StoreWarehouseRole.PRIMARY)
                .map(map -> map.getWarehouse())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_WAREHOUSE_NOT_FOUND));
    }

    // 사용하는 메서드: create, update
    // 카테고리 매핑 조회 맵을 생성한다.
    private CategoryLookup buildCategoryLookup() {
        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        Map<Long, Category> categoryById = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<String, Category> categoryByCode = categories.stream()
                .collect(Collectors.toMap(Category::getCode, Function.identity()));
        return new CategoryLookup(categoryById, categoryByCode);
    }

    // 사용하는 메서드: create
    // 생성 라인을 저장한다.
    // 변환 규칙에 따라 CreateLineReq.toEntity(CreateLineContext)를 직접 사용한다.
    private void saveCreateOrderItems(Long headerId, List<StoreOrderDto.CreateLineReq> items, CategoryLookup categoryLookup) {
        List<StoreOrderItem> entities = new ArrayList<>();
        for (StoreOrderDto.CreateLineReq req : items) {
            ResolvedItem resolved = resolveSingleItem(req.getSkuCode(), categoryLookup);
            StoreOrderDto.CreateLineContext context = StoreOrderDto.CreateLineContext.builder()
                    .orderHeaderId(headerId)
                    .skuId(resolved.sku.getId())
                    .productCode(resolved.product.getCode())
                    .productName(resolved.product.getName())
                    .mainCategory(resolved.mainCategory)
                    .subCategory(resolved.subCategory)
                    .color(resolved.sku.getColor())
                    .size(resolved.sku.getSize())
                    .unitPrice(resolved.sku.getUnitPrice())
                    .build();
            entities.add(req.toEntity(context));
        }
        itemRepository.saveAll(entities);
    }

    // 사용하는 메서드: update
    // 수정 요청 아이템을 스냅샷 저장용 컨텍스트로 해석한다.
    private void saveUpdateOrderItems(Long headerId, List<StoreOrderDto.UpdateLineReq> items, CategoryLookup categoryLookup) {
        List<StoreOrderItem> entities = new ArrayList<>();
        for (StoreOrderDto.UpdateLineReq req : items) {
            ResolvedItem resolved = resolveSingleItem(req.getSkuCode(), categoryLookup);
            StoreOrderDto.UpdateLineContext context = StoreOrderDto.UpdateLineContext.builder()
                    .orderHeaderId(headerId)
                    .skuId(resolved.sku.getId())
                    .productCode(resolved.product.getCode())
                    .productName(resolved.product.getName())
                    .mainCategory(resolved.mainCategory)
                    .subCategory(resolved.subCategory)
                    .color(resolved.sku.getColor())
                    .size(resolved.sku.getSize())
                    .unitPrice(resolved.sku.getUnitPrice())
                    .build();
            entities.add(req.toEntity(context));
        }
        itemRepository.saveAll(entities);
    }

    // 사용하는 메서드: create, update
    // SKU/상품/카테고리를 조회해 단일 아이템 컨텍스트를 구성한다.
    private ResolvedItem resolveSingleItem(String skuCode, CategoryLookup categoryLookup) {
        ProductSku sku = lookupActiveSku(skuCode);
        ProductMaster product = productMasterRepository.findByCode(sku.getProductCode())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        Category child = categoryLookup.categoryByCode.get(product.getCategoryCode());
        if (child == null) throw BaseException.from(BaseResponseStatus.CATEGORY_NOT_FOUND);
        Category parent = child.getParentId() == null ? child : categoryLookup.categoryById.get(child.getParentId());
        String mainCategory = parent == null ? child.getName() : parent.getName();
        String subCategory = child.getName();
        return ResolvedItem.builder()
                .sku(sku)
                .product(product)
                .mainCategory(mainCategory)
                .subCategory(subCategory)
                .build();
    }

    // 사용하는 메서드: create, update
    // 활성 SKU를 조회한다.
    private ProductSku lookupActiveSku(String skuCode) {
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_SKU_NOT_FOUND));
        if (sku.getStatus() != ProductStatus.ACTIVE) {
            throw BaseException.from(BaseResponseStatus.STORE_ORDER_SKU_NOT_FOUND);
        }
        return sku;
    }

    // 사용하는 메서드: create
    // 발주 헤더 엔티티를 생성한다.
    private StoreOrderHeader createHeader(StoreOrderDto.CreateReq dto, Infrastructure store,
                                          Infrastructure warehouse, Date now) {
        return dto.toEntity(
                StoreOrderDto.CreateHeaderContext.builder()
                        .storeId(store.getId())
                        .warehouseId(warehouse.getId())
                        .requestedAt(now)
                        .totalSkuCount(dto.getItems().size())
                        .totalRequestedQuantity(dto.getItems().stream()
                                .mapToInt(StoreOrderDto.CreateLineReq::getRequestedQuantity)
                                .sum())
                        .memo(trimToNull(dto.getMemo()))
                        .temporaryOrderNo("TEMP-" + UUID.randomUUID())
                        .build()
        );
    }

    // 사용하는 메서드: create, update, cancel
    // 주문 상태 이력을 1건 저장한다.
    private void appendOrderStatusHistory(Long headerId, String status, Date changedAt,
                                          String changedByMemberId, String changedByName, String reason) {
        historyRepository.save(
                StoreOrderStatusHistory.builder()
                        .orderHeaderId(headerId)
                        .historyType(StoreOrderHistoryType.ORDER_STATUS)
                        .status(status)
                        .changedAt(changedAt)
                        .changedByMemberId(changedByMemberId)
                        .changedByName(changedByName)
                        .reason(reason)
                        .build()
        );
    }

    // 사용하는 메서드: approve, approveByBatch
    // 발주 승인 핵심 처리
    private StoreOrderDto.ApproveRes approveInternal(StoreOrderHeader header, String actorMemberId, String actorName, String reason) {
        // 1) 발주 상태를 APPROVED로 전이한다.
        Date now = new Date();
        header.markApproved();

        // 2) 출고/입고 오케스트레이션을 실행한다.
        List<StoreOrderItem> items = itemRepository.findAllByOrderHeaderIdOrderByIdAsc(header.getId());
        approvalOrchestrationService.createOutboundInboundForApprovedOrder(header, items, actorMemberId, actorName, reason);

        // 3) 승인 상태 이력을 적재하고 응답을 반환한다.
        appendOrderStatusHistory(header.getId(), StoreOrderStatus.APPROVED.name(), now,
                actorMemberId, actorName, reason);
        return StoreOrderDto.ApproveRes.from(buildCreateRes(header));
    }

    // 사용하는 메서드: detail, create, update, cancel
    // 헤더/아이템/이력을 결합해 생성 응답 DTO를 구성한다.
    private StoreOrderDto.CreateRes buildCreateRes(StoreOrderHeader header) {
        Infrastructure store = infrastructureRepository.findById(header.getStoreId()).orElse(null);
        List<StoreOrderItem> items = itemRepository.findAllByOrderHeaderIdOrderByIdAsc(header.getId());
        List<StoreOrderStatusHistory> history = historyRepository.findAllByOrderHeaderIdOrderByChangedAtAscIdAsc(header.getId());
        List<StoreInboundHeader> inboundHeaders = storeInboundHeaderRepository.findAllBySourceRefNo(header.getOrderNo());
        return StoreOrderDto.CreateRes.from(
                header,
                store == null ? "" : store.getCode(),
                store == null ? "" : store.getName(),
                inboundHeaders.size(),
                (int) inboundHeaders.stream().filter(h -> h.getStatus() == StoreInboundStatus.RECEIVED).count(),
                toInboundProgress(
                        inboundHeaders.size(),
                        (int) inboundHeaders.stream().filter(h -> h.getStatus() == StoreInboundStatus.RECEIVED).count()
                ),
                inboundHeaders.stream().map(this::toInboundSummary).toList(),
                items.stream().map(StoreOrderDto.CreateLineRes::from).toList(),
                history.stream().map(StoreOrderDto.CreateHistoryRes::from).toList()
        );
    }

    // 사용하는 메서드: create
    // PK와 요청일을 기준으로 주문번호를 생성한다.
    private String generateOrderNo(Long id, Date requestedAt) {
        LocalDate day = requestedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String date = day.format(ORDER_DATE_FORMAT);
        return "SOR-" + date + "-" + String.format("%05d", id);
    }

    // 사용하는 메서드: list, analytics
    // 카테고리 정렬 우선순위를 비교한다.
    private int compareCategory(String a, String b) {
        int ai = CATEGORY_ORDER.indexOf(a);
        int bi = CATEGORY_ORDER.indexOf(b);
        int nAi = ai == -1 ? CATEGORY_ORDER.size() : ai;
        int nBi = bi == -1 ? CATEGORY_ORDER.size() : bi;
        if (nAi != nBi) return Integer.compare(nAi, nBi);
        return a.compareToIgnoreCase(b);
    }

    // 사용하는 메서드: list
    // 상태 문자열 필터를 enum으로 변환한다.
    private StoreOrderStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "전체".equals(status)) return null;
        try {
            return StoreOrderStatus.valueOf(status);
        } catch (Exception e) {
            return null;
        }
    }

    // 사용하는 메서드: list
    // 목록 노출용 헤드라인을 생성한다.
    private String buildHeadline(List<StoreOrderItem> items) {
        if (items == null || items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0).getProductName();
        return items.get(0).getProductName() + " 외 " + (items.size() - 1) + "건";
    }

    // 사용하는 메서드: list
    // 키워드 조건에 맞는 주문인지 판단한다.
    private boolean matchesKeyword(StoreOrderHeader header, List<StoreOrderItem> items, String safeKeyword) {
        if (safeKeyword.isBlank()) return true;
        String joined = (header.getOrderNo() + " " + items.stream()
                .map(i -> i.getProductName() + " " + i.getMainCategory() + " " + i.getSubCategory())
                .collect(Collectors.joining(" "))).toLowerCase(Locale.ROOT);
        return joined.contains(safeKeyword);
    }

    // 사용하는 메서드: list
    // 헤더 필터 조건 일치 여부를 판단한다.
    private boolean matchesHeaderFilter(StoreOrderHeader header, Long storeIdFilter, StoreOrderStatus statusFilter,
                                        Date fromDate, Date toDateExclusive) {
        if (storeIdFilter != null && !storeIdFilter.equals(header.getStoreId())) return false;
        if (statusFilter != null && statusFilter != header.getStatus()) return false;
        if (fromDate != null && header.getRequestedAt().before(fromDate)) return false;
        return toDateExclusive == null || header.getRequestedAt().before(toDateExclusive);
    }

    // 사용하는 메서드: list
    // 헤더 목록 기준 매장 조회 맵을 구성한다.
    private Map<Long, Infrastructure> loadStoreMap(List<StoreOrderHeader> headers) {
        Set<Long> storeIds = headers.stream().map(StoreOrderHeader::getStoreId).collect(Collectors.toSet());
        return infrastructureRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
    }

    // 사용하는 메서드: list, analytics
    // 헤더 목록 기준 아이템 그룹 맵을 구성한다.
    private Map<Long, List<StoreOrderItem>> loadItemsByHeader(List<StoreOrderHeader> headers) {
        Set<Long> headerIds = headers.stream().map(StoreOrderHeader::getId).collect(Collectors.toSet());
        if (headerIds.isEmpty()) return Map.of();
        return itemRepository.findAllByOrderHeaderIdIn(headerIds).stream()
                .collect(Collectors.groupingBy(StoreOrderItem::getOrderHeaderId));
    }

    // 사용하는 메서드: update, cancel, detail, analytics
    // 주문번호 기준 헤더를 조회한다.
    private StoreOrderHeader getOrderByOrderNo(String orderNo) {
        return headerRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_NOT_FOUND));
    }

    // 사용하는 메서드: update, cancel, approve, detail
    // 주문 소유 매장과 로그인 매장의 일치 여부를 검증한다.
    private StoreOrderHeader getOwnedOrderByOrderNo(String orderNo, Long actorStoreId) {
        StoreOrderHeader header = getOrderByOrderNo(orderNo);
        if (!Objects.equals(header.getStoreId(), actorStoreId)) {
            throw BaseException.from(BaseResponseStatus.STORE_ORDER_SCOPE_FORBIDDEN);
        }
        return header;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String blankTo(String s, String fallback) {
        if (s == null || s.isBlank()) return fallback;
        return s;
    }

    // 사용하는 메서드: list
    // 발주번호 기준 연계 입고 전체 건수를 계산한다.
    private int totalInboundCount(String orderNo) {
        return storeInboundHeaderRepository.findAllBySourceRefNo(orderNo).size();
    }

    // 사용하는 메서드: list
    // 발주번호 기준 RECEIVED 상태 입고 건수를 계산한다.
    private int receivedInboundCount(String orderNo) {
        return (int) storeInboundHeaderRepository.findAllBySourceRefNo(orderNo).stream()
                .filter(h -> h.getStatus() == StoreInboundStatus.RECEIVED)
                .count();
    }

    // 사용하는 메서드: list
    // 입고 진행 집계값을 파생 enum으로 변환한다.
    private StoreOrderDto.InboundProgress inboundProgress(String orderNo) {
        int total = totalInboundCount(orderNo);
        int received = receivedInboundCount(orderNo);
        return toInboundProgress(total, received);
    }

    // 사용하는 메서드: buildCreateRes, inboundProgress
    // 총 입고건/수령건 수치로 NOT_STARTED/PARTIAL/FULL 상태를 계산한다.
    private StoreOrderDto.InboundProgress toInboundProgress(int total, int received) {
        if (total <= 0 || received <= 0) return StoreOrderDto.InboundProgress.NOT_STARTED;
        if (received >= total) return StoreOrderDto.InboundProgress.FULL;
        return StoreOrderDto.InboundProgress.PARTIAL;
    }

    // 사용하는 메서드: buildCreateRes
    // 입고 헤더 엔티티를 발주 상세 응답용 요약 DTO로 변환한다.
    private StoreOrderDto.InboundSummaryRes toInboundSummary(StoreInboundHeader inbound) {
        return StoreOrderDto.InboundSummaryRes.builder()
                .inboundNo(inbound.getInboundNo())
                .outboundNo(inbound.getOutboundNo())
                .fromWarehouseId(inbound.getFromWarehouseId())
                .expectedArrivalAt(inbound.getExpectedArrivalAt())
                .totalExpectedQuantity(inbound.getTotalExpectedQuantity())
                .status(inbound.getStatus())
                .build();
    }

    private record CategoryLookup(Map<Long, Category> categoryById, Map<String, Category> categoryByCode) {}

    @Builder
    private static class ResolvedItem {
        private ProductSku sku;
        private ProductMaster product;
        private String mainCategory;
        private String subCategory;
    }

    private static class SkuAgg {
        private final String skuCode;
        private final String productName;
        private final String categoryLabel;
        private int requestedQuantity;
        private int orderCount;

        private SkuAgg(String skuCode, String productName, String categoryLabel, int requestedQuantity, int orderCount) {
            this.skuCode = skuCode;
            this.productName = productName;
            this.categoryLabel = categoryLabel;
            this.requestedQuantity = requestedQuantity;
            this.orderCount = orderCount;
        }
    }

    private static class CategoryAgg {
        private final String mainCategory;
        private final String subCategory;
        private final String label;
        private int requestedQuantity;

        private CategoryAgg(String mainCategory, String subCategory, String label, int requestedQuantity) {
            this.mainCategory = mainCategory;
            this.subCategory = subCategory;
            this.label = label;
            this.requestedQuantity = requestedQuantity;
        }
    }
}
