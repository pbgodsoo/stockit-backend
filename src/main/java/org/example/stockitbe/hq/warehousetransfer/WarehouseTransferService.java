package org.example.stockitbe.hq.warehousetransfer;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
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
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferDto;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferHeader;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferItem;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferStatus;
import org.example.stockitbe.warehouse.outbound.model.OutboundDestinationType;
import org.example.stockitbe.warehouse.outbound.model.OutboundSourceType;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundItem;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundHeaderRepository;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundItemRepository;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundStatusHistoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseTransferService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WarehouseTransferHeaderRepository headerRepository;
    private final WarehouseTransferItemRepository itemRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMasterRepository productMasterRepository;
    private final InventoryRepository inventoryRepository;
    private final WhOutboundHeaderRepository outboundHeaderRepository;
    private final WhOutboundItemRepository outboundItemRepository;
    private final WhOutboundStatusHistoryRepository outboundStatusHistoryRepository;
    private final PlatformTransactionManager transactionManager;

    // 재고이동 실행
    // 라우트(출발/도착 창고) 단위로 독립 처리하며, 실패 라우트는 수집해 부분성공으로 반환한다.
    @Transactional
    public WarehouseTransferDto.ExecuteRes execute(WarehouseTransferDto.ExecuteReq request) {
        // 1) 요청 라인 유효성을 검증한다.
        List<WarehouseTransferDto.ExecuteLineReq> lines = request.getLines() == null ? List.of() : request.getLines();
        if (lines.isEmpty()) throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);

        String requestedBy = trimToDefault(request.getRequestedBy(), "본사 관리자");
        Map<String, List<WarehouseTransferDto.ExecuteLineReq>> grouped = lines.stream()
                .collect(Collectors.groupingBy(this::routeKey, LinkedHashMap::new, Collectors.toList()));

        List<WarehouseTransferDto.ExecuteLineResultRes> lineResults = new ArrayList<>();
        List<WarehouseTransferDto.ExecuteTransferRes> createdTransfers = new ArrayList<>();
        List<WarehouseTransferDto.ExecuteFailedRouteRes> failedTransfers = new ArrayList<>();

        // 2) 라우트별 독립 트랜잭션으로 처리한다.
        for (List<WarehouseTransferDto.ExecuteLineReq> groupLines : grouped.values()) {
            try {
                ExecuteGroupResult routeResult = processRouteInNewTransaction(groupLines, requestedBy);
                lineResults.addAll(routeResult.lineResults());
                createdTransfers.add(routeResult.transferRes());
            } catch (BaseException e) {
                // 도메인 예외는 상태코드/메시지를 그대로 노출한다.
                failedTransfers.add(toFailedRoute(groupLines, e.getStatus()));
            } catch (Exception e) {
                // 비도메인 예외는 공통 실패 코드로 정규화한다.
                failedTransfers.add(toFailedRoute(groupLines, BaseResponseStatus.FAIL));
            }
        }

        // 3) 성공/실패 라우트를 분리해 응답한다.
        return WarehouseTransferDto.ExecuteRes.builder()
                .requestedCount(lines.size())
                .successCount(lineResults.size())
                .failureCount(failedTransfers.size())
                .lineResults(lineResults)
                .createdTransfers(createdTransfers)
                .failedTransfers(failedTransfers)
                .build();
    }

    private ExecuteGroupResult processRouteInNewTransaction(
            List<WarehouseTransferDto.ExecuteLineReq> groupLines,
            String requestedBy
    ) {
        // 라우트 단위 부분성공을 위해 신규 트랜잭션으로 경계를 분리한다.
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> processRoute(groupLines, requestedBy));
    }

    private ExecuteGroupResult processRoute(List<WarehouseTransferDto.ExecuteLineReq> groupLines, String requestedBy) {
        // 1) 라우트 검증 + 헤더 생성
        ExecuteGroupContext context = validateGroup(groupLines);
        WarehouseTransferHeader savedHeader = saveHeaderWithTransferNoRetry(
                context,
                requestedBy,
                buildReasonSummary(groupLines),
                buildMemoSummary(groupLines)
        );
        String transferNo = savedHeader.getTransferNo();

        // 2) 라우트 라인별 transfer item 및 결과 응답 라인을 구성한다.
        List<WarehouseTransferItem> items = new ArrayList<>();
        List<WarehouseTransferDto.ExecuteLineResultRes> lineResults = new ArrayList<>();
        int totalQty = 0;
        for (WarehouseTransferDto.ExecuteLineReq line : groupLines) {
            int qty = safeQty(line.getQty());
            totalQty += qty;

            ProductSku sku = context.skuByCode.get(line.getSkuCode());
            int fromBefore = context.fromAvailableBySkuId.getOrDefault(sku.getId(), 0);
            int toBefore = context.toAvailableBySkuId.getOrDefault(sku.getId(), 0);

            items.add(WarehouseTransferItem.builder()
                    .header(savedHeader)
                    .skuId(sku.getId())
                    .quantity(qty)
                    .reason(trimToNull(line.getReason()))
                    .memo(trimToNull(line.getMemo()))
                    .fromAvailableBefore(fromBefore)
                    .toAvailableBefore(toBefore)
                    .fromAvailableAfter(Math.max(0, fromBefore - qty))
                    .toAvailableAfter(toBefore + qty)
                    .build());

            lineResults.add(WarehouseTransferDto.ExecuteLineResultRes.builder()
                    .lineId(line.getLineId())
                    .skuCode(line.getSkuCode())
                    .fromWarehouseCode(line.getFromWarehouseCode())
                    .toWarehouseCode(line.getToWarehouseCode())
                    .qty(qty)
                    .success(true)
                    .message("SUCCESS")
                    .transferNo(transferNo)
                    .build());
        }
        // 3) transfer item 저장 후 outbound를 생성/연계한다.
        List<WarehouseTransferItem> savedItems = itemRepository.saveAll(items);
        createWhTransferOutbound(savedHeader, savedItems, context, requestedBy);
        // 4) 재고이동 상태를 출고 흐름의 시작 상태로 동기화한다.
        savedHeader.markReadyToShip();

        WarehouseTransferDto.ExecuteTransferRes transferRes = WarehouseTransferDto.ExecuteTransferRes.builder()
                .transferNo(transferNo)
                .fromWarehouseCode(context.fromWarehouse.getCode())
                .fromWarehouseName(context.fromWarehouse.getName())
                .toWarehouseCode(context.toWarehouse.getCode())
                .toWarehouseName(context.toWarehouse.getName())
                .status(savedHeader.getStatus().name())
                .skuCount(items.size())
                .totalQty(totalQty)
                .build();
        return new ExecuteGroupResult(lineResults, transferRes);
    }

    @Transactional(readOnly = true)
    public List<WarehouseTransferDto.TransferListItemRes> findTransfers(
            WarehouseTransferStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword
    ) {
        Date from = atStart(fromDate == null ? LocalDate.now().minusMonths(1) : fromDate);
        Date toExclusive = atStart((toDate == null ? LocalDate.now() : toDate).plusDays(1));

        List<WarehouseTransferHeader> headers = status == null
                ? headerRepository.findByRequestedAtBetweenOrderByRequestedAtDescIdDesc(from, toExclusive)
                : headerRepository.findByStatusAndRequestedAtBetweenOrderByRequestedAtDescIdDesc(status, from, toExclusive);

        return buildListItems(headers, keyword);
    }

    @Transactional(readOnly = true)
    public WarehouseTransferDto.TransferDetailRes findTransferDetail(String transferNo) {
        WarehouseTransferHeader header = headerRepository.findByTransferNo(transferNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOT_FOUND_DATA));
        return toDetail(header);
    }

    @Transactional(readOnly = true)
    public List<WarehouseTransferDto.WarehouseSkuDistributionRes> findSkuDistribution(String skuCode) {
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));
        ProductMaster master = productMasterRepository.findByCode(sku.getProductCode())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));

        Map<Long, WarehouseSkuStockAggregate> stockByLocation = new HashMap<>();
        inventoryRepository.findAllBySkuIdIn(List.of(sku.getId())).stream()
                .filter(inv -> InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inv.getInventoryStatus()))
                .forEach(inv -> {
                    WarehouseSkuStockAggregate stock = stockByLocation.computeIfAbsent(inv.getLocationId(), ignored -> new WarehouseSkuStockAggregate());
                    stock.onHand += Math.max(0, nz(inv.getQuantity()));
                    stock.available += Math.max(0, nz(inv.getAvailableQuantity()));
                    if (stock.updatedAt == null || (inv.getUpdatedAt() != null && inv.getUpdatedAt().after(stock.updatedAt))) {
                        stock.updatedAt = inv.getUpdatedAt();
                    }
                });

        return infrastructureRepository.findByLocationTypeOrderByIdDesc(LocationType.WAREHOUSE).stream()
                .map(warehouse -> {
                    WarehouseSkuStockAggregate stock = stockByLocation.get(warehouse.getId());
                    int onHand = stock == null ? 0 : stock.onHand;
                    int available = stock == null ? 0 : stock.available;
                    int reserved = Math.max(0, onHand - available);
                    int safety = Math.max(0, nz(master.getWarehouseSafetyStock()));
                    String status = available <= 0 ? "품절" : (available < safety ? "부족" : "정상");
                    return WarehouseTransferDto.WarehouseSkuDistributionRes.builder()
                            .warehouseCode(warehouse.getCode())
                            .warehouseName(warehouse.getName())
                            .location(warehouse.getRegion())
                            .onHandStock(onHand)
                            .reservedStock(reserved)
                            .availableStock(available)
                            .safetyStock(safety)
                            .status(status)
                            .updatedAt(stock == null ? null : stock.updatedAt)
                            .build();
                })
                .sorted(Comparator.comparing(WarehouseTransferDto.WarehouseSkuDistributionRes::getWarehouseCode))
                .toList();
    }

    private static class WarehouseSkuStockAggregate {
        private int onHand;
        private int available;
        private Date updatedAt;
    }

    private List<WarehouseTransferDto.TransferListItemRes> buildListItems(List<WarehouseTransferHeader> headers, String keyword) {
        if (headers.isEmpty()) return List.of();

        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Map<Long, Infrastructure> infraById = loadInfraMap(headers);
        Map<Long, List<WarehouseTransferItem>> itemsByHeaderId = loadItemsMap(headers);
        Set<Long> skuIds = itemsByHeaderId.values().stream().flatMap(List::stream).map(WarehouseTransferItem::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuById = productSkuRepository.findAllById(skuIds).stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Map<String, ProductMaster> masterByCode = loadMasterMap(skuById.values());

        return headers.stream()
                .map(header -> {
                    List<WarehouseTransferDto.TransferLineRes> lines = toLines(
                            itemsByHeaderId.getOrDefault(header.getId(), List.of()),
                            skuById,
                            masterByCode
                    );
                    Infrastructure from = infraById.get(header.getFromWarehouseId());
                    Infrastructure to = infraById.get(header.getToWarehouseId());
                    WarehouseTransferDto.TransferListItemRes row = toListRow(header, from, to, lines);
                    if (safeKeyword.isBlank()) return row;
                    return matchesKeyword(row, safeKeyword) ? row : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private WarehouseTransferDto.TransferDetailRes toDetail(WarehouseTransferHeader header) {
        Map<Long, Infrastructure> infraById = infrastructureRepository.findAllById(
                List.of(header.getFromWarehouseId(), header.getToWarehouseId())
        ).stream().collect(Collectors.toMap(Infrastructure::getId, Function.identity()));

        List<WarehouseTransferItem> items = itemRepository.findAllByHeader_IdOrderByIdAsc(header.getId());
        Set<Long> skuIds = items.stream().map(WarehouseTransferItem::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuById = productSkuRepository.findAllById(skuIds).stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Map<String, ProductMaster> masterByCode = loadMasterMap(skuById.values());
        List<WarehouseTransferDto.TransferLineRes> lines = toLines(items, skuById, masterByCode);

        Infrastructure from = infraById.get(header.getFromWarehouseId());
        Infrastructure to = infraById.get(header.getToWarehouseId());
        return WarehouseTransferDto.TransferDetailRes.builder()
                .transferNo(header.getTransferNo())
                .fromWarehouseCode(from == null ? "" : from.getCode())
                .fromWarehouseName(from == null ? "" : from.getName())
                .toWarehouseCode(to == null ? "" : to.getCode())
                .toWarehouseName(to == null ? "" : to.getName())
                .requestedBy(header.getRequestedBy())
                .requestedAt(header.getRequestedAt())
                .status(header.getStatus().name())
                .lines(lines)
                .skuCount(lines.size())
                .totalQty(lines.stream().mapToInt(line -> safeQty(line.getQty())).sum())
                .reasonCount((int) lines.stream().map(WarehouseTransferDto.TransferLineRes::getReason).filter(s -> s != null && !s.isBlank()).distinct().count())
                .memoCount((int) lines.stream().map(WarehouseTransferDto.TransferLineRes::getMemo).filter(s -> s != null && !s.isBlank()).count())
                .build();
    }

    private WarehouseTransferDto.TransferListItemRes toListRow(
            WarehouseTransferHeader header,
            Infrastructure from,
            Infrastructure to,
            List<WarehouseTransferDto.TransferLineRes> lines
    ) {
        return WarehouseTransferDto.TransferListItemRes.builder()
                .transferNo(header.getTransferNo())
                .fromWarehouseCode(from == null ? "" : from.getCode())
                .fromWarehouseName(from == null ? "" : from.getName())
                .toWarehouseCode(to == null ? "" : to.getCode())
                .toWarehouseName(to == null ? "" : to.getName())
                .requestedBy(header.getRequestedBy())
                .requestedAt(header.getRequestedAt())
                .status(header.getStatus().name())
                .lines(lines)
                .skuCount(lines.size())
                .totalQty(lines.stream().mapToInt(line -> safeQty(line.getQty())).sum())
                .reasonCount((int) lines.stream().map(WarehouseTransferDto.TransferLineRes::getReason).filter(s -> s != null && !s.isBlank()).distinct().count())
                .memoCount((int) lines.stream().map(WarehouseTransferDto.TransferLineRes::getMemo).filter(s -> s != null && !s.isBlank()).count())
                .build();
    }

    private boolean matchesKeyword(WarehouseTransferDto.TransferListItemRes row, String keyword) {
        String joined = String.join(" ",
                blankTo(row.getTransferNo()),
                blankTo(row.getFromWarehouseCode()),
                blankTo(row.getFromWarehouseName()),
                blankTo(row.getToWarehouseCode()),
                blankTo(row.getToWarehouseName()),
                row.getLines().stream().map(line -> blankTo(line.getSkuCode()) + " " + blankTo(line.getItemName()) + " " + blankTo(line.getReason()) + " " + blankTo(line.getMemo())).collect(Collectors.joining(" "))
        ).toLowerCase(Locale.ROOT);
        return joined.contains(keyword);
    }

    private Map<Long, Infrastructure> loadInfraMap(List<WarehouseTransferHeader> headers) {
        Set<Long> infraIds = new HashSet<>();
        for (WarehouseTransferHeader header : headers) {
            infraIds.add(header.getFromWarehouseId());
            infraIds.add(header.getToWarehouseId());
        }
        return infrastructureRepository.findAllById(infraIds).stream()
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
    }

    private Map<Long, List<WarehouseTransferItem>> loadItemsMap(List<WarehouseTransferHeader> headers) {
        Set<Long> headerIds = headers.stream().map(WarehouseTransferHeader::getId).collect(Collectors.toSet());
        if (headerIds.isEmpty()) return Map.of();
        return itemRepository.findAllByHeader_IdIn(headerIds).stream()
                .collect(Collectors.groupingBy(item -> item.getHeader().getId()));
    }

    private Map<String, ProductMaster> loadMasterMap(Collection<ProductSku> skus) {
        Set<String> productCodes = skus.stream().map(ProductSku::getProductCode).collect(Collectors.toSet());
        if (productCodes.isEmpty()) return Map.of();
        return productMasterRepository.findAllByCodeIn(productCodes).stream()
                .collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));
    }

    private List<WarehouseTransferDto.TransferLineRes> toLines(
            List<WarehouseTransferItem> items,
            Map<Long, ProductSku> skuById,
            Map<String, ProductMaster> masterByCode
    ) {
        return items.stream().map(item -> {
            ProductSku sku = skuById.get(item.getSkuId());
            ProductMaster master = sku == null ? null : masterByCode.get(sku.getProductCode());
            return WarehouseTransferDto.TransferLineRes.builder()
                    .skuCode(sku == null ? "" : sku.getSkuCode())
                    .itemCode(master == null ? "" : master.getCode())
                    .itemName(master == null ? "" : master.getName())
                    .color(sku == null ? "" : sku.getColor())
                    .size(sku == null ? "" : sku.getSize())
                    .qty(item.getQuantity())
                    .reason(item.getReason())
                    .memo(item.getMemo())
                    .fromStockBefore(item.getFromAvailableBefore())
                    .toStockBefore(item.getToAvailableBefore())
                    .fromStockAfter(item.getFromAvailableAfter())
                    .toStockAfter(item.getToAvailableAfter())
                    .build();
        }).toList();
    }

    private ExecuteGroupContext validateGroup(List<WarehouseTransferDto.ExecuteLineReq> groupLines) {
        WarehouseTransferDto.ExecuteLineReq first = groupLines.get(0);
        Infrastructure fromWarehouse = loadWarehouse(first.getFromWarehouseCode());
        Infrastructure toWarehouse = loadWarehouse(first.getToWarehouseCode());
        if (fromWarehouse.getId().equals(toWarehouse.getId())) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        Map<String, ProductSku> skuByCode = new HashMap<>();
        Map<Long, ProductSku> skuById = new HashMap<>();
        Map<Long, Integer> fromAvailableBySkuId = new HashMap<>();
        Map<Long, Integer> toAvailableBySkuId = new HashMap<>();

        for (WarehouseTransferDto.ExecuteLineReq line : groupLines) {
            if (!Objects.equals(first.getFromWarehouseCode(), line.getFromWarehouseCode())
                    || !Objects.equals(first.getToWarehouseCode(), line.getToWarehouseCode())) {
                throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            }
            if (safeQty(line.getQty()) < 1) throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            if (line.getFromWarehouseCode().equals(line.getToWarehouseCode())) throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);

            ProductSku sku = productSkuRepository.findBySkuCode(line.getSkuCode())
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));
            skuByCode.put(line.getSkuCode(), sku);
            skuById.put(sku.getId(), sku);

            int fromAvailable = sumAvailableForTransfer(sku.getId(), fromWarehouse.getId());
            int toAvailable = sumAvailableForTransfer(sku.getId(), toWarehouse.getId());
            if (safeQty(line.getQty()) > Math.max(0, fromAvailable)) {
                throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            }
            fromAvailableBySkuId.put(sku.getId(), Math.max(0, fromAvailable));
            toAvailableBySkuId.put(sku.getId(), Math.max(0, toAvailable));
        }
        Map<String, ProductMaster> masterByCode = loadMasterMap(skuByCode.values());
        return new ExecuteGroupContext(
                fromWarehouse,
                toWarehouse,
                skuByCode,
                skuById,
                masterByCode,
                fromAvailableBySkuId,
                toAvailableBySkuId
        );
    }

    private int sumAvailableForTransfer(Long skuId, Long locationId) {
        return inventoryRepository.findAllBySkuIdAndLocationId(skuId, locationId).stream()
                .filter(inv -> InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inv.getInventoryStatus()))
                .mapToInt(inv -> Math.max(0, nz(inv.getAvailableQuantity())))
                .sum();
    }

    private Infrastructure loadWarehouse(String code) {
        return infrastructureRepository.findByCodeAndLocationType(code, LocationType.WAREHOUSE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND));
    }

    private String createTransferNoWithRetry(Date requestedAt) {
        for (int i = 0; i < 5; i++) {
            String transferNo = generateTransferNo(requestedAt);
            if (headerRepository.findByTransferNo(transferNo).isEmpty()) {
                return transferNo;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private WarehouseTransferHeader saveHeaderWithTransferNoRetry(
            ExecuteGroupContext context,
            String requestedBy,
            String reasonSummary,
            String memoSummary
    ) {
        for (int i = 0; i < 5; i++) {
            Date now = new Date();
            String transferNo = createTransferNoWithRetry(now);
            try {
                return headerRepository.save(
                        WarehouseTransferHeader.builder()
                                .transferNo(transferNo)
                                .fromWarehouseId(context.fromWarehouse.getId())
                                .toWarehouseId(context.toWarehouse.getId())
                                .status(WarehouseTransferStatus.READY_TO_SHIP)
                                .requestedBy(requestedBy)
                                .requestedAt(now)
                                .reasonSummary(reasonSummary)
                                .memoSummary(memoSummary)
                                .build()
                );
            } catch (DataIntegrityViolationException ignore) {
                // unique 충돌 시 재시도
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private String generateTransferNo(Date requestedAt) {
        LocalDate day = requestedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String dayToken = day.format(DAY_FORMAT);
        String prefix = "STF-" + dayToken + "-";
        int nextSeq = headerRepository.findTopByTransferNoStartingWithOrderByTransferNoDesc(prefix)
                .map(WarehouseTransferHeader::getTransferNo)
                .map(this::parseSeq)
                .orElse(0) + 1;
        return prefix + String.format("%05d", nextSeq);
    }

    private int parseSeq(String transferNo) {
        try {
            return Integer.parseInt(transferNo.substring(transferNo.lastIndexOf('-') + 1));
        } catch (Exception e) {
            return 0;
        }
    }

    private Date atStart(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String routeKey(WarehouseTransferDto.ExecuteLineReq line) {
        return line.getFromWarehouseCode() + "=>" + line.getToWarehouseCode();
    }

    private String buildReasonSummary(List<WarehouseTransferDto.ExecuteLineReq> lines) {
        Set<String> reasons = lines.stream().map(WarehouseTransferDto.ExecuteLineReq::getReason)
                .map(this::trimToNull).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (reasons.isEmpty()) return null;
        return String.join(", ", reasons);
    }

    private String buildMemoSummary(List<WarehouseTransferDto.ExecuteLineReq> lines) {
        List<String> memos = lines.stream().map(WarehouseTransferDto.ExecuteLineReq::getMemo)
                .map(this::trimToNull).filter(Objects::nonNull).toList();
        if (memos.isEmpty()) return null;
        return memos.size() + "건 메모";
    }

    private String trimToDefault(String s, String def) {
        String t = trimToNull(s);
        return t == null ? def : t;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private int safeQty(Integer qty) {
        return qty == null ? 0 : qty;
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private String blankTo(String s) {
        return s == null ? "" : s;
    }

    private void createWhTransferOutbound(
            WarehouseTransferHeader transferHeader,
            List<WarehouseTransferItem> transferItems,
            ExecuteGroupContext context,
            String requestedBy
    ) {
        // 1) 헤더를 멱등 upsert한다.
        Date now = transferHeader.getRequestedAt() == null ? new Date() : transferHeader.getRequestedAt();
        WhOutboundHeader outbound = upsertWhTransferOutboundHeader(transferHeader, context, requestedBy, now);

        // 2) 기존 라인이 없을 때만 outbound item을 스냅샷으로 생성한다.
        if (outboundItemRepository.findAllByOutboundHeaderIdOrderByIdAsc(outbound.getId()).isEmpty()) {
            List<WhOutboundItem> outboundItems = new ArrayList<>();
            for (WarehouseTransferItem transferItem : transferItems) {
                ProductSku sku = context.skuById.get(transferItem.getSkuId());
                if (sku == null) throw BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND);
                ProductMaster master = context.masterByCode.get(sku.getProductCode());
                if (master == null) throw BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND);

                outboundItems.add(WhOutboundItem.builder()
                        .outboundHeaderId(outbound.getId())
                        .sourceLineRefId(transferItem.getId())
                        .skuId(transferItem.getSkuId())
                        .skuCode(sku.getSkuCode())
                        .productCode(master.getCode())
                        .productName(master.getName())
                        .mainCategory(master.getCategoryCode())
                        .subCategory(master.getCategoryCode())
                        .color(sku.getColor())
                        .size(sku.getSize())
                        .unitPrice(sku.getUnitPrice())
                        .requestedQuantity(transferItem.getQuantity())
                        .memo(transferItem.getMemo())
                        .build());
            }
            outboundItemRepository.saveAll(outboundItems);
        }

        // 3) READY_TO_SHIP 최초 이력이 없으면 1건 생성한다.
        boolean historyExists = outboundStatusHistoryRepository
                .findAllByOutboundHeaderIdOrderByChangedAtAscIdAsc(outbound.getId())
                .stream()
                .anyMatch(h -> h.getStatus() == OutboundStatus.READY_TO_SHIP);
        if (!historyExists) {
            outboundStatusHistoryRepository.save(
                    WhOutboundStatusHistory.builder()
                            .outboundHeaderId(outbound.getId())
                            .status(OutboundStatus.READY_TO_SHIP)
                            .changedAt(now)
                            .changedByMemberId(requestedBy)
                            .changedByName(requestedBy)
                            .reason("WAREHOUSE_TRANSFER_EXECUTE")
                            .build()
            );
        }
    }

    private WhOutboundHeader upsertWhTransferOutboundHeader(
            WarehouseTransferHeader transferHeader,
            ExecuteGroupContext context,
            String requestedBy,
            Date now
    ) {
        // 1차 방어: 사전 조회
        Optional<WhOutboundHeader> existing = outboundHeaderRepository.findBySourceTypeAndSourceRefNoAndSourceRefSeq(
                OutboundSourceType.WAREHOUSE_TRANSFER,
                transferHeader.getTransferNo(),
                1
        );
        if (existing.isPresent()) return existing.get();

        try {
            // 2차 방어: 유니크 충돌 전까지 신규 생성 시도
            WhOutboundHeader saved = outboundHeaderRepository.save(
                    WhOutboundHeader.builder()
                            .outboundNo("TEMP-" + UUID.randomUUID())
                            .sourceType(OutboundSourceType.WAREHOUSE_TRANSFER)
                            .sourceRefNo(transferHeader.getTransferNo())
                            .sourceRefSeq(1)
                            .sourceRefId(transferHeader.getId())
                            .warehouseId(transferHeader.getFromWarehouseId())
                            .destinationType(OutboundDestinationType.WAREHOUSE)
                            .destinationId(transferHeader.getToWarehouseId())
                            .status(OutboundStatus.READY_TO_SHIP)
                            .totalRequestedQuantity(transferItemsQuantity(transferHeader.getId()))
                            .requestedAt(now)
                            .requestedByMemberId(requestedBy)
                            .requestedByName(requestedBy)
                            .memo(transferHeader.getMemoSummary())
                            .build()
            );
            saved.assignOutboundNo(generateOutboundNo(saved.getId(), now));
            return saved;
        } catch (DataIntegrityViolationException e) {
            // 동시성 충돌 시 재조회로 멱등 완료 처리
            return outboundHeaderRepository.findBySourceTypeAndSourceRefNoAndSourceRefSeq(
                            OutboundSourceType.WAREHOUSE_TRANSFER, transferHeader.getTransferNo(), 1)
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.FAIL));
        }
    }

    private int transferItemsQuantity(Long transferHeaderId) {
        return itemRepository.findAllByHeader_IdOrderByIdAsc(transferHeaderId).stream()
                .mapToInt(item -> Math.max(0, safeQty(item.getQuantity())))
                .sum();
    }

    private String generateOutboundNo(Long id, Date requestedAt) {
        // 출고번호 정책: WOB-YYYYMMDD-00001
        LocalDate day = requestedAt.toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        return "WOB-" + day.format(DAY_FORMAT) + "-" + String.format("%05d", id);
    }

    private WarehouseTransferDto.ExecuteFailedRouteRes toFailedRoute(
            List<WarehouseTransferDto.ExecuteLineReq> groupLines,
            BaseResponseStatus status
    ) {
        // 라우트 실패를 라인 단위 상세와 함께 응답 DTO로 변환한다.
        WarehouseTransferDto.ExecuteLineReq first = groupLines.get(0);
        List<WarehouseTransferDto.ExecuteLineFailureRes> failedLines = groupLines.stream()
                .map(line -> WarehouseTransferDto.ExecuteLineFailureRes.builder()
                        .lineId(line.getLineId())
                        .skuCode(line.getSkuCode())
                        .qty(safeQty(line.getQty()))
                        .reason(status.getMessage())
                        .build())
                .toList();
        return WarehouseTransferDto.ExecuteFailedRouteRes.builder()
                .fromWarehouseCode(first.getFromWarehouseCode())
                .toWarehouseCode(first.getToWarehouseCode())
                .errorCode(status.getCode())
                .errorMessage(status.getMessage())
                .failedLines(failedLines)
                .build();
    }

    private record ExecuteGroupResult(
            List<WarehouseTransferDto.ExecuteLineResultRes> lineResults,
            WarehouseTransferDto.ExecuteTransferRes transferRes
    ) {
    }

    private record ExecuteGroupContext(
            Infrastructure fromWarehouse,
            Infrastructure toWarehouse,
            Map<String, ProductSku> skuByCode,
            Map<Long, ProductSku> skuById,
            Map<String, ProductMaster> masterByCode,
            Map<Long, Integer> fromAvailableBySkuId,
            Map<Long, Integer> toAvailableBySkuId
    ) {
    }
}
