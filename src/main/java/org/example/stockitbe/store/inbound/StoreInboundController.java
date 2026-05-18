package org.example.stockitbe.store.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.store.inbound.model.dto.StoreInboundDto;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/store/inbound")
@RequiredArgsConstructor
public class StoreInboundController {

    private final StoreInboundService storeInboundService;

    // 입고 내역 목록 조회
    @GetMapping
    public BaseResponse<List<StoreInboundDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(storeInboundService.list(me, status, from, to, keyword));
    }

    // 입고 내역 상세 조회
    @GetMapping("/{inboundNo}")
    public BaseResponse<StoreInboundDto.DetailRes> detail(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String inboundNo
    ) {
        return BaseResponse.success(storeInboundService.detail(me, inboundNo));
    }

    // 입고 확정 처리
    // 입고 수량을 실재고에 반영
    @PostMapping("/{inboundNo}/confirm")
    public BaseResponse<StoreInboundDto.DetailRes> confirm(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String inboundNo,
            @RequestParam(required = false) String reason
    ) {
        return BaseResponse.success(storeInboundService.confirm(me, inboundNo, reason));
    }
}

