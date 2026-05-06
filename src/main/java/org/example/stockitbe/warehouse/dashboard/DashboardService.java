package org.example.stockitbe.warehouse.dashboard;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderRepository;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderStatusHistoryRepository;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatusHistory;
import org.example.stockitbe.warehouse.dashboard.model.DashboardDto;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WHS-001 입고 진행률 — 창고 관리자 대시보드 집계 서비스.
 *
 * 집계 대상은 `purchase_order` + `purchase_order_status_history` 단일 진실 원천 (ADR-015).
 * 별 테이블·Entity 신설 없이 read-only 활용만 한다.
 *
 * 인증 사용자의 locationCode 를 받아 자기 창고 row 만 집계. Long warehouseId 는
 * Infrastructure lookup 한 번에 격리되어 기존 Specification 로직 변경 없음.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderStatusHistoryRepository historyRepository;
    private final InfrastructureRepository infrastructureRepository;

    @Transactional(readOnly = true)
    public DashboardDto.InboundProgressRes getInboundProgress(String locationCode,
                                                                LocalDate from, LocalDate to) {
        Long warehouseId = infrastructureRepository
                .findByCodeAndLocationType(locationCode, LocationType.WAREHOUSE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND))
                .getId();
        Specification<PurchaseOrder> spec = buildSpec(warehouseId, from, to);
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll(spec);

        Map<PurchaseOrderStatus, Long> statusBreakdown = buildStatusBreakdown(orders);

        // scheduledCount 의미 — 입고 예정(미완료 거래처 단계 합산): READY_TO_SHIP + IN_TRANSIT + ARRIVED.
        // 단일 SHIPPING 상태가 사라지고 거래처 단계가 4단계로 늘어났으므로 합산이 자연.
        long scheduledCount = statusBreakdown.get(PurchaseOrderStatus.READY_TO_SHIP)
                + statusBreakdown.get(PurchaseOrderStatus.IN_TRANSIT)
                + statusBreakdown.get(PurchaseOrderStatus.ARRIVED);
        long completedCount = statusBreakdown.get(PurchaseOrderStatus.COMPLETED);
        long cancelledCount = statusBreakdown.get(PurchaseOrderStatus.CANCELLED);
        long totalCount = orders.size() - cancelledCount;
        double progressRate = totalCount == 0 ? 0.0 : (double) completedCount / totalCount;

        Double avgProcessingHours = computeAvgProcessingHours(orders);

        DashboardDto.Kpi kpi = DashboardDto.Kpi.builder()
                .scheduledCount(scheduledCount)
                .completedCount(completedCount)
                .totalCount(totalCount)
                .progressRate(progressRate)
                .avgProcessingHours(avgProcessingHours)
                .build();

        return DashboardDto.InboundProgressRes.builder()
                .kpi(kpi)
                .statusBreakdown(statusBreakdown)
                .build();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Map<PurchaseOrderStatus, Long> buildStatusBreakdown(List<PurchaseOrder> orders) {
        Map<PurchaseOrderStatus, Long> breakdown = new EnumMap<>(PurchaseOrderStatus.class);
        for (PurchaseOrderStatus s : PurchaseOrderStatus.values()) {
            breakdown.put(s, 0L);
        }
        Map<PurchaseOrderStatus, Long> grouped = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getStatus, Collectors.counting()));
        breakdown.putAll(grouped);
        return breakdown;
    }

    /**
     * COMPLETED 발주들의 (COMPLETED.changedAt - createdAt) 평균 시간.
     * - COMPLETED 발주 0건이면 null.
     * - 각 발주의 COMPLETED-status history 행은 batch 1회 조회 (N+1 회피).
     * - 매핑되는 history 가 없는 발주는 스킵 (방어선).
     * - 결과 0건이면 null, 있으면 소수점 1자리 반올림.
     */
    private Double computeAvgProcessingHours(List<PurchaseOrder> orders) {
        List<PurchaseOrder> completedOrders = orders.stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.COMPLETED)
                .toList();
        if (completedOrders.isEmpty()) {
            return null;
        }

        Set<Long> completedIds = completedOrders.stream()
                .map(PurchaseOrder::getId)
                .collect(Collectors.toSet());

        Map<Long, Date> completedAtById = historyRepository
                .findAllByPurchaseOrderIdInAndStatus(completedIds, PurchaseOrderStatus.COMPLETED)
                .stream()
                .collect(Collectors.toMap(
                        PurchaseOrderStatusHistory::getPurchaseOrderId,
                        PurchaseOrderStatusHistory::getChangedAt,
                        (a, b) -> a));

        double sum = 0.0;
        int count = 0;
        for (PurchaseOrder po : completedOrders) {
            Date completedAt = completedAtById.get(po.getId());
            if (completedAt == null || po.getCreatedAt() == null) {
                continue;
            }
            double hours = (completedAt.getTime() - po.getCreatedAt().getTime()) / (1000.0 * 60 * 60);
            sum += hours;
            count++;
        }

        if (count == 0) {
            return null;
        }
        double avg = sum / count;
        return Math.round(avg * 10.0) / 10.0;
    }

    private Specification<PurchaseOrder> buildSpec(Long warehouseId, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            }
            if (from != null) {
                Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (to != null) {
                Date toDate = Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                predicates.add(cb.lessThan(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
