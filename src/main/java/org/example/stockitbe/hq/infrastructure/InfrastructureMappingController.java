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
import org.example.stockitbe.hq.infrastructure.model.InfrastructureMappingDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/infrastructure-mappings")
@RequiredArgsConstructor
@Tag(name = "매장-창고 매핑 관리", description = "매장별 주/예비 창고 매핑 조회, 저장, 옵션 조회 API")
public class InfrastructureMappingController {

    private final InfrastructureMappingService service;

    @Operation(
            summary = "매장별 창고 매핑 목록 조회",
            description = "매장별 주 창고와 예비 창고 매핑 정보를 조회한다. status 허용값: ACTIVE, INACTIVE, SUSPENDED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장별 창고 매핑 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/stores")
    public BaseResponse<List<InfrastructureMappingDto.StoreMappingItem>> getStoreMappings(
            @Parameter(description = "매장명 검색어", example = "강남")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "지역명 또는 지역 코드. 예: 서울/SL, 경기/GG, 인천/IC, 부산/BS, 대전/DJ, 광주/GJ, 강원/GW, 제주/JJ, 충청/CN, 영남/YN, 호남/HN", example = "서울")
            @RequestParam(required = false) String region,
            @Parameter(description = "매장 상태. 허용값: ACTIVE, INACTIVE, SUSPENDED", example = "ACTIVE")
            @RequestParam(required = false) InfraStatus status
    ) {
        return BaseResponse.success(service.getStoreMappings(keyword, region, status));
    }

    @Operation(
            summary = "매장 창고 매핑 저장",
            description = "매장에 주 창고와 선택적 예비 창고를 저장한다. 주 창고는 필수이며, 주/예비 창고는 같을 수 없다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장 창고 매핑 저장 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PutMapping("/stores/{storeCode}")
    public BaseResponse<InfrastructureMappingDto.StoreMappingItem> saveStoreMappings(
            @Parameter(description = "매장 코드", example = "ST-SL-0001")
            @PathVariable String storeCode,
            @Valid @RequestBody InfrastructureMappingDto.UpsertReq req
    ) {
        return BaseResponse.success(service.saveStoreMappings(storeCode, req));
    }

    @Operation(
            summary = "매핑용 매장/창고 옵션 조회",
            description = "매장-창고 매핑 화면에서 선택 가능한 매장과 창고 옵션 목록을 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매핑 옵션 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/options")
    public BaseResponse<InfrastructureMappingDto.OptionsRes> getOptions() {
        return BaseResponse.success(service.getOptions());
    }
}
