package org.example.stockitbe.hq.analytics.dashboardanalytics;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.analytics.dashboardanalytics.model.DashboardAnalyticsDto;
import org.example.stockitbe.hq.analytics.dashboardanalytics.model.DashboardPeriod;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hq/analytics/dashboard")
@RequiredArgsConstructor
public class DashboardAnalyticsController {

    private final DashboardAnalyticsService service;

    @GetMapping
    public BaseResponse<DashboardAnalyticsDto.Res> getDashboard(
            @RequestParam(defaultValue = "YEAR") DashboardPeriod period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return BaseResponse.success(service.getDashboardAnalytics(period, from, to));
    }
}
