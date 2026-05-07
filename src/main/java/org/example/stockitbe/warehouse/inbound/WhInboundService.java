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
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderItem;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.inbound.model.InboundType;
import org.example.stockitbe.warehouse.inbound.model.WhInboundDto;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundItem;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
 *   - WAREHOUSE_TRANSFER : WhOutbound.status (다른 팀원 합류 후 분기 추가)
 *
 * inbound 의 책임:
 *   1. createFromPurchaseOrder — READY_TO_SHIP 시점 inbound row INSERT (멱등)
 *   2. confirmInbound — 도착 후 자산 확정 (completedAt + confirmedBy* + 실재고 인식 + PO mirror)
 *   3. findAll/findByCode — inbound + source LEFT JOIN read 로 status 응답 채움
 *
 * 인벤토리 hook:
 *   - 가용재고+ : PurchaseOrderService.startInTransit (PR #173 위치)
 *   - 실재고 인식 : confirmInbound 시점 (이번 사이클에 inbound 도메인으로 일원화)
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

    /**
     * 본사 발주 입고 row 생성 (READY_TO_SHIP 진입 시점에 PurchaseOrderBatchService 가 호출).
     * 멱등 — 같은 sourceRefNo + PURCHASE_ORDER 이미 있으면 기존 return.
     * inbound 자체 status 컬럼 없음 — 진행 단계는 PO.status 가 진실 원천 (응답은 join 으로 채움).
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
                .warehouseId(po.getWarehouse().getId())
                .warehouseName(po.getWarehouseName())
                .sourceName(po.getVendorName())
                .totalQuantity(totalQuantity)
                .totalAmount(po.getTotalAmount());

        WhInboundHeader header = saveHeaderWithCodeRetry(headerBuilder);

        // items 시점 복사
        List<WhInboundItem> items = poItems.stream()
                .map(it -> WhInboundItem.builder()
                        .inboundHeaderId(header.getId())
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
        itemRepository.saveAll(items);

        return header;
    }

    /**
     * 입고 확정 (도착 후 자산 인식). 창고 [입고 확정] 매뉴얼 트리거.
     * 자기 창고 격리: me.locationCode 의 warehouseId 와 inbound.warehouseId 매칭. 불일치 시 INBOUND_NOT_FOUND.
     * 책임:
     *   1. inbound.markConfirmed → completedAt + confirmedBy* 박음
     *   2. inventoryService.markPhysical → 실재고 인식 (가용 → 실재고 이동)
     *   3. inboundType 분기 → source 도메인의 종료 단계 호출 (PO.markCompleted 또는 outbound 처리)
     */
    @Transactional
    public WhInboundHeader confirmInbound(String inboundCode, AuthUserDetails me) {
        WhInboundHeader inbound = headerRepository.findByInboundCode(inboundCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND));

        Long myWarehouseId = resolveWarehouseId(me.getLocationCode());
        if (!inbound.getWarehouseId().equals(myWarehouseId)) {
            throw BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND);
        }

        // PO 의 status==ARRIVED 검증 — 입고 확정 전제. PURCHASE_ORDER 한정.
        if (inbound.getInboundType() == InboundType.PURCHASE_ORDER) {
            PurchaseOrder po = purchaseOrderRepository.findByCode(inbound.getSourceRefNo())
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.PURCHASE_ORDER_NOT_FOUND));
            if (po.getStatus() != PurchaseOrderStatus.ARRIVED) {
                throw BaseException.from(BaseResponseStatus.INBOUND_NOT_CONFIRMABLE);
            }
        }

        String byName = me.getName() + " (" + me.getLocationName() + ")";
        Date now = new Date();
        inbound.markConfirmed(now, me.getEmployeeCode(), byName);

        // 인벤토리 실재고 인식
        List<WhInboundItem> items = itemRepository.findAllByInboundHeaderIdOrderByIdAsc(inbound.getId());
        items.forEach(item -> inventoryService.markPhysical(
                inbound.getWarehouseId(), item.getSkuCode(), item.getQuantity()));

        // source 도메인 mirror — 종료 단계 박음
        if (inbound.getInboundType() == InboundType.PURCHASE_ORDER) {
            purchaseOrderService.completeFromInbound(inbound.getSourceRefNo(), me);
        }
        // WAREHOUSE_TRANSFER 분기는 다른 팀원 outbound 도메인 합류 후 별 사이클

        return inbound;
    }

    /**
     * 자기 창고 입고 목록.
     * inbound + PO LEFT JOIN read — status 필드를 PO.status 로 채움.
     * REQUESTED/APPROVED/CANCELLED 상태의 PO 는 입고 화면에서 숨김 (READY_TO_SHIP 이상만 노출).
     */
    @Transactional(readOnly = true)
    public List<WhInboundDto.ListRes> findAll(AuthUserDetails me, String statusFilter,
                                              LocalDate from, LocalDate to) {
        Long warehouseId = resolveWarehouseId(me.getLocationCode());

        // inbound row 조회 (PURCHASE_ORDER 만 — transfer 합류 후 분기 추가)
        List<WhInboundHeader> headers = headerRepository
                .findAllByWarehouseIdAndInboundTypeOrderByCreatedAtDesc(warehouseId, InboundType.PURCHASE_ORDER);

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

        // PO 일괄 조회 (status 진실 원천)
        Set<String> poCodes = headers.stream()
                .map(WhInboundHeader::getSourceRefNo)
                .collect(Collectors.toSet());
        Map<String, PurchaseOrder> poByCode = purchaseOrderRepository.findAll().stream()
                .filter(po -> poCodes.contains(po.getCode()))
                .collect(Collectors.toMap(PurchaseOrder::getCode, Function.identity()));

        // items batch 조회 (N+1 회피)
        List<Long> headerIds = headers.stream().map(WhInboundHeader::getId).toList();
        Map<Long, List<WhInboundItem>> itemsByHeader = itemRepository
                .findAllByInboundHeaderIdInOrderByIdAsc(headerIds).stream()
                .collect(Collectors.groupingBy(WhInboundItem::getInboundHeaderId));

        return headers.stream()
                .map(h -> {
                    PurchaseOrder po = poByCode.get(h.getSourceRefNo());
                    String effectiveStatus = resolveEffectiveStatus(h, po);
                    if (effectiveStatus == null) return null;
                    if (statusFilter != null && !statusFilter.equals(effectiveStatus)) return null;
                    return WhInboundDto.ListRes.from(h, itemsByHeader.getOrDefault(h.getId(), List.of()), effectiveStatus);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 입고 단건 — 자기 창고 row 만 조회 가능.
     * status 는 PO.status 와 inbound.completedAt 으로 산출.
     */
    @Transactional(readOnly = true)
    public WhInboundDto.DetailRes findByCode(AuthUserDetails me, String inboundCode) {
        Long myWarehouseId = resolveWarehouseId(me.getLocationCode());
        WhInboundHeader inbound = headerRepository.findByInboundCode(inboundCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND));
        if (!inbound.getWarehouseId().equals(myWarehouseId)) {
            throw BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND);
        }
        List<WhInboundItem> items = itemRepository
                .findAllByInboundHeaderIdOrderByIdAsc(inbound.getId());

        PurchaseOrder po = inbound.getInboundType() == InboundType.PURCHASE_ORDER
                ? purchaseOrderRepository.findByCode(inbound.getSourceRefNo()).orElse(null)
                : null;
        String effectiveStatus = resolveEffectiveStatus(inbound, po);

        return WhInboundDto.DetailRes.from(inbound, items, effectiveStatus);
    }

    /**
     * 기존 PO 입고 데이터 → inbound row 일괄 변환.
     * dev/시연 1회성. 멱등 — 이미 inbound row 있으면 skip.
     * inbound 자체 status 컬럼 없음 — completedAt 만 PO.status==COMPLETED 인 경우 박음.
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
                    .warehouseId(po.getWarehouse().getId())
                    .warehouseName(po.getWarehouseName())
                    .sourceName(po.getVendorName())
                    .totalQuantity(totalQuantity)
                    .totalAmount(po.getTotalAmount())
                    .completedAt(completedAt);

            WhInboundHeader header = saveHeaderWithCodeRetry(builder);

            List<WhInboundItem> items = poItems.stream()
                    .map(it -> WhInboundItem.builder()
                            .inboundHeaderId(header.getId())
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
            itemRepository.saveAll(items);

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
     * inbound + source 도메인의 status 산출 룰:
     *   - inbound.completedAt != null → "COMPLETED"
     *   - else → source.status (PO.status 또는 outbound.status)
     *   - source 가 REQUESTED/APPROVED/CANCELLED 이면 입고 화면에서 숨김 (null 반환)
     */
    private String resolveEffectiveStatus(WhInboundHeader inbound, PurchaseOrder po) {
        if (inbound.getCompletedAt() != null) {
            return "COMPLETED";
        }
        if (po == null) {
            return null;
        }
        return switch (po.getStatus()) {
            case READY_TO_SHIP -> "READY_TO_SHIP";
            case IN_TRANSIT -> "IN_TRANSIT";
            case ARRIVED -> "ARRIVED";
            case COMPLETED -> "COMPLETED";
            case REQUESTED, APPROVED, CANCELLED -> null;
        };
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

    private WhInboundHeader saveHeaderWithCodeRetry(WhInboundHeader.WhInboundHeaderBuilder builder) {
        for (int i = 0; i < 5; i++) {
            String code = codeGenerator.nextCode(new Date());
            try {
                return headerRepository.save(builder.inboundCode(code).build());
            } catch (DataIntegrityViolationException ignore) {
                // unique 충돌 — retry
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }
}
