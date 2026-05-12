package org.example.stockitbe.hq.purchaseorder;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderDto;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hq/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @GetMapping
    public BaseResponse<List<PurchaseOrderDto.ListRes>> list(
            @RequestParam(required = false) String vendorCode,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(service.findAll(vendorCode, status, from, to));
    }

    @GetMapping("/{code}")
    public BaseResponse<PurchaseOrderDto.DetailRes> detail(@PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @PostMapping
    public BaseResponse<PurchaseOrderDto.DetailRes> create(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody PurchaseOrderDto.CreateReq req) {
        return BaseResponse.success(service.create(req, me));
    }

    @PostMapping("/batch")
    public BaseResponse<PurchaseOrderDto.BatchCreateRes> createBatch(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody PurchaseOrderDto.BatchCreateReq req) {
        return BaseResponse.success(service.createBatch(req, me));
    }

    @PatchMapping("/{code}")
    public BaseResponse<PurchaseOrderDto.DetailRes> update(@PathVariable String code,
                                                             @Valid @RequestBody PurchaseOrderDto.UpdateReq req) {
        return BaseResponse.success(service.update(code, req));
    }

    @PostMapping("/{code}/complete")
    public BaseResponse<PurchaseOrderDto.DetailRes> complete(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String code) {
        return BaseResponse.success(service.complete(code, me));
    }

    @PostMapping("/{code}/cancel")
    public BaseResponse<PurchaseOrderDto.DetailRes> cancel(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String code,
            @Valid @RequestBody PurchaseOrderDto.CancelReq req) {
        return BaseResponse.success(service.cancel(code, req, me));
    }
}
