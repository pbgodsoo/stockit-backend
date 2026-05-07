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
import org.example.stockitbe.warehouse.inbound.model.InboundStatus;
import org.example.stockitbe.warehouse.inbound.model.InboundType;
import org.example.stockitbe.warehouse.inbound.model.WhInboundDto;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundItem;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundStatusHistory;
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
import java.util.stream.Collectors;

/**
 * 창고 입고 도메인 service. 본사 발주 입고 + 창고간 이동 입고 두 source 통합.
 *
 * Mirror 패턴 (PO → inbound):
 *   READY_TO_SHIP 시점 createFromPurchaseOrder 호출 (멱등 INSERT)
 *   IN_TRANSIT 시점 markInTransit 호출 (UPDATE + 인벤토리 가용재고+)
 *   ARRIVED 시점 markArrived 호출 (UPDATE)
 *
 * 멱등성: 같은 sourceRefNo + PURCHASE_ORDER 두 번 호출 시 두 번째는 silent.
 * 안전성: mark 메소드들은 inbound row 없으면 silent skip (호출자 누락 대비).
 */
@Service
@RequiredArgsConstructor
public class WhInboundService {

    private final WhInboundHeaderRepository headerRepository;
    private final WhInboundItemRepository itemRepository;
    private final WhInboundStatusHistoryRepository historyRepository;
    private final InboundCodeGenerator codeGenerator;
    private final InventoryService inventoryService;
    private final InfrastructureRepository infrastructureRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

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
                .warehouseId(po.getWarehouseId())
                .warehouseName(po.getWarehouseName())
                .sourceName(po.getVendorName())
                .status(InboundStatus.READY_TO_SHIP)
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

        // statusHistory append — 거래처 시점 스냅샷 (배치 호출이라 me=null)
        String byName = vendorActorLabel(po);
        historyRepository.save(WhInboundStatusHistory.builder()
                .inboundHeaderId(header.getId())
                .status(InboundStatus.READY_TO_SHIP)
                .changedByName(byName)
                .build());

        return header;
    }

    /**
     * IN_TRANSIT mirror — PO 가 IN_TRANSIT 으로 전환될 때 호출.
     * inbound row 없으면 silent skip. 인벤토리 가용재고+ hook 박힘 (PR #173 위치 이동).
     */
    @Transactional
    public void markInTransit(PurchaseOrder po) {
        Optional<WhInboundHeader> opt = headerRepository.findBySourceRefNoAndInboundType(
                po.getCode(), InboundType.PURCHASE_ORDER);
        if (opt.isEmpty()) return;
        WhInboundHeader header = opt.get();

        header.markInTransit();

        String byName = vendorActorLabel(po);
        historyRepository.save(WhInboundStatusHistory.builder()
                .inboundHeaderId(header.getId())
                .status(InboundStatus.IN_TRANSIT)
                .changedByName(byName)
                .build());

        // 인벤토리 가용재고+
        List<WhInboundItem> items = itemRepository.findAllByInboundHeaderIdOrderByIdAsc(header.getId());
        items.forEach(item -> inventoryService.increaseAvailable(
                header.getWarehouseId(), item.getSkuCode(), item.getQuantity()));
    }

    /**
     * ARRIVED mirror — PO 가 ARRIVED 로 전환될 때 호출.
     * 인벤토리 갱신 없음 (실재고 인식은 confirmInbound 시점).
     */
    @Transactional
    public void markArrived(PurchaseOrder po) {
        Optional<WhInboundHeader> opt = headerRepository.findBySourceRefNoAndInboundType(
                po.getCode(), InboundType.PURCHASE_ORDER);
        if (opt.isEmpty()) return;
        WhInboundHeader header = opt.get();

        header.markArrived(new Date());

        String byName = vendorActorLabel(po);
        historyRepository.save(WhInboundStatusHistory.builder()
                .inboundHeaderId(header.getId())
                .status(InboundStatus.ARRIVED)
                .changedByName(byName)
                .build());
    }

    /**
     * 입고 확정 (ARRIVED → COMPLETED). 창고 [입고 확정] 매뉴얼 트리거.
     * 자기 창고 격리: me.locationCode 의 warehouseId 와 inbound.warehouseId 매칭. 불일치 시 INBOUND_NOT_FOUND.
     * 실재고 인식 (PR #173 위치 이동) + PO mirror (PURCHASE_ORDER 일 때만, 다음 사이클 transfer 분기 추가).
     */
    @Transactional
    public WhInboundHeader confirmInbound(String inboundCode, AuthUserDetails me) {
        WhInboundHeader inbound = headerRepository.findByInboundCode(inboundCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND));

        Long myWarehouseId = resolveWarehouseId(me.getLocationCode());
        if (!inbound.getWarehouseId().equals(myWarehouseId)) {
            // LOCATION_MISMATCH 노출 X — 다른 창고 존재 사실 회피
            throw BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND);
        }

        String byName = me.getName() + " (" + me.getLocationName() + ")";
        Date now = new Date();
        inbound.markCompleted(now, me.getEmployeeCode(), byName);

        historyRepository.save(WhInboundStatusHistory.builder()
                .inboundHeaderId(inbound.getId())
                .status(InboundStatus.COMPLETED)
                .changedByMemberId(me.getEmployeeCode())
                .changedByName(byName)
                .build());

        // 인벤토리 실재고 인식
        List<WhInboundItem> items = itemRepository.findAllByInboundHeaderIdOrderByIdAsc(inbound.getId());
        items.forEach(item -> inventoryService.markPhysical(
                inbound.getWarehouseId(), item.getSkuCode(), item.getQuantity()));

        // PO mirror — inbound.confirmInbound 가 PO 도 COMPLETED 박음 (본사 발주 리스트 "종료" 표시)
        if (inbound.getInboundType() == InboundType.PURCHASE_ORDER) {
            purchaseOrderService.completeFromInbound(inbound.getSourceRefNo(), me);
        }
        // WAREHOUSE_TRANSFER 분기는 다른 팀원 outbound 도메인 합류 후 별 사이클

        return inbound;
    }

    /**
     * 자기 창고 입고 목록.
     * status 미지정 시 4상태 (READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED) 모두.
     * 기간 필터는 inbound.createdAt 기준.
     */
    @Transactional(readOnly = true)
    public List<WhInboundDto.ListRes> findAll(AuthUserDetails me, InboundStatus status,
                                              LocalDate from, LocalDate to) {
        Long warehouseId = resolveWarehouseId(me.getLocationCode());

        List<InboundStatus> statusFilter = (status != null)
                ? List.of(status)
                : List.of(InboundStatus.READY_TO_SHIP, InboundStatus.IN_TRANSIT,
                        InboundStatus.ARRIVED, InboundStatus.COMPLETED);

        List<WhInboundHeader> headers = headerRepository
                .findAllByWarehouseIdAndStatusInOrderByCreatedAtDesc(warehouseId, statusFilter);

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

        // N+1 회피 — items batch 조회
        List<Long> headerIds = headers.stream().map(WhInboundHeader::getId).toList();
        Map<Long, List<WhInboundItem>> itemsByHeader = itemRepository
                .findAllByInboundHeaderIdInOrderByIdAsc(headerIds).stream()
                .collect(Collectors.groupingBy(WhInboundItem::getInboundHeaderId));

        return headers.stream()
                .map(h -> WhInboundDto.ListRes.from(h, itemsByHeader.getOrDefault(h.getId(), List.of())))
                .toList();
    }

    /**
     * 입고 단건 — 자기 창고 row 만 조회 가능. 다른 창고 코드 직접 URL 접근 시 INBOUND_NOT_FOUND.
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
        List<WhInboundStatusHistory> history = historyRepository
                .findAllByInboundHeaderIdOrderByChangedAtAsc(inbound.getId());
        return WhInboundDto.DetailRes.from(inbound, items, history);
    }

    /**
     * 기존 PO 입고 데이터 → inbound row 일괄 변환.
     * dev/시연 1회성. 멱등 — 이미 inbound row 있으면 skip.
     * 인벤토리 갱신 X (이미 PO complete 시점에 처리됐을 가능성, 중복 갱신 회피).
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

            // PO status → InboundStatus 1:1 매핑 (enum 이름 동일)
            InboundStatus inboundStatus = InboundStatus.valueOf(po.getStatus().name());

            Date arrivedAt = (po.getStatus() == PurchaseOrderStatus.ARRIVED
                    || po.getStatus() == PurchaseOrderStatus.COMPLETED)
                    ? po.getUpdatedAt() : null;
            Date completedAt = po.getStatus() == PurchaseOrderStatus.COMPLETED
                    ? po.getUpdatedAt() : null;

            WhInboundHeader.WhInboundHeaderBuilder builder = WhInboundHeader.builder()
                    .inboundType(InboundType.PURCHASE_ORDER)
                    .sourceRefNo(po.getCode())
                    .sourceRefId(po.getId())
                    .warehouseId(po.getWarehouseId())
                    .warehouseName(po.getWarehouseName())
                    .sourceName(po.getVendorName())
                    .status(inboundStatus)
                    .totalQuantity(totalQuantity)
                    .totalAmount(po.getTotalAmount())
                    .arrivedAt(arrivedAt)
                    .completedAt(completedAt);

            WhInboundHeader header = saveHeaderWithCodeRetry(builder);

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

            // statusHistory — 단순화로 현재 status 한 row 만 append (정확한 PO history 매핑은 별 사이클)
            historyRepository.save(WhInboundStatusHistory.builder()
                    .inboundHeaderId(header.getId())
                    .status(inboundStatus)
                    .changedByName(vendorActorLabel(po))
                    .note("backfill")
                    .build());

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

    private String vendorActorLabel(PurchaseOrder po) {
        return po.getVendorContactName() + " (" + po.getVendorName() + ")";
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
                // unique 충돌 — 다음 iteration 으로 retry
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }
}
