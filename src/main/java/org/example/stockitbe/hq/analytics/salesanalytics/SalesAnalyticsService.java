package org.example.stockitbe.hq.analytics.salesanalytics;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.analytics.salesanalytics.model.SalesAnalyticsDto;
import org.example.stockitbe.hq.analytics.salesanalytics.model.SalesPeriod;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.InfraStatus;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.store.sale.StoreSaleHeaderRepository;
import org.example.stockitbe.store.sale.StoreSaleItemRepository;
import org.example.stockitbe.store.sale.model.StoreSaleStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesAnalyticsService {
    private static final StoreSaleStatus COMPLETED = StoreSaleStatus.COMPLETED;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MM/dd");

    private final StoreSaleHeaderRepository headerRepo;
    private final StoreSaleItemRepository itemRepo;
    private final InfrastructureRepository infraRepo;


    public SalesAnalyticsDto.Res getSalesAnalytics(
            SalesPeriod period, LocalDate from, LocalDate to,
            String storeCode, String mainCategory) {

        // 1) storeCode → storeId 변환 (옵션)
        Long storeId = null;
        if (storeCode != null && !storeCode.isBlank()) {
            storeId = infraRepo.findByCode(storeCode.trim())
                    .map(Infrastructure::getId).orElse(-1L);  // -1 = 매칭 없으면 결과 0
        }
        String mainCat = (mainCategory != null && !mainCategory.isBlank()) ? mainCategory.trim() : null;

        // 2) 기간 변환 (current / previous)
        Date fromDate = toDate(from.atStartOfDay());
        Date toDateExcl = toDate(to.plusDays(1).atStartOfDay());
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevFrom = from.minusDays(days);
        LocalDate prevTo = from.minusDays(1);
        Date prevFromDate = toDate(prevFrom.atStartOfDay());
        Date prevToDateExcl = toDate(from.atStartOfDay());

        // 3) KPI
        SalesAnalyticsDto.KpiSummary kpi = buildKpi(
                fromDate, toDateExcl, prevFromDate, prevToDateExcl, storeId, mainCat);

        // 4) Trend
        SalesAnalyticsDto.TrendData trend = buildTrend(
                period, from, to, prevFrom, prevTo, storeId);

        // 5) Category summary
        List<SalesAnalyticsDto.CategorySummary> categorySummary =
                buildCategorySummary(fromDate, toDateExcl, storeId);

        // 6) Sub-category stats
        List<SalesAnalyticsDto.SubCategoryStats> subCategoryStats =
                buildSubCategoryStats(fromDate, toDateExcl, storeId, mainCat);

        // 7) Product details (sub_category → list)
        Map<String, List<SalesAnalyticsDto.ProductStats>> productDetails =
                buildProductDetails(fromDate, toDateExcl, storeId, mainCat);

        return SalesAnalyticsDto.Res.builder()
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .period(period)
                .kpi(kpi)
                .trend(trend)
                .categorySummary(categorySummary)
                .subCategoryStats(subCategoryStats)
                .productDetailsBySubCategory(productDetails)
                .build();
    }

    // ── KPI ──
    private SalesAnalyticsDto.KpiSummary buildKpi(
            Date from, Date to, Date prevFrom, Date prevTo,
            Long storeId, String mainCat) {

        long totalRev = headerRepo.sumTotalAmount(COMPLETED, from, to, storeId);
        long totalQty = headerRepo.sumTotalQuantity(COMPLETED, from, to, storeId);
        long prevRev = headerRepo.sumTotalAmount(COMPLETED, prevFrom, prevTo, storeId);
        long prevQty = headerRepo.sumTotalQuantity(COMPLETED, prevFrom, prevTo, storeId);

        long activeStores = headerRepo.countActiveStores(COMPLETED, from, to);
        long prevActiveStores = headerRepo.countActiveStores(COMPLETED, prevFrom, prevTo);
        long totalStores = infraRepo.countByLocationTypeAndStatus(
                LocationType.STORE, InfraStatus.ACTIVE);

        // best category
        List<Object[]> catAgg = itemRepo.aggregateByMainCategory(COMPLETED, from, to, storeId);
        long grandTotal = catAgg.stream()
                .mapToLong(r -> ((Number) r[2]).longValue()).sum();
        String bestName = "";
        BigDecimal bestShare = BigDecimal.ZERO;
        if (!catAgg.isEmpty() && grandTotal > 0) {
            Object[] top = catAgg.stream()
                    .max(Comparator.comparingLong(r -> ((Number) r[2]).longValue()))
                    .orElse(null);
            if (top != null) {
                bestName = (String) top[0];
                long topAmount = ((Number) top[2]).longValue();
                bestShare = BigDecimal.valueOf(topAmount * 100.0 / grandTotal)
                        .setScale(1, RoundingMode.HALF_UP);
            }
        }

        return SalesAnalyticsDto.KpiSummary.builder()
                .totalRevenue(BigDecimal.valueOf(totalRev))
                .totalRevenueTrendPct(trendPct(totalRev, prevRev))
                .totalQuantity(totalQty)
                .totalQuantityTrendPct(trendPct(totalQty, prevQty))
                .activeStoreCount((int) activeStores)
                .totalStoreCount((int) totalStores)
                .activeStoreCountDelta((int) (activeStores - prevActiveStores))
                .bestCategoryName(bestName)
                .bestCategorySharePct(bestShare)
                .build();
    }

    // ── Trend ──
    private SalesAnalyticsDto.TrendData buildTrend(
            SalesPeriod period, LocalDate from, LocalDate to,
            LocalDate prevFrom, LocalDate prevTo, Long storeId) {

        List<SalesAnalyticsDto.TrendPoint> cur = dailyTrend(from, to, storeId);
        List<SalesAnalyticsDto.TrendPoint> prev = dailyTrend(prevFrom, prevTo, storeId);

        // period 가 WEEK/MONTH/... 이면 여기서 bucketing 가능
        // 1차 구현은 DAY 기준만 (FE 도 일자별 mock)
        return SalesAnalyticsDto.TrendData.builder()
                .current(cur).previous(prev).build();
    }

    private List<SalesAnalyticsDto.TrendPoint> dailyTrend(
            LocalDate from, LocalDate to, Long storeId) {
        Date f = toDate(from.atStartOfDay());
        Date t = toDate(to.plusDays(1).atStartOfDay());
        List<Object[]> rows = headerRepo.dailyRevenue(COMPLETED.name(), f, t, storeId);

        Map<String, Long> byDay = new HashMap<>();
        for (Object[] r : rows) {
            byDay.put(r[0].toString(), ((Number) r[1]).longValue());
        }
        List<SalesAnalyticsDto.TrendPoint> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String key = d.format(DATE_FMT);
            long rev = byDay.getOrDefault(key, 0L);
            result.add(SalesAnalyticsDto.TrendPoint.builder()
                    .label(d.format(LABEL_FMT))
                    .revenue(BigDecimal.valueOf(rev))
                    .build());
        }
        return result;
    }

    // ── Category Summary ──
    private List<SalesAnalyticsDto.CategorySummary> buildCategorySummary(
            Date from, Date to, Long storeId) {
        List<Object[]> rows = itemRepo.aggregateByMainCategory(COMPLETED, from, to, storeId);
        long grand = rows.stream().mapToLong(r -> ((Number) r[2]).longValue()).sum();

        return rows.stream().map(r -> {
            long amount = ((Number) r[2]).longValue();
            return SalesAnalyticsDto.CategorySummary.builder()
                    .mainCategory((String) r[0])
                    .productCount(((Number) r[1]).longValue())
                    .salesAmount(BigDecimal.valueOf(amount))
                    .quantity(((Number) r[3]).longValue())
                    .sharePct(grand == 0 ? BigDecimal.ZERO :
                            BigDecimal.valueOf(amount * 100.0 / grand)
                                    .setScale(1, RoundingMode.HALF_UP))
                    .build();
        }).collect(Collectors.toList());
    }

    // ── SubCategory Stats ──
    private List<SalesAnalyticsDto.SubCategoryStats> buildSubCategoryStats(
            Date from, Date to, Long storeId, String mainCat) {
        List<Object[]> rows = itemRepo.aggregateBySubCategory(COMPLETED, from, to, storeId, mainCat);
        long grand = rows.stream().mapToLong(r -> ((Number) r[3]).longValue()).sum();

        return rows.stream().map(r -> {
            long amount = ((Number) r[3]).longValue();
            return SalesAnalyticsDto.SubCategoryStats.builder()
                    .mainCategory((String) r[0])
                    .subCategory((String) r[1])
                    .quantity(((Number) r[2]).longValue())
                    .salesAmount(BigDecimal.valueOf(amount))
                    .sharePct(grand == 0 ? BigDecimal.ZERO :
                            BigDecimal.valueOf(amount * 100.0 / grand)
                                    .setScale(1, RoundingMode.HALF_UP))
                    .build();
        }).collect(Collectors.toList());
    }

    // ── Product Details ──
    private Map<String, List<SalesAnalyticsDto.ProductStats>> buildProductDetails(
            Date from, Date to, Long storeId, String mainCat) {
        List<Object[]> rows = itemRepo.aggregateByProduct(COMPLETED, from, to, storeId, mainCat);

        Map<String, List<SalesAnalyticsDto.ProductStats>> grouped = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String subCat = (String) r[3];
            SalesAnalyticsDto.ProductStats ps = SalesAnalyticsDto.ProductStats.builder()
                    .productCode((String) r[0])
                    .productName((String) r[1])
                    .mainCategory((String) r[2])
                    .subCategory(subCat)
                    .quantity(((Number) r[4]).longValue())
                    .salesAmount(BigDecimal.valueOf(((Number) r[5]).longValue()))
                    .build();
            grouped.computeIfAbsent(subCat, k -> new ArrayList<>()).add(ps);
        }
        return grouped;
    }

    // ── 유틸 ──
    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static BigDecimal trendPct(long current, long previous) {
        if (previous == 0) return current == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        return BigDecimal.valueOf((current - previous) * 100.0 / previous)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
