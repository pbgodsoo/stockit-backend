package org.example.stockitbe.hq.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.category.model.CategoryDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/categories")
@RequiredArgsConstructor
@Tag(name = "카테고리 관리", description = "제품 카테고리 트리 조회, 상세 조회, 등록, 수정, 삭제 API")
public class CategoryController {

    private final CategoryService service;

    @Operation(
            summary = "카테고리 트리 조회",
            description = "루트/하위 카테고리를 트리 구조로 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리 트리 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public BaseResponse<List<CategoryDto.TreeRes>> list() {
        return BaseResponse.success(service.findAllTree());
    }

    @Operation(
            summary = "카테고리 상세 조회",
            description = "카테고리 코드로 카테고리 상세 정보를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "카테고리 없음 또는 잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/{code}")
    public BaseResponse<CategoryDto.DetailRes> detail(
            @Parameter(description = "카테고리 코드", example = "CAT-L2-TOP-SS")
            @PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @Operation(
            summary = "카테고리 등록",
            description = "카테고리를 등록한다. level 허용값: ROOT, CHILD. status 허용값: ACTIVE, SUSPENDED, INACTIVE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    public BaseResponse<CategoryDto.DetailRes> create(@Valid @RequestBody CategoryDto.CreateReq req) {
        return BaseResponse.success(service.create(req));
    }

    @Operation(
            summary = "카테고리 수정",
            description = "카테고리명과 상태를 수정한다. status 허용값: ACTIVE, SUSPENDED, INACTIVE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/{code}")
    public BaseResponse<CategoryDto.DetailRes> update(
            @Parameter(description = "카테고리 코드", example = "CAT-L2-TOP-SS")
            @PathVariable String code,
            @Valid @RequestBody CategoryDto.UpdateReq req) {
        return BaseResponse.success(service.update(code, req));
    }

    @Operation(
            summary = "카테고리 삭제",
            description = "카테고리 코드로 카테고리를 삭제한다. 하위 카테고리가 있는 루트 카테고리는 삭제할 수 없다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "삭제 불가 또는 카테고리 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{code}")
    public BaseResponse<Void> delete(
            @Parameter(description = "카테고리 코드", example = "CAT-L2-TOP-SS")
            @PathVariable String code) {
        service.delete(code);
        return BaseResponse.success(null);
    }
}
