package org.example.stockitbe.hq.esg.carbonprice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.esg.carbonprice.model.CarbonPriceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonPriceService {

    private final CarbonPriceApiClient apiClient;

    @Value("${esg.carbon-api.fallback-price:9200}")
    private int fallbackPrice;

    /** 그래프 기간 옵션 — FE 탭과 1:1 매핑.
     *  dayRange 는 호출 시점 today 기준 minusDays 값 (휴장일 포함 여유분).
     *  SEVEN_DAYS 는 KPI 카드 "7거래일 최고/최저" 계산용으로도 사용되어 enum 보존. */
    public enum Period {
        SEVEN_DAYS(14),
        ONE_MONTH(45),
        THREE_MONTHS(100),
        SIX_MONTHS(200);

        public final int dayRange;
        Period(int dayRange) { this.dayRange = dayRange; }
    }

    /** 가장 최근 거래일의 배출권 종가 (KPI 카드용) — 14일 범위에서 추출.
     *  KOC 류는 거래일 드물어서 trqu>0 만으로는 빈 결과 빈번 → clpr 기반으로 변경. */
    public CarbonPriceDto.Snapshot getLatestPrice() {
        List<CarbonPriceDto.Item> items = apiClient.fetchRecentPrices().stream()
                .filter(this::hasClosingPrice)
                .toList();
        return items.stream()
                .max(Comparator.comparing(CarbonPriceDto.Item::getBasDt))
                .map(this::toSnapshot)
                .orElseGet(() -> {
                    log.warn("배출권 시세 폴백 사용 (가격={}원/톤)", fallbackPrice);
                    return new CarbonPriceDto.Snapshot(
                            fallbackPrice, "FALLBACK", null, null, true);
                });
    }

    /** 시계열 — 기간 옵션에 따라 호출 범위 다르게, 종가(clpr) 있는 날만 표시.
     *  KOC 류는 거래량 0 인 날이 대부분이라 hasTrading 만으로는 차트가 비어버림.
     *  → clpr(전일 종가) 만 있으면 차트에 포함 (실제 거래 없이 마감가 유지되는 채권 시세 표현 방식). */
    public List<CarbonPriceDto.Snapshot> getTrend(Period period) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(period.dayRange);

        return apiClient.fetchPrices(from, today).stream()
                .filter(this::hasClosingPrice)         // 종가 > 0 만 (거래량 0 도 허용)
                .sorted(Comparator.comparing(CarbonPriceDto.Item::getBasDt))
                .map(this::toSnapshot)
                .toList();
    }

    /** 월별 시계열 — 최근 N개월의 "월말 종가" 반환.
     *  KOC 류는 일별 차트가 거의 평탄 → 월별 집계로 장기 트렌드 가시화.
     *  각 월의 가장 늦은 날짜(=월말 직전 거래일)의 clpr 을 대표값으로 사용 — 금융 시세 보고 관행.
     *
     * @param months 최근 몇 개월치를 가져올지 (기본 12개월 = 1년 트렌드)
     */
    public List<CarbonPriceDto.Snapshot> getMonthlyTrend(int months) {
        LocalDate today = LocalDate.now();
        // months+1 개월 호출 — 현재월 + 과거 N개월. 외부 API daily 데이터를 모두 받아옴.
        LocalDate from = today.minusMonths(months);

        // 월별 그룹핑: key=YYYYMM, value=그 달의 마지막 거래일 Item
        Map<String, CarbonPriceDto.Item> lastOfMonth = apiClient.fetchPrices(from, today).stream()
                .filter(this::hasClosingPrice)
                .collect(Collectors.toMap(
                        item -> item.getBasDt().substring(0, 6),   // YYYYMM 부분만 키로 사용
                        item -> item,
                        (a, b) -> a.getBasDt().compareTo(b.getBasDt()) >= 0 ? a : b   // 더 늦은 날짜 우선
                ));

        return lastOfMonth.values().stream()
                .sorted(Comparator.comparing(CarbonPriceDto.Item::getBasDt))
                .map(this::toSnapshot)
                .toList();
    }

    /** 종가(clpr) 가 0 보다 큰지 검사 — 거래 없어도 종가가 있으면 차트 표시 대상. */
    private boolean hasClosingPrice(CarbonPriceDto.Item item) {
        String clpr = item.getClpr();
        if (clpr == null || clpr.isBlank()) return false;
        try {
            return Long.parseLong(clpr.replace(",", "")) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 거래량 > 0 인지 검사 — 최신 종가 추출 시에도 종가 기반으로 변경되어 사용처 없음 (호환용 보존). */
    @SuppressWarnings("unused")
    private boolean hasTrading(CarbonPriceDto.Item item) {
        String trqu = item.getTrqu();
        if (trqu == null || trqu.isBlank()) return false;
        try {
            return Long.parseLong(trqu.replace(",", "")) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private CarbonPriceDto.Snapshot toSnapshot(CarbonPriceDto.Item item) {
        return new CarbonPriceDto.Snapshot(
                Integer.parseInt(item.getClpr().replace(",", "")),
                item.getItmsNm(),
                item.getBasDt(),
                item.getFltRt(),
                false
        );
    }
}
