package org.example.stockitbe.hq.inventory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/circular-material-prices")
@RequiredArgsConstructor
@Tag(name = "순환재고 소재 단가", description = "순환재고 소재별 kg 단가 조회 및 수정 API")
// 순환재고 소재 단가 정책 컨트롤러
// 소재별 kg 단가 조회/수정 API를 제공한다.
public class CircularMaterialPriceController {

    private final InventoryService inventoryService;

    // 순환재고 소재 단가 목록 조회 API
    @Operation(
            summary = "순환재고 소재 단가 목록 조회",
            description = "순환재고 판매가 산정에 사용하는 소재별 kg 단가 정책을 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "소재 단가 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public BaseResponse<List<InventoryDto.CircularMaterialPriceRes>> getCircularMaterialPrices() {
        return BaseResponse.success(inventoryService.findCircularMaterialPrices());
    }

    // 순환재고 소재 단가 수정 API
    @Operation(
            summary = "순환재고 소재 단가 수정",
            description = "소재 코드에 해당하는 순환재고 kg 단가를 수정한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "소재 단가 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PutMapping("/{materialCode}")
    public BaseResponse<InventoryDto.CircularMaterialPriceRes> updateCircularMaterialPrice(
            @Parameter(description = "소재 코드", example = "POLYESTER")
            @PathVariable String materialCode,
            @RequestBody @Valid InventoryDto.CircularMaterialPriceUpdateReq request
    ) {
        return BaseResponse.success(inventoryService.updateCircularMaterialPrice(materialCode, request));
    }
}
