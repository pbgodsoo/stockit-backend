package org.example.stockitbe.hq.warehousetransfer;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.InventoryRepository;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.hq.inventory.model.InventoryStatusPolicy;
import org.example.stockitbe.warehouse.inbound.WhInboundService;
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
    private final InventoryService inventoryService;
    private final WhOutboundHeaderRepository outboundHeaderRepository;
    private final WhOutboundItemRepository outboundItemRepository;
    private final WhOutboundStatusHistoryRepository outboundStatusHistoryRepository;
    private final WhInboundService whInboundService;
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
        return WarehouseTransferDto.ExecuteRes.from(lines.size(), lineResults, createdTransfers, failedTransfers);
    }

    // 라우트 단위 처리(신규 트랜잭션)
    // 부분성공을 위해 각 라우트를 REQUIRES_NEW 경계에서 실행한다.
    private ExecuteGroupResult processRouteInNewTransaction(
            List<WarehouseTransferDto.ExecuteLineReq> groupLines,
            String requestedBy
    ) {
        // 라우트 단위 부분성공을 위해 신규 트랜잭션으로 경계를 분리한다.
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> processRoute(groupLines, requestedBy));
    }

    // 단일 라우트 처리
    // transfer 생성 -> outbound 생성 -> 상태 동기화 순으로 처리한다.
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

            items.add(line.toEntity(
                    WarehouseTransferDto.ExecuteLineContext.builder()
                            .header(savedHeader)
                            .skuId(sku.getId())
                            .fromAvailableBefore(fromBefore)
                            .toAvailableBefore(toBefore)
                            .fromAvailableAfter(Math.max(0, fromBefore - qty))
                            .toAvailableAfter(toBefore + qty)
                            .build()
            ));

            // 출발 창고 가용재고를 reserved 로 예약 (PO 매장발주 승인과 동일 패턴).
            // 송신 [출고 확정] 시 moveReservedToInTransit 가 이 reserved 를 inTransit 으로 옮긴다.
            int reserved = inventoryService.reserveForOutboundUpTo(context.fromWarehouse.getId(), sku.getId(), qty);
            if (reserved != qty) {
                throw BaseException.from(BaseResponseStatus.OUTBOUND_RESERVED_STOCK_NOT_ENOUGH);
            }

            lineResults.add(WarehouseTransferDto.ExecuteLineResultRes.from(line, transferNo, "SUCCESS"));
        }
        // 3) transfer item 저장 후 outbound를 생성/연계한다.
        List<WarehouseTransferItem> savedItems = itemRepository.saveAll(items);
        createWhTransferOutbound(savedHeader, savedItems, context, requestedBy);
        // 4) 재고이동 상태를 출고 흐름의 시작 상태로 동기화한다.
        savedHeader.markReadyToShip();

        WarehouseTransferDto.ExecuteTransferRes transferRes = WarehouseTransferDto.ExecuteTransferRes.from(
                savedHeader,
                context.fromWarehouse.getCode(),
                context.fromWarehouse.getName(),
                context.toWarehouse.getCode(),
                context.toWarehouse.getName(),
                items.size(),
                totalQty
        );
        return new ExecuteGroupResult(lineResults, transferRes);
    }

    // 재고이동 내역 목록 조회
    // 상태/기간/키워드 조건으로 헤더를 조회한 뒤 응답 DTO를 구성한다.
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

    // 재고이동 상세 조회
    // transferNo 기준 헤더/라인 정보를 조합해 상세 응답을 반환한다.
    @Transactional(readOnly = true)
    public WarehouseTransferDto.TransferDetailRes findTransferDetail(String transferNo) {
        WarehouseTransferHeader header = headerRepository.findByTransferNo(transferNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOT_FOUND_DATA));
        return toDetail(header);
    }

    // SKU 창고 분포 조회
    // SKU 기준으로 창고별 on-hand/available/safety 지표를 계산해 반환한다.
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
                    return WarehouseTransferDto.WarehouseSkuDistributionRes.from(
                            warehouse.getCode(),
                            warehouse.getName(),
                            warehouse.getRegion(),
                            onHand,
                            reserved,
                            available,
                            safety,
                            status,
                            stock == null ? null : stock.updatedAt
                    );
                })
                .sorted(Comparator.comparing(WarehouseTransferDto.WarehouseSkuDistributionRes::getWarehouseCode))
                .toList();
    }

    private static class WarehouseSkuStockAggregate {
        private int onHand;
        private int available;
        private Date updatedAt;
    }

    // 목록 응답 조합
    // 헤더 목록에 대해 창고/라인/상품 정보를 결합하고 키워드 필터를 적용한다.
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

    // 상세 응답 조합
    // 단일 헤더의 창고/라인/상품 정보를 결합해 상세 DTO를 생성한다.
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
        return WarehouseTransferDto.TransferDetailRes.from(
                header,
                from == null ? "" : from.getCode(),
                from == null ? "" : from.getName(),
                to == null ? "" : to.getCode(),
                to == null ? "" : to.getName(),
                lines,
                lines.size(),
                lines.stream().mapToInt(line -> safeQty(line.getQty())).sum(),
                (int) lines.stream().map(WarehouseTransferDto.TransferLineRes::getReason).filter(s -> s != null && !s.isBlank()).distinct().count(),
                (int) lines.stream().map(WarehouseTransferDto.TransferLineRes::getMemo).filter(s -> s != null && !s.isBlank()).count()
        );
    }

    // 목록 행 조합
    // 헤더와 계산된 라인 통계를 기반으로 리스트 1행 DTO를 구성한다.
    private WarehouseTransferDto.TransferListItemRes toListRow(
            WarehouseTransferHeader header,
            Infrastructure from,
            Infrastructure to,
            List<WarehouseTransferDto.TransferLineRes> lines
    ) {
        return WarehouseTransferDto.TransferListItemRes.from(
                header,
                from == null ? "" : from.getCode(),
                from == null ? "" : from.getName(),
                to == null ? "" : to.getCode(),
                to == null ? "" : to.getName(),
                lines,
                lines.size(),
                lines.stream().mapToInt(line -> safeQty(line.getQty())).sum(),
                (int) lines.stream().map(WarehouseTransferDto.TransferLineRes::getReason).filter(s -> s != null && !s.isBlank()).distinct().count(),
                (int) lines.stream().map(WarehouseTransferDto.TransferLineRes::getMemo).filter(s -> s != null && !s.isBlank()).count()
        );
    }

    // 키워드 매칭
    // 헤더/창고/라인 텍스트를 합쳐 포함 검색을 수행한다.
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

    // 헤더 기준 창고 맵 로드
    // 출발/도착 창고 ID를 수집해 인프라 맵으로 조회한다.
    private Map<Long, Infrastructure> loadInfraMap(List<WarehouseTransferHeader> headers) {
        Set<Long> infraIds = new HashSet<>();
        for (WarehouseTransferHeader header : headers) {
            infraIds.add(header.getFromWarehouseId());
            infraIds.add(header.getToWarehouseId());
        }
        return infrastructureRepository.findAllById(infraIds).stream()
                .collect(Collectors.toMap(Infrastructure::getId, Function.identity()));
    }

    // 헤더 기준 아이템 그룹 맵 로드
    // transfer header ID별 아이템 리스트를 그룹핑해 반환한다.
    private Map<Long, List<WarehouseTransferItem>> loadItemsMap(List<WarehouseTransferHeader> headers) {
        Set<Long> headerIds = headers.stream().map(WarehouseTransferHeader::getId).collect(Collectors.toSet());
        if (headerIds.isEmpty()) return Map.of();
        return itemRepository.findAllByHeader_IdIn(headerIds).stream()
                .collect(Collectors.groupingBy(item -> item.getHeader().getId()));
    }

    // 상품 마스터 맵 로드
    // SKU 컬렉션에서 productCode를 추출해 ProductMaster 맵을 구성한다.
    private Map<String, ProductMaster> loadMasterMap(Collection<ProductSku> skus) {
        Set<String> productCodes = skus.stream().map(ProductSku::getProductCode).collect(Collectors.toSet());
        if (productCodes.isEmpty()) return Map.of();
        return productMasterRepository.findAllByCodeIn(productCodes).stream()
                .collect(Collectors.toMap(ProductMaster::getCode, Function.identity()));
    }

    // 라인 DTO 변환
    // transfer item과 SKU/마스터 스냅샷을 결합해 응답 라인으로 변환한다.
    private List<WarehouseTransferDto.TransferLineRes> toLines(
            List<WarehouseTransferItem> items,
            Map<Long, ProductSku> skuById,
            Map<String, ProductMaster> masterByCode
    ) {
        return items.stream().map(item -> {
            ProductSku sku = skuById.get(item.getSkuId());
            ProductMaster master = sku == null ? null : masterByCode.get(sku.getProductCode());
            return WarehouseTransferDto.TransferLineRes.from(
                    item,
                    sku == null ? "" : sku.getSkuCode(),
                    master == null ? "" : master.getCode(),
                    master == null ? "" : master.getName(),
                    sku == null ? "" : sku.getColor(),
                    sku == null ? "" : sku.getSize()
            );
        }).toList();
    }

    // 라우트 검증
    // 동일 라우트 강제, 수량 검증, SKU/가용수량 검증, 후속 매핑 컨텍스트를 구성한다.
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

    // 라우트 가용재고 합산
    // 이동 가능 상태 재고만 대상으로 available 수량을 합산한다.
    private int sumAvailableForTransfer(Long skuId, Long locationId) {
        return inventoryRepository.findAllBySkuIdAndLocationId(skuId, locationId).stream()
                .filter(inv -> InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.contains(inv.getInventoryStatus()))
                .mapToInt(inv -> Math.max(0, nz(inv.getAvailableQuantity())))
                .sum();
    }

    // 창고 조회
    // 코드 + WAREHOUSE 타입으로 인프라를 조회한다.
    private Infrastructure loadWarehouse(String code) {
        return infrastructureRepository.findByCodeAndLocationType(code, LocationType.WAREHOUSE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND));
    }

    // 이동번호 생성 재시도
    // transferNo 중복 충돌을 피하기 위해 최대 5회 생성/검증을 수행한다.
    private String createTransferNoWithRetry(Date requestedAt) {
        for (int i = 0; i < 5; i++) {
            String transferNo = generateTransferNo(requestedAt);
            if (headerRepository.findByTransferNo(transferNo).isEmpty()) {
                return transferNo;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    // 재고이동 헤더 저장 재시도
    // 번호 유니크 충돌 시 재시도하며 헤더를 저장한다.
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
                WarehouseTransferDto.ExecuteReq requestBridge = WarehouseTransferDto.ExecuteReq.builder()
                        .requestedBy(requestedBy)
                        .lines(List.of())
                        .build();
                return headerRepository.save(
                        requestBridge.toEntity(
                                WarehouseTransferDto.ExecuteHeaderContext.builder()
                                        .transferNo(transferNo)
                                        .fromWarehouseId(context.fromWarehouse.getId())
                                        .toWarehouseId(context.toWarehouse.getId())
                                        .status(WarehouseTransferStatus.READY_TO_SHIP)
                                        .requestedBy(requestedBy)
                                        .requestedAt(now)
                                        .reasonSummary(reasonSummary)
                                        .memoSummary(memoSummary)
                                        .build()
                        )
                );
            } catch (DataIntegrityViolationException ignore) {
                // unique 충돌 시 재시도
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    // 이동번호 생성
    // 정책: WTF-YYYYMMDD-00001
    private String generateTransferNo(Date requestedAt) {
        LocalDate day = requestedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String dayToken = day.format(DAY_FORMAT);
        String prefix = "WTF-" + dayToken + "-";
        int nextSeq = headerRepository.findTopByTransferNoStartingWithOrderByTransferNoDesc(prefix)
                .map(WarehouseTransferHeader::getTransferNo)
                .map(this::parseSeq)
                .orElse(0) + 1;
        return prefix + String.format("%05d", nextSeq);
    }

    // 이동번호 시퀀스 파싱
    // 번호 접미부를 정수로 파싱하며 실패 시 0을 반환한다.
    private int parseSeq(String transferNo) {
        try {
            return Integer.parseInt(transferNo.substring(transferNo.lastIndexOf('-') + 1));
        } catch (Exception e) {
            return 0;
        }
    }

    // 날짜 시작시각 변환
    // LocalDate를 시스템 타임존 기준 00:00:00 Date로 변환한다.
    private Date atStart(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // 라우트 키 생성
    // from/to 창고코드 조합으로 그룹핑 키를 만든다.
    private String routeKey(WarehouseTransferDto.ExecuteLineReq line) {
        return line.getFromWarehouseCode() + "=>" + line.getToWarehouseCode();
    }

    // 사유 요약 생성
    // 라인별 reason을 중복 제거해 헤더 요약 문자열로 만든다.
    private String buildReasonSummary(List<WarehouseTransferDto.ExecuteLineReq> lines) {
        Set<String> reasons = lines.stream().map(WarehouseTransferDto.ExecuteLineReq::getReason)
                .map(this::trimToNull).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (reasons.isEmpty()) return null;
        return String.join(", ", reasons);
    }

    // 메모 요약 생성
    // 라인 메모 개수를 "N건 메모" 형식으로 요약한다.
    private String buildMemoSummary(List<WarehouseTransferDto.ExecuteLineReq> lines) {
        List<String> memos = lines.stream().map(WarehouseTransferDto.ExecuteLineReq::getMemo)
                .map(this::trimToNull).filter(Objects::nonNull).toList();
        if (memos.isEmpty()) return null;
        return memos.size() + "건 메모";
    }

    // 기본값 보정
    // 문자열이 비어 있으면 기본값을 반환한다.
    private String trimToDefault(String s, String def) {
        String t = trimToNull(s);
        return t == null ? def : t;
    }

    // 문자열 정규화
    // trim 후 빈 문자열을 null로 변환한다.
    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // 수량 null-safe 변환
    // null이면 0으로 보정한다.
    private int safeQty(Integer qty) {
        return qty == null ? 0 : qty;
    }

    // 숫자 null-safe 변환
    // null이면 0으로 보정한다.
    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    // 문자열 null-safe 변환
    // null이면 빈 문자열로 보정한다.
    private String blankTo(String s) {
        return s == null ? "" : s;
    }

    // 재고이동 -> 출고 생성 오케스트레이션
    // outbound header/item/history를 멱등하게 생성한다.
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

        // 4) 도착 창고 입고 도큐먼트 mirror INSERT — ERP GRN 패턴.
        //    inbound 측이 멱등 보장 (findBySourceRefNoAndInboundType).
        whInboundService.createFromOutbound(outbound);
    }

    // WH_TRANSFER 출고 헤더 멱등 upsert
    // 사전조회 후 신규 생성, 유니크 충돌 시 재조회로 복구한다.
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

    // transfer 아이템 총수량 계산
    // 헤더 기준 라인 requestedQuantity 합계를 반환한다.
    private int transferItemsQuantity(Long transferHeaderId) {
        return itemRepository.findAllByHeader_IdOrderByIdAsc(transferHeaderId).stream()
                .mapToInt(item -> Math.max(0, safeQty(item.getQuantity())))
                .sum();
    }

    // 출고번호 생성
    // 정책: WOB-YYYYMMDD-00001
    private String generateOutboundNo(Long id, Date requestedAt) {
        // 출고번호 정책: WOB-YYYYMMDD-00001
        LocalDate day = requestedAt.toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        return "WOB-" + day.format(DAY_FORMAT) + "-" + String.format("%05d", id);
    }

    // 실패 라우트 DTO 변환
    // 라우트 공통 오류와 라인별 실패 정보를 함께 매핑한다.
    private WarehouseTransferDto.ExecuteFailedRouteRes toFailedRoute(
            List<WarehouseTransferDto.ExecuteLineReq> groupLines,
            BaseResponseStatus status
    ) {
        // 라우트 실패를 라인 단위 상세와 함께 응답 DTO로 변환한다.
        WarehouseTransferDto.ExecuteLineReq first = groupLines.get(0);
        List<WarehouseTransferDto.ExecuteLineFailureRes> failedLines = groupLines.stream()
                .map(line -> WarehouseTransferDto.ExecuteLineFailureRes.from(line, status.getMessage()))
                .toList();
        return WarehouseTransferDto.ExecuteFailedRouteRes.from(
                first.getFromWarehouseCode(),
                first.getToWarehouseCode(),
                status.getCode(),
                status.getMessage(),
                failedLines
        );
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
