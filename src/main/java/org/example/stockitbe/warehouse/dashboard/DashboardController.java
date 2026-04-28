package org.example.stockitbe.warehouse.dashboard;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.warehouse.dashboard.model.DashboardDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * WHS-001 입고 진행률 — 창고 관리자 대시보드 컨트롤러.
 *
 * 권한군 prefix: /api/warehouse/dashboard/...
 * 인증 미정(ADR-011) 이라 warehouseId 는 옵셔널 query 파라미터.
 * 인증 도입 시 @AuthenticationPrincipal 로 me.warehouseId 자동 주입으로 마이그레이션.
 */
@RestController
@RequestMapping("/api/warehouse/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/inbound-progress")
    public BaseResponse<DashboardDto.InboundProgressRes> getInboundProgress(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(service.getInboundProgress(warehouseId, from, to));
    }
}
