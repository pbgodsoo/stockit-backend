package org.example.stockitbe.hq.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.infrastructure.model.InfrastructureDto;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/infrastructures")
@RequiredArgsConstructor
public class PublicInfrastructureController {

    private final InfrastructureService service;

    /** 회원가입 페이지 — 비로그인 상태에서 매장/창고 목록 제공 */
    @GetMapping
    public BaseResponse<List<InfrastructureDto.PublicRes>> list(
            @RequestParam(required = false) LocationType type,
            @RequestParam(required = false) String region
    ) {
        return BaseResponse.success(service.findActiveForSignup(type, region));
    }
}
