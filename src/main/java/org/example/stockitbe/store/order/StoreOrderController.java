package org.example.stockitbe.store.order;

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

@RestController
@RequestMapping("/api/store/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderService service;

    // 매장 발주 요청 생성
    @PostMapping
    public BaseResponse<StoreOrderDto.CreateRes> create(@AuthenticationPrincipal AuthUserDetails me,
                                                         @Valid @RequestBody StoreOrderDto.CreateReq dto) {
        // 1. 요청 수신: 매장 코드, 요청자, 발주 라인 목록을 받는다.
        // 2. 서비스 호출: 발주 헤더/아이템/상태이력 생성 로직을 수행한다.
        StoreOrderDto.CreateRes result = service.create(dto, me);
        // 3. 응답 반환: 생성된 발주 상세를 공통 응답 포맷으로 반환한다.
        return BaseResponse.success(result);
    }

    // 매장 발주 요청 수정 (REQUESTED 상태일 때만 허용)
    @PutMapping("/{orderNo}")
    public BaseResponse<StoreOrderDto.UpdateRes> update(@PathVariable String orderNo,
                                                         @AuthenticationPrincipal AuthUserDetails me,
                                                         @Valid @RequestBody StoreOrderDto.UpdateReq dto) {
        // 1. 요청 수신: 발주번호와 수정 요청 라인/메모를 받는다.
        // 2. 서비스 호출: 상태 검증 후 아이템 재구성과 합계 재계산을 수행한다.
        StoreOrderDto.UpdateRes result = service.update(orderNo, dto, me);
        // 3. 응답 반환: 수정된 발주 상세를 공통 응답 포맷으로 반환한다.
        return BaseResponse.success(result);
    }

    // 매장 발주 요청 취소 (REQUESTED 상태만 허용)
    @PatchMapping("/{orderNo}/cancel")
    public BaseResponse<StoreOrderDto.CancelRes> cancel(@PathVariable String orderNo,
                                                         @AuthenticationPrincipal AuthUserDetails me,
                                                         @Valid @RequestBody StoreOrderDto.CancelReq dto) {
        // 1. 요청 수신: 발주번호와 취소 사유를 받는다.
        // 2. 서비스 호출: 상태 검증 후 취소 처리 및 상태이력 적재를 수행한다.
        StoreOrderDto.CancelRes result = service.cancel(orderNo, dto, me);
        // 3. 응답 반환: 취소 반영된 발주 상세를 공통 응답 포맷으로 반환한다.
        return BaseResponse.success(result);
    }

    // 승인 완료 상태로 변경시 가용 재고에 발주량 반영
    @PatchMapping("/{orderNo}/approve")
    public BaseResponse<StoreOrderDto.ApproveRes> approve(@PathVariable String orderNo,
                                                           @AuthenticationPrincipal AuthUserDetails me,
                                                           @RequestBody(required = false) StoreOrderDto.ApproveReq dto) {
        StoreOrderDto.ApproveRes result = service.approve(orderNo, dto == null ? StoreOrderDto.ApproveReq.builder().build() : dto, me);
        return BaseResponse.success(result);
    }

    // 매장 발주 내역 목록 조회
    @GetMapping
    public BaseResponse<List<StoreOrderDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword
    ) {
        // 1. 요청 수신: 매장/상태/기간/키워드 필터를 받는다.
        // 2. 서비스 호출: 필터 조건에 맞는 발주 내역 목록을 조회한다.
        List<StoreOrderDto.ListRes> result = service.list(status, from, to, keyword, me);
        // 3. 응답 반환: 목록 데이터를 공통 응답 포맷으로 반환한다.
        return BaseResponse.success(result);
    }

    // 매장 발주 상세 조회
    @GetMapping("/{orderNo}")
    public BaseResponse<StoreOrderDto.DetailRes> detail(@PathVariable String orderNo,
                                                         @AuthenticationPrincipal AuthUserDetails me) {
        // 1. 요청 수신: 발주번호를 경로 변수로 받는다.
        // 2. 서비스 호출: 헤더/아이템/상태이력을 포함한 상세를 조회한다.
        StoreOrderDto.DetailRes result = service.detail(orderNo, me);
        // 3. 응답 반환: 상세 데이터를 공통 응답 포맷으로 반환한다.
        return BaseResponse.success(result);
    }

    // 매장 발주 분석 조회
    @GetMapping("/analytics")
    public BaseResponse<StoreOrderDto.AnalyticsRes> analytics(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // 1. 요청 수신: 매장/기간 필터를 받는다.
        // 2. 서비스 호출: 발주 집계 및 SKU/카테고리 분석 데이터를 계산한다.
        StoreOrderDto.AnalyticsRes result = service.analytics(from, to, me);
        // 3. 응답 반환: 분석 결과를 공통 응답 포맷으로 반환한다.
        return BaseResponse.success(result);
    }
}
