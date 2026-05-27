package org.example.stockitbe.hq.warehousetransfer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferDto;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hq/warehouse-transfers")
@RequiredArgsConstructor
@Tag(name = "창고간 재고이동", description = "창고간 재고이동 실행, 조회, 불균형 SKU 및 SKU 창고별 분포 API")
public class WarehouseTransferController {

    private final InventoryService inventoryService;
    private final WarehouseTransferService warehouseTransferService;

    @Operation(
            summary = "창고별 재고 불균형 SKU 조회",
            description = "창고 간 가용재고 편차가 있는 SKU를 조회해 재고이동 후보를 확인한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "불균형 SKU 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/imbalanced-skus")
    public BaseResponse<List<InventoryDto.ImbalancedSkuRes>> getImbalancedSkus() {
        return BaseResponse.success(inventoryService.findImbalancedSkus());
    }

    @Operation(
            summary = "창고간 재고이동 실행",
            description = "요청 라인을 출발/도착 창고 라우트별로 묶어 재고이동 지시와 물류 출고를 생성한다. 라우트 단위 부분 성공을 지원한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재고이동 실행 결과 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/execute")
    public BaseResponse<WarehouseTransferDto.ExecuteRes> execute(
            @RequestBody @Valid WarehouseTransferDto.ExecuteReq request
    ) {
        return BaseResponse.success(warehouseTransferService.execute(request));
    }

    @Operation(
            summary = "창고간 재고이동 내역 목록 조회",
            description = "상태, 요청일 기간, 키워드로 창고간 재고이동 내역을 조회한다. 상태 허용값: READY_TO_SHIP, IN_TRANSIT, ARRIVED. 기간 미지정 시 최근 1개월부터 오늘까지 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재고이동 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public BaseResponse<List<WarehouseTransferDto.TransferListItemRes>> getTransfers(
            @Parameter(description = "재고이동 상태. 허용값: READY_TO_SHIP, IN_TRANSIT, ARRIVED", example = "READY_TO_SHIP")
            @RequestParam(required = false) WarehouseTransferStatus status,
            @Parameter(description = "조회 시작일. 미지정 시 오늘 기준 1개월 전", example = "2026-05-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "조회 종료일. 미지정 시 오늘", example = "2026-05-27")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "이동번호, 창고, SKU, 품목명, 사유, 메모 검색어", example = "WTF-20260527")
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(warehouseTransferService.findTransfers(status, fromDate, toDate, keyword));
    }

    @Operation(
            summary = "창고간 재고이동 상세 조회",
            description = "이동번호로 창고간 재고이동 상세와 라인별 재고 스냅샷을 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재고이동 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "이동번호에 해당하는 데이터 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/{transferNo}")
    public BaseResponse<WarehouseTransferDto.TransferDetailRes> getTransferDetail(
            @Parameter(description = "창고간 재고이동 번호", example = "WTF-20260527-00001")
            @PathVariable String transferNo) {
        return BaseResponse.success(warehouseTransferService.findTransferDetail(transferNo));
    }

    @Operation(
            summary = "SKU별 창고 재고 분포 조회",
            description = "SKU 코드 기준으로 전체 창고의 실재고, 예약재고, 가용재고, 안전재고와 상태를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 창고별 재고 분포 조회 성공"),
            @ApiResponse(responseCode = "400", description = "SKU 또는 상품 데이터 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/sku-distribution")
    public BaseResponse<List<WarehouseTransferDto.WarehouseSkuDistributionRes>> getSkuDistribution(
            @Parameter(description = "SKU 코드", example = "SKU-TOP-SS-001-BLK-M", required = true)
            @RequestParam String skuCode
    ) {
        return BaseResponse.success(warehouseTransferService.findSkuDistribution(skuCode));
    }
}
