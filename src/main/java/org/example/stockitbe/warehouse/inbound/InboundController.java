package org.example.stockitbe.warehouse.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderDto;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.springframework.format.annotation.DateTimeFormat;
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
 * 권한군 prefix: /api/warehouse/...
 * 인증 미정(ADR-011) 이라 warehouseId 는 query 파라미터로 받음 — 임시 신뢰 모델.
 * 인증 도입 시 @AuthenticationPrincipal 로 me.warehouseId 자동 주입으로 마이그레이션.
 */
@RestController
@RequestMapping("/api/warehouse/inbound")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService service;

    @GetMapping
    public BaseResponse<List<PurchaseOrderDto.ListRes>> list(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(service.findAll(status, warehouseId, from, to));
    }

    @GetMapping("/{code}")
    public BaseResponse<PurchaseOrderDto.DetailRes> detail(@PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @PostMapping("/{code}/confirm")
    public BaseResponse<PurchaseOrderDto.DetailRes> confirm(@PathVariable String code) {
        return BaseResponse.success(service.confirm(code));
    }
}
