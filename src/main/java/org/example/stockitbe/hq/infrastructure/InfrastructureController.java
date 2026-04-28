package org.example.stockitbe.hq.infrastructure;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.infrastructure.model.InfraStatus;
import org.example.stockitbe.hq.infrastructure.model.InfrastructureDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq")
@RequiredArgsConstructor
public class InfrastructureController {

    private final InfrastructureService service;

    @GetMapping("/stores")
    public BaseResponse<List<InfrastructureDto.StoreRes>> listStores(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) String region,
                                                                     @RequestParam(required = false) InfraStatus status) {
        return BaseResponse.success(service.findStores(keyword, region, status));
    }

    @PostMapping("/stores")
    public BaseResponse<InfrastructureDto.StoreRes> createStore(@Valid @RequestBody InfrastructureDto.StoreUpsertReq req) {
        return BaseResponse.success(service.createStore(req));
    }

    @PatchMapping("/stores/{code}")
    public BaseResponse<InfrastructureDto.StoreRes> updateStore(@PathVariable String code,
                                                                 @Valid @RequestBody InfrastructureDto.StoreUpsertReq req) {
        return BaseResponse.success(service.updateStore(code, req));
    }

    @GetMapping("/warehouses")
    public BaseResponse<List<InfrastructureDto.WarehouseRes>> listWarehouses(@RequestParam(required = false) String keyword,
                                                                             @RequestParam(required = false) String region,
                                                                             @RequestParam(required = false) InfraStatus status) {
        return BaseResponse.success(service.findWarehouses(keyword, region, status));
    }

    @PostMapping("/warehouses")
    public BaseResponse<InfrastructureDto.WarehouseRes> createWarehouse(@Valid @RequestBody InfrastructureDto.WarehouseUpsertReq req) {
        return BaseResponse.success(service.createWarehouse(req));
    }

    @PatchMapping("/warehouses/{code}")
    public BaseResponse<InfrastructureDto.WarehouseRes> updateWarehouse(@PathVariable String code,
                                                                         @Valid @RequestBody InfrastructureDto.WarehouseUpsertReq req) {
        return BaseResponse.success(service.updateWarehouse(code, req));
    }
}
