package org.example.stockitbe.warehouse.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderDto;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * WHS-005/007/008 — 창고 관리자 입고 컨트롤러.
 *
 * 권한군 prefix: /api/warehouse/...  (SecurityConfig 가 hasRole("WAREHOUSE") 자동 차단)
 * 자기 창고 데이터 격리 — 인증 사용자의 locationCode 만 신뢰. query param 으로 warehouseId
 * 받지 않음 (위변조 방지). detail 도 @AuthenticationPrincipal 받아 다른 창고 발주 직접 URL 접근 차단.
 */
@RestController
@RequestMapping("/api/warehouse/inbound")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService service;

    @GetMapping
    public BaseResponse<List<PurchaseOrderDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(service.findAll(me.getLocationCode(), status, from, to));
    }

    @GetMapping("/{code}")
    public BaseResponse<PurchaseOrderDto.DetailRes> detail(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String code) {
        return BaseResponse.success(service.findByCode(code, me.getLocationCode()));
    }

    @PostMapping("/{code}/confirm")
    public BaseResponse<PurchaseOrderDto.DetailRes> confirm(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String code) {
        return BaseResponse.success(service.confirm(code, me));
    }
}
