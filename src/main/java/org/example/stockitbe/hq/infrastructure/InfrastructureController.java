package org.example.stockitbe.hq.infrastructure;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.infrastructure.model.InfraStatus;
import org.example.stockitbe.hq.infrastructure.model.InfrastructureDto;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq")
@RequiredArgsConstructor
public class InfrastructureController {

    private final InfrastructureService service;

    @GetMapping("/infrastructures")
    public BaseResponse<List<InfrastructureDto.Res>> listInfrastructures(@RequestParam(required = false) LocationType type,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) String region,
                                                                         @RequestParam(required = false) InfraStatus status) {
        return BaseResponse.success(service.findInfrastructures(type, keyword, region, status));
    }

    @GetMapping("/infrastructures/{code}")
    public BaseResponse<InfrastructureDto.Res> getInfrastructure(@PathVariable String code) {
        return BaseResponse.success(service.findInfrastructureByCode(code));
    }

    @PostMapping("/infrastructures")
    public BaseResponse<InfrastructureDto.Res> createInfrastructure(@Valid @RequestBody InfrastructureDto.UpsertReq req) {
        return BaseResponse.success(service.createInfrastructure(req));
    }

    @PatchMapping("/infrastructures/{code}")
    public BaseResponse<InfrastructureDto.Res> updateInfrastructure(@PathVariable String code,
                                                                    @Valid @RequestBody InfrastructureDto.UpsertReq req) {
        return BaseResponse.success(service.updateInfrastructure(code, req));
    }
}
