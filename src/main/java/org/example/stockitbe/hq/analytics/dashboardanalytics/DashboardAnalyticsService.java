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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAnalyticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SalesAnalyticsService salesService;
    private final TurnoverAnalyticsService turnoverService;
    private final VendorAnalyticsService vendorService;
    private final OrderStatsAnalyticsService orderStatsService;

    public DashboardAnalyticsDto.Res getDashboardAnalytics(
            DashboardPeriod period, LocalDate from, LocalDate to) {

        // 4개 통계 service 호출
        SalesAnalyticsDto.Res sales = salesService.getSalesAnalytics(
                mapSalesPeriod(period), from, to, null, null);
        TurnoverAnalyticsDto.Res turnover = turnoverService.getTurnoverAnalytics(
                mapTurnoverPeriod(period), from, to, TurnoverScope.ALL, null);
        VendorAnalyticsDto.Res vendor = vendorService.getVendorAnalytics(
                mapVendorPeriod(period), from, to);
        OrderStatsAnalyticsDto.Res orders = orderStatsService.getOrderStats(
                mapOrderStatsPeriod(period), from, to, null);

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
