package org.example.stockitbe.warehouse.outbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.store.inbound.StoreInboundHeaderRepository;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundHeader;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.example.stockitbe.warehouse.outbound.model.WhOutboundDto;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundItem;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WhOutboundService {

    private final WhOutboundHeaderRepository outboundHeaderRepository;
    private final WhOutboundItemRepository outboundItemRepository;
    private final WhOutboundStatusHistoryRepository outboundStatusHistoryRepository;
    private final StoreInboundHeaderRepository inboundHeaderRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<WhOutboundDto.ListRes> list(AuthUserDetails me, String status, LocalDate from, LocalDate to, String keyword) {
        Long myWarehouseId = resolveWarehouseId(me);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        OutboundStatus statusFilter = parseStatus(status);
        Date fromDate = from == null ? null : Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDateExclusive = to == null ? null : Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<WhOutboundHeader> headers = outboundHeaderRepository.findAllByWarehouseIdOrderByRequestedAtDescIdDesc(myWarehouseId);
        if (headers.isEmpty()) return List.of();

        Infrastructure myWarehouse = infrastructureRepository.findById(myWarehouseId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.OUTBOUND_NOT_FOUND));

        return headers.stream()
                .filter(h -> statusFilter == null || h.getStatus() == statusFilter)
                .filter(h -> fromDate == null || !h.getRequestedAt().before(fromDate))
                .filter(h -> toDateExclusive == null || h.getRequestedAt().before(toDateExclusive))
                .filter(h -> safeKeyword.isBlank() || matchesKeyword(h, safeKeyword))
                .map(h -> WhOutboundDto.toListRes(h, myWarehouse.getCode(), myWarehouse.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public WhOutboundDto.DetailRes detail(AuthUserDetails me, String outboundNo) {
        Long myWarehouseId = resolveWarehouseId(me);
        WhOutboundHeader header = findOwnedOutbound(outboundNo, myWarehouseId);
        Infrastructure warehouse = infrastructureRepository.findById(header.getWarehouseId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.OUTBOUND_NOT_FOUND));
        List<WhOutboundItem> items = outboundItemRepository.findAllByOutboundHeaderIdOrderByIdAsc(header.getId());
        List<WhOutboundStatusHistory> history = outboundStatusHistoryRepository.findAllByOutboundHeaderIdOrderByChangedAtAscIdAsc(header.getId());
        StoreInboundHeader inbound = inboundHeaderRepository.findByOutboundNo(header.getOutboundNo()).orElse(null);

        return WhOutboundDto.DetailRes.builder()
                .outboundNo(header.getOutboundNo())
                .sourceType(header.getSourceType().name())
                .sourceRefNo(header.getSourceRefNo())
                .sourceRefSeq(header.getSourceRefSeq())
                .sourceRefId(header.getSourceRefId())
                .warehouseId(header.getWarehouseId())
                .warehouseCode(warehouse.getCode())
                .warehouseName(warehouse.getName())
                .destinationType(header.getDestinationType().name())
                .destinationId(header.getDestinationId())
                .status(header.getStatus())
                .totalRequestedQuantity(header.getTotalRequestedQuantity())
                .requestedAt(header.getRequestedAt())
                .confirmedAt(header.getConfirmedAt())
                .departedAt(header.getDepartedAt())
                .arrivedAt(header.getArrivedAt())
                .requestedByMemberId(header.getRequestedByMemberId())
                .requestedByName(header.getRequestedByName())
                .memo(header.getMemo())
                .items(items.stream().map(WhOutboundDto.ItemRes::from).toList())
                .statusHistory(history.stream().map(WhOutboundDto.StatusHistoryRes::from).toList())
                .inbound(inbound == null ? null : WhOutboundDto.InboundSummaryRes.builder()
                        .inboundNo(inbound.getInboundNo())
                        .inboundStatus(inbound.getStatus())
                        .build())
                .build();
    }

    @Transactional
    public WhOutboundDto.DetailRes confirm(AuthUserDetails me, String outboundNo, String reason) {
        Long myWarehouseId = resolveWarehouseId(me);
        WhOutboundHeader header = findOwnedOutbound(outboundNo, myWarehouseId);
        if (header.getStatus() != OutboundStatus.READY_TO_SHIP) {
            throw BaseException.from(BaseResponseStatus.OUTBOUND_INVALID_STATUS_TRANSITION);
        }

        List<WhOutboundItem> items = outboundItemRepository.findAllByOutboundHeaderIdOrderByIdAsc(header.getId());
        for (WhOutboundItem item : items) {
            int moved = inventoryService.moveReservedToInTransit(header.getWarehouseId(), item.getSkuId(), item.getRequestedQuantity());
            if (moved != item.getRequestedQuantity()) {
                throw BaseException.from(BaseResponseStatus.OUTBOUND_RESERVED_STOCK_NOT_ENOUGH);
            }
        }

        Date now = new Date();
        header.markInTransit(now);
        outboundStatusHistoryRepository.save(
                WhOutboundStatusHistory.builder()
                        .outboundHeaderId(header.getId())
                        .status(OutboundStatus.IN_TRANSIT)
                        .changedAt(now)
                        .changedByMemberId(me.getEmployeeCode())
                        .changedByName(me.getName())
                        .reason(reason == null || reason.isBlank() ? "OUTBOUND_CONFIRM" : reason)
                        .build()
        );

        return detail(me, outboundNo);
    }

    @Transactional
    public WhOutboundDto.DetailRes arrive(AuthUserDetails me, String outboundNo, String reason) {
        Long myWarehouseId = resolveWarehouseId(me);
        WhOutboundHeader header = findOwnedOutbound(outboundNo, myWarehouseId);
        if (header.getStatus() != OutboundStatus.IN_TRANSIT) {
            throw BaseException.from(BaseResponseStatus.OUTBOUND_INVALID_STATUS_TRANSITION);
        }

        Date now = new Date();
        header.markArrived(now);
        outboundStatusHistoryRepository.save(
                WhOutboundStatusHistory.builder()
                        .outboundHeaderId(header.getId())
                        .status(OutboundStatus.ARRIVED)
                        .changedAt(now)
                        .changedByMemberId(me.getEmployeeCode())
                        .changedByName(me.getName())
                        .reason(reason == null || reason.isBlank() ? "OUTBOUND_ARRIVED" : reason)
                        .build()
        );

        return detail(me, outboundNo);
    }

    private Long resolveWarehouseId(AuthUserDetails me) {
        String locationCode = me == null ? null : me.getLocationCode();
        if (locationCode == null || locationCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.OUTBOUND_SCOPE_FORBIDDEN);
        }
        return infrastructureRepository.findByCodeAndLocationType(locationCode, LocationType.WAREHOUSE)
                .map(Infrastructure::getId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.OUTBOUND_SCOPE_FORBIDDEN));
    }

    private WhOutboundHeader findOwnedOutbound(String outboundNo, Long warehouseId) {
        WhOutboundHeader header = outboundHeaderRepository.findByOutboundNo(outboundNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.OUTBOUND_NOT_FOUND));
        if (!header.getWarehouseId().equals(warehouseId)) {
            throw BaseException.from(BaseResponseStatus.OUTBOUND_SCOPE_FORBIDDEN);
        }
        return header;
    }

    private OutboundStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "전체".equals(status)) return null;
        try {
            return OutboundStatus.valueOf(status);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchesKeyword(WhOutboundHeader header, String safeKeyword) {
        String text = (header.getOutboundNo() + " " + header.getSourceRefNo() + " " + header.getDestinationType().name())
                .toLowerCase(Locale.ROOT);
        return text.contains(safeKeyword);
    }
}
