package org.example.stockitbe.hq.circularsale;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerTransaction;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerTransactionRepository;
import org.example.stockitbe.hq.circularsale.model.CircularSaleStatus;
import org.example.stockitbe.hq.circularsale.model.dto.CircularSaleDto;
import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleHeader;
import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleItem;
import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleItemMaterial;
import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleStatusHistory;
import org.example.stockitbe.hq.circularsale.repository.CircularSaleHeaderRepository;
import org.example.stockitbe.hq.circularsale.repository.CircularSaleItemMaterialRepository;
import org.example.stockitbe.hq.circularsale.repository.CircularSaleItemRepository;
import org.example.stockitbe.hq.circularsale.repository.CircularSaleStatusHistoryRepository;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.inventory.InventoryRepository;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductMaterialComposition;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.product.model.ProductStatus;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.example.stockitbe.warehouse.outbound.model.OutboundDestinationType;
import org.example.stockitbe.warehouse.outbound.model.OutboundSourceType;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundItem;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundHeaderRepository;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundItemRepository;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundStatusHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
public class CircularSaleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int DECIMAL_SCALE_3 = 3;

    private final CircularSaleHeaderRepository saleHeaderRepository;
    private final CircularSaleItemRepository saleItemRepository;
    private final CircularSaleItemMaterialRepository saleItemMaterialRepository;
    private final CircularSaleStatusHistoryRepository saleStatusHistoryRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final CircularBuyerRepository circularBuyerRepository;
    // Phase 3: 실제 판매 시 ESG 점수 가산용 거래 row 를 함께 INSERT 하기 위한 Repository
    private final CircularBuyerTransactionRepository transactionRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final WhOutboundHeaderRepository outboundHeaderRepository;
    private final WhOutboundItemRepository outboundItemRepository;
    private final WhOutboundStatusHistoryRepository outboundStatusHistoryRepository;

    // 순환재고 판매 생성
    @Transactional
    public CircularSaleDto.CreateRes create(CircularSaleDto.CreateReq request, AuthUserDetails me) {
        // 1) 요청 본문 최소 검증(빈 라인/소재구분 누락 차단)
        validateCreateRequest(request);
        // 2) saleType 결정 — null 이면 "SALE" 기본
        String saleType = request.getSaleType() == null ? "SALE" : request.getSaleType().toUpperCase(Locale.ROOT);
        // 3) 거래처/카테고리 기준 데이터 로드 (DONATION은 buyer 조회 스킵)
        CircularBuyer buyer = "DONATION".equals(saleType) ? null : findBuyerByCode(request.getBuyerCode());
        CategoryLookup categoryLookup = buildCategoryLookup();

        // 4) 라인별 SKU/재고/소재 스냅샷 컨텍스트 구성 + 단일 창고 검증
        List<LineContext> contexts = buildLineContexts(request.getItems(), request.getMaterialType(), categoryLookup);
        Long warehouseId = resolveSingleWarehouse(contexts);
        for (LineContext context : contexts) {
            context.availableQuantitySnapshot = Math.max(
                    0,
                    context.inventory.getAvailableQuantity() == null ? 0 : context.inventory.getAvailableQuantity()
            );
        }

        // 5) 재고를 즉시 차감하지 않고 예약 수량으로 선반영
        applyInventoryReservations(contexts);
        // 6) 판매/출고/상태이력을 한 트랜잭션으로 동시 저장
        SalePersistResult persisted = persistSaleAndOutbound(request, me, buyer, warehouseId, contexts, saleType);

        // 7) 응답 DTO 조립 후 반환
        List<CircularSaleDto.LineRes> lines = mapLineRes(persisted.items, persisted.materialRows);
        return CircularSaleDto.CreateRes.from(
                persisted.header,
                buyer != null ? buyer.getCode() : null,
                buyer != null ? buyer.getCompanyName() : request.getDoneeName(),
                persisted.outbound.getOutboundNo(),
                persisted.outbound.getStatus(),
                lines
        );
    }

    // 순환재고 판매 목록 조회
    @Transactional(readOnly = true)
    public CircularSaleDto.ListPageRes list(
            Integer page, Integer size, String sort,
            LocalDate from, LocalDate to,
            String buyerCode, String materialType, String keyword, String saleType
    ) {
        // 1) 페이지/정렬 파라미터 정규화
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 20 : Math.min(Math.max(1, size), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize, parseSort(sort));

        // 2) 검색 조건 정규화(기간/키워드/소재/거래처코드/판매유형)
        Date fromDate = from == null ? null : Date.from(from.atStartOfDay(KST).toInstant());
        Date toDateExclusive = to == null ? null : Date.from(to.plusDays(1).atStartOfDay(KST).toInstant());
        String safeKeyword = keyword == null ? null : keyword.trim();
        String safeMaterialType = materialType == null ? null : materialType.trim();
        String safeSaleType = blankToNull(saleType);
        Long buyerId = resolveBuyerIdOrNull(buyerCode);

        // saleType 조건은 search() 에 추가 (CircularSaleHeaderRepository.search() 시그니처 업데이트 필요)
        Page<CircularSaleHeader> headers = saleHeaderRepository.search(
                fromDate, toDateExclusive, buyerId, blankToNull(safeMaterialType), blankToNull(safeKeyword),
                safeSaleType, pageable
        );
        if (headers.isEmpty()) {
            return CircularSaleDto.ListPageRes.from(new PageImpl<>(List.of(), pageable, 0));
        }

        // 3) 목록 렌더링에 필요한 연관 데이터(거래처/출고/아이템) 배치 조회
        //    DONATION 헤더는 buyerId=null 이므로 null 제외 후 조회
        Map<Long, CircularBuyer> buyerById = circularBuyerRepository.findAllById(
                headers.getContent().stream().map(CircularSaleHeader::getBuyerId)
                        .filter(Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(CircularBuyer::getId, Function.identity()));
        Map<Long, WhOutboundHeader> outboundById = outboundHeaderRepository.findAllById(
                headers.getContent().stream()
                        .map(CircularSaleHeader::getOutboundHeaderId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(WhOutboundHeader::getId, Function.identity()));
        Map<Long, Infrastructure> warehouseById = infrastructureRepository.findAllById(
                outboundById.values().stream()
                        .map(WhOutboundHeader::getWarehouseId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Infrastructure::getId, Function.identity()));

        Map<Long, List<CircularSaleItem>> itemsByHeader = saleItemRepository.findAllBySaleHeaderIdIn(
                headers.getContent().stream().map(CircularSaleHeader::getId).toList()
        ).stream().collect(Collectors.groupingBy(CircularSaleItem::getSaleHeaderId));

        // 4) 헤더 단위 목록 행 DTO로 매핑
        List<CircularSaleDto.ListRowRes> rows = new ArrayList<>();
        for (CircularSaleHeader header : headers.getContent()) {
            CircularBuyer buyer = buyerById.get(header.getBuyerId());
            WhOutboundHeader outbound = outboundById.get(header.getOutboundHeaderId());
            Infrastructure outboundWarehouse = outbound == null ? null : warehouseById.get(outbound.getWarehouseId());
            List<CircularSaleItem> items = itemsByHeader.getOrDefault(header.getId(), List.of());
            rows.add(CircularSaleDto.ListRowRes.builder()
                    .saleId(header.getId())
                    .saleNo(header.getSaleNo())
                    .status(header.getStatus())
                    .outboundNo(outbound == null ? null : outbound.getOutboundNo())
                    .outboundWarehouseCode(outboundWarehouse == null ? null : outboundWarehouse.getCode())
                    .outboundWarehouseName(outboundWarehouse == null ? null : outboundWarehouse.getName())
                    .outboundStatus(outbound == null ? null : outbound.getStatus())
                    .soldAt(header.getSoldAt())
                    .completedAt(header.getCompletedAt())
                    .buyerCode(buyer == null ? null : buyer.getCode())
                    .buyerName(buyer == null ? null : buyer.getCompanyName())
                    .buyerIndustryGroup(buyer == null ? null : buyer.getIndustryGroup())
                    .materialType(header.getMaterialType())
                    .totalSkuCount(header.getTotalSkuCount())
                    .totalActualWeightKg(header.getTotalActualWeightKg())
                    .totalSoldQuantity(header.getTotalSoldQuantity())
                    .totalAmount(header.getTotalAmount())
                    .headline(buildHeadline(items))
                    .saleType(header.getSaleType())
                    .doneeName(header.getDoneeName())
                    .build());
        }
        // 5) 페이지 메타 포함 응답으로 반환함
        return CircularSaleDto.ListPageRes.from(new PageImpl<>(rows, pageable, headers.getTotalElements()));
    }

    // 순환재고 판매 상세 조회
    @Transactional(readOnly = true)
    public CircularSaleDto.DetailRes detail(Long saleId) {
        // 1) 판매 헤더/거래처/출고 헤더 조회
        CircularSaleHeader header = saleHeaderRepository.findById(saleId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.CIRCULAR_SALE_NOT_FOUND));
        // DONATION은 buyerId=null 이므로 null-safe 처리
        CircularBuyer buyer = header.getBuyerId() == null ? null
                : circularBuyerRepository.findById(header.getBuyerId()).orElse(null);
        WhOutboundHeader outbound = header.getOutboundHeaderId() == null ? null
                : outboundHeaderRepository.findById(header.getOutboundHeaderId()).orElse(null);

        // 2) 판매 라인 + 소재 스냅샷 조회 후 라인 응답으로 변환
        List<CircularSaleItem> items = saleItemRepository.findAllBySaleHeaderIdOrderByIdAsc(header.getId());
        List<Long> itemIds = items.stream().map(CircularSaleItem::getId).toList();
        Map<Long, List<CircularSaleDto.MaterialRes>> materialsByItem = saleItemMaterialRepository
                .findAllBySaleItemIdInOrderBySortOrderAscIdAsc(itemIds).stream()
                .collect(Collectors.groupingBy(
                        CircularSaleItemMaterial::getSaleItemId,
                        Collectors.mapping(CircularSaleDto.MaterialRes::from, Collectors.toList())
                ));
        Infrastructure outboundWarehouse = header.getWarehouseId() == null ? null
                : infrastructureRepository.findById(header.getWarehouseId()).orElse(null);

        List<CircularSaleDto.LineRes> lines = items.stream()
                .map(item -> {
                    CircularSaleDto.LineRes base = CircularSaleDto.LineRes.from(item, materialsByItem.getOrDefault(item.getId(), List.of()));
                    return CircularSaleDto.LineRes.builder()
                            .itemId(base.getItemId())
                            .inventoryId(base.getInventoryId())
                            .skuCode(base.getSkuCode())
                            .productCode(base.getProductCode())
                            .productName(base.getProductName())
                            .mainCategory(base.getMainCategory())
                            .subCategory(base.getSubCategory())
                            .color(base.getColor())
                            .size(base.getSize())
                            .materialType(base.getMaterialType())
                            .requestedWeightKg(base.getRequestedWeightKg())
                            .actualWeightKg(base.getActualWeightKg())
                            .estimatedQuantity(base.getEstimatedQuantity())
                            .soldQuantity(base.getSoldQuantity())
                            .availableQuantity(base.getAvailableQuantity())
                            .availableWeightKg(base.getAvailableWeightKg())
                            .warehouseCode(outboundWarehouse == null ? null : outboundWarehouse.getCode())
                            .warehouseName(outboundWarehouse == null ? null : outboundWarehouse.getName())
                            .unitPrice(base.getUnitPrice())
                            .lineAmount(base.getLineAmount())
                            .memo(base.getMemo())
                            .materials(base.getMaterials())
                            .build();
                })
                .toList();

        // 3) 상태 이력 조회
        List<CircularSaleDto.StatusHistoryRes> histories = saleStatusHistoryRepository
                .findAllBySaleHeaderIdOrderByChangedAtAscIdAsc(header.getId()).stream()
                .map(CircularSaleDto.StatusHistoryRes::from)
                .toList();

        // 4) 상세 응답 조립
        return CircularSaleDto.DetailRes.builder()
                .saleId(header.getId())
                .saleNo(header.getSaleNo())
                .status(header.getStatus())
                .outboundNo(outbound == null ? null : outbound.getOutboundNo())
                .outboundStatus(outbound == null ? null : outbound.getStatus())
                .soldAt(header.getSoldAt())
                .completedAt(header.getCompletedAt())
                .soldByMemberId(header.getSoldByMemberId())
                .soldByName(header.getSoldByName())
                .outboundHeaderId(header.getOutboundHeaderId())
                .outboundWarehouseCode(outboundWarehouse == null ? null : outboundWarehouse.getCode())
                .outboundWarehouseName(outboundWarehouse == null ? null : outboundWarehouse.getName())
                .buyerCode(buyer != null ? buyer.getCode() : null)
                .buyerName(buyer != null ? buyer.getCompanyName() : header.getDoneeName())
                .buyerIndustryGroup(buyer != null ? buyer.getIndustryGroup() : null)
                .materialType(header.getMaterialType())
                .totalSkuCount(header.getTotalSkuCount())
                .totalRequestedWeightKg(header.getTotalRequestedWeightKg())
                .totalActualWeightKg(header.getTotalActualWeightKg())
                .totalSoldQuantity(header.getTotalSoldQuantity())
                .totalAmount(header.getTotalAmount())
                .memo(header.getMemo())
                .saleType(header.getSaleType())
                .doneeName(header.getDoneeName())
                .items(lines)
                .statusHistory(histories)
                .build();
    }

    // 출고 상태 -> 판매 상태 동기화
    @Transactional
    public void syncStatusByOutboundTransition(WhOutboundHeader outbound, OutboundStatus toStatus,
                                               String actorMemberId, String actorName, String reason, Date changedAt) {
        // 1) 순환재고 판매 출처가 아닌 출고는 동기화 대상에서 제외
        if (outbound == null || outbound.getSourceType() != OutboundSourceType.CIRCULAR_SALE) {
            return;
        }

        // 2) 출고 헤더 기준으로 연결된 판매 헤더 조회(우선 outboundHeaderId, 보조로 saleNo)
        CircularSaleHeader sale = saleHeaderRepository.findByOutboundHeaderId(outbound.getId())
                .orElseGet(() -> saleHeaderRepository.findBySaleNo(outbound.getSourceRefNo())
                        .orElseThrow(() -> BaseException.from(BaseResponseStatus.CIRCULAR_SALE_NOT_FOUND)));

        // 3) 목표 상태 계산 및 멱등 처리(동일 상태면 no-op)
        CircularSaleStatus target = toCircularStatus(toStatus);
        if (target == null || sale.getStatus() == target) {
            return;
        }

        // 4) 허용 전이만 통과(READY_TO_SHIP -> IN_TRANSIT -> ARRIVED)
        CircularSaleStatus from = sale.getStatus();
        if (target == CircularSaleStatus.IN_TRANSIT && from != CircularSaleStatus.READY_TO_SHIP) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_STATUS_TRANSITION);
        }
        if (target == CircularSaleStatus.ARRIVED && from != CircularSaleStatus.IN_TRANSIT) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_STATUS_TRANSITION);
        }

        if (target == CircularSaleStatus.IN_TRANSIT) {
            sale.markInTransit();
        } else if (target == CircularSaleStatus.ARRIVED) {
            sale.markArrived(changedAt);
        }

        // 5) 판매 상태 이력 적재
        saleStatusHistoryRepository.save(CircularSaleStatusHistory.builder()
                .saleHeaderId(sale.getId())
                .fromStatus(from)
                .status(target)
                .changedAt(changedAt)
                .changedByMemberId(actorMemberId)
                .changedByName(actorName)
                .reason(reason)
                .build());
    }

    // ------------------------------ 내부 메서드 ----------------------------------

    // 사용하는 메서드: create
    // 요청 본문 필수값을 검증한다.
    private void validateCreateRequest(CircularSaleDto.CreateReq request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_EMPTY_ITEMS);
        }
        if (request.getMaterialType() == null || request.getMaterialType().isBlank()) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
        // DONATION은 doneeName 으로 기부처 식별 — buyerCode 검증 건너뜀
        String saleType = request.getSaleType() == null ? "SALE" : request.getSaleType().toUpperCase(Locale.ROOT);
        if (!"DONATION".equals(saleType) && (request.getBuyerCode() == null || request.getBuyerCode().isBlank())) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_BUYER_NOT_FOUND);
        }
    }

    // 사용하는 메서드: create
    // 거래처 코드를 기준으로 순환 판매 거래처를 조회한다.
    private CircularBuyer findBuyerByCode(String buyerCode) {
        if (buyerCode == null || buyerCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_BUYER_NOT_FOUND);
        }
        return circularBuyerRepository.findByCode(buyerCode.trim())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.CIRCULAR_SALE_BUYER_NOT_FOUND));
    }

    // 사용하는 메서드: list
    // buyerCode가 있으면 buyerId로 변환한다.
    private Long resolveBuyerIdOrNull(String buyerCode) {
        if (buyerCode == null || buyerCode.isBlank()) {
            return null;
        }
        return circularBuyerRepository.findByCode(buyerCode.trim())
                .map(CircularBuyer::getId)
                .orElse(null);
    }

    // 사용하는 메서드: create
    // 카테고리 조회 성능을 위해 id/code 맵을 생성한다.
    private CategoryLookup buildCategoryLookup() {
        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        Map<Long, Category> byId = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<String, Category> byCode = categories.stream().collect(Collectors.toMap(Category::getCode, Function.identity()));
        return new CategoryLookup(byId, byCode);
    }

    // 사용하는 메서드: create
    // 라인별 SKU/재고/상품/소재 스냅샷 정보를 조합해 생성 컨텍스트를 만든다.
    private List<LineContext> buildLineContexts(List<CircularSaleDto.CreateLineReq> lines, String expectedMaterialType, CategoryLookup categoryLookup) {
        // 1) 요청 라인 SKU를 일괄 조회해 미존재 SKU를 빠르게 검출
        List<String> skuCodes = lines.stream().map(CircularSaleDto.CreateLineReq::getSkuCode).toList();
        Map<String, ProductSku> skuByCode = productSkuRepository.findAllBySkuCodeIn(skuCodes).stream()
                .collect(Collectors.toMap(ProductSku::getSkuCode, Function.identity()));
        if (skuByCode.size() != new HashSet<>(skuCodes).size()) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_SKU_NOT_FOUND);
        }

        Map<String, ProductMaster> masterByCode = productMasterRepository.findAllByCodeIn(
                skuByCode.values().stream().map(ProductSku::getProductCode).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));

        // 2) 라인별로 요청값/재고/카테고리/소재 스냅샷을 검증하고 컨텍스트화
        Set<Long> seenInventoryIds = new HashSet<>();
        List<LineContext> contexts = new ArrayList<>();
        for (CircularSaleDto.CreateLineReq reqLine : lines) {
            validateCreateLine(reqLine, seenInventoryIds);
            ProductSku sku = skuByCode.get(reqLine.getSkuCode());
            if (sku == null || sku.getStatus() != ProductStatus.ACTIVE) {
                throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_SKU_NOT_FOUND);
            }
            ProductMaster master = masterByCode.get(sku.getProductCode());
            if (master == null) {
                throw BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND);
            }

            Inventory inventory = inventoryRepository.findWithLockById(reqLine.getInventoryId())
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INSUFFICIENT_STOCK));
            validateInventoryForSale(inventory, sku, reqLine);

            Category child = categoryLookup.byCode.get(master.getCategoryCode());
            if (child == null) {
                throw BaseException.from(BaseResponseStatus.CATEGORY_NOT_FOUND);
            }
            Category parent = child.getParentId() == null ? child : categoryLookup.byId.get(child.getParentId());

            if (!Objects.equals(blankToNull(expectedMaterialType), blankToNull(deriveMaterialType(master)))) {
                throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
            }

            contexts.add(LineContext.builder()
                    .request(reqLine)
                    .sku(sku)
                    .master(master)
                    .inventory(inventory)
                    .mainCategory(parent == null ? child.getName() : parent.getName())
                    .subCategory(child.getName())
                    .materialType(expectedMaterialType.trim())
                    .materials(master.getMaterialCompositions().stream()
                            .map(c -> MaterialSnapshot.builder()
                                    .materialCode(c.getMaterial().getCode())
                                    .materialName(c.getMaterial().getNameKo())
                                    .ratio(c.getRatio())
                                    .sortOrder(c.getCompositionOrder())
                                    .build())
                            .toList())
                    .build());
        }
        // 3) 검증 완료된 컨텍스트 목록 반환
        return contexts;
    }

    // 사용하는 메서드: create
    // 라인 단위 요청값을 검증하고 중복 inventory 요청을 차단한다.
    private void validateCreateLine(CircularSaleDto.CreateLineReq line, Set<Long> seenInventoryIds) {
        if (line.getInventoryId() == null || line.getInventoryId() <= 0 || line.getSkuCode() == null || line.getSkuCode().isBlank()) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
        if (!seenInventoryIds.add(line.getInventoryId())) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
        if (line.getRequestedWeightKg() == null || line.getRequestedWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
        if (line.getActualWeightKg() != null && line.getActualWeightKg().compareTo(BigDecimal.ZERO) < 0) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
        if (line.getEstimatedQuantity() != null && line.getEstimatedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
        if (line.getSoldQuantity() == null || line.getSoldQuantity() <= 0) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_QUANTITY);
        }
        if (line.getUnitPrice() == null || line.getUnitPrice() < 0) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
        if (line.getLineAmount() == null || line.getLineAmount() < 0) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
    }

    // 사용하는 메서드: create
    // 재고/sku 정합성 및 판매 가능 수량을 검증한다.
    private void validateInventoryForSale(Inventory inventory, ProductSku sku, CircularSaleDto.CreateLineReq reqLine) {
        if (!Objects.equals(inventory.getSkuId(), sku.getId())) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_REQUEST);
        }
        if (inventory.getInventoryStatus() != InventoryStatus.CIRCULAR) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INSUFFICIENT_STOCK);
        }
        if (inventory.getAvailableQuantity() == null || inventory.getAvailableQuantity() < reqLine.getSoldQuantity()) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INSUFFICIENT_STOCK);
        }
    }

    // 사용하는 메서드: create
    // 모든 라인의 출고 창고가 동일한지 검증하고 창고 ID를 반환한다.
    private Long resolveSingleWarehouse(List<LineContext> contexts) {
        Long warehouseId = contexts.get(0).inventory.getLocationId();
        for (LineContext context : contexts) {
            if (!Objects.equals(warehouseId, context.inventory.getLocationId())) {
                throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INVALID_WAREHOUSE);
            }
        }
        return warehouseId;
    }

    // 사용하는 메서드: create
    // 검증 완료된 라인 기준으로 재고를 reserved 로 예약한다.
    private void applyInventoryReservations(List<LineContext> contexts) {
        // 라인마다 InventoryService를 통해 판매 재고를 예약 처리하고 부족 수량이 있으면 즉시 실패시킨다.
        for (LineContext context : contexts) {
            int reserved = inventoryService.reserveForOutboundUpToByInventoryId(
                    context.inventory.getId(),
                    context.request.getSoldQuantity()
            );
            if (reserved != context.request.getSoldQuantity()) {
                throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INSUFFICIENT_STOCK);
            }
        }
    }

    // 사용하는 메서드: create
    // 판매/출고/상태이력 데이터를 한 트랜잭션에서 저장한다.
    private SalePersistResult persistSaleAndOutbound(CircularSaleDto.CreateReq request, AuthUserDetails me,
                                                     CircularBuyer buyer, Long warehouseId,
                                                     List<LineContext> contexts, String saleType) {
        Date now = new Date();
        // DONATION은 buyerId=null (CircularSaleHeader.buyerId 컬럼이 nullable=true 이어야 함)
        Long buyerId = buyer != null ? buyer.getId() : null;

        // 1) 판매 헤더 저장(임시번호 저장 후 PK 기반 정식번호 재할당)
        CircularSaleHeader header = saleHeaderRepository.save(CircularSaleHeader.builder()
                .saleNo(generateTempNo("TMP"))
                .buyerId(buyerId)
                .warehouseId(warehouseId)
                .status(CircularSaleStatus.READY_TO_SHIP)
                .soldAt(now)
                .soldByMemberId(me == null ? "SYSTEM" : me.getEmployeeCode())
                .soldByName(me == null ? "SYSTEM" : me.getName())
                .materialType(request.getMaterialType().trim())
                .totalSkuCount(contexts.size())
                .totalRequestedWeightKg(sumBigDecimal(contexts, c -> c.request.getRequestedWeightKg()))
                .totalActualWeightKg(sumBigDecimal(contexts, c -> c.request.getActualWeightKg() == null ? c.request.getRequestedWeightKg() : c.request.getActualWeightKg()))
                .totalSoldQuantity(sumInt(contexts, c -> c.request.getSoldQuantity()))
                .totalAmount(sumLong(contexts, c -> c.request.getLineAmount()))
                .memo(request.getMemo())
                .saleType(saleType)
                .doneeName(request.getDoneeName())
                .build());
        header.assignSaleNo(generateSaleNo(header.getId(), now, saleType));

        // 2) 판매 아이템 저장
        List<CircularSaleItem> items = new ArrayList<>();
        for (LineContext context : contexts) {
            CircularSaleDto.CreateLineReq reqLine = context.request;
            int stockQuantitySnapshot = Math.max(0, context.availableQuantitySnapshot == null ? 0 : context.availableQuantitySnapshot);
            BigDecimal stockWeightKgSnapshot = estimateSnapshotWeightKg(reqLine, stockQuantitySnapshot);
            items.add(CircularSaleItem.builder()
                    .saleHeaderId(header.getId())
                    .inventoryId(reqLine.getInventoryId())
                    .skuId(context.sku.getId())
                    .skuCode(context.sku.getSkuCode())
                    .productCode(context.master.getCode())
                    .productName(context.master.getName())
                    .mainCategory(context.mainCategory)
                    .subCategory(context.subCategory)
                    .color(context.sku.getColor())
                    .size(context.sku.getSize())
                    .materialType(context.materialType)
                    .requestedWeightKg(scale3(reqLine.getRequestedWeightKg()))
                    .actualWeightKg(scale3(reqLine.getActualWeightKg() == null ? reqLine.getRequestedWeightKg() : reqLine.getActualWeightKg()))
                    .estimatedQuantity(scale3(reqLine.getEstimatedQuantity() == null ? BigDecimal.valueOf(reqLine.getSoldQuantity()) : reqLine.getEstimatedQuantity()))
                    .soldQuantity(reqLine.getSoldQuantity())
                    .stockQuantitySnapshot(stockQuantitySnapshot)
                    .stockWeightKgSnapshot(stockWeightKgSnapshot)
                    .unitPrice(reqLine.getUnitPrice())
                    .lineAmount(reqLine.getLineAmount())
                    .memo(reqLine.getMemo())
                    .build());
        }
        List<CircularSaleItem> savedItems = saleItemRepository.saveAll(items);

        // 3) 판매 아이템별 소재 스냅샷 저장
        List<CircularSaleItemMaterial> materialRows = new ArrayList<>();
        for (int i = 0; i < savedItems.size(); i++) {
            CircularSaleItem savedItem = savedItems.get(i);
            List<MaterialSnapshot> materials = contexts.get(i).materials;
            for (MaterialSnapshot material : materials) {
                materialRows.add(CircularSaleItemMaterial.builder()
                        .saleItemId(savedItem.getId())
                        .materialCode(material.materialCode)
                        .materialName(material.materialName)
                        .ratio(material.ratio)
                        .sortOrder(material.sortOrder)
                        .build());
            }
        }
        saleItemMaterialRepository.saveAll(materialRows);

        // 4) 출고 헤더/출고 아이템 저장 및 판매 헤더에 연결
        //    SALE: destinationType=CIRCULAR_BUYER, destinationId=buyer.id
        //    DONATION: destinationType=DONATION, destinationId=null, destinationName=기부처명
        boolean isDonation = "DONATION".equals(saleType);
        WhOutboundHeader outbound = outboundHeaderRepository.save(WhOutboundHeader.builder()
                .outboundNo(generateTempNo("TMP"))
                .sourceType(OutboundSourceType.CIRCULAR_SALE)
                .sourceRefNo(header.getSaleNo())
                .sourceRefSeq(1)
                .sourceRefId(header.getId())
                .warehouseId(warehouseId)
                .destinationType(isDonation ? OutboundDestinationType.DONATION : OutboundDestinationType.CIRCULAR_BUYER)
                .destinationId(isDonation ? null : buyer.getId())
                .destinationName(isDonation ? request.getDoneeName() : null)
                .status(OutboundStatus.READY_TO_SHIP)
                .totalRequestedQuantity(sumInt(contexts, c -> c.request.getSoldQuantity()))
                .requestedAt(now)
                .requestedByMemberId(me == null ? "SYSTEM" : me.getEmployeeCode())
                .requestedByName(me == null ? "SYSTEM" : me.getName())
                .memo(request.getMemo())
                .build());
        outbound.assignOutboundNo(generateOutboundNo(outbound.getId(), now));
        header.linkOutboundHeader(outbound.getId());

        List<WhOutboundItem> outboundItems = new ArrayList<>();
        for (int i = 0; i < contexts.size(); i++) {
            LineContext context = contexts.get(i);
            CircularSaleItem savedItem = savedItems.get(i);
            outboundItems.add(WhOutboundItem.builder()
                    .outboundHeaderId(outbound.getId())
                    .sourceLineRefId(savedItem.getId())
                    .skuId(context.sku.getId())
                    .skuCode(context.sku.getSkuCode())
                    .productCode(context.master.getCode())
                    .productName(context.master.getName())
                    .mainCategory(context.mainCategory)
                    .subCategory(context.subCategory)
                    .color(context.sku.getColor())
                    .size(context.sku.getSize())
                    .unitPrice(context.request.getUnitPrice())
                    .requestedQuantity(context.request.getSoldQuantity())
                    .memo(context.request.getMemo())
                    .build());
        }
        outboundItemRepository.saveAll(outboundItems);

        // 5) 판매/출고 상태이력의 최초 상태(READY_TO_SHIP) 적재
        saleStatusHistoryRepository.save(CircularSaleStatusHistory.builder()
                .saleHeaderId(header.getId())
                .fromStatus(null)
                .status(CircularSaleStatus.READY_TO_SHIP)
                .changedAt(now)
                .changedByMemberId(me == null ? "SYSTEM" : me.getEmployeeCode())
                .changedByName(me == null ? "SYSTEM" : me.getName())
                .reason("CREATE")
                .build());
        outboundStatusHistoryRepository.save(WhOutboundStatusHistory.builder()
                .outboundHeaderId(outbound.getId())
                .status(OutboundStatus.READY_TO_SHIP)
                .changedAt(now)
                .changedByMemberId(me == null ? "SYSTEM" : me.getEmployeeCode())
                .changedByName(me == null ? "SYSTEM" : me.getName())
                .reason("CIRCULAR_SALE_CREATE")
                .build());

        // 6) Phase 3 — 실제 판매를 ESG 점수에 즉시 반영 (circular_buyer_transaction 자동 INSERT)
        //    sale_item 별로 transaction 1건씩 생성 + 혼방 주 소재 참조용 저장 (탄소 계산 미사용)
        createTransactionsFromSale(header, buyer, savedItems, contexts, saleType, request.getDoneeName());

        // 7) 생성 결과 반환
        return new SalePersistResult(header, outbound, savedItems, materialRows);
    }

    // 사용하는 메서드: create
    // 판매번호 규칙: CSR-YYYYMMDD-00001 (DONATION은 DON-YYYYMMDD-00001)
    private String generateSaleNo(Long id, Date soldAt, String saleType) {
        LocalDate day = soldAt.toInstant().atZone(KST).toLocalDate();
        String prefix = "DONATION".equals(saleType) ? "DON" : "CSR";
        return prefix + "-" + day.format(NUMBER_DATE_FORMAT) + "-" + String.format("%05d", id);
    }

    // 사용하는 메서드: create
    // 출고번호 규칙: WOB-YYYYMMDD-00001
    private String generateOutboundNo(Long id, Date requestedAt) {
        LocalDate day = requestedAt.toInstant().atZone(KST).toLocalDate();
        return "WOB-" + day.format(NUMBER_DATE_FORMAT) + "-" + String.format("%05d", id);
    }

    // 사용하는 메서드: detail/create
    // 판매 라인과 소재 스냅샷을 응답 DTO로 매핑한다.
    private List<CircularSaleDto.LineRes> mapLineRes(List<CircularSaleItem> items, List<CircularSaleItemMaterial> materials) {
        Map<Long, List<CircularSaleDto.MaterialRes>> materialMap = materials.stream()
                .collect(Collectors.groupingBy(
                        CircularSaleItemMaterial::getSaleItemId,
                        Collectors.mapping(CircularSaleDto.MaterialRes::from, Collectors.toList())
                ));
        return items.stream()
                .map(item -> CircularSaleDto.LineRes.from(item, materialMap.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    // 사용하는 메서드: list
    // 판매 목록 대표 품목 문구를 생성한다.
    private String buildHeadline(List<CircularSaleItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0).getProductName();
        }
        return items.get(0).getProductName() + " 외 " + (items.size() - 1) + "건";
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Phase 3 — 운영 판매 → ESG 점수 가산 파이프라인
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * 사용하는 메서드: persistSaleAndOutbound
     * Phase 3: 실제 판매 완료 시 ESG 점수 가산을 위해 circular_buyer_transaction 에도 거래 row 를 INSERT.
     *  - sale_item 별로 transaction 1건씩 생성 (product 별 정밀 ESG 점수)
     *  - DONATION은 buyer=null (entity optional=true), doneeName 기록
     *  - 거래 시점 단가/금액은 sale_item 의 값을 그대로 스냅샷 (KAU 시세와 무관, 실제 거래가 기준)
     */
    private void createTransactionsFromSale(CircularSaleHeader header,
                                             CircularBuyer buyer,
                                             List<CircularSaleItem> savedItems,
                                             List<LineContext> contexts,
                                             String saleType,
                                             String doneeName) {
        // sold_at(Date) → LocalDateTime(KST) 변환 — circular_buyer_transaction.transacted_at 컬럼 매칭
        java.time.LocalDateTime soldAt = header.getSoldAt().toInstant()
                .atZone(KST).toLocalDateTime();

        List<CircularBuyerTransaction> rows = new ArrayList<>();
        for (int i = 0; i < savedItems.size(); i++) {
            CircularSaleItem item = savedItems.get(i);
            ProductMaster master = contexts.get(i).master;

            TxMaterial tx = deriveTransactionMaterial(master);

            rows.add(CircularBuyerTransaction.builder()
                    .buyer(buyer)  // SALE: 거래처 엔티티, DONATION: null (optional=true)
                    .materialCode(tx.materialCode())
                    .weightKg(item.getActualWeightKg() != null
                            ? item.getActualWeightKg().intValue()
                            : 0)
                    // CircularSaleItem.unitPrice 는 Long, CircularBuyerTransaction.unitPrice 는 Integer
                    // → 시드/운영 단가가 Integer 범위(약 21억) 내라 안전하게 intValue() 변환
                    .unitPrice(item.getUnitPrice() != null ? item.getUnitPrice().intValue() : 0)
                    .totalAmount(item.getLineAmount() != null ? item.getLineAmount() : 0L)
                    .transactedAt(soldAt)
                    .saleType(saleType)
                    .doneeName(doneeName)
                    .build());
        }

        if (!rows.isEmpty()) {
            transactionRepository.saveAll(rows);
        }
    }

    /**
     * 사용하는 메서드: createTransactionsFromSale
     * product 의 composition 분포로 transaction.material_code 산출.
     *  - composition 단일 → 그 material code
     *  - composition 2개 이상 또는 없음 → 'BLEND'
     */
    private TxMaterial deriveTransactionMaterial(ProductMaster master) {
        List<ProductMaterialComposition> comps = master.getMaterialCompositions();
        if (comps == null || comps.isEmpty()) {
            return new TxMaterial("BLEND");
        }
        if (comps.size() == 1) {
            return new TxMaterial(comps.get(0).getMaterial().getCode());
        }
        return new TxMaterial("BLEND");
    }

    /** Phase 3 — transaction 의 material 결정 결과 보관용 record */
    private record TxMaterial(String materialCode) {}

    // 사용하는 메서드: create
    // 소재 조합 기반으로 판매 소재 구분 라벨을 산출한다.
    private String deriveMaterialType(ProductMaster master) {
        List<ProductMaterialComposition> comps = master.getMaterialCompositions();
        if (comps == null || comps.isEmpty()) {
            return "혼방";
        }
        if (comps.size() >= 2) {
            return "혼방";
        }
        ProductMaterialComposition single = comps.get(0);
        String group = single.getMaterial().getMaterialGroup();
        // Phase 1: material_group 어휘를 'NATURAL' → 'NATURAL_SINGLE' 로 통일하면서
        //          기존 'NATURAL' 도 안전망으로 함께 수용 (시드 적용 전/후 호환).
        if ("NATURAL_SINGLE".equalsIgnoreCase(group) || "NATURAL".equalsIgnoreCase(group)) {
            return "천연 단일 섬유";
        }
        if ("SYNTHETIC".equalsIgnoreCase(group)) {
            return "합성 섬유";
        }
        return "혼방";
    }

    // 사용하는 메서드: list
    // 정렬 파라미터를 pageable sort로 변환한다.
    private Sort parseSort(String sort) {
        String safe = sort == null || sort.isBlank() ? "soldAt,desc" : sort.trim();
        String[] tokens = safe.split(",");
        String field = tokens[0].trim();
        String direction = tokens.length >= 2 ? tokens[1].trim() : "desc";
        if (!Set.of("soldAt", "totalAmount", "totalActualWeightKg", "totalSoldQuantity").contains(field)) {
            field = "soldAt";
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(new Sort.Order(dir, field), new Sort.Order(Sort.Direction.DESC, "id"));
    }

    // 사용하는 메서드: syncStatusByOutboundTransition
    // 출고 상태를 순환판매 상태로 변환한다.
    private CircularSaleStatus toCircularStatus(OutboundStatus status) {
        if (status == null) return null;
        return switch (status) {
            case READY_TO_SHIP -> CircularSaleStatus.READY_TO_SHIP;
            case IN_TRANSIT -> CircularSaleStatus.IN_TRANSIT;
            case ARRIVED -> CircularSaleStatus.ARRIVED;
        };
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value;
    }

    // 사용하는 메서드: persistSaleAndOutbound
    // DB 길이 제약을 넘지 않도록 하이픈을 제거한 UUID 기반 임시번호를 생성한다.
    private String generateTempNo(String prefix) {
        String safePrefix = (prefix == null || prefix.isBlank()) ? "TMP" : prefix.trim();
        return safePrefix + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private BigDecimal sumBigDecimal(Collection<LineContext> contexts, Function<LineContext, BigDecimal> extractor) {
        BigDecimal sum = BigDecimal.ZERO;
        for (LineContext context : contexts) {
            BigDecimal value = extractor.apply(context);
            if (value != null) {
                sum = sum.add(scale3(value));
            }
        }
        return scale3(sum);
    }

    private BigDecimal scale3(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO.setScale(DECIMAL_SCALE_3, RoundingMode.HALF_UP);
        return value.setScale(DECIMAL_SCALE_3, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateSnapshotWeightKg(CircularSaleDto.CreateLineReq reqLine, int stockQuantitySnapshot) {
        if (stockQuantitySnapshot <= 0) return BigDecimal.ZERO.setScale(DECIMAL_SCALE_3, RoundingMode.HALF_UP);
        BigDecimal soldQty = BigDecimal.valueOf(Math.max(1, reqLine.getSoldQuantity() == null ? 0 : reqLine.getSoldQuantity()));
        BigDecimal baseWeight = reqLine.getActualWeightKg() == null ? reqLine.getRequestedWeightKg() : reqLine.getActualWeightKg();
        if (baseWeight == null || baseWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(DECIMAL_SCALE_3, RoundingMode.HALF_UP);
        }
        BigDecimal unitWeight = baseWeight.divide(soldQty, DECIMAL_SCALE_3, RoundingMode.HALF_UP);
        return unitWeight.multiply(BigDecimal.valueOf(stockQuantitySnapshot)).setScale(DECIMAL_SCALE_3, RoundingMode.HALF_UP);
    }

    private Integer sumInt(Collection<LineContext> contexts, Function<LineContext, Integer> extractor) {
        int sum = 0;
        for (LineContext context : contexts) {
            sum += extractor.apply(context);
        }
        return sum;
    }

    private Long sumLong(Collection<LineContext> contexts, Function<LineContext, Long> extractor) {
        long sum = 0L;
        for (LineContext context : contexts) {
            sum += extractor.apply(context);
        }
        return sum;
    }

    @Builder
    private static class MaterialSnapshot {
        private String materialCode;
        private String materialName;
        private Integer ratio;
        private Integer sortOrder;
    }

    @Builder
    private static class LineContext {
        private CircularSaleDto.CreateLineReq request;
        private ProductSku sku;
        private ProductMaster master;
        private Inventory inventory;
        private Integer availableQuantitySnapshot;
        private String mainCategory;
        private String subCategory;
        private String materialType;
        private List<MaterialSnapshot> materials;
    }

    private record CategoryLookup(Map<Long, Category> byId, Map<String, Category> byCode) {
    }

    private record SalePersistResult(
            CircularSaleHeader header,
            WhOutboundHeader outbound,
            List<CircularSaleItem> items,
            List<CircularSaleItemMaterial> materialRows
    ) {
    }
}
