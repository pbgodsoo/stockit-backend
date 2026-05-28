package org.example.stockitbe.hq.infrastructure;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.infrastructure.model.InfrastructureDto;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "회원가입용 거점 조회", description = "비로그인 상태에서 회원가입 페이지가 사용하는 매장/창고 공개 목록 API")
@RestController
@RequestMapping("/api/public/infrastructures")
@RequiredArgsConstructor
public class PublicInfrastructureController {

    private final InfrastructureService service;

    /** 회원가입 페이지 — 비로그인 상태에서 매장/창고 목록 제공 */
    @Operation(
            summary = "공개 매장/창고 목록 조회",
            description = "회원가입 화면에서 비로그인 사용자가 자기 소속 거점을 선택할 때 노출되는 활성 거점 목록. type/region 으로 필터링."
    )
    @GetMapping
    public BaseResponse<List<InfrastructureDto.PublicRes>> list(
            @Parameter(description = "거점 유형 필터. 허용값: STORE, WAREHOUSE", example = "WAREHOUSE")
            @RequestParam(required = false) LocationType type,
            @Parameter(description = "지역 한글명 또는 코드 필터. 예: 서울, 경기, 강원", example = "강원")
            @RequestParam(required = false) String region
    ) {
        return BaseResponse.success(service.findActiveForSignup(type, region));
    }
}
