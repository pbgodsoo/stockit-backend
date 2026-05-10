package org.example.stockitbe.warehouse.outbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.warehouse.outbound.model.dto.WhOutboundDto;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse/outbound")
public class WhOutboundController {

    private final WhOutboundService whOutboundService;

    // 출고 내역 목록 조회
    @GetMapping
    public BaseResponse<List<WhOutboundDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(whOutboundService.list(me, status, from, to, keyword));
    }

    // 출고 내역 상세 조회
    @GetMapping("/{outboundNo}")
    public BaseResponse<WhOutboundDto.DetailRes> detail(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String outboundNo
    ) {
        return BaseResponse.success(whOutboundService.detail(me, outboundNo));
    }

    // 출고 확정 처리
    // 배송 중 단계로 이동 (InTransit으로 재고가 반영됨)
    @PostMapping("/{outboundNo}/confirm")
    public BaseResponse<WhOutboundDto.DetailRes> confirm(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String outboundNo,
            @RequestBody(required = false) WhOutboundDto.ActionReq req
    ) {
        return BaseResponse.success(whOutboundService.confirm(me, outboundNo, req == null ? null : req.getReason()));
    }

    // 도착 확정 (매장 배송 완료)
    @PostMapping("/{outboundNo}/arrive")
    public BaseResponse<WhOutboundDto.DetailRes> arrive(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String outboundNo,
            @RequestBody(required = false) WhOutboundDto.ActionReq req
    ) {
        return BaseResponse.success(whOutboundService.arrive(me, outboundNo, req == null ? null : req.getReason()));
    }
}
