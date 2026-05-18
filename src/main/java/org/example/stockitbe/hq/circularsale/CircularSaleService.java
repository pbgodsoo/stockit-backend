package org.example.stockitbe.hq.circularsale;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
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
import org.example.stockitbe.hq.inventory.InventoryRepository;
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
    private final CircularBuyerRepository circularBuyerRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final WhOutboundHeaderRepository outboundHeaderRepository;
    private final WhOutboundItemRepository outboundItemRepository;
    private final WhOutboundStatusHistoryRepository outboundStatusHistoryRepository;

    // 순환재고 판매 생성
    @Transactional
    public CircularSaleDto.CreateRes create(CircularSaleDto.CreateReq request, AuthUserDetails me) {
        validateCreateRequest(request);
        CircularBuyer buyer = findBuyerByCode(request.getBuyerCode());
        CategoryLookup categoryLookup = buildCategoryLookup();

        List<LineContext> contexts = buildLineContexts(request.getItems(), request.getMaterialType(), categoryLookup);
        Long warehouseId = resolveSingleWarehouse(contexts);

        applyInventoryReservations(contexts);
        SalePersistResult persisted = persistSaleAndOutbound(request, me, buyer, warehouseId, contexts);

        List<CircularSaleDto.LineRes> lines = mapLineRes(persisted.items, persisted.materialRows);
        return CircularSaleDto.CreateRes.from(
                persisted.header,
                buyer.getCode(),
                buyer.getCompanyName(),
                persisted.outbound.getOutboundNo(),
                persisted.outbound.getStatus(),
                lines
        );
    }

    // 순환재고 판매 목록 조회
    @Transactional(readOnly = true)
    public CircularSaleDto.PageRes list(
            Integer page, Integer size, String sort,
            LocalDate from, LocalDate to,
            String buyerCode, String materialType, String keyword
    ) {
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 20 : Math.min(Math.max(1, size), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize, parseSort(sort));

        Date fromDate = from == null ? null : Date.from(from.atStartOfDay(KST).toInstant());
        Date toDateExclusive = to == null ? null : Date.from(to.plusDays(1).atStartOfDay(KST).toInstant());
        String safeKeyword = keyword == null ? null : keyword.trim();
        String safeMaterialType = materialType == null ? null : materialType.trim();
        Long buyerId = resolveBuyerIdOrNull(buyerCode);

        Page<CircularSaleHeader> headers = saleHeaderRepository.search(
                fromDate, toDateExclusive, buyerId, blankToNull(safeMaterialType), blankToNull(safeKeyword), pageable
        );
        if (headers.isEmpty()) {
            return CircularSaleDto.PageRes.from(new PageImpl<>(List.of(), pageable, 0));
        }

        Map<Long, CircularBuyer> buyerById = circularBuyerRepository.findAllById(
                headers.getContent().stream().map(CircularSaleHeader::getBuyerId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(CircularBuyer::getId, Function.identity()));
        Map<Long, WhOutboundHeader> outboundById = outboundHeaderRepository.findAllById(
                headers.getContent().stream()
                        .map(CircularSaleHeader::getOutboundHeaderId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(WhOutboundHeader::getId, Function.identity()));

        Map<Long, List<CircularSaleItem>> itemsByHeader = saleItemRepository.findAllBySaleHeaderIdIn(
                headers.getContent().stream().map(CircularSaleHeader::getId).toList()
        ).stream().collect(Collectors.groupingBy(CircularSaleItem::getSaleHeaderId));

        List<CircularSaleDto.ListRowRes> rows = new ArrayList<>();
        for (CircularSaleHeader header : headers.getContent()) {
            CircularBuyer buyer = buyerById.get(header.getBuyerId());
            WhOutboundHeader outbound = outboundById.get(header.getOutboundHeaderId());
            List<CircularSaleItem> items = itemsByHeader.getOrDefault(header.getId(), List.of());
            rows.add(CircularSaleDto.ListRowRes.builder()
                    .saleId(header.getId())
                    .saleNo(header.getSaleNo())
                    .status(header.getStatus())
                    .outboundNo(outbound == null ? null : outbound.getOutboundNo())
                    .outboundStatus(outbound == null ? null : outbound.getStatus())
                    .soldAt(header.getSoldAt())
                    .completedAt(header.getCompletedAt())
                    .buyerCode(buyer == null ? null : buyer.getCode())
                    .buyerName(buyer == null ? null : buyer.getCompanyName())
                    .materialType(header.getMaterialType())
                    .totalSkuCount(header.getTotalSkuCount())
                    .totalActualWeightKg(header.getTotalActualWeightKg())
                    .totalSoldQuantity(header.getTotalSoldQuantity())
                    .totalAmount(header.getTotalAmount())
                    .headline(buildHeadline(items))
                    .build());
        }
        return CircularSaleDto.PageRes.from(new PageImpl<>(rows, pageable, headers.getTotalElements()));
    }

    // 순환재고 판매 상세 조회
    @Transactional(readOnly = true)
    public CircularSaleDto.DetailRes detail(Long saleId) {
        CircularSaleHeader header = saleHeaderRepository.findById(saleId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.CIRCULAR_SALE_NOT_FOUND));
        CircularBuyer buyer = circularBuyerRepository.findById(header.getBuyerId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.CIRCULAR_SALE_BUYER_NOT_FOUND));
        WhOutboundHeader outbound = header.getOutboundHeaderId() == null ? null
                : outboundHeaderRepository.findById(header.getOutboundHeaderId()).orElse(null);

        List<CircularSaleItem> items = saleItemRepository.findAllBySaleHeaderIdOrderByIdAsc(header.getId());
        List<Long> itemIds = items.stream().map(CircularSaleItem::getId).toList();
        Map<Long, List<CircularSaleDto.MaterialRes>> materialsByItem = saleItemMaterialRepository
                .findAllBySaleItemIdInOrderBySortOrderAscIdAsc(itemIds).stream()
                .collect(Collectors.groupingBy(
                        CircularSaleItemMaterial::getSaleItemId,
                        Collectors.mapping(CircularSaleDto.MaterialRes::from, Collectors.toList())
                ));
        List<CircularSaleDto.LineRes> lines = items.stream()
                .map(item -> CircularSaleDto.LineRes.from(item, materialsByItem.getOrDefault(item.getId(), List.of())))
                .toList();

        List<CircularSaleDto.StatusHistoryRes> histories = saleStatusHistoryRepository
                .findAllBySaleHeaderIdOrderByChangedAtAscIdAsc(header.getId()).stream()
                .map(CircularSaleDto.StatusHistoryRes::from)
                .toList();

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
                .buyerCode(buyer.getCode())
                .buyerName(buyer.getCompanyName())
                .buyerIndustryGroup(buyer.getIndustryGroup())
                .materialType(header.getMaterialType())
                .totalSkuCount(header.getTotalSkuCount())
                .totalRequestedWeightKg(header.getTotalRequestedWeightKg())
                .totalActualWeightKg(header.getTotalActualWeightKg())
                .totalSoldQuantity(header.getTotalSoldQuantity())
                .totalAmount(header.getTotalAmount())
                .memo(header.getMemo())
                .items(lines)
                .statusHistory(histories)
                .build();
    }

    // 출고 상태 -> 판매 상태 동기화
    @Transactional
    public void syncStatusByOutboundTransition(WhOutboundHeader outbound, OutboundStatus toStatus,
                                               String actorMemberId, String actorName, String reason, Date changedAt) {
        if (outbound == null || outbound.getSourceType() != OutboundSourceType.CIRCULAR_SALE) {
            return;
        }

        CircularSaleHeader sale = saleHeaderRepository.findByOutboundHeaderId(outbound.getId())
                .orElseGet(() -> saleHeaderRepository.findBySaleNo(outbound.getSourceRefNo())
                        .orElseThrow(() -> BaseException.from(BaseResponseStatus.CIRCULAR_SALE_NOT_FOUND)));

        CircularSaleStatus target = toCircularStatus(toStatus);
        if (target == null || sale.getStatus() == target) {
            return;
        }

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
        List<String> skuCodes = lines.stream().map(CircularSaleDto.CreateLineReq::getSkuCode).toList();
        Map<String, ProductSku> skuByCode = productSkuRepository.findAllBySkuCodeIn(skuCodes).stream()
                .collect(Collectors.toMap(ProductSku::getSkuCode, Function.identity()));
        if (skuByCode.size() != new HashSet<>(skuCodes).size()) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_SKU_NOT_FOUND);
        }

        Map<String, ProductMaster> masterByCode = productMasterRepository.findAllByCodeIn(
                skuByCode.values().stream().map(ProductSku::getProductCode).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));

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
        for (LineContext context : contexts) {
            int reserved = context.inventory.reserveUpTo(context.request.getSoldQuantity());
            if (reserved != context.request.getSoldQuantity()) {
                throw BaseException.from(BaseResponseStatus.CIRCULAR_SALE_INSUFFICIENT_STOCK);
            }
        }
    }

    // 사용하는 메서드: create
    // 판매/출고/상태이력 데이터를 한 트랜잭션에서 저장한다.
    private SalePersistResult persistSaleAndOutbound(CircularSaleDto.CreateReq request, AuthUserDetails me, CircularBuyer buyer,
                                                     Long warehouseId, List<LineContext> contexts) {
        Date now = new Date();
        CircularSaleHeader header = saleHeaderRepository.save(CircularSaleHeader.builder()
                .saleNo("TEMP-" + UUID.randomUUID())
                .buyerId(buyer.getId())
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
                .build());
        header.assignSaleNo(generateSaleNo(header.getId(), now));

        List<CircularSaleItem> items = new ArrayList<>();
        for (LineContext context : contexts) {
            CircularSaleDto.CreateLineReq reqLine = context.request;
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
                    .unitPrice(reqLine.getUnitPrice())
                    .lineAmount(reqLine.getLineAmount())
                    .memo(reqLine.getMemo())
                    .build());
        }
        List<CircularSaleItem> savedItems = saleItemRepository.saveAll(items);

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

        WhOutboundHeader outbound = outboundHeaderRepository.save(WhOutboundHeader.builder()
                .outboundNo("TEMP-" + UUID.randomUUID())
                .sourceType(OutboundSourceType.CIRCULAR_SALE)
                .sourceRefNo(header.getSaleNo())
                .sourceRefSeq(1)
                .sourceRefId(header.getId())
                .warehouseId(warehouseId)
                .destinationType(OutboundDestinationType.CIRCULAR_BUYER)
                .destinationId(buyer.getId())
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

        return new SalePersistResult(header, outbound, savedItems, materialRows);
    }

    // 사용하는 메서드: create
    // 판매번호 규칙: CSR-YYYYMMDD-00001
    private String generateSaleNo(Long id, Date soldAt) {
        LocalDate day = soldAt.toInstant().atZone(KST).toLocalDate();
        return "CSR-" + day.format(NUMBER_DATE_FORMAT) + "-" + String.format("%05d", id);
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
        if ("NATURAL".equalsIgnoreCase(group)) {
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
