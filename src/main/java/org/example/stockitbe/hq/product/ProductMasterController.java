package org.example.stockitbe.hq.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.product.model.ProductDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq")
@RequiredArgsConstructor
@Tag(name = "제품 마스터 관리", description = "제품 마스터와 SKU 옵션 조회, 등록, 수정, 삭제 API")
public class ProductMasterController {

    private final ProductMasterService service;

    @Operation(
            summary = "제품 마스터 목록 조회",
            description = "제품명을 검색하거나 카테고리 코드로 필터링해 제품 마스터 목록을 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제품 마스터 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/products")
    public BaseResponse<List<ProductDto.ProductMasterRes>> listProducts(
            @Parameter(description = "제품명 검색어", example = "자켓")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "카테고리 코드", example = "CAT-L2-OUT-JK")
            @RequestParam(required = false) String categoryCode) {
        return BaseResponse.success(service.findProducts(keyword, categoryCode));
    }

    @Operation(
            summary = "제품 마스터 상세 조회",
            description = "제품 코드로 제품 마스터 상세 정보를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제품 마스터 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "제품 마스터 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/products/{code}")
    public BaseResponse<ProductDto.ProductMasterRes> detailProduct(
            @Parameter(description = "제품 코드", example = "PRD-OUT-JK-001")
            @PathVariable String code) {
        return BaseResponse.success(service.findProductByCode(code));
    }

    @Operation(
            summary = "제품 마스터 등록",
            description = "제품 마스터를 등록한다. 소재 타입 허용값: NATURAL_SINGLE, SYNTHETIC, BLEND. 제품 상태 허용값: ACTIVE, INACTIVE, SUSPENDED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제품 마스터 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/products")
    public BaseResponse<ProductDto.ProductMasterRes> createProduct(@Valid @RequestBody ProductDto.ProductMasterUpsertReq req) {
        return BaseResponse.success(service.createProduct(req));
    }

    @Operation(
            summary = "제품 마스터 수정",
            description = "제품 코드에 해당하는 제품 마스터 정보를 수정한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제품 마스터 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/products/{code}")
    public BaseResponse<ProductDto.ProductMasterRes> updateProduct(
            @Parameter(description = "제품 코드", example = "PRD-OUT-JK-001")
            @PathVariable String code,
            @Valid @RequestBody ProductDto.ProductMasterUpsertReq req) {
        return BaseResponse.success(service.updateProduct(code, req));
    }

    @Operation(
            summary = "제품 마스터 삭제",
            description = "제품 코드에 해당하는 제품 마스터와 하위 SKU를 삭제한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제품 마스터 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "제품 마스터 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/products/{code}")
    public BaseResponse<Void> deleteProduct(
            @Parameter(description = "제품 코드", example = "PRD-OUT-JK-001")
            @PathVariable String code) {
        service.deleteProduct(code);
        return BaseResponse.success(null);
    }

    @Operation(
            summary = "제품 SKU 목록 조회",
            description = "제품 코드에 해당하는 SKU 옵션 목록을 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "제품 마스터 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/products/{code}/skus")
    public BaseResponse<List<ProductDto.ProductSkuRes>> listSkus(
            @Parameter(description = "제품 코드", example = "PRD-OUT-JK-001")
            @PathVariable String code) {
        return BaseResponse.success(service.findSkus(code));
    }

    @Operation(
            summary = "제품 SKU 단건 등록",
            description = "제품에 SKU 옵션을 단건 등록한다. color 허용값: BLK, WHT, NVY, GRY. size 허용값: XS, S, M, L, XL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/products/{code}/skus")
    public BaseResponse<ProductDto.ProductSkuRes> createSku(
            @Parameter(description = "제품 코드", example = "PRD-OUT-JK-001")
            @PathVariable String code,
            @Valid @RequestBody ProductDto.ProductSkuUpsertReq req) {
        return BaseResponse.success(service.createSku(code, req));
    }

    @Operation(
            summary = "제품 SKU 벌크 등록",
            description = "색상과 사이즈 조합으로 제품 SKU를 벌크 등록한다. 중복 조합은 건너뛴다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 벌크 등록 결과 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/products/{code}/skus/bulk")
    public BaseResponse<ProductDto.ProductSkuBulkCreateRes> createSkusBulk(
            @Parameter(description = "제품 코드", example = "PRD-OUT-JK-001")
            @PathVariable String code,
            @Valid @RequestBody ProductDto.ProductSkuBulkCreateReq req) {
        return BaseResponse.success(service.bulkCreateSkus(code, req));
    }

    @Operation(
            summary = "제품 SKU 단건 수정",
            description = "SKU 코드에 해당하는 색상, 사이즈, 단가, 상태를 수정한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/skus/{skuCode}")
    public BaseResponse<ProductDto.ProductSkuRes> updateSku(
            @Parameter(description = "SKU 코드", example = "PRD-OUT-JK-001-BLK-M")
            @PathVariable String skuCode,
            @Valid @RequestBody ProductDto.ProductSkuUpsertReq req) {
        return BaseResponse.success(service.updateSku(skuCode, req));
    }

    @Operation(
            summary = "제품 SKU 전체 가격 수정",
            description = "제품 코드에 속한 모든 SKU의 단가를 일괄 수정한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 전체 가격 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/products/{code}/skus/price")
    public BaseResponse<ProductDto.ProductSkuPriceBulkUpdateRes> updateSkuPrices(
            @Parameter(description = "제품 코드", example = "PRD-OUT-JK-001")
            @PathVariable String code,
            @Valid @RequestBody ProductDto.ProductSkuPriceBulkUpdateReq req) {
        return BaseResponse.success(service.updateAllSkuPrices(code, req));
    }

    @Operation(
            summary = "제품 SKU 전체 상태 수정",
            description = "제품 코드에 속한 모든 SKU의 상태를 일괄 수정한다. 상태 허용값: ACTIVE, INACTIVE, SUSPENDED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 전체 상태 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/products/{code}/skus/status")
    public BaseResponse<ProductDto.ProductSkuStatusBulkUpdateRes> updateSkuStatus(
            @Parameter(description = "제품 코드", example = "PRD-OUT-JK-001")
            @PathVariable String code,
            @Valid @RequestBody ProductDto.ProductSkuStatusBulkUpdateReq req) {
        return BaseResponse.success(service.updateAllSkuStatus(code, req));
    }

    @Operation(
            summary = "제품 SKU 삭제",
            description = "SKU 코드에 해당하는 SKU 옵션을 삭제한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "SKU 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/skus/{skuCode}")
    public BaseResponse<Void> deleteSku(
            @Parameter(description = "SKU 코드", example = "PRD-OUT-JK-001-BLK-M")
            @PathVariable String skuCode) {
        service.deleteSku(skuCode);
        return BaseResponse.success(null);
    }
}
