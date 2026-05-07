package org.example.stockitbe.warehouse.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.inbound.model.WhInboundDto;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * WHS-005/007/008 — 창고 관리자 입고 컨트롤러 (ERP 표준 모델).
 *
 * status 파라미터는 String — inbound 자체 status enum 이 없고 PO/outbound 의 status 를
 * join 결과로 받기 때문 (READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED 문자열).
 */
@RestController
@RequestMapping("/api/warehouse/inbound")
@RequiredArgsConstructor
public class WhInboundController {

    private final WhInboundService whInboundService;

    @GetMapping
    public BaseResponse<List<WhInboundDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(whInboundService.findAll(me, status, from, to));
    }

    @GetMapping("/{inboundCode}")
    public BaseResponse<WhInboundDto.DetailRes> detail(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String inboundCode) {
        return BaseResponse.success(whInboundService.findByCode(me, inboundCode));
    }

    @PostMapping("/{inboundCode}/confirm")
    public BaseResponse<WhInboundDto.DetailRes> confirm(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String inboundCode,
            @RequestBody(required = false) WhInboundDto.ConfirmReq req) {
        WhInboundHeader saved = whInboundService.confirmInbound(inboundCode, me);
        return BaseResponse.success(whInboundService.findByCode(me, saved.getInboundCode()));
    }

    @PostMapping("/backfill")
    public BaseResponse<WhInboundDto.BackfillRes> backfill() {
        return BaseResponse.success(whInboundService.backfillFromPurchaseOrders());
    }
}
