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

    // 매장 입고 목록 조회
    // 로그인 사용자의 매장 범위 내 입고건만 조회하고, 상태/기간/키워드 필터를 적용한다.
    @Transactional(readOnly = true)
    public List<StoreInboundDto.ListRes> list(AuthUserDetails me, String status, LocalDate from, LocalDate to, String keyword) {
        // 1) 로그인 컨텍스트에서 매장 ID를 해석한다.
        Long myStoreId = resolveStoreId(me);

        // 2) 필터 입력값을 안전하게 정규화한다.
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        StoreInboundStatus statusFilter = parseStatus(status);
        Date fromDate = from == null ? null : Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDateExclusive = to == null ? null : Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 3) 매장 소속 입고건을 조회 후 필터링하여 응답 DTO로 변환한다.
        return inboundHeaderRepository.findAllByStoreIdOrderByRequestedAtDescIdDesc(myStoreId).stream()
                .filter(h -> statusFilter == null || h.getStatus() == statusFilter)
                .filter(h -> fromDate == null || !h.getRequestedAt().before(fromDate))
                .filter(h -> toDateExclusive == null || h.getRequestedAt().before(toDateExclusive))
                .filter(h -> matchesKeyword(h, safeKeyword))
                .map(StoreInboundDto.ListRes::from)
                .toList();
    }

    // 매장 입고 상세 조회
    // 본인 매장 소유 입고건인지 검증한 뒤, 아이템/이력/연계 출고 요약을 함께 반환한다.
    @Transactional(readOnly = true)
    public StoreInboundDto.DetailRes detail(AuthUserDetails me, String inboundNo) {
        // 1) 로그인 사용자 매장 범위를 확인한다.
        Long myStoreId = resolveStoreId(me);

        // 2) 입고번호로 대상 건을 찾고 소유 범위를 검증한다.
        StoreInboundHeader inbound = findOwnedInbound(inboundNo, myStoreId);

        // 3) 상세 응답을 조합해 반환한다.
        return buildDetailRes(inbound);
    }

    // 매장 입고 확정 처리
    // 동일 트랜잭션 내에서 재고 반영, 입고 상태 전이/이력, 발주 완료 전이를 원자적으로 수행한다.
    @Transactional
    public StoreInboundDto.DetailRes confirm(AuthUserDetails me, String inboundNo, String reason) {
        // 1) 로그인 사용자 매장 범위를 확인한다.
        Long myStoreId = resolveStoreId(me);

        // 2) 입고번호로 대상 건을 찾고 소유 범위를 검증한다.
        StoreInboundHeader inbound = findOwnedInbound(inboundNo, myStoreId);

        // 3) PENDING_RECEIPT 상태에서만 확정이 가능하다.
        if (inbound.getStatus() != StoreInboundStatus.PENDING_RECEIPT) {
            throw BaseException.from(BaseResponseStatus.INVALID_INBOUND_STATUS_TRANSITION);
        }

        // 4) 입고 라인 기준으로 매장 NORMAL 재고를 증가시킨다.
        List<StoreInboundItem> inboundItems = inboundItemRepository.findAllByInboundHeaderIdOrderByIdAsc(inbound.getId());
        for (StoreInboundItem item : inboundItems) {
            inventoryService.increaseOnHandAndAvailable(
                    inbound.getStoreId(),
                    item.getSkuId(),
                    item.getExpectedQuantity()
            );
        }

        // 5) 입고 헤더를 RECEIVED로 전이하고 확정자/시각을 기록한다.
        Date now = new Date();
        String actorMemberId = me == null ? null : me.getEmployeeCode();
        String actorName = me == null ? null : me.getName();
        inbound.markReceived(now, actorMemberId, actorName);

        // 6) 입고 상태 이력(RECEIVED)을 누락 없이 저장한다.
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

        // 7) 같은 발주의 연계 입고가 모두 RECEIVED면 발주를 COMPLETED로 자동 전이한다.
        completeOrderWhenAllInboundReceived(inbound.getSourceRefNo(), actorMemberId, actorName);

        // 8) 최신 상태 기준 상세 응답을 반환한다.
        return buildDetailRes(inbound);
    }

    // ----------------------------- 내부 메서드 --------------------------------

    // 연계 발주 완료 전이 처리
    // 같은 sourceRefNo(발주번호)의 입고가 전건 RECEIVED일 때만 발주를 COMPLETED로 전이한다.
    private void completeOrderWhenAllInboundReceived(String orderNo, String actorMemberId, String actorName) {
        // 1) 발주번호가 비어있으면 처리하지 않는다.
        if (orderNo == null || orderNo.isBlank()) return;

        // 2) 발주 헤더를 조회한다.
        StoreOrderHeader order = storeOrderHeaderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_NOT_FOUND));

        // 3) 연계 입고 전건이 RECEIVED인지 확인한다.
        List<StoreInboundHeader> allInbound = inboundHeaderRepository.findAllBySourceRefNo(orderNo);
        boolean allReceived = !allInbound.isEmpty()
                && allInbound.stream().allMatch(h -> h.getStatus() == StoreInboundStatus.RECEIVED);
        if (!allReceived) return;

        // 4) 이미 COMPLETED 상태면 중복 전이하지 않는다.
        if (order.getStatus() == StoreOrderStatus.COMPLETED) return;

        // 5) 발주 상태를 COMPLETED로 전이하고 상태 이력을 기록한다.
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

    // 입고 상세 응답 조합
    // 헤더/아이템/상태이력/연계 출고 요약을 단일 DTO로 구성한다.
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

    // 로그인 사용자 매장 ID 해석
    // locationCode를 STORE 타입 인프라와 매핑하여 매장 범위를 강제한다.
    private Long resolveStoreId(AuthUserDetails me) {
        String locationCode = me == null ? null : me.getLocationCode();
        if (locationCode == null || locationCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.STORE_ORDER_SCOPE_FORBIDDEN);
        }

        Infrastructure infra = infrastructureRepository.findByCodeAndLocationType(locationCode, LocationType.STORE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_SCOPE_FORBIDDEN));
        return infra.getId();
    }

    // 입고 소유 검증 조회
    // inboundNo로 조회한 뒤, 로그인 매장(storeId)과 일치하지 않으면 차단한다.
    private StoreInboundHeader findOwnedInbound(String inboundNo, Long storeId) {
        StoreInboundHeader inbound = inboundHeaderRepository.findByInboundNo(inboundNo)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.INBOUND_NOT_FOUND));
        if (!inbound.getStoreId().equals(storeId)) {
            throw BaseException.from(BaseResponseStatus.STORE_ORDER_SCOPE_FORBIDDEN);
        }
        return inbound;
    }

    // 상태 필터 파싱
    // 빈 값/전체는 null(필터 미적용), 유효 enum만 변환한다.
    private StoreInboundStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "전체".equals(status)) return null;
        try {
            return StoreInboundStatus.valueOf(status);
        } catch (Exception e) {
            return null;
        }
    }

    // 키워드 필터 매칭
    // inboundNo/sourceRefNo/outboundNo 조합 문자열에 포함 여부를 검사한다.
    private boolean matchesKeyword(StoreInboundHeader header, String safeKeyword) {
        if (safeKeyword.isBlank()) return true;
        String search = (header.getInboundNo() + " " + header.getSourceRefNo() + " " + header.getOutboundNo())
                .toLowerCase(Locale.ROOT);
        return search.contains(safeKeyword);
    }
}