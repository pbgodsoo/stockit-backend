package org.example.stockitbe.hq.analytics.vendoranalytics;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.analytics.vendoranalytics.model.VendorAnalyticsDto;
import org.example.stockitbe.hq.analytics.vendoranalytics.model.VendorPeriod;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hq/analytics/vendor")
@RequiredArgsConstructor
public class VendorAnalyticsController {

    private final VendorAnalyticsService service;

    @GetMapping
    public BaseResponse<VendorAnalyticsDto.Res> getVendorAnalytics(
            @RequestParam(defaultValue = "MONTH") VendorPeriod period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return BaseResponse.success(service.getVendorAnalytics(period, from, to));
    }
}
