package org.example.stockitbe.warehouse.outbound;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
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

@Tag(name = "창고 출고", description = "창고 출고 내역 조회, 출고 확정, 도착 확정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse/outbound")
public class WhOutboundController {

    private final WhOutboundService whOutboundService;

    @Operation(summary = "출고 목록 조회", description = "로그인 창고의 출고 내역을 상태·기간·키워드로 필터링해 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public BaseResponse<List<WhOutboundDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "출고 상태 (PENDING / IN_TRANSIT / ARRIVED / CANCELLED)", example = "IN_TRANSIT") @RequestParam(required = false) String status,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-05-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-05-27") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "출고번호·참조번호·목적지 검색 키워드", example = "OUT-20260527") @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(whOutboundService.list(me, status, from, to, keyword));
    }

    @Operation(summary = "출고 상세 조회", description = "출고번호로 헤더·아이템·입고 연계 정보·상태이력을 포함한 상세를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "출고 없음")
    })
    @GetMapping("/{outboundNo}")
    public BaseResponse<WhOutboundDto.DetailRes> detail(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "출고번호", example = "WOB-20260528-00002") @PathVariable String outboundNo
    ) {
        return BaseResponse.success(whOutboundService.detail(me, outboundNo));
    }

    @Operation(summary = "출고 확정", description = "출고를 확정하여 재고를 InTransit 상태로 전환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확정 성공"),
            @ApiResponse(responseCode = "400", description = "확정 불가 상태")
    })
    @PostMapping("/{outboundNo}/confirm")
    public BaseResponse<WhOutboundDto.DetailRes> confirm(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "출고번호", example = "WOB-20260528-00002") @PathVariable String outboundNo,
            @RequestBody(required = false) WhOutboundDto.ActionReq req
    ) {
        return BaseResponse.success(whOutboundService.confirm(me, outboundNo, req == null ? null : req.getReason()));
    }

    @Operation(summary = "도착 확정", description = "출고 도착을 확정하여 매장 배송 완료로 처리한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "도착 확정 성공"),
            @ApiResponse(responseCode = "400", description = "확정 불가 상태")
    })
    @PostMapping("/{outboundNo}/arrive")
    public BaseResponse<WhOutboundDto.DetailRes> arrive(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "출고번호", example = "WOB-20260528-00002") @PathVariable String outboundNo,
            @RequestBody(required = false) WhOutboundDto.ActionReq req
    ) {
        return BaseResponse.success(whOutboundService.arrive(me, outboundNo, req == null ? null : req.getReason()));
    }
}
