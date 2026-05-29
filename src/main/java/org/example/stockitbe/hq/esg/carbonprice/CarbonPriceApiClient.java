package org.example.stockitbe.hq.esg.carbonprice;

import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.esg.carbonprice.model.CarbonPriceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.LocalDate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CarbonPriceApiClient {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String SUCCESS_CODE = "00";   // 공공데이터포털 정상 코드
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String serviceKey;
    private final String targetSymbol;

    public CarbonPriceApiClient(
            @Value("${esg.carbon-api.base-url}") String baseUrl,
            @Value("${esg.carbon-api.service-key}") String serviceKey,
            @Value("${esg.carbon-api.target-symbol}") String targetSymbol
    ) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        this.webClient = WebClient.builder()
                .uriBuilderFactory(factory)
                .build();
        this.serviceKey = serviceKey;
        this.targetSymbol = targetSymbol;
    }

    public List<CarbonPriceDto.Item> fetchPrices(LocalDate from, LocalDate to) {
        try {
            CarbonPriceDto.ApiResponse res = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getCertifiedEmissionReductionPriceInfo")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("numOfRows", 1000)              // 6개월치
                            .queryParam("pageNo", 1)
                            .queryParam("resultType", "json")
                            .queryParam("itmsNm", targetSymbol)
                            .queryParam("beginBasDt", from.format(YYYYMMDD))
                            .queryParam("endBasDt", to.format(YYYYMMDD))
                            .build())
                    .retrieve()
                    .bodyToMono(CarbonPriceDto.ApiResponse.class)
                    .block(API_TIMEOUT);

            if (res == null
                    || res.getResponse() == null
                    || res.getResponse().getHeader() == null) {
                log.error("배출권 API 응답 구조 비정상");
                return Collections.emptyList();
            }
            CarbonPriceDto.Header header = res.getResponse().getHeader();
            if (!SUCCESS_CODE.equals(header.getResultCode())) {
                log.error("배출권 API 비정상 응답: code={}, msg={}",
                        header.getResultCode(), header.getResultMsg());
                return Collections.emptyList();
            }
            return Optional.ofNullable(res.getResponse().getBody())
                    .map(CarbonPriceDto.Body::getItems)
                    .map(CarbonPriceDto.Items::getItem)
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("배출권 시세 API 호출 실패", e);
            return Collections.emptyList();
        }
    }




    /**
     * KAU25 시세 최근 2주 일별 데이터 조회.
     *
     * 정책 — Graceful Degradation:
     *  - 네트워크 / 파싱 / 외부 API 비정상응답 모두 빈 리스트로 반환
     *  - 호출 측(Service) 에서 fallbackPrice 로 대체 → 화면 끊김 방지
     *  - 의심 케이스는 log.error 로 흔적 남김 (운영 모니터링 시 확인)
     */
    public List<CarbonPriceDto.Item> fetchRecentPrices() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(14);

        try {
            CarbonPriceDto.ApiResponse res = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getCertifiedEmissionReductionPriceInfo")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("numOfRows", 30)
                            .queryParam("pageNo", 1)
                            .queryParam("resultType", "json")
                            .queryParam("itmsNm", targetSymbol)
                            .queryParam("beginBasDt", from.format(YYYYMMDD))
                            .queryParam("endBasDt", today.format(YYYYMMDD))
                            .build())
                    .retrieve()
                    .bodyToMono(CarbonPriceDto.ApiResponse.class)
                    .block(API_TIMEOUT);

            // 응답 헤더 검증 — resultCode 가 "00" 이 아니면 외부 API 비정상 응답
            if (res == null
                    || res.getResponse() == null
                    || res.getResponse().getHeader() == null) {
                log.error("배출권 API 응답 구조 비정상 (헤더 없음)");
                return Collections.emptyList();
            }

            CarbonPriceDto.Header header = res.getResponse().getHeader();
            if (!SUCCESS_CODE.equals(header.getResultCode())) {
                log.error("배출권 API 비정상 응답: code={}, msg={}",
                        header.getResultCode(), header.getResultMsg());
                return Collections.emptyList();
            }

            return Optional.ofNullable(res.getResponse().getBody())
                    .map(CarbonPriceDto.Body::getItems)
                    .map(CarbonPriceDto.Items::getItem)
                    .orElse(Collections.emptyList());

        } catch (Exception e) {
            log.error("배출권 시세 API 호출 실패", e);
            return Collections.emptyList();   // 폴백 (Service 에서 fallbackPrice 사용)
        }
    }
}
