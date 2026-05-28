package org.example.stockitbe.hq.purchaseorder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderDto;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "공급처 발주", description = "본사 발주 헤더 CRUD · 상태 전환(완료/취소) API (CEN-035~040)")
@RestController
@RequestMapping("/api/hq/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @Operation(summary = "발주 목록 조회", description = "거래처/상태/기간 필터와 페이징을 지원한다. 기본 정렬은 createdAt DESC.")
    @GetMapping
    public BaseResponse<Page<PurchaseOrderDto.ListRes>> list(
            @Parameter(description = "거래처 코드 필터", example = "VND-001") @RequestParam(required = false) String vendorCode,
            @Parameter(description = "발주 상태 필터 (PENDING/APPROVED/READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED/CANCELLED)", example = "PENDING") @RequestParam(required = false) PurchaseOrderStatus status,
            @Parameter(description = "발주 생성일 시작 (yyyy-MM-dd)", example = "2026-05-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "발주 생성일 종료 (yyyy-MM-dd)", example = "2026-05-27") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return BaseResponse.success(service.findAll(vendorCode, status, from, to, pageable));
    }

    @Operation(summary = "발주 단건 상세 조회", description = "발주 코드(PO-{YYYYMMDD}-{NNNNN}) 로 헤더 + 라인 + 거래처 스냅샷을 반환한다.")
    @GetMapping("/{code}")
    public BaseResponse<PurchaseOrderDto.DetailRes> detail(
            @Parameter(description = "발주 코드 (PO-{YYYYMMDD}-{NNNNN})", example = "PO-20260520-00001") @PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @Operation(summary = "발주 단건 생성", description = "거래처·창고·라인을 입력받아 PENDING 상태 발주 1건을 생성한다. 발주 코드는 서버가 채번.")
    @PostMapping
    public BaseResponse<PurchaseOrderDto.DetailRes> create(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody PurchaseOrderDto.CreateReq req) {
        return BaseResponse.success(service.create(req, me));
    }

    @Operation(summary = "발주 일괄 생성", description = "카탈로그 화면에서 선택한 다수 SKU 를 거래처별로 묶어 발주 N건을 한 번에 생성한다.")
    @PostMapping("/batch")
    public BaseResponse<PurchaseOrderDto.BatchCreateRes> createBatch(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody PurchaseOrderDto.BatchCreateReq req) {
        return BaseResponse.success(service.createBatch(req, me));
    }

    @Operation(summary = "발주 수정", description = "PENDING 상태 발주의 라인(수량/단가)·창고·메모를 수정한다. APPROVED 이후 단계는 변경 불가.")
    @PatchMapping("/{code}")
    public BaseResponse<PurchaseOrderDto.DetailRes> update(
            @Parameter(description = "발주 코드", example = "PO-20260520-00001") @PathVariable String code,
            @Valid @RequestBody PurchaseOrderDto.UpdateReq req) {
        return BaseResponse.success(service.update(code, req));
    }

    @Operation(summary = "발주 완료 처리", description = "ARRIVED 상태 발주를 COMPLETED 로 전환한다. 본사 책임 마지막 단계.")
    @PostMapping("/{code}/complete")
    public BaseResponse<PurchaseOrderDto.DetailRes> complete(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "발주 코드", example = "PO-20260520-00001") @PathVariable String code) {
        return BaseResponse.success(service.complete(code, me));
    }

    @Operation(summary = "발주 취소", description = "PENDING/APPROVED 상태 발주를 CANCELLED 로 전환하고 취소 사유를 기록한다. SHIPPING 이후는 취소 불가.")
    @PostMapping("/{code}/cancel")
    public BaseResponse<PurchaseOrderDto.DetailRes> cancel(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "발주 코드", example = "PO-20260520-00001") @PathVariable String code,
            @Valid @RequestBody PurchaseOrderDto.CancelReq req) {
        return BaseResponse.success(service.cancel(code, req, me));
    }
}
