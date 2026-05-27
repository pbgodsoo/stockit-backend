package org.example.stockitbe.store.sale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "매장 판매", description = "매장 판매 생성 및 판매 내역 조회 API")
@RestController
@RequestMapping("/api/store/sales")
@RequiredArgsConstructor
public class StoreSaleController {

    private final StoreSaleService service;

    @Operation(summary = "판매 생성", description = "매장에서 SKU별 판매 처리를 수행하고 재고를 차감한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "판매 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 재고 부족")
    })
    @PostMapping
    public BaseResponse<StoreSaleDto.SaleRes> create(@AuthenticationPrincipal AuthUserDetails me,
                                                     @Valid @RequestBody StoreSaleDto.SaleReq dto) {
        StoreSaleDto.SaleRes result = service.create(dto, me);
        return BaseResponse.success(result);
    }

    @Operation(summary = "판매 목록 조회", description = "로그인 매장의 판매 내역을 기간·키워드로 필터링해 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public BaseResponse<List<StoreSaleDto.SaleListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2024-01-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2024-12-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "판매번호·상품명 검색 키워드") @RequestParam(required = false) String keyword) {
        List<StoreSaleDto.SaleListRes> result = service.findAll(me, from, to, keyword);
        return BaseResponse.success(result);
    }

    @Operation(summary = "판매 상세 조회", description = "판매번호로 헤더·SKU 라인 상세 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "판매 없음")
    })
    @GetMapping("/{saleNo}")
    public BaseResponse<StoreSaleDto.SaleDetailRes> detail(@AuthenticationPrincipal AuthUserDetails me,
                                                           @Parameter(description = "판매번호", example = "SAL-20240101-001") @PathVariable String saleNo) {
        StoreSaleDto.SaleDetailRes result = service.findDetail(saleNo, me);
        return BaseResponse.success(result);
    }
}
