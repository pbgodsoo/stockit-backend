package org.example.stockitbe.warehouse.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.example.stockitbe.warehouse.dashboard.model.DashboardDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * WHS-001 입고 진행률 — 창고 관리자 대시보드 컨트롤러.
 *
 * 권한군 prefix: /api/warehouse/dashboard/...  (SecurityConfig 가 hasRole("WAREHOUSE") 자동 차단)
 * 자기 창고 데이터 격리 — 인증 사용자의 locationCode 만 신뢰. query param 으로 warehouseId
 * 받지 않음 (위변조 방지).
 */
@Tag(name = "창고 대시보드", description = "창고 관리자 대시보드 API (WHS-001). 자기 창고 데이터 격리 — 인증 사용자의 locationCode 만 신뢰")
@RestController
@RequestMapping("/api/warehouse/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @Operation(
            summary = "입고 진행률 조회",
            description = "로그인한 창고의 입고 진행률(예정/도착/완료) 을 기간 필터로 집계. warehouseId 는 query param 으로 받지 않고 인증 사용자의 locationCode 만 사용 — 위변조 방지."
    )
    @GetMapping("/inbound-progress")
    public BaseResponse<DashboardDto.InboundProgressRes> getInboundProgress(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "집계 기간 시작 (yyyy-MM-dd). 미지정 시 서비스 기본값", example = "2026-05-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "집계 기간 종료 (yyyy-MM-dd). 미지정 시 서비스 기본값", example = "2026-05-27") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(service.getInboundProgress(me.getLocationCode(), from, to));
    }
}
