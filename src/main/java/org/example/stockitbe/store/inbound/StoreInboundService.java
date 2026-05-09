package org.example.stockitbe.store.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.store.inbound.model.dto.StoreInboundDto;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundHeader;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundItem;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundStatusHistory;
import org.example.stockitbe.store.order.StoreOrderHeaderRepository;
import org.example.stockitbe.store.order.StoreOrderStatusHistoryRepository;
import org.example.stockitbe.store.order.model.StoreOrderHistoryType;
import org.example.stockitbe.store.order.model.StoreOrderStatus;
import org.example.stockitbe.store.order.model.entity.StoreOrderHeader;
import org.example.stockitbe.store.order.model.entity.StoreOrderStatusHistory;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.outbound.WhOutboundHeaderRepository;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StoreInboundService {

    private final StoreInboundHeaderRepository inboundHeaderRepository;
    private final StoreInboundItemRepository inboundItemRepository;
    private final StoreInboundStatusHistoryRepository inboundStatusHistoryRepository;
    private final StoreOrderHeaderRepository storeOrderHeaderRepository;
    private final StoreOrderStatusHistoryRepository storeOrderStatusHistoryRepository;
    private final WhOutboundHeaderRepository whOutboundHeaderRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<StoreInboundDto.ListRes> list(AuthUserDetails me, String status, LocalDate from, LocalDate to, String keyword) {
        Long myStoreId = resolveStoreId(me);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        StoreInboundStatus statusFilter = parseStatus(status);
        Date fromDate = from == null ? null : Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDateExclusive = to == null ? null : Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        return inboundHeaderRepository.findAllByStoreIdOrderByRequestedAtDescIdDesc(myStoreId).stream()
                .filter(h -> statusFilter == null || h.getStatus() == statusFilter)
                .filter(h -> fromDate == null || !h.getRequestedAt().before(fromDate))
                .filter(h -> toDateExclusive == null || h.getRequestedAt().before(toDateExclusive))
                .filter(h -> matchesKeyword(h, safeKeyword))
                .map(StoreInboundDto.ListRes::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreInboundDto.DetailRes detail(AuthUserDetails me, String inboundNo) {
        Long myStoreId = resolveStoreId(me);
        StoreInboundHeader inbound = findOwnedInbound(inboundNo, myStoreId);
        return buildDetailRes(inbound);
    }

    @Transactional
    public StoreInboundDto.DetailRes confirm(AuthUserDetails me, String inboundNo, String reason) {
        Long myStoreId = resolveStoreId(me);
        StoreInboundHeader inbound = findOwnedInbound(inboundNo, myStoreId);
        if (inbound.getStatus() != StoreInboundStatus.PENDING_RECEIPT) {
            throw BaseException.from(BaseResponseStatus.INVALID_INBOUND_STATUS_TRANSITION);
        }

        List<StoreInboundItem> inboundItems = inboundItemRepository.findAllByInboundHeaderIdOrderByIdAsc(inbound.getId());
        for (StoreInboundItem item : inboundItems) {
            inventoryService.increaseOnHandAndAvailable(
                    inbound.getStoreId(),
                    item.getSkuId(),
                    item.getExpectedQuantity()
            );
        }

        Date now = new Date();
        String actorMemberId = me == null ? null : me.getEmployeeCode();
        String actorName = me == null ? null : me.getName();
        inbound.markReceived(now, actorMemberId, actorName);

        inboundStatusHistoryRepository.save(
                StoreInboundStatusHistory.builder()
                        .inboundHeaderId(inbound.getId())
                        .status(StoreInboundStatus.RECEIVED)
                        .changedAt(now)
                        .changedByMemberId(actorMemberId)
                        .changedByName(actorName)
                        .reason(reason == null || reason.isBlank() ? "STORE_INBOUND_CONFIRM" : reason)
                        .build()
        );

        completeOrderWhenAllInboundReceived(inbound.getSourceRefNo(), actorMemberId, actorName);
        return buildDetailRes(inbound);
    }

    private void completeOrderWhenAllInboundReceived(String orderNo, String actorMemberId, String actorName) {
        if (orderNo == null || orderNo.isBlank()) return;
        StoreOrderHeader order = storeOrderHeaderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_NOT_FOUND));

        List<StoreInboundHeader> allInbound = inboundHeaderRepository.findAllBySourceRefNo(orderNo);
        boolean allReceived = !allInbound.isEmpty()
                && allInbound.stream().allMatch(h -> h.getStatus() == StoreInboundStatus.RECEIVED);
        if (!allReceived) return;
        if (order.getStatus() == StoreOrderStatus.COMPLETED) return;

        order.markCompleted();
        storeOrderStatusHistoryRepository.save(
                StoreOrderStatusHistory.builder()
                        .orderHeaderId(order.getId())
                        .historyType(StoreOrderHistoryType.ORDER_STATUS)
                        .status(StoreOrderStatus.COMPLETED.name())
                        .changedAt(new Date())
                        .changedByMemberId(actorMemberId)
                        .changedByName(actorName)
                        .reason("ALL_STORE_INBOUND_RECEIVED")
                        .build()
        );
    }

    private StoreInboundDto.DetailRes buildDetailRes(StoreInboundHeader inbound) {
        List<StoreInboundItem> items = inboundItemRepository.findAllByInboundHeaderIdOrderByIdAsc(inbound.getId());
        List<StoreInboundStatusHistory> history = inboundStatusHistoryRepository
                .findAllByInboundHeaderIdOrderByChangedAtAscIdAsc(inbound.getId());
        WhOutboundHeader outbound = whOutboundHeaderRepository.findByOutboundNo(inbound.getOutboundNo()).orElse(null);
        StoreInboundDto.OutboundSummaryRes outboundSummary = outbound == null ? null :
                StoreInboundDto.OutboundSummaryRes.builder()
                        .outboundNo(outbound.getOutboundNo())
                        .outboundStatus(outbound.getStatus())
                        .build();
        return StoreInboundDto.DetailRes.of(inbound, items, history, outboundSummary);
    }

    private Long resolveStoreId(AuthUserDetails me) {
        String locationCode = me == null ? null : me.getLocationCode();
        if (locationCode == null || locationCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.STORE_ORDER_SCOPE_FORBIDDEN);
        }
        Infrastructure infra = infrastructureRepository.findByCodeAndLocationType(locationCode, LocationType.STORE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_SCOPE_FORBIDDEN));
        return infra.getId();
    }

    private StoreInboundHeader findOwnedInbound(String inboundNo, Long storeId) {
        StoreInboundHeader inbound = inboundHeaderRepository.findByInboundNo(inboundNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND));
        if (!inbound.getStoreId().equals(storeId)) {
            throw BaseException.from(BaseResponseStatus.STORE_ORDER_SCOPE_FORBIDDEN);
        }
        return inbound;
    }

    private StoreInboundStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "전체".equals(status)) return null;
        try {
            return StoreInboundStatus.valueOf(status);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchesKeyword(StoreInboundHeader header, String safeKeyword) {
        if (safeKeyword.isBlank()) return true;
        String search = (header.getInboundNo() + " " + header.getSourceRefNo() + " " + header.getOutboundNo())
                .toLowerCase(Locale.ROOT);
        return search.contains(safeKeyword);
    }
}
