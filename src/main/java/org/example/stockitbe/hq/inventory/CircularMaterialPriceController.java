package org.example.stockitbe.hq.inventory;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/circular-material-prices")
@RequiredArgsConstructor
// 순환재고 소재 단가 정책 컨트롤러
// 소재별 kg 단가 조회/수정 API를 제공한다.
public class CircularMaterialPriceController {

    private final InventoryService inventoryService;

    // 순환재고 소재 단가 목록 조회 API
    @GetMapping
    public BaseResponse<List<InventoryDto.CircularMaterialPriceRes>> getCircularMaterialPrices() {
        return BaseResponse.success(inventoryService.findCircularMaterialPrices());
    }

    // 순환재고 소재 단가 수정 API
    @PutMapping("/{materialCode}")
    public BaseResponse<InventoryDto.CircularMaterialPriceRes> updateCircularMaterialPrice(
            @PathVariable String materialCode,
            @RequestBody @Valid InventoryDto.CircularMaterialPriceUpdateReq request
    ) {
        return BaseResponse.success(inventoryService.updateCircularMaterialPrice(materialCode, request));
    }
}
