package org.example.stockitbe.store.inbound;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.store.inbound.model.dto.StoreInboundDto;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "매장 입고", description = "매장 입고 내역 조회 및 입고 확정 API")
@RestController
@RequestMapping("/api/store/inbound")
@RequiredArgsConstructor
public class StoreInboundController {

    private final StoreInboundService storeInboundService;

    @Operation(summary = "입고 목록 조회", description = "로그인 매장의 입고 내역을 상태·기간·키워드로 필터링해 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public BaseResponse<List<StoreInboundDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "입고 상태 (PENDING / RECEIVED / CANCELLED)", example = "PENDING") @RequestParam(required = false) String status,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-05-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-05-27") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "입고번호·출고번호 검색 키워드", example = "INB-20260527") @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(storeInboundService.list(me, status, from, to, keyword));
    }

    @Operation(summary = "입고 상세 조회", description = "입고번호로 헤더·아이템·출고 연계 정보·상태이력을 포함한 상세를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "입고 없음")
    })
    @GetMapping("/{inboundNo}")
    public BaseResponse<StoreInboundDto.DetailRes> detail(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "입고번호", example = "INB-20240101-001") @PathVariable String inboundNo
    ) {
        return BaseResponse.success(storeInboundService.detail(me, inboundNo));
    }

    @Operation(summary = "입고 확정", description = "입고를 확정하여 입고 수량을 실재고에 반영한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확정 성공"),
            @ApiResponse(responseCode = "400", description = "확정 불가 상태")
    })
    @PostMapping("/{inboundNo}/confirm")
    public BaseResponse<StoreInboundDto.DetailRes> confirm(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "입고번호", example = "INB-20240101-001") @PathVariable String inboundNo,
            @RequestBody(required = false) StoreInboundDto.ActionReq req
    ) {
        return BaseResponse.success(storeInboundService.confirm(me, inboundNo, req == null ? null : req.getReason()));
    }
}
