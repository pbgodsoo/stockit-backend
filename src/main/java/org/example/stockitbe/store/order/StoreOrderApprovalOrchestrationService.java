package org.example.stockitbe.store.order;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.StoreWarehouseMapRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.StoreWarehouseMap;
import org.example.stockitbe.hq.infrastructure.model.StoreWarehouseRole;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.store.inbound.StoreInboundHeaderRepository;
import org.example.stockitbe.store.inbound.StoreInboundItemRepository;
import org.example.stockitbe.store.inbound.StoreInboundStatusHistoryRepository;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundHeader;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundItem;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundStatusHistory;
import org.example.stockitbe.store.order.model.entity.StoreOrderHeader;
import org.example.stockitbe.store.order.model.entity.StoreOrderItem;
import org.example.stockitbe.warehouse.outbound.WhOutboundHeaderRepository;
import org.example.stockitbe.warehouse.outbound.WhOutboundItemRepository;
import org.example.stockitbe.warehouse.outbound.WhOutboundStatusHistoryRepository;
import org.example.stockitbe.warehouse.outbound.model.OutboundDestinationType;
import org.example.stockitbe.warehouse.outbound.model.OutboundSourceType;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundItem;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreOrderApprovalOrchestrationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ETA_DAYS = 2;

    private final StoreWarehouseMapRepository storeWarehouseMapRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final InventoryService inventoryService;
    private final WhOutboundHeaderRepository outboundHeaderRepository;
    private final WhOutboundItemRepository outboundItemRepository;
    private final WhOutboundStatusHistoryRepository outboundStatusHistoryRepository;
    private final StoreInboundHeaderRepository inboundHeaderRepository;
    private final StoreInboundItemRepository inboundItemRepository;
    private final StoreInboundStatusHistoryRepository inboundStatusHistoryRepository;

    // 사용하는 메서드: StoreOrderService.approveInternal
    // 발주 승인 직후 출고/입고 문서를 동기 오케스트레이션으로 생성한다.
    // 1) 기존 생성 여부(멱등) 확인
    // 2) Primary/Backup 창고 결정
    // 3) SKU별 예약/분배 수행
    // 4) 창고별 출고/입고 헤더·아이템·상태이력 생성
    @Transactional
    public void createOutboundInboundForApprovedOrder(
            StoreOrderHeader header,
            List<StoreOrderItem> orderItems,
            String actorMemberId,
            String actorName,
            String reason
    ) {
        List<WhOutboundHeader> existing = outboundHeaderRepository
                .findAllBySourceTypeAndSourceRefNoOrderBySourceRefSeqAsc(OutboundSourceType.STORE_ORDER, header.getOrderNo());
        if (!existing.isEmpty()) {
            validateIdempotentPair(existing);
            return;
        }

        WarehouseSelection warehouseSelection = resolveWarehouses(header.getStoreId(), header.getWarehouseId());
        AllocationBundle allocationBundle = allocateAndReserve(orderItems, warehouseSelection);
        String deliveryGroupNo = "DGR-" + header.getOrderNo() + "-" + UUID.randomUUID().toString().substring(0, 8);
        Date now = new Date();

        int maxSeq = Optional.ofNullable(
                outboundHeaderRepository.findMaxSourceRefSeq(OutboundSourceType.STORE_ORDER, header.getOrderNo())
        ).orElse(0);

        // 창고별 할당 결과를 순회하면서 출고/입고를 한 쌍으로 생성한다.
        int seq = maxSeq;
        for (WarehouseAllocation allocation : allocationBundle.allocations()) {
            seq++;
            WhOutboundHeader outbound = outboundHeaderRepository.save(
                    WhOutboundHeader.builder()
                            .outboundNo("TEMP-" + UUID.randomUUID())
                            .sourceType(OutboundSourceType.STORE_ORDER)
                            .sourceRefNo(header.getOrderNo())
                            .sourceRefSeq(seq)
                            .sourceRefId(header.getId())
                            .warehouseId(allocation.warehouseId())
                            .destinationType(OutboundDestinationType.STORE)
                            .destinationId(header.getStoreId())
                            .status(OutboundStatus.READY_TO_SHIP)
                            .totalRequestedQuantity(allocation.totalQuantity())
                            .requestedAt(now)
                            .requestedByMemberId(actorMemberId)
                            .requestedByName(actorName)
                            .memo(reason)
                            .build()
            );
            outbound.assignOutboundNo(generateOutboundNo(outbound.getId(), now));

            // 출고 라인은 원천 발주 라인을 스냅샷 복제한다.
            List<WhOutboundItem> outboundItems = new ArrayList<>();
            for (AllocatedLine line : allocation.lines()) {
                outboundItems.add(WhOutboundItem.builder()
                        .outboundHeaderId(outbound.getId())
                        .sourceLineRefId(line.sourceLineRefId())
                        .skuId(line.skuId())
                        .skuCode(line.skuCode())
                        .productCode(line.productCode())
                        .productName(line.productName())
                        .mainCategory(line.mainCategory())
                        .subCategory(line.subCategory())
                        .color(line.color())
                        .size(line.size())
                        .unitPrice(line.unitPrice())
                        .requestedQuantity(line.quantity())
                        .memo(null)
                        .build());
            }
            List<WhOutboundItem> savedOutboundItems = outboundItemRepository.saveAll(outboundItems);

            // 출고 최초 상태(READY_TO_SHIP) 이력을 즉시 적재한다.
            outboundStatusHistoryRepository.save(
                    WhOutboundStatusHistory.builder()
                            .outboundHeaderId(outbound.getId())
                            .status(OutboundStatus.READY_TO_SHIP)
                            .changedAt(now)
                            .changedByMemberId(actorMemberId)
                            .changedByName(actorName)
                            .reason(reason)
                            .build()
            );

            StoreInboundHeader inbound = inboundHeaderRepository.save(
                    StoreInboundHeader.builder()
                            .inboundNo("TEMP-" + UUID.randomUUID())
                            .sourceRefNo(header.getOrderNo())
                            .sourceRefId(header.getId())
                            .outboundNo(outbound.getOutboundNo())
                            .storeId(header.getStoreId())
                            .fromWarehouseId(allocation.warehouseId())
                            .status(StoreInboundStatus.PENDING_RECEIPT)
                            .totalSkuCount(allocation.lines().size())
                            .totalExpectedQuantity(allocation.totalQuantity())
                            .expectedArrivalAt(Date.from(now.toInstant().plusSeconds(60L * 60 * 24 * ETA_DAYS)))
                            .requestedAt(now)
                            .requestedByMemberId(actorMemberId)
                            .requestedByName(actorName)
                            .deliveryGroupNo(deliveryGroupNo)
                            .memo(reason)
                            .build()
            );
            inbound.assignInboundNo(generateInboundNo(inbound.getId(), now));

            // 입고 라인의 outbound_item_id 연결을 위한 매핑을 만든다.
            Map<String, Long> outboundItemIdByKey = new HashMap<>();
            for (WhOutboundItem oi : savedOutboundItems) {
                outboundItemIdByKey.put(oi.getSkuCode(), oi.getId());
            }

            List<StoreInboundItem> inboundItems = new ArrayList<>();
            for (AllocatedLine line : allocation.lines()) {
                inboundItems.add(StoreInboundItem.builder()
                        .inboundHeaderId(inbound.getId())
                        .outboundItemId(outboundItemIdByKey.get(line.skuCode()))
                        .sourceLineRefId(line.sourceLineRefId())
                        .skuId(line.skuId())
                        .skuCode(line.skuCode())
                        .productCode(line.productCode())
                        .productName(line.productName())
                        .mainCategory(line.mainCategory())
                        .subCategory(line.subCategory())
                        .color(line.color())
                        .size(line.size())
                        .unitPrice(line.unitPrice())
                        .expectedQuantity(line.quantity())
                        .memo(null)
                        .build());
            }
            inboundItemRepository.saveAll(inboundItems);

            // 입고 최초 상태(PENDING_RECEIPT) 이력을 즉시 적재한다.
            inboundStatusHistoryRepository.save(
                    StoreInboundStatusHistory.builder()
                            .inboundHeaderId(inbound.getId())
                            .status(StoreInboundStatus.PENDING_RECEIPT)
                            .changedAt(now)
                            .changedByMemberId(actorMemberId)
                            .changedByName(actorName)
                            .reason(reason)
                            .build()
            );
        }
    }

    // 사용하는 메서드: createOutboundInboundForApprovedOrder
    // 기존 출고가 이미 있으면 대응 입고(outbound_no 기준)도 존재하는지 검증한다.
    // 한쪽만 존재하는 비정상 상태는 FAIL로 중단해 데이터 정합성 붕괴를 막는다.
    private void validateIdempotentPair(List<WhOutboundHeader> existingOutbounds) {
        for (WhOutboundHeader outbound : existingOutbounds) {
            if (inboundHeaderRepository.findByOutboundNo(outbound.getOutboundNo()).isEmpty()) {
                throw BaseException.from(BaseResponseStatus.FAIL);
            }
        }
    }

    // 사용하는 메서드: createOutboundInboundForApprovedOrder
    // 발주 매장의 Primary/Backup 창고를 조회한다.
    // 발주 헤더의 warehouseId와 매핑 Primary가 다르면 발주 헤더 값을 우선한다.
    private WarehouseSelection resolveWarehouses(Long storeId, Long primaryWarehouseIdFromOrder) {
        Infrastructure storeRef = infrastructureRepository.findById(storeId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_STORE_NOT_FOUND));
        StoreWarehouseMap primaryMap = storeWarehouseMapRepository.findByStoreAndRole(storeRef, StoreWarehouseRole.PRIMARY)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_WAREHOUSE_NOT_FOUND));
        StoreWarehouseMap backupMap = storeWarehouseMapRepository.findByStoreAndRole(storeRef, StoreWarehouseRole.BACKUP)
                .orElse(null);

        Long primaryWarehouseId = primaryMap.getWarehouse().getId();
        if (!Objects.equals(primaryWarehouseId, primaryWarehouseIdFromOrder)) {
            primaryWarehouseId = primaryWarehouseIdFromOrder;
        }
        Long backupWarehouseId = backupMap == null ? null : backupMap.getWarehouse().getId();
        return new WarehouseSelection(primaryWarehouseId, backupWarehouseId);
    }

    // 사용하는 메서드: createOutboundInboundForApprovedOrder
    // SKU별로 Primary 창고 우선 예약 후 부족분은 Backup 창고로 재예약한다.
    // Primary+Backup 합산으로도 수량이 부족하면 승인 실패 예외를 발생시킨다.
    private AllocationBundle allocateAndReserve(List<StoreOrderItem> orderItems, WarehouseSelection warehouseSelection) {
        List<AllocatedLine> primaryLines = new ArrayList<>();
        List<AllocatedLine> backupLines = new ArrayList<>();
        int primaryTotal = 0;
        int backupTotal = 0;

        for (StoreOrderItem item : orderItems) {
            int requested = item.getRequestedQuantity();
            int primaryReserved = inventoryService.reserveForOutboundUpTo(
                    warehouseSelection.primaryWarehouseId(), item.getSkuId(), requested
            );
            int remaining = requested - primaryReserved;

            if (primaryReserved > 0) {
                primaryLines.add(toAllocatedLine(item, primaryReserved));
                primaryTotal += primaryReserved;
            }

            int backupReserved = 0;
            if (remaining > 0 && warehouseSelection.backupWarehouseId() != null) {
                backupReserved = inventoryService.reserveForOutboundUpTo(
                        warehouseSelection.backupWarehouseId(), item.getSkuId(), remaining
                );
            }
            int unresolved = remaining - backupReserved;
            if (backupReserved > 0) {
                backupLines.add(toAllocatedLine(item, backupReserved));
                backupTotal += backupReserved;
            }

            if (unresolved > 0) {
                throw BaseException.from(BaseResponseStatus.STORE_ORDER_APPROVE_INSUFFICIENT_WAREHOUSE_STOCK);
            }
        }

        List<WarehouseAllocation> allocations = new ArrayList<>();
        if (primaryTotal > 0) {
            allocations.add(new WarehouseAllocation(warehouseSelection.primaryWarehouseId(), primaryTotal, primaryLines));
        }
        if (backupTotal > 0) {
            allocations.add(new WarehouseAllocation(warehouseSelection.backupWarehouseId(), backupTotal, backupLines));
        }
        return new AllocationBundle(allocations);
    }

    // 사용하는 메서드: allocateAndReserve
    // 발주 라인을 출고/입고 생성용 할당 라인 객체로 변환한다.
    private AllocatedLine toAllocatedLine(StoreOrderItem item, int quantity) {
        return new AllocatedLine(
                item.getId(),
                item.getSkuId(),
                item.getSkuCode(),
                item.getProductCode(),
                item.getProductName(),
                item.getMainCategory(),
                item.getSubCategory(),
                item.getColor(),
                item.getSize(),
                item.getUnitPrice(),
                quantity
        );
    }

    // 사용하는 메서드: createOutboundInboundForApprovedOrder
    // 출고번호 규칙: WOB-YYYYMMDD-00001
    private String generateOutboundNo(Long id, Date requestedAt) {
        LocalDate day = requestedAt.toInstant().atZone(KST).toLocalDate();
        return "WOB-" + day.format(NUMBER_DATE_FORMAT) + "-" + String.format("%05d", id);
    }

    // 사용하는 메서드: createOutboundInboundForApprovedOrder
    // 입고번호 규칙: SIB-YYYYMMDD-00001
    private String generateInboundNo(Long id, Date requestedAt) {
        LocalDate day = requestedAt.toInstant().atZone(KST).toLocalDate();
        return "SIB-" + day.format(NUMBER_DATE_FORMAT) + "-" + String.format("%05d", id);
    }

    private record WarehouseSelection(Long primaryWarehouseId, Long backupWarehouseId) {
    }

    private record AllocatedLine(
            Long sourceLineRefId,
            Long skuId,
            String skuCode,
            String productCode,
            String productName,
            String mainCategory,
            String subCategory,
            String color,
            String size,
            Long unitPrice,
            Integer quantity
    ) {
    }

    private record WarehouseAllocation(Long warehouseId, Integer totalQuantity, List<AllocatedLine> lines) {
    }

    private record AllocationBundle(List<WarehouseAllocation> allocations) {
    }
}
