package org.example.stockitbe.hq.circularsale;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.circularsale.model.dto.CircularSaleDto;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hq/circular-inventory/sales")
@RequiredArgsConstructor
public class CircularSaleController {

    private final CircularSaleService circularSaleService;

    // 순환 재고 판매 (생성)
    @PostMapping
    public BaseResponse<CircularSaleDto.CreateRes> create(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody CircularSaleDto.CreateReq request
    ) {
        // 1. 요청 수신: 판매 생성 요청을 받는다.
        // 2. 서비스 호출: 판매/출고 동시 생성 오케스트레이션을 수행한다.
        CircularSaleDto.CreateRes result = circularSaleService.create(request, me);
        // 3. 응답 반환: 생성 결과를 공통 응답으로 감싸 반환한다.
        return BaseResponse.success(result);
    }

    // 순환 재고 판매 내역 목록 조회
    @GetMapping
    public BaseResponse<CircularSaleDto.PageRes> list(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "soldAt,desc") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String buyerCode,
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String keyword
    ) {
        // 1. 요청 수신: 판매 이력 조회 조건/페이지 파라미터를 받는다.
        // 2. 서비스 호출: 조건 기반 목록 조회를 수행한다.
        CircularSaleDto.PageRes result = circularSaleService.list(page, size, sort, from, to, buyerCode, materialType, keyword);

        // 3. 응답 반환: 페이지 응답을 공통 응답으로 반환한다.
        return BaseResponse.success(result);
    }

    // 순환 재고 판매 내역 상세 조회
    @GetMapping("/{saleId}")
    public BaseResponse<CircularSaleDto.DetailRes> detail(@PathVariable Long saleId) {
        // 1. 요청 수신: 판매 식별자(saleId)를 받는다.
        CircularSaleDto.DetailRes result = circularSaleService.detail(saleId);
        // 2. 서비스 호출: 판매 상세/라인/소재/상태이력을 조회한다.
        // 3. 응답 반환: 상세 응답을 공통 응답으로 반환한다.
        return BaseResponse.success(result);
    }
}

