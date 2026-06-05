package org.example.stockitbe.hq.esg.carbonprice;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.esg.carbonprice.model.CarbonPriceDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hq/esg")
@RequiredArgsConstructor
public class CarbonPriceController {

    private final CarbonPriceService carbonPriceService;

    @GetMapping("/carbon-price/latest")
    public ResponseEntity<BaseResponse<CarbonPriceDto.Snapshot>> getLatest() {
        return ResponseEntity.ok(BaseResponse.success(carbonPriceService.getLatestPrice()));
    }

    @GetMapping("/carbon-price/trend")
    public ResponseEntity<BaseResponse<List<CarbonPriceDto.Snapshot>>> getTrend(
            @RequestParam(defaultValue = "SEVEN_DAYS") CarbonPriceService.Period period
    ) {
        return ResponseEntity.ok(BaseResponse.success(carbonPriceService.getTrend(period)));
    }

    /** 월별 집계 시계열 — 최근 N개월의 월말 종가 반환. KOC 류 장기 트렌드 표현용. */
    @GetMapping("/carbon-price/trend/monthly")
    public ResponseEntity<BaseResponse<List<CarbonPriceDto.Snapshot>>> getMonthlyTrend(
            @RequestParam(defaultValue = "12") int months
    ) {
        return ResponseEntity.ok(BaseResponse.success(carbonPriceService.getMonthlyTrend(months)));
    }
}
