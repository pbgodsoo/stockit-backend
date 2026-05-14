package org.example.stockitbe.hq.esg.circularrevenue;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.esg.circularrevenue.model.CircularRevenueDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/esg")
@RequiredArgsConstructor
public class CircularRevenueController {

    private final CircularRevenueService service;

    /** 순환재고 월별 판매 수익 (12개월 시계열 + 통계) */
    @GetMapping("/circular-revenue")
    public ResponseEntity<BaseResponse<CircularRevenueDto.Response>> getRevenue(
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(BaseResponse.success(service.getMonthlyRevenue(year)));
    }
}
