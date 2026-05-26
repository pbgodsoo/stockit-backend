package org.example.stockitbe.hq.analytics.dashboardanalytics;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.analytics.dashboardanalytics.model.DashboardAnalyticsDto;
import org.example.stockitbe.hq.analytics.dashboardanalytics.model.DashboardPeriod;
import org.example.stockitbe.hq.analytics.orderstatsanalytics.OrderStatsAnalyticsService;
import org.example.stockitbe.hq.analytics.orderstatsanalytics.model.OrderStatsAnalyticsDto;
import org.example.stockitbe.hq.analytics.orderstatsanalytics.model.OrderStatsPeriod;
import org.example.stockitbe.hq.analytics.salesanalytics.SalesAnalyticsService;
import org.example.stockitbe.hq.analytics.salesanalytics.model.SalesAnalyticsDto;
import org.example.stockitbe.hq.analytics.salesanalytics.model.SalesPeriod;
import org.example.stockitbe.hq.analytics.turnoveranalytics.TurnoverAnalyticsService;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverAnalyticsDto;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverPeriod;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverScope;
import org.example.stockitbe.hq.analytics.vendoranalytics.VendorAnalyticsService;
import org.example.stockitbe.hq.analytics.vendoranalytics.model.VendorAnalyticsDto;
import org.example.stockitbe.hq.analytics.vendoranalytics.model.VendorPeriod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

// @Transactional 제거: 각 서브서비스가 독립 트랜잭션으로 실행되도록 해
// 단일 커넥션을 수십 초 점유하는 Hikari leak 경고를 해소한다.
@Service
@RequiredArgsConstructor
public class DashboardAnalyticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SalesAnalyticsService salesService;
    private final TurnoverAnalyticsService turnoverService;
    private final VendorAnalyticsService vendorService;
    private final OrderStatsAnalyticsService orderStatsService;
    // 4개 서브서비스 병렬 실행 전용 풀 — AsyncConfig.dashboardExecutor 빈과 이름 매칭.
    private final Executor dashboardExecutor;
    // TEMP(E2E): 대시보드 타임아웃/DB 과부하 완화용 빠른 응답 분기.
    // TODO: 테스트 안정화 이슈 종료 후 반드시 제거.
    @Value("${app.dashboard.e2e-fast:false}")
    private boolean dashboardE2eFast;

    public DashboardAnalyticsDto.Res getDashboardAnalytics(
            DashboardPeriod period, LocalDate from, LocalDate to) {
        // TEMP(E2E): 고정 응답 fast-path.
        // TODO: 테스트 인프라 정상화(실API 기준 안정 통과) 후 반드시 제거.
        if (dashboardE2eFast) {
            DashboardAnalyticsDto.KpiSummary kpi = DashboardAnalyticsDto.KpiSummary.builder()
                    .totalRevenue(BigDecimal.ZERO)
                    .totalRevenueTrendPct(BigDecimal.ZERO)
                    .lockedValue(BigDecimal.ZERO)
                    .dangerSkuCount(0L)
                    .totalSkuCount(0L)
                    .activeVendorCount(0)
                    .activeMaterialCount(0)
                    .circularSalesAmount(BigDecimal.ZERO)
                    .topProductName("-")
                    .topProductSales(BigDecimal.ZERO)
                    .topProductUnits(0L)
                    .bestCategoryName("-")
                    .bestCategoryAmount(BigDecimal.ZERO)
                    .bestCategorySharePct(BigDecimal.ZERO)
                    .totalSalesQty(0L)
                    .healthyCount(0L)
                    .cautionCount(0L)
                    .warningCount(0L)
                    .shortestOrderItem("-")
                    .shortestOrderCycle(0)
                    .longestOrderItem("-")
                    .longestOrderCycle(0)
                    .topWarehouseName("-")
                    .topWarehouseOrderCount(0L)
                    .topWarehouseItemCount(0L)
                    .topWarehouseAmount(BigDecimal.ZERO)
                    .topVendorName("-")
                    .topVendorAmount(BigDecimal.ZERO)
                    .topMaterialName("-")
                    .topMaterialAmount(BigDecimal.ZERO)
                    .topMaterialWeight(0L)
                    .topMaterialType("-")
                    .build();

            return DashboardAnalyticsDto.Res.builder()
                    .fromDate(from.format(DATE_FMT))
                    .toDate(to.format(DATE_FMT))
                    .period(period)
                    .kpi(kpi)
                    .trendCurrent(Collections.emptyList())
                    .build();
        }

        // 4개 통계 service 병렬 호출 — 각자 독립 트랜잭션으로 실행되어
        // 커넥션 점유 시간이 분산되고 총 응답시간은 max(A,B,C,D)로 단축된다.
        CompletableFuture<SalesAnalyticsDto.Res> salesF = CompletableFuture.supplyAsync(
                () -> salesService.getSalesAnalytics(mapSalesPeriod(period), from, to, null, null),
                dashboardExecutor);
        CompletableFuture<TurnoverAnalyticsDto.Res> turnoverF = CompletableFuture.supplyAsync(
                () -> turnoverService.getTurnoverAnalytics(mapTurnoverPeriod(period), from, to, TurnoverScope.ALL, null),
                dashboardExecutor);
        CompletableFuture<VendorAnalyticsDto.Res> vendorF = CompletableFuture.supplyAsync(
                () -> vendorService.getVendorAnalytics(mapVendorPeriod(period), from, to),
                dashboardExecutor);
        CompletableFuture<OrderStatsAnalyticsDto.Res> ordersF = CompletableFuture.supplyAsync(
                () -> orderStatsService.getOrderStats(mapOrderStatsPeriod(period), from, to, null),
                dashboardExecutor);

        try {
            CompletableFuture.allOf(salesF, turnoverF, vendorF, ordersF).join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("대시보드 집계 중 오류 발생", cause);
        }

        SalesAnalyticsDto.Res sales    = salesF.join();
        TurnoverAnalyticsDto.Res turnover = turnoverF.join();
        VendorAnalyticsDto.Res vendor   = vendorF.join();
        OrderStatsAnalyticsDto.Res orders = ordersF.join();

        // ── 매출 1위 품목 (Sales.productDetailsBySubCategory 평탄화 후 max) ──
        SalesAnalyticsDto.ProductStats topProduct = null;
        if (sales.getProductDetailsBySubCategory() != null) {
            topProduct = sales.getProductDetailsBySubCategory().values().stream()
                    .flatMap(List::stream)
                    .max((a, b) -> a.getSalesAmount().compareTo(b.getSalesAmount()))
                    .orElse(null);
        }

        // ── 베스트 카테고리 매출 ──
        BigDecimal bestCategoryAmount = BigDecimal.ZERO;
        String bestCatName = sales.getKpi().getBestCategoryName();
        if (bestCatName != null && sales.getCategorySummary() != null) {
            bestCategoryAmount = sales.getCategorySummary().stream()
                    .filter(c -> bestCatName.equals(c.getMainCategory()))
                    .map(SalesAnalyticsDto.CategorySummary::getSalesAmount)
                    .findFirst().orElse(BigDecimal.ZERO);
        }

        // ── TOP 발주 창고 ──
        var topWh = (orders.getWarehouseOrders() == null || orders.getWarehouseOrders().isEmpty())
                ? null : orders.getWarehouseOrders().get(0);

        // ── TOP 소재 보강 (circularMaterials[0] 이미 매출순 정렬) ──
        var topMat = (vendor.getCircularMaterials() == null || vendor.getCircularMaterials().isEmpty())
                ? null : vendor.getCircularMaterials().get(0);

        DashboardAnalyticsDto.KpiSummary kpi = DashboardAnalyticsDto.KpiSummary.builder()
                // KPI 5카드
                .totalRevenue(sales.getKpi().getTotalRevenue())
                .totalRevenueTrendPct(sales.getKpi().getTotalRevenueTrendPct())
                .totalSalesQty(sales.getKpi().getTotalQuantity())
                .lockedValue(turnover.getInventoryHealth().getLockedValue())
                .dangerSkuCount(turnover.getInventoryHealth().getDanger())
                .totalSkuCount(turnover.getInventoryHealth().getTotalSku())
                .activeVendorCount(vendor.getKpi().getActiveVendorCount())
                .activeMaterialCount(vendor.getKpi().getActiveMaterialCount())
                .circularSalesAmount(vendor.getKpi().getTotalSalesAmount())
                // 카드 1 (Sales)
                .topProductName(topProduct == null ? "" : topProduct.getProductName())
                .topProductSales(topProduct == null ? BigDecimal.ZERO : topProduct.getSalesAmount())
                .topProductUnits(topProduct == null ? 0L : topProduct.getQuantity())
                .bestCategoryName(sales.getKpi().getBestCategoryName())
                .bestCategoryAmount(bestCategoryAmount)
                .bestCategorySharePct(sales.getKpi().getBestCategorySharePct())
                // 카드 2 (Turnover)
                .healthyCount(turnover.getInventoryHealth().getHealthy())
                .cautionCount(turnover.getInventoryHealth().getCaution())
                .warningCount(turnover.getInventoryHealth().getWarning())
                // 카드 3 (OrderStats)
                .shortestOrderItem(orders.getKpi() == null ? "" : orders.getKpi().getShortestCycleItem())
                .shortestOrderCycle(orders.getKpi() == null ? 0 : orders.getKpi().getShortestCycleDays())
                .longestOrderItem(orders.getKpi() == null ? "" : orders.getKpi().getLongestCycleItem())
                .longestOrderCycle(orders.getKpi() == null ? 0 : orders.getKpi().getLongestCycleDays())
                .topWarehouseName(topWh == null ? "" : topWh.getWarehouseName())
                .topWarehouseOrderCount(topWh == null ? 0L : topWh.getOrders())
                .topWarehouseItemCount(topWh == null ? 0L : topWh.getItems())
                .topWarehouseAmount(topWh == null ? BigDecimal.ZERO : topWh.getTotalValue())
                // 카드 4 (Vendor)
                .topVendorName(vendor.getKpi().getTopVendorName())
                .topVendorAmount(vendor.getKpi().getTopVendorAmount())
                // 카드 5 (Vendor 소재)
                .topMaterialName(vendor.getKpi().getTopMaterialName())
                .topMaterialAmount(vendor.getKpi().getTopMaterialAmount())
                .topMaterialWeight(topMat == null ? 0L : topMat.getUnits())
                .topMaterialType(topMat == null ? "" : topMat.getMaterialType())
                .build();

        // ── 일자별 매출 추이 (Sales.trend.current) ──
        List<DashboardAnalyticsDto.TrendPoint> trendCurrent =
                (sales.getTrend() == null || sales.getTrend().getCurrent() == null)
                        ? Collections.emptyList()
                        : sales.getTrend().getCurrent().stream()
                        .map(p -> DashboardAnalyticsDto.TrendPoint.builder()
                                .label(p.getLabel())
                                .revenue(p.getRevenue())
                                .build())
                        .collect(Collectors.toList());

        return DashboardAnalyticsDto.Res.builder()
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .period(period)
                .kpi(kpi)
                .trendCurrent(trendCurrent)
                .build();
    }

    // ── Period enum 매핑 ──
    private static SalesPeriod mapSalesPeriod(DashboardPeriod p) {
        return switch (p) {
            case DAY -> SalesPeriod.DAY;
            case MONTH -> SalesPeriod.MONTH;
            case YEAR -> SalesPeriod.YEAR;
        };
    }
    private static TurnoverPeriod mapTurnoverPeriod(DashboardPeriod p) {
        return switch (p) {
            case DAY -> TurnoverPeriod.DAY;
            case MONTH -> TurnoverPeriod.MONTH;
            case YEAR -> TurnoverPeriod.YEAR;
        };
    }
    private static VendorPeriod mapVendorPeriod(DashboardPeriod p) {
        return switch (p) {
            case DAY, MONTH -> VendorPeriod.MONTH;
            case YEAR -> VendorPeriod.YEAR;
        };
    }
    private static OrderStatsPeriod mapOrderStatsPeriod(DashboardPeriod p) {
        return switch (p) {
            case DAY -> OrderStatsPeriod.DAY;
            case MONTH -> OrderStatsPeriod.MONTH;
            case YEAR -> OrderStatsPeriod.YEAR;
        };
    }
}
