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
public class CircularMaterialPriceController {

    private final InventoryService inventoryService;

    @GetMapping
    public BaseResponse<List<InventoryDto.CircularMaterialPriceRes>> getCircularMaterialPrices() {
        return BaseResponse.success(inventoryService.findCircularMaterialPrices());
    }

    @PutMapping("/{materialCode}")
    public BaseResponse<InventoryDto.CircularMaterialPriceRes> updateCircularMaterialPrice(
            @PathVariable String materialCode,
            @RequestBody @Valid InventoryDto.CircularMaterialPriceUpdateReq request
    ) {
        return BaseResponse.success(inventoryService.updateCircularMaterialPrice(materialCode, request));
    }
}
