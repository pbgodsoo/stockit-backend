package org.example.stockitbe.hq.infrastructure;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "매장/창고 정보 관리", description = "본사 매장/창고 정보 조회, 등록, 수정 API")
public class InfrastructureController {

    private final InfrastructureService service;

    @Operation(
            summary = "매장/창고 목록 조회",
            description = "매장/창고 정보를 유형, 키워드, 지역, 상태 조건으로 조회한다. type 허용값: STORE, WAREHOUSE. status 허용값: ACTIVE, INACTIVE, SUSPENDED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장/창고 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/infrastructures")
    public BaseResponse<List<InfrastructureDto.Res>> listInfrastructures(
            @Parameter(description = "거점 유형. 허용값: STORE, WAREHOUSE", example = "STORE")
            @RequestParam(required = false) LocationType type,
            @Parameter(description = "매장/창고명 검색어", example = "강남")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "지역명 또는 지역 코드. 예: 서울/SL, 경기/GG, 인천/IC, 부산/BS, 대전/DJ, 광주/GJ, 강원/GW, 제주/JJ, 충청/CN, 영남/YN, 호남/HN", example = "서울")
            @RequestParam(required = false) String region,
            @Parameter(description = "거점 상태. 허용값: ACTIVE, INACTIVE, SUSPENDED", example = "ACTIVE")
            @RequestParam(required = false) InfraStatus status) {
        return BaseResponse.success(service.findInfrastructures(type, keyword, region, status));
    }

    @Operation(
            summary = "매장/창고 상세 조회",
            description = "매장/창고 코드로 상세 정보를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장/창고 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "매장/창고 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/infrastructures/{code}")
    public BaseResponse<InfrastructureDto.Res> getInfrastructure(
            @Parameter(description = "매장/창고 코드", example = "ST-SL-0001")
            @PathVariable String code) {
        return BaseResponse.success(service.findInfrastructureByCode(code));
    }

    @Operation(
            summary = "매장/창고 등록",
            description = "매장 또는 창고 정보를 등록한다. 지역은 지원 지역명 또는 코드만 허용되며, 코드가 자동 생성된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장/창고 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/infrastructures")
    public BaseResponse<InfrastructureDto.Res> createInfrastructure(@Valid @RequestBody InfrastructureDto.UpsertReq req) {
        return BaseResponse.success(service.createInfrastructure(req));
    }

    @Operation(
            summary = "매장/창고 수정",
            description = "매장/창고 코드에 해당하는 정보를 수정한다. 기존 거점 유형과 요청 locationType이 다르면 실패한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장/창고 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/infrastructures/{code}")
    public BaseResponse<InfrastructureDto.Res> updateInfrastructure(
            @Parameter(description = "매장/창고 코드", example = "ST-SL-0001")
            @PathVariable String code,
            @Valid @RequestBody InfrastructureDto.UpsertReq req) {
        return BaseResponse.success(service.updateInfrastructure(code, req));
    }
}
