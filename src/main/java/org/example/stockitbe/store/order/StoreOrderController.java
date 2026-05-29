package org.example.stockitbe.store.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.store.order.model.dto.StoreOrderDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.example.stockitbe.user.model.entity.AuthUserDetails;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "매장 발주", description = "매장 발주 요청 생성·수정·취소·승인 및 조회 API")
@RestController
@RequestMapping("/api/store/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderService service;

    // ── C : Create ──────────────────────────────────────────────────────────────
    @Operation(summary = "발주 요청 생성", description = "매장의 SKU별 발주 요청을 생성한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping
    public BaseResponse<StoreOrderDto.CreateRes> create(@AuthenticationPrincipal AuthUserDetails me,
                                                         @Valid @RequestBody StoreOrderDto.CreateReq dto) {
        StoreOrderDto.CreateRes result = service.create(dto, me);
        return BaseResponse.success(result);
    }

    // ── R : Read ─────────────────────────────────────────────────────────────
    @Operation(summary = "발주 목록 조회", description = "로그인 매장의 발주 내역을 상태·기간·키워드로 필터링해 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public BaseResponse<List<StoreOrderDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "발주 상태 (REQUESTED / APPROVED / COMPLETED / CANCELLED)", example = "REQUESTED") @RequestParam(required = false) String status,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-05-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-05-27") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "발주번호·상품명·SKU 코드 검색 키워드", example = "코튼") @RequestParam(required = false) String keyword
    ) {
        List<StoreOrderDto.ListRes> result = service.list(status, from, to, keyword, me);
        return BaseResponse.success(result);
    }

    @Operation(summary = "발주 상세 조회", description = "발주번호로 헤더·아이템·입고 요약·상태이력을 포함한 상세 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "발주 없음")
    })
    @GetMapping("/{orderNo}")
    public BaseResponse<StoreOrderDto.DetailRes> detail(
            @Parameter(description = "발주번호", example = "SOR-20260510-00001") @PathVariable String orderNo,
            @AuthenticationPrincipal AuthUserDetails me) {
        StoreOrderDto.DetailRes result = service.detail(orderNo, me);
        return BaseResponse.success(result);
    }

    @Operation(summary = "발주 분석 조회", description = "기간 내 발주 현황을 상태별·SKU별·카테고리별로 집계해 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/analytics")
    public BaseResponse<StoreOrderDto.AnalyticsRes> analytics(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-05-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-05-27") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        StoreOrderDto.AnalyticsRes result = service.analytics(from, to, me);
        return BaseResponse.success(result);
    }

    // ── U : Update ────────────────────────────────────────────────────────────
    @Operation(summary = "발주 요청 수정", description = "REQUESTED 상태인 발주의 메모 및 SKU 라인을 수정한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "수정 불가 상태이거나 잘못된 요청 데이터")
    })
    @PutMapping("/{orderNo}")
    public BaseResponse<StoreOrderDto.UpdateRes> update(
            @Parameter(description = "발주번호", example = "SOR-20260510-00001") @PathVariable String orderNo,
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody StoreOrderDto.UpdateReq dto) {
        StoreOrderDto.UpdateRes result = service.update(orderNo, dto, me);
        return BaseResponse.success(result);
    }

    @Operation(summary = "발주 요청 취소", description = "REQUESTED 상태인 발주를 취소 처리하고 상태이력을 기록한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소 불가 상태이거나 잘못된 요청 데이터")
    })
    @PatchMapping("/{orderNo}/cancel")
    public BaseResponse<StoreOrderDto.CancelRes> cancel(
            @Parameter(description = "발주번호", example = "SOR-20260510-00001") @PathVariable String orderNo,
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody StoreOrderDto.CancelReq dto) {
        StoreOrderDto.CancelRes result = service.cancel(orderNo, dto, me);
        return BaseResponse.success(result);
    }

    @Operation(summary = "발주 승인", description = "발주를 승인 완료 상태로 변경하고 가용 재고에 발주량을 반영한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인 성공"),
            @ApiResponse(responseCode = "404", description = "발주 없음")
    })
    @PatchMapping("/{orderNo}/approve")
    public BaseResponse<StoreOrderDto.ApproveRes> approve(
            @Parameter(description = "발주번호", example = "SOR-20260510-00001") @PathVariable String orderNo,
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestBody(required = false) StoreOrderDto.ApproveReq dto) {
        StoreOrderDto.ApproveRes result = service.approve(orderNo, dto == null ? StoreOrderDto.ApproveReq.builder().build() : dto, me);
        return BaseResponse.success(result);
    }
}
