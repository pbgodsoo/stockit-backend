package org.example.stockitbe.hq.esg.carbonprice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.esg.carbonprice.model.CarbonPriceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonPriceService {

    private final CarbonPriceApiClient apiClient;

    @Value("${esg.carbon-api.fallback-price:9200}")
    private int fallbackPrice;

    /** 그래프 기간 옵션 — FE 탭과 1:1 매핑 */
    public enum Period {
        SEVEN_DAYS(14),
        ONE_MONTH(45),
        SIX_MONTHS(200);

        public final int dayRange;
        Period(int dayRange) { this.dayRange = dayRange; }
    }

    /** 가장 최근 거래일의 KAU 종가 (KPI 카드용) — 14일 범위에서 추출 */
    public CarbonPriceDto.Snapshot getLatestPrice() {
        List<CarbonPriceDto.Item> items = apiClient.fetchRecentPrices().stream()
                .filter(this::hasTrading)
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

    /** 시계열 — 기간 옵션에 따라 호출 범위 다르게, 거래량 0 제외 */
    public List<CarbonPriceDto.Snapshot> getTrend(Period period) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(period.dayRange);

        return apiClient.fetchPrices(from, today).stream()
                .filter(this::hasTrading)              // 거래량 > 0 만
                .sorted(Comparator.comparing(CarbonPriceDto.Item::getBasDt))
                .map(this::toSnapshot)
                .toList();
    }

    /** 거래량 > 0 인지 검사 (실제 거래된 날짜만 의미 있음) */
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
