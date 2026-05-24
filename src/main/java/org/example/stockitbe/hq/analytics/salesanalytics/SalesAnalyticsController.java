package org.example.stockitbe.hq.analytics.salesanalytics;


import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.analytics.salesanalytics.model.SalesAnalyticsDto;
import org.example.stockitbe.hq.analytics.salesanalytics.model.SalesPeriod;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hq/analytics/sales")
@RequiredArgsConstructor
public class SalesAnalyticsController {

    private final SalesAnalyticsService salesAnalyticsService;

    @GetMapping
    public BaseResponse<SalesAnalyticsDto.Res> getSales(
            @RequestParam(defaultValue = "MONTH") SalesPeriod period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String storeCode,
            @RequestParam(required = false) String mainCategory
    ) {
        return BaseResponse.success(
                salesAnalyticsService.getSalesAnalytics(period, from, to, storeCode, mainCategory));
    }
}

