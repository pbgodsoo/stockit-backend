package org.example.stockitbe.warehouse.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderItemRepository;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderRepository;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderService;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderStatusHistoryRepository;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderItem;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatusHistory;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.inbound.model.InboundType;
import org.example.stockitbe.warehouse.inbound.model.WhInboundDto;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundItem;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 창고 입고 도메인 service (ERP 표준 — Goods Receipt Note 패턴).
 *
 * inbound 자체는 진행 단계 status 컬럼이 없다. 진행 단계의 진실 원천은 source 도메인 —
 *   - PURCHASE_ORDER : PurchaseOrder.status (READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED)
 *   - WAREHOUSE_TRANSFER : WhOutbound.status (READY_TO_SHIP/IN_TRANSIT/ARRIVED)
 *
 * inbound 의 책임:
 *   1. createFromPurchaseOrder / createFromOutbound — source 시점 inbound row INSERT (멱등)
 *   2. confirmInbound — 도착 후 자산 확정 (completedAt + confirmedBy* + 실재고 인식 + (PO 한정) source mirror)
 *   3. findAll/findByCode — inbound + source LEFT JOIN read 로 status / statusHistory 응답 채움
 *
 * 인벤토리 hook:
 *   - PURCHASE_ORDER 가용재고+   : PurchaseOrderService.startInTransit (PR #173 위치, ADR-024)
 *   - WAREHOUSE_TRANSFER 가용재고+: WhOutboundService.confirm() 의 destinationType=WAREHOUSE 분기 (다른 팀원 영역)
 *   - 실재고 인식                : confirmInbound 시점 (markPhysical)
 *   - 송신측 inTransit−          : confirmInbound 의 WAREHOUSE_TRANSFER 분기 (reduceInTransit)
 */
@Service
@RequiredArgsConstructor
public class WhInboundService {

    private final WhInboundHeaderRepository headerRepository;
    private final WhInboundItemRepository itemRepository;
    private final InboundCodeGenerator codeGenerator;
    private final InventoryService inventoryService;
    private final InfrastructureRepository infrastructureRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderStatusHistoryRepository purchaseOrderStatusHistoryRepository;
    private final WhOutboundHeaderRepository outboundHeaderRepository;
    private final WhOutboundItemRepository outboundItemRepository;
    private final WhOutboundStatusHistoryRepository outboundStatusHistoryRepository;

    /**
     * 본사 발주 입고 row 생성 (READY_TO_SHIP 진입 시점에 PurchaseOrderBatchService 가 호출).
     * 멱등 — 같은 sourceRefNo + PURCHASE_ORDER 이미 있으면 기존 return.
     */
    @Transactional
    public WhInboundHeader createFromPurchaseOrder(PurchaseOrder po) {
        Optional<WhInboundHeader> existing = headerRepository.findBySourceRefNoAndInboundType(
                po.getCode(), InboundType.PURCHASE_ORDER);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<PurchaseOrderItem> poItems = purchaseOrderItemRepository.findAllByPurchaseOrderId(po.getId());
        long totalQuantity = poItems.stream().mapToInt(PurchaseOrderItem::getQuantity).sum();

        WhInboundHeader.WhInboundHeaderBuilder headerBuilder = WhInboundHeader.builder()
                .inboundType(InboundType.PURCHASE_ORDER)
                .sourceRefNo(po.getCode())
                .sourceRefId(po.getId())
                .warehouse(po.getWarehouse())
                .warehouseName(po.getWarehouseName())
                .sourceName(po.getVendorName())
                .totalQuantity(totalQuantity)
                .totalAmount(po.getTotalAmount());

        // items 시점 복사 — replaceItems + cascade=ALL 로 부모 save 시 자식 자동 INSERT.
        List<WhInboundItem> items = poItems.stream()
                .map(it -> WhInboundItem.builder()
                        .productCode(it.getProductCode())
                        .productName(it.getProductName())
                        .skuCode(it.getSkuCode())
                        .color(it.getColor())
                        .size(it.getSize())
                        .quantity(it.getQuantity())
                        .unitPrice(it.getUnitPrice())
                        .subtotal(it.getSubtotal())
                        .build())
                .toList();

        return saveHeaderWithCodeRetry(headerBuilder, items);
    }

    /**
     * 창고간 이동 출고에 연동된 입고 row 생성 — WarehouseTransferService.execute() 의
     * 다른 팀원 wiring 한 줄 (`whInboundService.createFromOutbound(savedOutbound)`) 에서 호출.
     * 멱등 — 같은 outboundNo + WAREHOUSE_TRANSFER 이미 있으면 기존 return.
     * 도착 창고(destinationId) 를 warehouse 로, 출발 창고명을 sourceName 으로 박는다.
     */
    @Transactional
    public WhInboundHeader createFromOutbound(WhOutboundHeader outbound) {
        Optional<WhInboundHeader> existing = headerRepository.findBySourceRefNoAndInboundType(
                outbound.getOutboundNo(), InboundType.WAREHOUSE_TRANSFER);
        if (existing.isPresent()) {
            return existing.get();
        }

        Infrastructure destinationWarehouse = infrastructureRepository.findById(outbound.getDestinationId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND));
        String fromWarehouseName = infrastructureRepository.findById(outbound.getWarehouseId())
                .map(Infrastructure::getName)
                .orElse("");

        List<WhOutboundItem> outItems = outboundItemRepository.findAllByOutboundHeaderIdOrderByIdAsc(outbound.getId());
        long totalQuantity = outItems.stream()
                .mapToInt(i -> i.getRequestedQuantity() == null ? 0 : i.getRequestedQuantity())
                .sum();

        WhInboundHeader.WhInboundHeaderBuilder headerBuilder = WhInboundHeader.builder()
                .inboundType(InboundType.WAREHOUSE_TRANSFER)
                .sourceRefNo(outbound.getOutboundNo())
                .sourceRefId(outbound.getId())
                .warehouse(destinationWarehouse)
                .warehouseName(destinationWarehouse.getName())
                .sourceName("창고간 이동 — " + fromWarehouseName)
                .totalQuantity(totalQuantity)
                .totalAmount(null);

        // items 시점 복사 — unitPrice/subtotal 은 transfer 에서 의미 약함 (null 허용).
        List<WhInboundItem> items = outItems.stream()
                .map(i -> WhInboundItem.builder()
                        .productCode(i.getProductCode())
                        .productName(i.getProductName())
                        .skuCode(i.getSkuCode())
                        .color(i.getColor())
                        .size(i.getSize())
                        .quantity(i.getRequestedQuantity())
                        .unitPrice(null)
                        .subtotal(null)
                        .build())
                .toList();

        return saveHeaderWithCodeRetry(headerBuilder, items);
    }

    /**
     * 입고 확정 (도착 후 자산 인식). 창고 [입고 확정] 매뉴얼 트리거.
     * 자기 창고 격리: me.locationCode 의 warehouseId 와 inbound.warehouseId 매칭. 불일치 시 INBOUND_NOT_FOUND.
     * 책임 (PURCHASE_ORDER):
     *   1. PO.status==ARRIVED 검증
     *   2. inbound.markConfirmed (completedAt + confirmedBy*)
     *   3. inventoryService.markPhysical (수신 가용→실재고)
     *   4. purchaseOrderService.completeFromInbound (PO 종료 단계 mirror)
     * 책임 (WAREHOUSE_TRANSFER):
     *   1. outbound.status==ARRIVED 검증
     *   2. inbound.markConfirmed
     *   3. inventoryService.markPhysical (수신 가용→실재고) + reduceInTransit (송신 inTransit−)
     *   4. source mirror 없음 — outbound 의 ARRIVED 가 종료 단계
     */
    @Transactional
    public WhInboundHeader confirmInbound(String inboundCode, AuthUserDetails me) {
        WhInboundHeader inbound = headerRepository.findByInboundCode(inboundCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND));

        Long myWarehouseId = resolveWarehouseId(me.getLocationCode());
        if (!inbound.getWarehouse().getId().equals(myWarehouseId)) {
            throw BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND);
        }

        // source 도메인 별 ARRIVED 검증 + 송신 창고 ID 확보 (transfer 한정).
        Long fromWarehouseId = null;
        if (inbound.getInboundType() == InboundType.PURCHASE_ORDER) {
            PurchaseOrder po = purchaseOrderRepository.findByCode(inbound.getSourceRefNo())
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.PURCHASE_ORDER_NOT_FOUND));
            if (po.getStatus() != PurchaseOrderStatus.ARRIVED) {
                throw BaseException.from(BaseResponseStatus.INBOUND_NOT_CONFIRMABLE);
            }
        } else if (inbound.getInboundType() == InboundType.WAREHOUSE_TRANSFER) {
            WhOutboundHeader outbound = outboundHeaderRepository.findByOutboundNo(inbound.getSourceRefNo())
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_OUTBOUND_NOT_FOUND));
            if (outbound.getStatus() != OutboundStatus.ARRIVED) {
                throw BaseException.from(BaseResponseStatus.INBOUND_NOT_CONFIRMABLE);
            }
            fromWarehouseId = outbound.getWarehouseId();
        }

        String byName = me.getName() + " (" + me.getLocationName() + ")";
        Date now = new Date();
        inbound.markConfirmed(now, me.getEmployeeCode(), byName);

        // 인벤토리 hook — 수신 실재고 인식 (양쪽 inboundType 공통)
        List<WhInboundItem> items = itemRepository.findAllByInboundHeaderIdOrderByIdAsc(inbound.getId());
        items.forEach(item -> inventoryService.markPhysical(
                inbound.getWarehouse().getId(), item.getSkuCode(), item.getQuantity()));

        // 인벤토리 hook — 송신측 inTransit 차감 (WAREHOUSE_TRANSFER 한정)
        if (inbound.getInboundType() == InboundType.WAREHOUSE_TRANSFER && fromWarehouseId != null) {
            final Long fromId = fromWarehouseId;
            items.forEach(item -> inventoryService.reduceInTransit(fromId, item.getSkuCode(), item.getQuantity()));
        }

        // source 도메인 mirror — PURCHASE_ORDER 만 종료 단계 박음.
        // WAREHOUSE_TRANSFER 는 outbound 의 ARRIVED 가 종료 — mirror 없음.
        if (inbound.getInboundType() == InboundType.PURCHASE_ORDER) {
            purchaseOrderService.completeFromInbound(inbound.getSourceRefNo(), me);
        }

        return inbound;
    }

    /**
     * 자기 창고 입고 목록 — PURCHASE_ORDER + WAREHOUSE_TRANSFER 둘 다 노출.
     * source 도메인을 batch 조회해 effectiveStatus 산출.
     * source 가 REQUESTED/APPROVED/CANCELLED 면 입고 화면에서 숨김 (null 반환).
     */
    @Transactional(readOnly = true)
    public List<WhInboundDto.ListRes> findAll(AuthUserDetails me, String statusFilter,
                                              LocalDate from, LocalDate to) {
        Long warehouseId = resolveWarehouseId(me.getLocationCode());

        List<WhInboundHeader> headers = headerRepository.findAllByWarehouseIdOrderByCreatedAtDesc(warehouseId);

        // 기간 필터
        if (from != null) {
            Date fromDate = toDate(from);
            headers = headers.stream().filter(h -> !h.getCreatedAt().before(fromDate)).toList();
        }
        if (to != null) {
            Date toExclusive = toDate(to.plusDays(1));
            headers = headers.stream().filter(h -> h.getCreatedAt().before(toExclusive)).toList();
        }

        if (headers.isEmpty()) return List.of();

        // source 도메인 batch lookup (inboundType 분기)
        Map<String, PurchaseOrder> poByCode = batchLoadPurchaseOrders(headers);
        Map<String, WhOutboundHeader> outboundByNo = batchLoadOutbounds(headers);

        // items batch 조회 (N+1 회피)
        List<Long> headerIds = headers.stream().map(WhInboundHeader::getId).toList();
        Map<Long, List<WhInboundItem>> itemsByHeader = itemRepository
                .findAllByInboundHeaderIdInOrderByIdAsc(headerIds).stream()
                .collect(Collectors.groupingBy(it -> it.getInboundHeader().getId()));

        return headers.stream()
                .map(h -> {
                    String effectiveStatus = resolveEffectiveStatus(h, poByCode, outboundByNo);
                    if (effectiveStatus == null) return null;
                    if (statusFilter != null && !statusFilter.equals(effectiveStatus)) return null;
                    return WhInboundDto.ListRes.from(h, itemsByHeader.getOrDefault(h.getId(), List.of()), effectiveStatus);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 입고 단건 — 자기 창고 row 만 조회 가능.
     * statusHistory 는 source 도메인 history join + inbound.completedAt 4단계째 append.
     */
    @Transactional(readOnly = true)
    public WhInboundDto.DetailRes findByCode(AuthUserDetails me, String inboundCode) {
        Long myWarehouseId = resolveWarehouseId(me.getLocationCode());
        WhInboundHeader inbound = headerRepository.findByInboundCode(inboundCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND));
        if (!inbound.getWarehouse().getId().equals(myWarehouseId)) {
            throw BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND);
        }
        List<WhInboundItem> items = itemRepository
                .findAllByInboundHeaderIdOrderByIdAsc(inbound.getId());

        String effectiveStatus;
        List<WhInboundDto.StatusHistoryRes> history;

        if (inbound.getInboundType() == InboundType.PURCHASE_ORDER) {
            PurchaseOrder po = purchaseOrderRepository.findByCode(inbound.getSourceRefNo()).orElse(null);
            effectiveStatus = resolvePoStatus(inbound, po);
            history = buildPoHistory(inbound, po);
        } else {
            WhOutboundHeader outbound = outboundHeaderRepository.findByOutboundNo(inbound.getSourceRefNo()).orElse(null);
            effectiveStatus = resolveOutboundStatus(inbound, outbound);
            history = buildOutboundHistory(inbound, outbound);
        }

        return WhInboundDto.DetailRes.from(inbound, items, effectiveStatus, history);
    }

    /**
     * 기존 PO 입고 데이터 → inbound row 일괄 변환.
     * dev/시연 1회성. 멱등 — 이미 inbound row 있으면 skip.
     */
    @Transactional
    public WhInboundDto.BackfillRes backfillFromPurchaseOrders() {
        List<PurchaseOrderStatus> targetStatuses = List.of(
                PurchaseOrderStatus.READY_TO_SHIP,
                PurchaseOrderStatus.IN_TRANSIT,
                PurchaseOrderStatus.ARRIVED,
                PurchaseOrderStatus.COMPLETED);

        List<PurchaseOrder> orders = new ArrayList<>();
        for (PurchaseOrderStatus s : targetStatuses) {
            orders.addAll(purchaseOrderRepository.findAllByStatus(s));
        }

        int created = 0;
        int skipped = 0;
        List<String> createdCodes = new ArrayList<>();

        for (PurchaseOrder po : orders) {
            Optional<WhInboundHeader> existing = headerRepository
                    .findBySourceRefNoAndInboundType(po.getCode(), InboundType.PURCHASE_ORDER);
            if (existing.isPresent()) {
                skipped++;
                continue;
            }

            List<PurchaseOrderItem> poItems = purchaseOrderItemRepository.findAllByPurchaseOrderId(po.getId());
            long totalQuantity = poItems.stream().mapToInt(PurchaseOrderItem::getQuantity).sum();

            Date completedAt = po.getStatus() == PurchaseOrderStatus.COMPLETED
                    ? po.getUpdatedAt() : null;

            WhInboundHeader.WhInboundHeaderBuilder builder = WhInboundHeader.builder()
                    .inboundType(InboundType.PURCHASE_ORDER)
                    .sourceRefNo(po.getCode())
                    .sourceRefId(po.getId())
                    .warehouse(po.getWarehouse())
                    .warehouseName(po.getWarehouseName())
                    .sourceName(po.getVendorName())
                    .totalQuantity(totalQuantity)
                    .totalAmount(po.getTotalAmount())
                    .completedAt(completedAt);

            List<WhInboundItem> items = poItems.stream()
                    .map(it -> WhInboundItem.builder()
                            .productCode(it.getProductCode())
                            .productName(it.getProductName())
                            .skuCode(it.getSkuCode())
                            .color(it.getColor())
                            .size(it.getSize())
                            .quantity(it.getQuantity())
                            .unitPrice(it.getUnitPrice())
                            .subtotal(it.getSubtotal())
                            .build())
                    .toList();

            WhInboundHeader header = saveHeaderWithCodeRetry(builder, items);

            created++;
            createdCodes.add(header.getInboundCode());
        }

        return WhInboundDto.BackfillRes.builder()
                .createdCount(created)
                .skippedCount(skipped)
                .createdInboundCodes(createdCodes)
                .build();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /**
     * 목록용 effectiveStatus 산출 — inboundType 분기 후 PO/outbound 분기 status 룰 위임.
     */
    private String resolveEffectiveStatus(WhInboundHeader inbound,
                                          Map<String, PurchaseOrder> poByCode,
                                          Map<String, WhOutboundHeader> outboundByNo) {
        if (inbound.getInboundType() == InboundType.PURCHASE_ORDER) {
            return resolvePoStatus(inbound, poByCode.get(inbound.getSourceRefNo()));
        }
        return resolveOutboundStatus(inbound, outboundByNo.get(inbound.getSourceRefNo()));
    }

    /**
     * PO 기반 status 룰:
     *   - inbound.completedAt != null → "COMPLETED"
     *   - else → PO.status (READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED 만 노출, 그 외 null=숨김)
     */
    private String resolvePoStatus(WhInboundHeader inbound, PurchaseOrder po) {
        if (inbound.getCompletedAt() != null) return "COMPLETED";
        if (po == null) return null;
        return switch (po.getStatus()) {
            case READY_TO_SHIP -> "READY_TO_SHIP";
            case IN_TRANSIT -> "IN_TRANSIT";
            case ARRIVED -> "ARRIVED";
            case COMPLETED -> "COMPLETED";
            case REQUESTED, APPROVED, CANCELLED -> null;
        };
    }

    /**
     * outbound 기반 status 룰:
     *   - inbound.completedAt != null → "COMPLETED"
     *   - else → outbound.status (READY_TO_SHIP/IN_TRANSIT/ARRIVED 그대로 노출)
     */
    private String resolveOutboundStatus(WhInboundHeader inbound, WhOutboundHeader outbound) {
        if (inbound.getCompletedAt() != null) return "COMPLETED";
        if (outbound == null) return null;
        return switch (outbound.getStatus()) {
            case READY_TO_SHIP -> "READY_TO_SHIP";
            case IN_TRANSIT -> "IN_TRANSIT";
            case ARRIVED -> "ARRIVED";
        };
    }

    /**
     * PO history 매핑 + inbound.completedAt 이 박혀있으면 마지막 COMPLETED 항목 append.
     * note: PO history 의 PurchaseOrderStatus 중 화면에 안 보이는 단계(REQUESTED/APPROVED/CANCELLED)
     *       도 그대로 노출 — 입고 화면 FE 가 필터링 책임 (PR #200 패턴).
     */
    private List<WhInboundDto.StatusHistoryRes> buildPoHistory(WhInboundHeader inbound, PurchaseOrder po) {
        if (po == null) {
            return appendCompletedIfAny(Collections.emptyList(), inbound);
        }
        List<PurchaseOrderStatusHistory> rows = purchaseOrderStatusHistoryRepository
                .findAllByPurchaseOrderIdOrderByChangedAtAsc(po.getId());
        List<WhInboundDto.StatusHistoryRes> mapped = rows.stream()
                .map(WhInboundDto.StatusHistoryRes::fromPo)
                .collect(Collectors.toCollection(ArrayList::new));
        return appendCompletedIfAny(mapped, inbound);
    }

    /**
     * outbound history 매핑 + inbound.completedAt 박힌 경우 마지막 COMPLETED append.
     */
    private List<WhInboundDto.StatusHistoryRes> buildOutboundHistory(WhInboundHeader inbound, WhOutboundHeader outbound) {
        if (outbound == null) {
            return appendCompletedIfAny(Collections.emptyList(), inbound);
        }
        List<WhOutboundStatusHistory> rows = outboundStatusHistoryRepository
                .findAllByOutboundHeaderIdOrderByChangedAtAscIdAsc(outbound.getId());
        List<WhInboundDto.StatusHistoryRes> mapped = rows.stream()
                .map(WhInboundDto.StatusHistoryRes::fromOutbound)
                .collect(Collectors.toCollection(ArrayList::new));
        return appendCompletedIfAny(mapped, inbound);
    }

    private List<WhInboundDto.StatusHistoryRes> appendCompletedIfAny(
            List<WhInboundDto.StatusHistoryRes> history, WhInboundHeader inbound) {
        if (inbound.getCompletedAt() == null) return history;
        history.add(WhInboundDto.StatusHistoryRes.completed(inbound.getCompletedAt(), inbound.getConfirmedByName()));
        return history;
    }

    private Map<String, PurchaseOrder> batchLoadPurchaseOrders(List<WhInboundHeader> headers) {
        Set<String> codes = headers.stream()
                .filter(h -> h.getInboundType() == InboundType.PURCHASE_ORDER)
                .map(WhInboundHeader::getSourceRefNo)
                .collect(Collectors.toSet());
        if (codes.isEmpty()) return Map.of();
        return purchaseOrderRepository.findAll().stream()
                .filter(po -> codes.contains(po.getCode()))
                .collect(Collectors.toMap(PurchaseOrder::getCode, Function.identity()));
    }

    private Map<String, WhOutboundHeader> batchLoadOutbounds(List<WhInboundHeader> headers) {
        List<String> outboundNos = headers.stream()
                .filter(h -> h.getInboundType() == InboundType.WAREHOUSE_TRANSFER)
                .map(WhInboundHeader::getSourceRefNo)
                .toList();
        if (outboundNos.isEmpty()) return Map.of();
        return outboundHeaderRepository.findAllByOutboundNoIn(outboundNos).stream()
                .collect(Collectors.toMap(WhOutboundHeader::getOutboundNo, Function.identity()));
    }

    private Long resolveWarehouseId(String locationCode) {
        return infrastructureRepository
                .findByCodeAndLocationType(locationCode, LocationType.WAREHOUSE)
                .map(Infrastructure::getId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND));
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 헤더 + items 한 트랜잭션에 INSERT. cascade=ALL 이 자식 자동 INSERT.
     * inboundCode unique 충돌 시 재시도 (최대 5회).
     */
    private WhInboundHeader saveHeaderWithCodeRetry(WhInboundHeader.WhInboundHeaderBuilder builder,
                                                     List<WhInboundItem> items) {
        for (int i = 0; i < 5; i++) {
            String code = codeGenerator.nextCode(new Date());
            try {
                WhInboundHeader header = builder.inboundCode(code).build();
                header.replaceItems(items);
                return headerRepository.save(header);
            } catch (DataIntegrityViolationException ignore) {
                // unique 충돌 — retry
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }
}
