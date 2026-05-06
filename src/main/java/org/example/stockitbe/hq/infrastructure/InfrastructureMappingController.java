package org.example.stockitbe.hq.infrastructure;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.infrastructure.model.InfraStatus;
import org.example.stockitbe.hq.infrastructure.model.InfrastructureMappingDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/infrastructure-mappings")
@RequiredArgsConstructor
public class InfrastructureMappingController {

    private final InfrastructureMappingService service;

    @GetMapping("/stores")
    public BaseResponse<List<InfrastructureMappingDto.StoreMappingItem>> getStoreMappings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) InfraStatus status
    ) {
        return BaseResponse.success(service.getStoreMappings(keyword, region, status));
    }

    @PutMapping("/stores/{storeCode}")
    public BaseResponse<InfrastructureMappingDto.StoreMappingItem> saveStoreMappings(
            @PathVariable String storeCode,
            @Valid @RequestBody InfrastructureMappingDto.UpsertReq req
    ) {
        return BaseResponse.success(service.saveStoreMappings(storeCode, req));
    }

    @GetMapping("/options")
    public BaseResponse<InfrastructureMappingDto.OptionsRes> getOptions() {
        return BaseResponse.success(service.getOptions());
    }
}
