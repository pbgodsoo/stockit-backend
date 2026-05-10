package org.example.stockitbe.hq.analytics.orderstatsanalytics;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.analytics.orderstatsanalytics.model.OrderStatsAnalyticsDto;
import org.example.stockitbe.hq.analytics.orderstatsanalytics.model.OrderStatsPeriod;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hq/analytics/order-stats")
@RequiredArgsConstructor
public class OrderStatsAnalyticsController {

    private final OrderStatsAnalyticsService service;

    @GetMapping
    public BaseResponse<OrderStatsAnalyticsDto.Res> getOrderStats(
            @RequestParam(defaultValue = "MONTH") OrderStatsPeriod period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category
    ) {
        return BaseResponse.success(service.getOrderStats(period, from, to, category));
    }
}
