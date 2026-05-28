package org.example.stockitbe.hq.circularsale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.circularsale.model.dto.CircularSaleDto;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "순환재고 판매", description = "순환재고 판매 생성 및 판매 내역 조회 API (HQ 전용)")
@RestController
@RequestMapping("/api/hq/circular-inventory/sales")
@RequiredArgsConstructor
public class CircularSaleController {

    private final CircularSaleService circularSaleService;

    @Operation(summary = "순환재고 판매 생성", description = "거래처·소재구분·SKU 라인 정보로 순환재고 판매를 생성하고 출고를 동시에 처리한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "판매 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 재고 부족")
    })
    @PostMapping
    public BaseResponse<CircularSaleDto.CreateRes> create(
            @AuthenticationPrincipal AuthUserDetails me,
            @Valid @RequestBody CircularSaleDto.CreateReq request
    ) {
        CircularSaleDto.CreateRes result = circularSaleService.create(request, me);
        return BaseResponse.success(result);
    }

    @Operation(summary = "순환재고 판매 목록 조회", description = "판매 이력을 기간·거래처·소재구분·키워드로 필터링해 페이지 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public BaseResponse<CircularSaleDto.ListPageRes> list(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "정렬 조건 (필드,방향)", example = "soldAt,desc") @RequestParam(defaultValue = "soldAt,desc") String sort,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-05-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-05-27") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "거래처 코드 필터", example = "RCV-00001") @RequestParam(required = false) String buyerCode,
            @Parameter(description = "소재구분 필터", example = "POLYESTER") @RequestParam(required = false) String materialType,
            @Parameter(description = "판매번호·거래처명 검색 키워드", example = "그린리사이클") @RequestParam(required = false) String keyword
    ) {
        CircularSaleDto.ListPageRes result = circularSaleService.list(page, size, sort, from, to, buyerCode, materialType, keyword);
        return BaseResponse.success(result);
    }

    @Operation(summary = "순환재고 판매 상세 조회", description = "판매 ID로 헤더·SKU 라인·소재 스냅샷·상태이력을 포함한 상세를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "판매 없음")
    })
    @GetMapping("/{saleId}")
    public BaseResponse<CircularSaleDto.DetailRes> detail(
            @Parameter(description = "판매 ID", example = "1") @PathVariable Long saleId) {
        CircularSaleDto.DetailRes result = circularSaleService.detail(saleId);
        return BaseResponse.success(result);
    }
}
