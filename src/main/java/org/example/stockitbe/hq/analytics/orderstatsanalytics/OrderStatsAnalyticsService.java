package org.example.stockitbe.hq.analytics.orderstatsanalytics;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.analytics.orderstatsanalytics.model.OrderStatsAnalyticsDto;
import org.example.stockitbe.hq.analytics.orderstatsanalytics.model.OrderStatsPeriod;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderItemRepository;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderStatsAnalyticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // L2 prefix(10자) → [category, productType] — 기존 그대로
    private static final Map<String, String[]> L2_MAP = new LinkedHashMap<>();
    static {
        L2_MAP.put("PRD-TOP-SS", new String[]{"상의", "반팔"});
        L2_MAP.put("PRD-TOP-LS", new String[]{"상의", "긴팔"});
        L2_MAP.put("PRD-TOP-SH", new String[]{"상의", "셔츠"});
        L2_MAP.put("PRD-TOP-KN", new String[]{"상의", "니트"});
        L2_MAP.put("PRD-TOP-HD", new String[]{"상의", "후드티"});
        L2_MAP.put("PRD-PNT-DN", new String[]{"바지", "청바지"});
        L2_MAP.put("PRD-PNT-LG", new String[]{"바지", "긴바지"});
        L2_MAP.put("PRD-PNT-ST", new String[]{"바지", "반바지"});
        L2_MAP.put("PRD-PNT-TR", new String[]{"바지", "츄리닝"});
        L2_MAP.put("PRD-SKT-MN", new String[]{"치마", "미니스커트"});
        L2_MAP.put("PRD-SKT-LG", new String[]{"치마", "롱스커트"});
        L2_MAP.put("PRD-OUT-PD", new String[]{"아우터", "패딩"});
        L2_MAP.put("PRD-OUT-HZ", new String[]{"아우터", "후드집업"});
        L2_MAP.put("PRD-OUT-JK", new String[]{"아우터", "자켓"});
        L2_MAP.put("PRD-OUT-CD", new String[]{"아우터", "가디건"});
    }

    // ★ 추가: 카테고리 한글명 → product_code prefix (LIKE 'PRD-XXX%' 매칭용)
    private static final Map<String, String> CATEGORY_TO_PREFIX = Map.of(
            "상의",   "PRD-TOP",
            "바지",   "PRD-PNT",
            "치마",   "PRD-SKT",
            "아우터", "PRD-OUT"
    );

    private final PurchaseOrderRepository orderRepo;
    private final PurchaseOrderItemRepository itemRepo;

    public OrderStatsAnalyticsDto.Res getOrderStats(
            OrderStatsPeriod period, LocalDate from, LocalDate to, String category) {

        Date fromDt = toSqlDate(from);
        Date toDtExcl = toSqlDate(to.plusDays(1));

        // ★ 추가: 카테고리 → SQL prefix (null 이면 전체)
        String codePrefix = (category == null || category.isBlank())
                ? null : CATEGORY_TO_PREFIX.get(category);

        // 1) Item history
        List<Object[]> hist = itemRepo.findItemOrderHistory(fromDt, toDtExcl);

        // 2) ItemCycle
        List<OrderStatsAnalyticsDto.ItemCycle> itemCycles = buildItemCycles(hist, category);

        // 3) ProductCycle
        List<OrderStatsAnalyticsDto.ProductCycle> productCycles = buildProductCycles(hist, category);

        // 4) Warehouse — ★ codePrefix 전달
        List<OrderStatsAnalyticsDto.WarehouseStats> warehouseOrders =
                buildWarehouseStats(fromDt, toDtExcl, codePrefix);

        // 5) Monthly trend — ★ codePrefix 전달
        List<OrderStatsAnalyticsDto.MonthlyTrendPoint> monthlyTrend =
                buildMonthlyTrend(fromDt, toDtExcl, codePrefix);

        // 6) KPI — ★ hist 인자 제거, itemCycles 만 전달
        OrderStatsAnalyticsDto.KpiSummary kpi = buildKpi(itemCycles);

        return OrderStatsAnalyticsDto.Res.builder()
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .period(period)
                .kpi(kpi)
                .warehouseOrders(warehouseOrders)
                .orderCycleData(itemCycles)
                .productOrderData(productCycles)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    // ── ItemCycle: L2 prefix 단위 ── (기존 그대로)
    private List<OrderStatsAnalyticsDto.ItemCycle> buildItemCycles(
            List<Object[]> hist, String categoryFilter) {
        // ... 기존 로직 그대로 ...
        Map<String, List<DateQty>> byL2 = new LinkedHashMap<>();
        for (Object[] r : hist) {
            String l2 = (String) r[0];
            LocalDate od = ((java.sql.Date) r[3]).toLocalDate();
            long qty = ((Number) r[4]).longValue();
            byL2.computeIfAbsent(l2, k -> new ArrayList<>()).add(new DateQty(od, qty));
        }

        List<OrderStatsAnalyticsDto.ItemCycle> result = new ArrayList<>();
        for (Map.Entry<String, List<DateQty>> e : byL2.entrySet()) {
            String[] meta = L2_MAP.getOrDefault(e.getKey(), new String[]{"기타", e.getKey()});
            if (categoryFilter != null && !categoryFilter.isBlank()
                    && !meta[0].equals(categoryFilter)) continue;

            List<DateQty> rows = e.getValue();
            List<LocalDate> dates = rows.stream().map(d -> d.date)
                    .distinct().sorted().collect(Collectors.toList());
            long totalQty = rows.stream().mapToLong(d -> d.qty).sum();
            long totalOrders = rows.size();
            long avgQty = totalOrders == 0 ? 0 : totalQty / totalOrders;
            int avgCycle = avgGapDays(dates);
            String last = dates.isEmpty() ? "" : dates.get(dates.size() - 1).format(DATE_FMT);

            result.add(OrderStatsAnalyticsDto.ItemCycle.builder()
                    .item(meta[1])
                    .category(meta[0])
                    .avgCycle(avgCycle)
                    .avgQty(avgQty)
                    .totalOrders(totalOrders)
                    .lastOrderedAt(last)
                    .build());
        }
        result.sort(Comparator.comparingInt(OrderStatsAnalyticsDto.ItemCycle::getAvgCycle));
        return result;
    }

    // ── ProductCycle ── (기존 그대로 — 변경 없음)
    private List<OrderStatsAnalyticsDto.ProductCycle> buildProductCycles(
            List<Object[]> hist, String categoryFilter) {
        // ... 기존 로직 그대로 ...
        Map<String, List<DateQty>> byProduct = new LinkedHashMap<>();
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> l2Map = new HashMap<>();
        for (Object[] r : hist) {
            String pc = (String) r[1];
            byProduct.computeIfAbsent(pc, k -> new ArrayList<>())
                    .add(new DateQty(((java.sql.Date) r[3]).toLocalDate(),
                            ((Number) r[4]).longValue()));
            nameMap.put(pc, (String) r[2]);
            l2Map.put(pc, (String) r[0]);
        }

        List<OrderStatsAnalyticsDto.ProductCycle> result = new ArrayList<>();
        for (Map.Entry<String, List<DateQty>> e : byProduct.entrySet()) {
            String pc = e.getKey();
            String l2 = l2Map.get(pc);
            String[] meta = L2_MAP.getOrDefault(l2, new String[]{"기타", l2});
            if (categoryFilter != null && !categoryFilter.isBlank()
                    && !meta[0].equals(categoryFilter)) continue;

            List<DateQty> rows = e.getValue();
            List<LocalDate> dates = rows.stream().map(d -> d.date)
                    .distinct().sorted().collect(Collectors.toList());
            long totalQty = rows.stream().mapToLong(d -> d.qty).sum();
            long totalOrders = rows.size();
            long avgQty = totalOrders == 0 ? 0 : totalQty / totalOrders;
            int avgCycle = avgGapDays(dates);
            String last = dates.isEmpty() ? "" : dates.get(dates.size() - 1).format(DATE_FMT);

            result.add(OrderStatsAnalyticsDto.ProductCycle.builder()
                    .item(nameMap.get(pc))
                    .productCode(pc)
                    .productType(meta[1])
                    .category(meta[0])
                    .avgCycle(avgCycle)
                    .avgQty(avgQty)
                    .totalOrders(totalOrders)
                    .lastOrderedAt(last)
                    .build());
        }
        result.sort(Comparator.comparingInt(OrderStatsAnalyticsDto.ProductCycle::getAvgCycle));
        return result;
    }

    // ── Warehouse stats — ★ codePrefix 인자 추가 ──
    private List<OrderStatsAnalyticsDto.WarehouseStats> buildWarehouseStats(
            Date fromDt, Date toDtExcl, String codePrefix) {
        List<Object[]> rows = orderRepo.aggregateByWarehouse(fromDt, toDtExcl, codePrefix);
        long grand = rows.stream().mapToLong(r -> ((Number) r[4]).longValue()).sum();
        return rows.stream().map(r -> {
            long val = ((Number) r[4]).longValue();
            BigDecimal share = grand == 0 ? BigDecimal.ZERO :
                    BigDecimal.valueOf(val * 100.0 / grand).setScale(1, RoundingMode.HALF_UP);
            return OrderStatsAnalyticsDto.WarehouseStats.builder()
                    .warehouseCode((String) r[0])
                    .warehouseName((String) r[1])
                    .orders(((Number) r[2]).longValue())
                    .items(((Number) r[3]).longValue())
                    .totalValue(BigDecimal.valueOf(val))
                    .sharePct(share)
                    .build();
        }).collect(Collectors.toList());
    }

    // ── Monthly trend — ★ codePrefix 인자 추가 ──
    private List<OrderStatsAnalyticsDto.MonthlyTrendPoint> buildMonthlyTrend(
            Date fromDt, Date toDtExcl, String codePrefix) {
        List<Object[]> rows = orderRepo.monthlyTrend(fromDt, toDtExcl, codePrefix);
        return rows.stream().map(r -> OrderStatsAnalyticsDto.MonthlyTrendPoint.builder()
                .month((String) r[0])
                .orders(((Number) r[1]).longValue())
                .items(((Number) r[2]).longValue())
                .build()).collect(Collectors.toList());
    }

    // ── KPI — ★ hist 인자 제거, itemCycles 의 totalOrders 합으로 ──
    private OrderStatsAnalyticsDto.KpiSummary buildKpi(
            List<OrderStatsAnalyticsDto.ItemCycle> itemCycles) {
        int managedCount = itemCycles.size();
        int avgCycle = itemCycles.isEmpty() ? 0 : (int) Math.round(
                itemCycles.stream().mapToInt(OrderStatsAnalyticsDto.ItemCycle::getAvgCycle)
                        .filter(v -> v > 0).average().orElse(0));
        OrderStatsAnalyticsDto.ItemCycle shortest = itemCycles.stream()
                .filter(c -> c.getAvgCycle() > 0)
                .min(Comparator.comparingInt(OrderStatsAnalyticsDto.ItemCycle::getAvgCycle))
                .orElse(null);
        OrderStatsAnalyticsDto.ItemCycle longest = itemCycles.stream()
                .filter(c -> c.getAvgCycle() > 0)
                .max(Comparator.comparingInt(OrderStatsAnalyticsDto.ItemCycle::getAvgCycle))
                .orElse(null);
        long totalOrders = itemCycles.stream()
                .mapToLong(OrderStatsAnalyticsDto.ItemCycle::getTotalOrders)
                .sum();
        return OrderStatsAnalyticsDto.KpiSummary.builder()
                .managedItemCount(managedCount)
                .avgCycleDays(avgCycle)
                .shortestCycleDays(shortest == null ? 0 : shortest.getAvgCycle())
                .shortestCycleItem(shortest == null ? "-" : shortest.getItem())
                .longestCycleDays(longest == null ? 0 : longest.getAvgCycle())
                .longestCycleItem(longest == null ? "-" : longest.getItem())
                .totalOrders(totalOrders)
                .build();
    }

    // ── 유틸 (변경 없음) ──
    private static int avgGapDays(List<LocalDate> sortedDates) {
        if (sortedDates.size() < 2) return 0;
        long total = 0;
        for (int i = 1; i < sortedDates.size(); i++) {
            total += ChronoUnit.DAYS.between(sortedDates.get(i - 1), sortedDates.get(i));
        }
        return (int) Math.round((double) total / (sortedDates.size() - 1));
    }

    private static Date toSqlDate(LocalDate d) {
        return new Date(d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    private record DateQty(LocalDate date, long qty) {}
}
