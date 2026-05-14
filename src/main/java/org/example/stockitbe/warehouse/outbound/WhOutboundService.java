package org.example.stockitbe.warehouse.outbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.warehousetransfer.WarehouseTransferHeaderRepository;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferHeader;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundHeader;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundStatusHistory;
import org.example.stockitbe.store.inbound.repository.StoreInboundHeaderRepository;
import org.example.stockitbe.store.inbound.repository.StoreInboundStatusHistoryRepository;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.outbound.model.OutboundDestinationType;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.example.stockitbe.warehouse.outbound.model.OutboundSourceType;
import org.example.stockitbe.warehouse.outbound.model.dto.WhOutboundDto;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundItem;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundHeaderRepository;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundItemRepository;
import org.example.stockitbe.warehouse.outbound.repository.WhOutboundStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WhOutboundService {

    private final WhOutboundHeaderRepository outboundHeaderRepository;
    private final WhOutboundItemRepository outboundItemRepository;
    private final WhOutboundStatusHistoryRepository outboundStatusHistoryRepository;
    private final StoreInboundHeaderRepository inboundHeaderRepository;
    private final StoreInboundStatusHistoryRepository inboundStatusHistoryRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final CircularBuyerRepository circularBuyerRepository;
    private final InventoryService inventoryService;
    private final WarehouseTransferHeaderRepository warehouseTransferHeaderRepository;

    // 출고 목록 조회 함수
    @Transactional(readOnly = true)
    public List<WhOutboundDto.ListRes> list(AuthUserDetails me, String status, LocalDate from, LocalDate to, String keyword) {
        // 1) 로그인 사용자 기준 창고 범위를 해석하고 필터 값을 정규화한다.
        Long myWarehouseId = resolveWarehouseId(me);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        OutboundStatus statusFilter = parseStatus(status);
        Date fromDate = from == null ? null : Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDateExclusive = to == null ? null : Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 2) 대상 창고 출고 헤더를 조회한다.
        List<WhOutboundHeader> headers = outboundHeaderRepository.findAllByWarehouseIdOrderByRequestedAtDescIdDesc(myWarehouseId);
        if (headers.isEmpty()) return List.of();

        // 3) 응답 표시에 필요한 창고 정보를 조회한다.
        Infrastructure myWarehouse = infrastructureRepository.findById(myWarehouseId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.OUTBOUND_NOT_FOUND));
        Map<Long, String> destinationNameById = loadDestinationNameMap(headers);

        // 4) 상태/기간/키워드 필터를 적용해 목록 DTO를 구성한다.
        return headers.stream()
                .filter(h -> statusFilter == null || h.getStatus() == statusFilter)
                .filter(h -> fromDate == null || !h.getRequestedAt().before(fromDate))
                .filter(h -> toDateExclusive == null || h.getRequestedAt().before(toDateExclusive))
                .filter(h -> safeKeyword.isBlank() || matchesKeyword(h, safeKeyword))
                .map(h -> WhOutboundDto.toListRes(
                        h,
                        myWarehouse.getCode(),
                        myWarehouse.getName(),
                        destinationNameById.get(h.getDestinationId())
                ))
                .toList();
    }

    // 출고 상세 조회 함수
    @Transactional(readOnly = true)
    public WhOutboundDto.DetailRes detail(AuthUserDetails me, String outboundNo) {
        // 1) 로그인 창고 범위를 해석하고, 소유 출고건인지 검증한다.
        Long myWarehouseId = resolveWarehouseId(me);
        WhOutboundHeader header = findOwnedOutbound(outboundNo, myWarehouseId);

        // 2) 상세 조합에 필요한 헤더/라인/상태이력/연계입고 데이터를 조회한다.
        Infrastructure warehouse = infrastructureRepository.findById(header.getWarehouseId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.OUTBOUND_NOT_FOUND));
        List<WhOutboundItem> items = outboundItemRepository.findAllByOutboundHeaderIdOrderByIdAsc(header.getId());
        List<WhOutboundStatusHistory> history = outboundStatusHistoryRepository.findAllByOutboundHeaderIdOrderByChangedAtAscIdAsc(header.getId());
        StoreInboundHeader inbound = inboundHeaderRepository.findByOutboundNo(header.getOutboundNo()).orElse(null);
        String destinationName = resolveDestinationName(header.getDestinationType(), header.getDestinationId());

        // 3) 상세 응답 DTO를 조합해 반환한다.
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
                .destinationName(destinationName)
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

    // 출고 확정 처리 함수 (READY_TO_SHIP -> IN_TRANSIT)
    @Transactional
    public WhOutboundDto.DetailRes confirm(AuthUserDetails me, String outboundNo, String reason) {
        // 1) 로그인 창고 범위를 해석하고, 소유 출고건인지 검증한다.
        Long myWarehouseId = resolveWarehouseId(me);
        WhOutboundHeader header = findOwnedOutbound(outboundNo, myWarehouseId);

        // 2) READY_TO_SHIP 상태에서만 출고 확정이 가능하다.
        if (header.getStatus() != OutboundStatus.READY_TO_SHIP) {
            throw BaseException.from(BaseResponseStatus.OUTBOUND_INVALID_STATUS_TRANSITION);
        }

        // 3) 출고 라인별로 예약재고를 이동중 재고로 전이한다.
        //    destinationType=WAREHOUSE (창고간 이동) 인 경우 도착 창고 가용재고+ 도 같은 시점에 박는다.
        //    — PO 의 startInTransit 시점에 도착 창고 가용+ 와 동일 패턴 (ADR-024).
        List<WhOutboundItem> items = outboundItemRepository.findAllByOutboundHeaderIdOrderByIdAsc(header.getId());
        boolean toWarehouse = header.getDestinationType() == OutboundDestinationType.WAREHOUSE;
        for (WhOutboundItem item : items) {
            int moved = inventoryService.moveReservedToInTransit(header.getWarehouseId(), item.getSkuId(), item.getRequestedQuantity());
            if (moved != item.getRequestedQuantity()) {
                throw BaseException.from(BaseResponseStatus.OUTBOUND_RESERVED_STOCK_NOT_ENOUGH);
            }
            if (toWarehouse) {
                inventoryService.increaseAvailable(header.getDestinationId(), item.getSkuCode(), item.getRequestedQuantity());
            }
        }

        // 4) 출고 상태를 IN_TRANSIT으로 전이하고 상태 이력을 저장한다.
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
        syncTransferStatusFromOutbound(header, OutboundStatus.IN_TRANSIT);

        // 5) 최신 상세 정보를 반환한다.
        return detail(me, outboundNo);
    }

    // 배송 완료 처리 함수 (IN_TRANSIT -> ARRIVED)
    @Transactional
    public WhOutboundDto.DetailRes arrive(AuthUserDetails me, String outboundNo, String reason) {
        // 1) 로그인 창고 범위를 해석하고, 소유 출고건인지 검증한다.
        Long myWarehouseId = resolveWarehouseId(me);
        WhOutboundHeader header = findOwnedOutbound(outboundNo, myWarehouseId);

        // 2) IN_TRANSIT 상태에서만 배송완료 처리가 가능하다.
        if (header.getStatus() != OutboundStatus.IN_TRANSIT) {
            throw BaseException.from(BaseResponseStatus.OUTBOUND_INVALID_STATUS_TRANSITION);
        }

        // 3) 출고 상태를 ARRIVED로 전이하고 상태 이력을 저장한다.
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
        syncTransferStatusFromOutbound(header, OutboundStatus.ARRIVED);

        // 4) 최신 상세 정보를 반환한다.
        inboundHeaderRepository.findByOutboundNo(header.getOutboundNo()).ifPresent(inbound -> {
            boolean exists = inboundStatusHistoryRepository
                    .existsByInboundHeaderIdAndStatus(inbound.getId(), StoreInboundStatus.PENDING_RECEIPT);
            if (!exists) {
                inboundStatusHistoryRepository.save(
                        StoreInboundStatusHistory.builder()
                                .inboundHeaderId(inbound.getId())
                                .status(StoreInboundStatus.PENDING_RECEIPT)
                                .changedAt(now)
                                .changedByMemberId(me.getEmployeeCode())
                                .changedByName(me.getName())
                                .reason("OUTBOUND_ARRIVED")
                                .build()
                );
            }
        });

        return detail(me, outboundNo);
    }

    // 매장발주/순환판매 출고는 제외하고, 재고이동 원천만 상태를 동기화한다.
    private void syncTransferStatusFromOutbound(WhOutboundHeader header, OutboundStatus toStatus) {
        if (header.getSourceType() != OutboundSourceType.WAREHOUSE_TRANSFER) return;
        warehouseTransferHeaderRepository.findByTransferNo(header.getSourceRefNo()).ifPresent(transfer -> {
            applyTransferStatus(transfer, toStatus);
        });
    }

    // outbound 상태 전이를 transfer 상태로 미러링한다.
    private void applyTransferStatus(WarehouseTransferHeader transfer, OutboundStatus toStatus) {
        if (toStatus == OutboundStatus.IN_TRANSIT) {
            transfer.markInTransit();
            return;
        }
        if (toStatus == OutboundStatus.ARRIVED) {
            transfer.markArrived();
        }
    }

    // 로그인 사용자 기준 창고 ID 해석 함수
    private Long resolveWarehouseId(AuthUserDetails me) {
        // 1) 사용자 locationCode 유효성을 검증한다.
        String locationCode = me == null ? null : me.getLocationCode();
        if (locationCode == null || locationCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.OUTBOUND_SCOPE_FORBIDDEN);
        }
        // 2) locationCode를 창고 인프라 ID로 해석한다.
        return infrastructureRepository.findByCodeAndLocationType(locationCode, LocationType.WAREHOUSE)
                .map(Infrastructure::getId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.OUTBOUND_SCOPE_FORBIDDEN));
    }

    // 출고 소유권(창고 범위) 검증 조회 함수
    private WhOutboundHeader findOwnedOutbound(String outboundNo, Long warehouseId) {
        // 1) 출고번호로 헤더를 조회한다.
        WhOutboundHeader header = outboundHeaderRepository.findByOutboundNo(outboundNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.OUTBOUND_NOT_FOUND));
        // 2) 출고건의 창고와 로그인 창고가 다르면 접근을 차단한다.
        if (!header.getWarehouseId().equals(warehouseId)) {
            throw BaseException.from(BaseResponseStatus.OUTBOUND_SCOPE_FORBIDDEN);
        }
        return header;
    }

    // 상태 문자열 파싱 함수
    private OutboundStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "전체".equals(status)) return null;
        try {
            return OutboundStatus.valueOf(status);
        } catch (Exception e) {
            return null;
        }
    }

    // 키워드 매칭 함수
    private boolean matchesKeyword(WhOutboundHeader header, String safeKeyword) {
        String text = (header.getOutboundNo() + " " + header.getSourceRefNo() + " " + header.getDestinationType().name())
                .toLowerCase(Locale.ROOT);
        return text.contains(safeKeyword);
    }

    // [출고 목적지명 맵 조회] destinationType/destinationId 기준으로 목적지명을 조회한다.
    private Map<Long, String> loadDestinationNameMap(List<WhOutboundHeader> headers) {
        if (headers == null || headers.isEmpty()) return Map.of();

        Set<Long> infraDestinationIds = new HashSet<>();
        Set<Long> buyerDestinationIds = new HashSet<>();
        for (WhOutboundHeader header : headers) {
            if (header.getDestinationId() == null || header.getDestinationType() == null) continue;
            if (header.getDestinationType() == OutboundDestinationType.STORE
                    || header.getDestinationType() == OutboundDestinationType.WAREHOUSE) {
                infraDestinationIds.add(header.getDestinationId());
            } else if (header.getDestinationType() == OutboundDestinationType.CIRCULAR_BUYER) {
                buyerDestinationIds.add(header.getDestinationId());
            }
        }

        Map<Long, String> nameById = new HashMap<>();
        if (!infraDestinationIds.isEmpty()) {
            for (Infrastructure infra : infrastructureRepository.findAllById(infraDestinationIds)) {
                nameById.put(infra.getId(), infra.getName());
            }
        }
        if (!buyerDestinationIds.isEmpty()) {
            for (CircularBuyer buyer : circularBuyerRepository.findAllById(buyerDestinationIds)) {
                nameById.put(buyer.getId(), buyer.getCompanyName());
            }
        }
        return nameById;
    }

    // [목적지명 단건 조회] destinationType에 따라 인프라/거래처에서 이름을 조회한다.
    private String resolveDestinationName(OutboundDestinationType destinationType, Long destinationId) {
        if (destinationType == null || destinationId == null) return null;

        if (destinationType == OutboundDestinationType.STORE || destinationType == OutboundDestinationType.WAREHOUSE) {
            return infrastructureRepository.findById(destinationId).map(Infrastructure::getName).orElse(null);
        }
        if (destinationType == OutboundDestinationType.CIRCULAR_BUYER) {
            return circularBuyerRepository.findById(destinationId).map(CircularBuyer::getCompanyName).orElse(null);
        }
        return null;
    }
}
