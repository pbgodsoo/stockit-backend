package org.example.stockitbe.hq.analytics.turnoveranalytics;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverAnalyticsDto;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverPeriod;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverScope;
import org.example.stockitbe.hq.inventory.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurnoverAnalyticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 카테고리 prefix (10자) → 한글 L1 매핑. category_code 예: "CAT-L2-TOP-SS"
    private static final Map<String, String> CATEGORY_PREFIX_TO_KO = new LinkedHashMap<>();
    static {
        CATEGORY_PREFIX_TO_KO.put("CAT-L2-TOP", "상의");
        CATEGORY_PREFIX_TO_KO.put("CAT-L2-PNT", "바지");
        CATEGORY_PREFIX_TO_KO.put("CAT-L2-SKT", "치마");
        CATEGORY_PREFIX_TO_KO.put("CAT-L2-OUT", "아우터");
    }

    // 위치 상태 임계치 — 팀 컨벤션 (정상/부족/품절) 통일
    private static final BigDecimal STATUS_NORMAL = BigDecimal.valueOf(4);   // ≥ 4: 정상
    private static final BigDecimal STATUS_SHORT  = BigDecimal.valueOf(1);   // ≥ 1: 부족
    //  < 1: 품절

    // 신호등 임계치 (SKU 회전율)
    private static final BigDecimal HEALTH_HEALTHY = BigDecimal.valueOf(4);
    private static final BigDecimal HEALTH_CAUTION = BigDecimal.valueOf(2);
    private static final BigDecimal HEALTH_WARNING = BigDecimal.valueOf(1);

    private static final BigDecimal DAYS_ON_HAND_CAP = BigDecimal.valueOf(999.0);
    private static final int SKU_SAMPLE_LIMIT = 50;

    private final InventoryRepository inventoryRepo;

    public TurnoverAnalyticsDto.Res getTurnoverAnalytics(
            TurnoverPeriod period, LocalDate from, LocalDate to,
            TurnoverScope scope, String locationCode) {

        Date fromDt = toSqlDate(from);
        Date toDtExcl = toSqlDate(to.plusDays(1));
        TurnoverScope scopeNorm = (scope == null) ? TurnoverScope.ALL : scope;
        String scopeStr = scopeNorm.name();
        String locCode = (locationCode == null || locationCode.isBlank()) ? null : locationCode;

        List<TurnoverAnalyticsDto.LocationStats> locationStats =
                buildLocationStats(fromDt, toDtExcl, scopeStr, locCode);

        TurnoverAnalyticsDto.InventoryHealth inventoryHealth =
                buildInventoryHealth(fromDt, toDtExcl, scopeStr, locCode);

        return TurnoverAnalyticsDto.Res.builder()
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .period(period == null ? TurnoverPeriod.MONTH : period)
                .scope(scopeNorm)
                .locationCode(locCode)
                .locationStats(locationStats)
                .inventoryHealth(inventoryHealth)
                .build();
    }

    // ── 블록 2: 위치별 회전율 ──
    private List<TurnoverAnalyticsDto.LocationStats> buildLocationStats(
            Date fromDt, Date toDtExcl, String scopeStr, String locCode) {

        List<Object[]> rows = inventoryRepo.aggregateLocationTurnover(fromDt, toDtExcl, scopeStr, locCode);

        return rows.stream().map(r -> {
            String code = (String) r[0];
            String name = (String) r[1];
            String locType = (String) r[2];
            long avgInventory = ((Number) r[3]).longValue();
            long sales = ((Number) r[4]).longValue();

            BigDecimal turnover = calcTurnover(sales, avgInventory);
            BigDecimal daysOnHand = calcDaysOnHand(turnover);
            String status = classifyLocationStatus(turnover);
            String typeKo = "STORE".equals(locType) ? "매장" : "창고";

            return TurnoverAnalyticsDto.LocationStats.builder()
                    .code(code)
                    .name(name)
                    .type(typeKo)
                    .avgInventory(avgInventory)
                    .sales(sales)
                    .turnover(turnover)
                    .daysOnHand(daysOnHand)
                    .status(status)
                    .build();
        }).collect(Collectors.toList());
    }

    // ── 블록 5: 신호등 + 모달 SKU 리스트 ──
    private TurnoverAnalyticsDto.InventoryHealth buildInventoryHealth(
            Date fromDt, Date toDtExcl, String scopeStr, String locCode) {

        List<Object[]> rows = inventoryRepo.skuTurnoverList(fromDt, toDtExcl, scopeStr, locCode);

        // SKU+위치 단위 회전율 산출 + DTO 변환
        List<EnrichedSku> enriched = new ArrayList<>();
        for (Object[] r : rows) {
            String skuCode = (String) r[0];
            String productName = (String) r[1];
            String categoryCode = (String) r[2];
            String locationName = (String) r[4];
            long units = ((Number) r[6]).longValue();
            long unitPrice = ((Number) r[7]).longValue();
            long sales = ((Number) r[8]).longValue();

            BigDecimal turnover = calcTurnover(sales, units);
            BigDecimal daysOnHand = calcDaysOnHand(turnover);

            String categoryKo = mapCategoryKo(categoryCode);
            HealthLevel level = classifyHealth(turnover);
            long itemValue = units * unitPrice;
            BigDecimal valueM = BigDecimal.valueOf(itemValue)
                    .divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.HALF_UP);

            TurnoverAnalyticsDto.SkuItem dto = TurnoverAnalyticsDto.SkuItem.builder()
                    .skuCode(skuCode)
                    .productName(productName)
                    .category(categoryKo)
                    .location(locationName)
                    .turnover(turnover)
                    .daysOnHand(daysOnHand.longValue())
                    .units(units)
                    .value(valueM)
                    .build();

            enriched.add(new EnrichedSku(dto, level, turnover, itemValue));
        }

        // 4단계 분류
        Map<HealthLevel, List<EnrichedSku>> grouped = enriched.stream()
                .collect(Collectors.groupingBy(e -> e.level));

        List<EnrichedSku> healthyList = grouped.getOrDefault(HealthLevel.HEALTHY, Collections.emptyList());
        List<EnrichedSku> cautionList = grouped.getOrDefault(HealthLevel.CAUTION, Collections.emptyList());
        List<EnrichedSku> warningList = grouped.getOrDefault(HealthLevel.WARNING, Collections.emptyList());
        List<EnrichedSku> dangerList  = grouped.getOrDefault(HealthLevel.DANGER, Collections.emptyList());

        // 정렬 + Top N 샘플
        List<TurnoverAnalyticsDto.SkuItem> healthySkus = sample(healthyList, true);
        List<TurnoverAnalyticsDto.SkuItem> cautionSkus = sample(cautionList, true);
        List<TurnoverAnalyticsDto.SkuItem> warningSkus = sample(warningList, false);
        List<TurnoverAnalyticsDto.SkuItem> dangerSkus  = sample(dangerList, false);

        // 가치 합 (M원, 소수 1자리)
        long totalValueRaw = enriched.stream().mapToLong(e -> e.itemValue).sum();
        long lockedValueRaw = dangerList.stream().mapToLong(e -> e.itemValue).sum();
        BigDecimal totalValueM = toMillionWon(totalValueRaw);
        BigDecimal lockedValueM = toMillionWon(lockedValueRaw);

        return TurnoverAnalyticsDto.InventoryHealth.builder()
                .totalSku(enriched.size())
                .healthy(healthyList.size())
                .caution(cautionList.size())
                .warning(warningList.size())
                .danger(dangerList.size())
                .totalValue(totalValueM)
                .lockedValue(lockedValueM)
                .healthySkus(healthySkus)
                .cautionSkus(cautionSkus)
                .warningSkus(warningSkus)
                .dangerSkus(dangerSkus)
                .build();
    }

    // ── 유틸 ──
    /** turnover = sales / avgInventory (소수 1자리). avgInventory=0 이면 0. */
    private static BigDecimal calcTurnover(long sales, long avgInventory) {
        if (avgInventory <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf((double) sales / avgInventory).setScale(1, RoundingMode.HALF_UP);
    }

    /** daysOnHand = 365 / turnover (소수 1자리). turnover=0 이면 999.0 cap. */
    private static BigDecimal calcDaysOnHand(BigDecimal turnover) {
        if (turnover.compareTo(BigDecimal.ZERO) <= 0) return DAYS_ON_HAND_CAP;
        return BigDecimal.valueOf(365.0).divide(turnover, 1, RoundingMode.HALF_UP);
    }

    /** 위치 상태 — 팀 컨벤션 통일: 정상(≥4) / 부족(≥1) / 품절(<1). */
    private static String classifyLocationStatus(BigDecimal turnover) {
        if (turnover.compareTo(STATUS_NORMAL) >= 0) return "정상";
        if (turnover.compareTo(STATUS_SHORT) >= 0) return "부족";
        return "품절";
    }

    /** SKU 신호등: HEALTHY(>=4) / CAUTION(>=2) / WARNING(>=1) / DANGER(<1). */
    private static HealthLevel classifyHealth(BigDecimal turnover) {
        if (turnover.compareTo(HEALTH_HEALTHY) >= 0) return HealthLevel.HEALTHY;
        if (turnover.compareTo(HEALTH_CAUTION) >= 0) return HealthLevel.CAUTION;
        if (turnover.compareTo(HEALTH_WARNING) >= 0) return HealthLevel.WARNING;
        return HealthLevel.DANGER;
    }

    /** category_code 의 prefix(10자) → 한글. 매칭 안 되면 "기타". */
    private static String mapCategoryKo(String categoryCode) {
        if (categoryCode == null || categoryCode.length() < 10) return "기타";
        String prefix = categoryCode.substring(0, 10);
        return CATEGORY_PREFIX_TO_KO.getOrDefault(prefix, "기타");
    }

    /**
     * 단계별 SKU 샘플 추출.
     * @param descendByTurnover true 면 회전율 내림차순(좋은 것부터),
     *                          false 면 오름차순(악성에 가까운 것부터).
     */
    private static List<TurnoverAnalyticsDto.SkuItem> sample(List<EnrichedSku> list, boolean descendByTurnover) {
        Comparator<EnrichedSku> cmp = Comparator.comparing(e -> e.turnover);
        if (descendByTurnover) cmp = cmp.reversed();
        return list.stream()
                .sorted(cmp)
                .limit(SKU_SAMPLE_LIMIT)
                .map(e -> e.dto)
                .collect(Collectors.toList());
    }

    /** 원 단위 → M원 (소수 1자리). */
    private static BigDecimal toMillionWon(long won) {
        return BigDecimal.valueOf(won)
                .divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.HALF_UP);
    }

    private static Date toSqlDate(LocalDate d) {
        return new Date(d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    // 분류 enum
    private enum HealthLevel { HEALTHY, CAUTION, WARNING, DANGER }

    // 정렬/그룹화용 내부 record
    private record EnrichedSku(
            TurnoverAnalyticsDto.SkuItem dto,
            HealthLevel level,
            BigDecimal turnover,
            long itemValue
    ) {}
}
