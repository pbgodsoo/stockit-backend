package org.example.stockitbe.hq.analytics.turnoveranalytics;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverAnalyticsDto;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverPeriod;
import org.example.stockitbe.hq.analytics.turnoveranalytics.model.TurnoverScope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hq/analytics/turnover")
@RequiredArgsConstructor
public class TurnoverAnalyticsController {

    private final TurnoverAnalyticsService service;

    @GetMapping
    public BaseResponse<TurnoverAnalyticsDto.Res> getTurnover(
            @RequestParam(defaultValue = "MONTH") TurnoverPeriod period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") TurnoverScope scope,
            @RequestParam(required = false) String locationCode
    ) {
        return BaseResponse.success(
                service.getTurnoverAnalytics(period, from, to, scope, locationCode)
        );
    }
}
