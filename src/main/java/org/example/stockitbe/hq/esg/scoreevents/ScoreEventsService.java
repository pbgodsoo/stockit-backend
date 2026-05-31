package org.example.stockitbe.hq.esg.scoreevents;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerTransactionRepository;
import org.example.stockitbe.hq.esg.scoreevents.model.ScoreEventsDto;
import org.example.stockitbe.hq.product.MaterialRepository;
import org.example.stockitbe.hq.product.model.Material;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreEventsService {

    private final CircularBuyerTransactionRepository txRepo;
    // Phase 2: carbon_factor 룩업용. 응답마다 1회 조회해서 N+1 회피.
    private final MaterialRepository materialRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // ─────────── 점수 산식 상수 (Phase 2 — FE esgScore.js 의 SCORE_RULES 와 동기화된 SSOT) ───────────
    /** 점수 부여 최소 무게(kg) — 어뷰징 방지: 작은 거래로 정액 점수 펌핑 차단 */
    private static final int MIN_WEIGHT_KG = 10;
    /** 판매 실행 기본 점수 — 거래 1건당 */
    private static final int SALE_BASE = 100;
    /** 신규 거래처 보너스 — 해당 buyer 의 최초 거래 1회 한정 */
    private static final int NEW_BUYER_BONUS = 150;
    /** 지역 파트너 보너스 — partner_type=local_small / social_enterprise */
    private static final int LOCAL_PARTNER_BONUS = 150;
    // 탄소 점수 보정계수(CARBON_SCALE) 제거 — material.carbon_factor 표준화 재조정에 따른 산식 단순화.
    //   변경 전: carbon = weight × factor × 0.1
    //   변경 후: carbon = weight × factor

    // ─────────── 카테고리 필터 상수 (FE filterCategory 와 동일 값) ───────────
    private static final String CAT_ALL                = "ALL";
    private static final String CAT_SALE_EXECUTION     = "saleExecution";
    private static final String CAT_CARBON             = "carbon";
    private static final String CAT_NEW_BUYER          = "newBuyer";
    private static final String CAT_LOCAL_PARTNER      = "localPartner";
    private static final String CAT_DONATION_EXECUTION = "donationExecution";
    /** 기부 실행 기본 점수 */
    private static final int DONATION_BASE = 100;

    // ─────────── BE material.material_group 어휘 → FE 어휘 정규화 ───────────
    //  - BE: 'NATURAL' (ProductMasterService.MATERIAL_GROUP_NATURAL 상수와 일치)
    //  - FE: 'NATURAL_SINGLE' (esgScore.js / esgStore 기준)
    //  - 응답 직전에만 변환 — DB 어휘는 유지
    private static final Map<String, String> GROUP_NORMALIZATION = Map.of(
            "NATURAL", "NATURAL_SINGLE"
    );
    private static String normalizeGroup(String raw) {
        if (raw == null) return null;
        return GROUP_NORMALIZATION.getOrDefault(raw, raw);
    }

    /**
     * 친환경 나무 키우기 점수 — 풀세트 응답 (Phase 3 / A''-1).
     *  파라미터:
     *   - year     : 연도 (null 시 현재 연도). dateFrom/dateTo 가 있으면 그 값이 우선.
     *   - page/size: 0-based 페이지. size 는 안전상 1~200 사이로 클램프.
     *   - dateFrom/dateTo: "yyyy-MM-dd" 부분 범위. 둘 다 있으면 [dateFrom 00:00, dateTo+1day 00:00) 사용.
     *   - category : ALL / saleExecution / carbon / newBuyer / localPartner.
     *                ALL 이외는 "해당 카테고리에 점수가 1 이상인 이벤트" 만 통과.
     *
     *  처리 순서:
     *   ① 기간 범위 결정 → ② carbon factor 맵 1회 로딩 → ③ 거래 row 조회
     *   → ④ 각 row 당 점수 4종 계산 (EventDto 생성)
     *   → ⑤ 카테고리 필터 적용 → ⑥ 통계(summary/monthly/category) 집계
     *   → ⑦ 페이지 슬라이스 후 응답 빌드
     *
     *  주의: 통계는 "필터 적용 후 전체" 기준이므로 페이지를 넘겨도 KPI/차트는 그대로 유지됨.
     */
    public ScoreEventsDto.Response getEvents(
            Integer year, Integer page, Integer size,
            String dateFrom, String dateTo, String category) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        // 페이지 파라미터 정규화 — 음수/0 방지 + 과도한 size 방어 (서버 부하)
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? 20 : Math.min(size, 200);
        String safeCategory = (category == null || category.isBlank()) ? CAT_ALL : category;

        // ① 기간 범위 — dateFrom/dateTo 가 있으면 그 값이 우선 (FE 의 dateFilter UI 대응)
        //    없으면 연도 전체 [yyyy-01-01, (yyyy+1)-01-01)
        Date from;
        Date to;
        if (dateFrom != null && !dateFrom.isBlank() && dateTo != null && !dateTo.isBlank()) {
            LocalDate ldFrom = LocalDate.parse(dateFrom);
            LocalDate ldTo   = LocalDate.parse(dateTo);
            from = Date.from(ldFrom.atStartOfDay(ZoneId.systemDefault()).toInstant());
            // 종료일 포함을 위해 +1 day exclusive
            to   = Date.from(ldTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        } else {
            from = Date.from(LocalDate.of(targetYear, 1, 1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant());
            to   = Date.from(LocalDate.of(targetYear + 1, 1, 1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        // ② material 마스터 1회 로딩 → factor / group / 한글명 맵
        //   (Phase 3 B: 카본 분포 group 추가, Phase 4 그룹화: nameMap 추가)
        List<Material> materials = materialRepository.findAllByActiveTrueOrderByCodeAsc();
        Map<String, BigDecimal> factorMap = materials.stream()
                .collect(Collectors.toMap(Material::getCode, Material::getCarbonFactor));
        // code → normalized group ("NATURAL_SINGLE" / "SYNTHETIC" / "BLEND")
        Map<String, String> groupMap = materials.stream()
                .collect(Collectors.toMap(Material::getCode, m -> normalizeGroup(m.getMaterialGroup())));
        // code → 한글명 (활동 이력 카드의 소재명 표시용)
        Map<String, String> nameMap = materials.stream()
                .collect(Collectors.toMap(Material::getCode, Material::getNameKo));

        // ③ row: r[0] id, r[1] transacted_at, r[2] company_name, r[3] material_code,
        //         r[4] weight_kg, r[5] first_tx_at,
        //         r[6] partner_type, r[7] sale_type
        List<Object[]> rows = txRepo.findEventsForYear(from, to);

        // ④-A SKU별 EventDto — CarbonReduction(소재별/그룹별/월별 분포) 정밀 계산용 (그룹화 안 함)
        List<ScoreEventsDto.EventDto> skuEvents = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            skuEvents.add(buildEventDto(r, factorMap));
        }

        // ④-B 판매 단위(거래처+시점) 그룹화된 EventDto — 활동 이력 / 점수 / KPI 표시용
        //   - 판매 1건 (sale_header) = 같은 buyer + 같은 transacted_at(밀리초) → 1 그룹
        //   - 소재는 그룹 내 unique 한글명 콤마 구분 ("폴리에스터, 나일론")
        //   - 점수는 총 무게 기준 재계산 → 10kg 미만 SKU 들도 합쳐서 점수 부여
        List<ScoreEventsDto.EventDto> groupedEvents = buildGroupedEvents(rows, factorMap, nameMap);

        // ⑤ 카테고리 필터 — 그룹 단위 점수 > 0 인 이벤트만 통과
        List<ScoreEventsDto.EventDto> filtered = applyCategoryFilter(groupedEvents, safeCategory);

        // ⑥ 통계 집계 (필터 적용 후 전체 기준 — KPI / 도넛 / 월별 막대 차트 데이터)
        ScoreEventsDto.Summary summary             = buildSummary(filtered);
        ScoreEventsDto.CategoryBreakdown catBreak  = buildCategoryBreakdown(filtered);
        List<ScoreEventsDto.MonthlyBucket> monthly = buildMonthlyBreakdown(filtered);

        // ⑥-B (Phase 3 B): ESG 대시보드용 카본 절감량 분포 — 필터/페이지 무관, 연도 전체 기준.
        //   ⚠ 분포 차트의 정밀도 유지를 위해 SKU별 skuEvents 사용 (그룹화 안 함)
        ScoreEventsDto.CarbonReduction carbonReduction = buildCarbonReduction(skuEvents, factorMap, groupMap);

        // ⑦ 페이지 슬라이스 — Repository 정렬(id DESC) 그대로 사용
        long totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIdx = Math.min(safePage * safeSize, filtered.size());
        int toIdx   = Math.min(fromIdx + safeSize, filtered.size());
        List<ScoreEventsDto.EventDto> pageSlice = (fromIdx >= toIdx)
                ? Collections.emptyList()
                : new ArrayList<>(filtered.subList(fromIdx, toIdx));

        return ScoreEventsDto.Response.builder()
                .year(targetYear)
                .events(pageSlice)
                .summary(summary)
                .monthlyBreakdown(monthly)
                .categoryBreakdown(catBreak)
                .carbonReduction(carbonReduction)
                .page(safePage)
                .size(safeSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    // ─────────────────────────────── 내부 헬퍼 ───────────────────────────────

    /**
     * SKU별 row 를 (buyer, transactedAt) 기준으로 그룹화 후 활동 이벤트 1건씩 생성.
     *  - 판매 1건 (sale_header) = 같은 buyer + 같은 millisecond → 1 그룹 가정
     *    (CircularSaleService 가 같은 sold_at(=now) 으로 transaction INSERT 하므로 안전)
     *  - 그룹별 점수는 총 무게 기준 재계산 (10kg 미만 SKU 들도 합쳐서 점수 부여)
     *  - 소재는 그룹 내 unique 한글명을 콤마 구분 (예: "폴리에스터, 나일론")
     *  - newBuyer / localPartner 는 그룹 내 anyMatch 로 판정
     */
    private List<ScoreEventsDto.EventDto> buildGroupedEvents(
            List<Object[]> rows,
            Map<String, BigDecimal> factorMap,
            Map<String, String> nameMap) {

        // LinkedHashMap 으로 원본 row 정렬(id DESC) 보존 → 페이지/표시 순서 일관
        LinkedHashMap<String, List<Object[]>> grouped = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String buyer = (String) r[2];
            Timestamp txAt = (Timestamp) r[1];
            // 그룹키: buyer + transactedAt (밀리초 정밀)
            String key = buyer + "|" + txAt.getTime();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<ScoreEventsDto.EventDto> result = new ArrayList<>(grouped.size());
        for (List<Object[]> groupRows : grouped.values()) {
            result.add(buildGroupEventDto(groupRows, factorMap, nameMap));
        }
        return result;
    }

    /** 그룹화된 row 묶음 → EventDto 1건 (총 무게 기준 점수 재계산, SALE/DONATION 분기). */
    private ScoreEventsDto.EventDto buildGroupEventDto(
            List<Object[]> groupRows,
            Map<String, BigDecimal> factorMap,
            Map<String, String> nameMap) {

        Object[] first = groupRows.get(0);
        Long groupId = ((Number) first[0]).longValue();      // 대표 id (그룹 첫 SKU)
        Timestamp txAt = (Timestamp) first[1];
        String buyer = (String) first[2];
        String saleType = (String) first[7];                 // 그룹 첫 row 의 sale_type 사용

        int totalWeight = 0;
        BigDecimal totalCarbonAmount = BigDecimal.ZERO;       // sum(weight × effectiveFactor)
        boolean isNewBuyer = false;
        boolean isLocalPartner = false;
        LinkedHashSet<String> materialSet = new LinkedHashSet<>();   // 콤마 구분 한글명 모음

        for (Object[] r : groupRows) {
            int w = ((Number) r[4]).intValue();
            totalWeight += w;

            String materialCode = (String) r[3];

            // carbon 누적 (buildEventDto 와 동일 산식 재사용)
            BigDecimal factor = resolveFactor(materialCode, factorMap);
            totalCarbonAmount = totalCarbonAmount.add(BigDecimal.valueOf(w).multiply(factor));

            // newBuyer 판정 — 그룹 내 어느 SKU 라도 첫 거래 시점이면 true
            Timestamp firstAt = (Timestamp) r[5];
            if (txAt.equals(firstAt)) isNewBuyer = true;

            // localPartner — partner_type 기준 anyMatch
            String partnerType = (String) r[6];
            if ("local_small".equals(partnerType) || "social_enterprise".equals(partnerType)) {
                isLocalPartner = true;
            }

            // 소재 한글명 집계 — materialCode 직접 사용 (혼방이면 "혼방"으로 표시)
            materialSet.add(nameMap.getOrDefault(materialCode, materialCode));
        }

        boolean scoreValid = totalWeight >= MIN_WEIGHT_KG;
        int carbon = scoreValid ? totalCarbonAmount.intValue() : 0;

        int saleExecution, donationExecution, newBuyerScore, localPartnerScore;
        if ("DONATION".equals(saleType)) {
            saleExecution     = 0;
            donationExecution = scoreValid ? DONATION_BASE : 0;
            newBuyerScore     = 0;
            localPartnerScore = 0;
        } else {
            saleExecution     = scoreValid ? SALE_BASE : 0;
            donationExecution = 0;
            newBuyerScore     = (scoreValid && isNewBuyer)     ? NEW_BUYER_BONUS     : 0;
            localPartnerScore = (scoreValid && isLocalPartner) ? LOCAL_PARTNER_BONUS : 0;
        }
        int total = saleExecution + carbon + newBuyerScore + localPartnerScore + donationExecution;

        String materialNames = String.join(", ", materialSet);
        String date = txAt.toLocalDateTime().toLocalDate().format(DATE_FMT);

        return ScoreEventsDto.EventDto.builder()
                .id(groupId)
                .date(date)
                .type("sale")
                .buyer(buyer)
                .material(materialNames)
                .weightKg(totalWeight)
                .isNewBuyer(isNewBuyer)
                .isLocalPartner(isLocalPartner)
                .saleType(saleType)
                .saleExecution(saleExecution)
                .carbon(carbon)
                .newBuyer(newBuyerScore)
                .localPartner(localPartnerScore)
                .donationExecution(donationExecution)
                .total(total)
                .scoreValid(scoreValid)
                .build();
    }

    /** native row 1개를 EventDto 로 변환 + 점수 계산 (SALE/DONATION 분기). */
    private ScoreEventsDto.EventDto buildEventDto(Object[] r, Map<String, BigDecimal> factorMap) {
        Long id                  = ((Number) r[0]).longValue();
        LocalDateTime txAt       = ((Timestamp) r[1]).toLocalDateTime();
        String buyer             = (String) r[2];
        String material          = (String) r[3];
        Integer weightKg         = ((Number) r[4]).intValue();
        LocalDateTime firstAt    = ((Timestamp) r[5]).toLocalDateTime();
        String partnerType       = (String) r[6];
        String saleType          = (String) r[7];   // "SALE" | "DONATION"

        boolean isNewBuyer = txAt.equals(firstAt);
        // local_small (지역 소상공인) / social_enterprise (사회적기업) 모두 지역 파트너로 인정.
        // general 은 일반 거래처라 보너스 미적용.
        boolean isLocalPartner = "local_small".equals(partnerType)
                              || "social_enterprise".equals(partnerType);

        // effective factor — 모든 소재 자기 factor 사용
        BigDecimal factor = resolveFactor(material, factorMap);

        boolean scoreValid = weightKg != null && weightKg >= MIN_WEIGHT_KG;
        int carbon = scoreValid
                ? BigDecimal.valueOf(weightKg).multiply(factor).intValue()
                : 0;

        int saleExecution, donationExecution, newBuyerScore, localPartnerScore;
        if ("DONATION".equals(saleType)) {
            saleExecution     = 0;
            donationExecution = scoreValid ? DONATION_BASE : 0;
            newBuyerScore     = 0;
            localPartnerScore = 0;
        } else {
            saleExecution     = scoreValid ? SALE_BASE : 0;
            donationExecution = 0;
            newBuyerScore     = (scoreValid && isNewBuyer)     ? NEW_BUYER_BONUS     : 0;
            localPartnerScore = (scoreValid && isLocalPartner) ? LOCAL_PARTNER_BONUS : 0;
        }
        int total = saleExecution + carbon + newBuyerScore + localPartnerScore + donationExecution;

        return ScoreEventsDto.EventDto.builder()
                .id(id)
                .date(txAt.toLocalDate().format(DATE_FMT))
                .type("sale")
                .buyer(buyer)
                .material(material)
                .weightKg(weightKg)
                .isNewBuyer(isNewBuyer)
                .isLocalPartner(isLocalPartner)
                .saleType(saleType)
                .saleExecution(saleExecution)
                .carbon(carbon)
                .newBuyer(newBuyerScore)
                .localPartner(localPartnerScore)
                .donationExecution(donationExecution)
                .total(total)
                .scoreValid(scoreValid)
                .build();
    }

    /** 카테고리 필터 — ALL 은 그대로, 그 외는 해당 카테고리 점수가 1 이상인 이벤트만 통과. */
    private List<ScoreEventsDto.EventDto> applyCategoryFilter(
            List<ScoreEventsDto.EventDto> events, String category) {
        if (CAT_ALL.equals(category)) {
            return events;
        }
        return events.stream().filter(e -> switch (category) {
            case CAT_SALE_EXECUTION     -> e.getSaleExecution()     > 0;
            case CAT_CARBON             -> e.getCarbon()            > 0;
            case CAT_NEW_BUYER          -> e.getNewBuyer()          > 0;
            case CAT_LOCAL_PARTNER      -> e.getLocalPartner()      > 0;
            case CAT_DONATION_EXECUTION -> e.getDonationExecution() > 0;
            // 알 수 없는 카테고리는 ALL 과 동일하게 통과 (FE 가 새 값을 보내도 500 안 나도록)
            default -> true;
        }).collect(Collectors.toList());
    }

    /** Summary 집계 — 상단 KPI 카드. */
    private ScoreEventsDto.Summary buildSummary(List<ScoreEventsDto.EventDto> events) {
        long saleSum = 0, carbonSum = 0, newBuyerSum = 0, localPartnerSum = 0, donationSum = 0;
        long totalKg = 0;
        long validCnt = 0;
        for (ScoreEventsDto.EventDto e : events) {
            saleSum         += e.getSaleExecution();
            carbonSum       += e.getCarbon();
            newBuyerSum     += e.getNewBuyer();
            localPartnerSum += e.getLocalPartner();
            donationSum     += e.getDonationExecution();
            if (e.getWeightKg() != null) totalKg += e.getWeightKg();
            if (e.isScoreValid()) validCnt++;
        }
        long totalScore = saleSum + carbonSum + newBuyerSum + localPartnerSum + donationSum;
        long cnt = events.size();
        long avg = cnt > 0 ? Math.round((double) totalScore / cnt) : 0L;

        return ScoreEventsDto.Summary.builder()
                .totalScore(totalScore)
                .saleExecutionSum(saleSum)
                .carbonSum(carbonSum)
                .newBuyerSum(newBuyerSum)
                .localPartnerSum(localPartnerSum)
                .donationExecutionSum(donationSum)
                .totalEventCount(cnt)
                .validEventCount(validCnt)
                .totalKg(totalKg)
                .avgScore(avg)
                .build();
    }

    /** 도넛 차트용 카테고리별 합계. */
    private ScoreEventsDto.CategoryBreakdown buildCategoryBreakdown(List<ScoreEventsDto.EventDto> events) {
        long sale = 0, carbon = 0, newBuyer = 0, localPartner = 0, donation = 0;
        for (ScoreEventsDto.EventDto e : events) {
            sale         += e.getSaleExecution();
            carbon       += e.getCarbon();
            newBuyer     += e.getNewBuyer();
            localPartner += e.getLocalPartner();
            donation     += e.getDonationExecution();
        }
        return ScoreEventsDto.CategoryBreakdown.builder()
                .saleExecution(sale)
                .carbon(carbon)
                .newBuyer(newBuyer)
                .localPartner(localPartner)
                .donationExecution(donation)
                .build();
    }

    /**
     * 카본 절감량 분포 계산 (Phase 3 B) — ESG 대시보드 KPI/차트 SSOT.
     *  단위: kg CO₂. 산식: 거래별 (weight × effectiveFactor), 점수 carbon 과 동일 산식 (weight × effectiveFactor).
     *  - byMaterial: code → 누적 kg. 모든 active material code 키를 0 으로 시드해서 누락 방지.
     *  - byGroup   : 정규화된 group (NATURAL_SINGLE/SYNTHETIC/BLEND) → 누적 kg.
     *  - monthly   : 12개 List (1~12월). 거래 없는 달은 0.
     *  - total     : byMaterial 합계 = 전체 절감량.
     *
     *  카테고리 필터/페이지와 무관 → 인자로 받는 events 는 항상 전체(allEvents).
     */
    private ScoreEventsDto.CarbonReduction buildCarbonReduction(
            List<ScoreEventsDto.EventDto> events,
            Map<String, BigDecimal> factorMap,
            Map<String, String> groupMap) {

        // 모든 material 코드 키 0 시드 — FE 막대 그래프가 미존재 키에서 NaN 안 나도록.
        Map<String, Long> byMaterial = new LinkedHashMap<>();
        for (String code : factorMap.keySet()) byMaterial.put(code, 0L);

        // 그룹 키 3종 0 시드 — FE store 의 carbonReductionByGroupKg 기본 shape 와 동일.
        Map<String, Long> byGroup = new LinkedHashMap<>();
        byGroup.put("NATURAL_SINGLE", 0L);
        byGroup.put("SYNTHETIC", 0L);
        byGroup.put("BLEND", 0L);

        long[] monthlyKg = new long[12];
        long total = 0L;

        for (ScoreEventsDto.EventDto e : events) {
            if (e.getWeightKg() == null) continue;
            // effective factor — 모든 소재 자기 factor 사용 (buildEventDto 와 동일 산식)
            BigDecimal factor = resolveFactor(e.getMaterial(), factorMap);
            // weight × factor → 반올림하여 kg 정수. (IPCC Eq.5.2 소각 탄소 계수 — 소수, 반올림 영향 미미)
            long reduction = BigDecimal.valueOf(e.getWeightKg())
                    .multiply(factor)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .longValueExact();

            // byMaterial — 키 누락된 코드는 건너뜀 (active 가 풀린 material 거래 등 예외)
            if (byMaterial.containsKey(e.getMaterial())) {
                byMaterial.merge(e.getMaterial(), reduction, Long::sum);
            }

            // byGroup — material 의 group 으로 분류. 매핑 없으면 BLEND 폴백 (기존 InventoryService 흐름과 동일).
            String group = groupMap.getOrDefault(e.getMaterial(), "BLEND");
            byGroup.merge(group, reduction, Long::sum);

            // monthly — date 의 월 (1~12) → 0-based index
            int monthIdx = Integer.parseInt(e.getDate().substring(5, 7)) - 1;
            if (monthIdx >= 0 && monthIdx < 12) monthlyKg[monthIdx] += reduction;

            total += reduction;
        }

        List<Long> monthlyList = new ArrayList<>(12);
        for (long v : monthlyKg) monthlyList.add(v);

        return ScoreEventsDto.CarbonReduction.builder()
                .byMaterial(byMaterial)
                .byGroup(byGroup)
                .monthly(monthlyList)
                .total(total)
                .build();
    }

    /** 12개월 막대그래프 데이터 — 이벤트가 없는 달도 score=0 으로 채워서 반환. */
    private List<ScoreEventsDto.MonthlyBucket> buildMonthlyBreakdown(List<ScoreEventsDto.EventDto> events) {
        long[] byMonth = new long[12];
        for (ScoreEventsDto.EventDto e : events) {
            // e.getDate() = "yyyy-MM-dd"
            int month = Integer.parseInt(e.getDate().substring(5, 7));
            byMonth[month - 1] += e.getTotal();
        }
        List<ScoreEventsDto.MonthlyBucket> out = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            out.add(ScoreEventsDto.MonthlyBucket.builder()
                    .month(i + 1)
                    .score(byMonth[i])
                    .build());
        }
        return out;
    }

    /**
     * 거래의 effective carbon factor 산출.
     *  - 모든 소재(BLEND 포함) 자기 factor 반환
     *    (BLEND 고유 factor 5.5 그대로 사용)
     */
    private BigDecimal resolveFactor(String materialCode, Map<String, BigDecimal> factorMap) {
        return factorMap.getOrDefault(materialCode, BigDecimal.ZERO);
    }
}
