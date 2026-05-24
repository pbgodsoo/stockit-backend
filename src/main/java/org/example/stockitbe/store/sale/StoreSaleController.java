package org.example.stockitbe.store.sale;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.store.sale.model.dto.StoreSaleDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.stockitbe.user.model.entity.AuthUserDetails;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/store/sales")
@RequiredArgsConstructor
public class StoreSaleController {

    private final StoreSaleService service;

    // 판매
    @PostMapping
    public BaseResponse<StoreSaleDto.SaleRes> create(@AuthenticationPrincipal AuthUserDetails me,
                                                     @Valid @RequestBody StoreSaleDto.SaleReq dto) {
        // 판매 생성 요청 DTO를 검증하여 요청을 받음
        StoreSaleDto.SaleRes result = service.create(dto, me);
        return BaseResponse.success(result);
    }

    // 판매 내역 목록 조회
    @GetMapping
    public BaseResponse<List<StoreSaleDto.SaleListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword) {
        // 1. 요청 받기: 매장/기간/키워드 필터 파라미터를 받음
        // 2. 서비스 호출: 조건에 맞는 판매 목록을 조회
        List<StoreSaleDto.SaleListRes> result = service.findAll(me, from, to, keyword);
        // 3. 응답 반환: 공통 응답 포맷으로 감싸 반환
        return BaseResponse.success(result);
    }

    // 판매 내역 상세 조회
    @GetMapping("/{saleNo}")
    public BaseResponse<StoreSaleDto.SaleDetailRes> detail(@AuthenticationPrincipal AuthUserDetails me,
                                                           @PathVariable String saleNo) {
        // 1. 요청 받기: 판매번호(saleNo)를 경로 변수로 받음
        // 2. 서비스 호출: 판매 상세를 조회
        StoreSaleDto.SaleDetailRes result = service.findDetail(saleNo, me);
        // 3. 응답 반환: 공통 응답 포맷으로 감싸 반환
        return BaseResponse.success(result);
    }
}

