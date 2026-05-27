package org.example.stockitbe.hq.circularbuyer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hq/circular-buyers")
@RequiredArgsConstructor
@Tag(name = "순환 거래처", description = "순환재고 거래처 관리, 통계, 추천, 임베딩 백필 API")
public class CircularBuyerController {

    private final CircularBuyerService service;
    private final CircularBuyerRecommendService recommendService;

    @Operation(
            summary = "순환 거래처 목록 조회",
            description = "순환재고 거래처를 조건으로 조회한다. 페이지 없는 전체 조회는 최대 500건까지 반환된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래처 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "허용되지 않은 필터 값"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public BaseResponse<List<CircularBuyerDto.ListRes>> list(
            @Parameter(description = "거래처명, 설명, 주소 등 검색어", example = "리사이클")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "주요 적합 소재. 허용값: natural-single, synthetic, blended", example = "synthetic")
            @RequestParam(required = false) String materialFit,
            @Parameter(description = "거래처 유형. 허용값: local_small, social_enterprise, general", example = "general")
            @RequestParam(required = false) String partnerType) {
        return BaseResponse.success(service.findAll(keyword, materialFit, partnerType));
    }

    @Operation(
            summary = "순환 거래처 소재 적합도 통계",
            description = "주요 적합 소재별 순환 거래처 건수를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래처 통계 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/stats")
    public BaseResponse<Map<String, Long>> stats() {
        return BaseResponse.success(service.countByMaterialFit());
    }

    @Operation(
            summary = "순환 거래처 페이지 조회",
            description = "순환재고 거래처를 페이지 단위로 조회한다. size는 서비스에서 1~200 범위로 보정된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래처 페이지 조회 성공"),
            @ApiResponse(responseCode = "400", description = "허용되지 않은 필터 값"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/page")
    public BaseResponse<CircularBuyerDto.PageRes> page(
            @Parameter(description = "거래처명, 설명, 주소 등 검색어", example = "리사이클")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "주요 적합 소재. 허용값: natural-single, synthetic, blended", example = "synthetic")
            @RequestParam(required = false) String materialFit,
            @Parameter(description = "거래처 유형. 허용값: local_small, social_enterprise, general", example = "general")
            @RequestParam(required = false) String partnerType,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기. 최대 200", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return BaseResponse.success(service.findPage(keyword, materialFit, partnerType, PageRequest.of(safePage, safeSize)));
    }

    @Operation(
            summary = "순환 거래처 상세 조회",
            description = "거래처 코드로 순환 거래처 상세 정보를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래처 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "거래처 없음 또는 잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/{code}")
    public BaseResponse<CircularBuyerDto.DetailRes> detail(
            @Parameter(description = "순환 거래처 코드", example = "RCV-00001")
            @PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @Operation(
            summary = "순환 거래처 등록",
            description = "순환재고 거래처를 신규 등록한다. productTypes는 factoryProduct의 호환 alias로 입력 가능하다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래처 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    public BaseResponse<CircularBuyerDto.DetailRes> create(@Valid @RequestBody CircularBuyerDto.CreateReq req) {
        return BaseResponse.success(service.create(req));
    }

    @Operation(
            summary = "순환 거래처 수정",
            description = "거래처 코드에 해당하는 순환 거래처 정보를 수정한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래처 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/{code}")
    public BaseResponse<CircularBuyerDto.DetailRes> update(
            @Parameter(description = "순환 거래처 코드", example = "RCV-00001")
            @PathVariable String code,
            @Valid @RequestBody CircularBuyerDto.UpdateReq req) {
        return BaseResponse.success(service.update(code, req));
    }

    @Operation(
            summary = "순환 거래처 삭제",
            description = "거래처 코드에 해당하는 순환 거래처를 삭제한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래처 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "거래처 없음 또는 잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{code}")
    public BaseResponse<Void> delete(
            @Parameter(description = "순환 거래처 코드", example = "RCV-00001")
            @PathVariable String code) {
        service.delete(code);
        return BaseResponse.success(null);
    }

    /**
     * ADR-021 backfill — embedding == null 인 거래처들을 limit 건씩 나눠 remaining == 0 까지 임베딩.
     * 인증 가드는 ADR-011 미정으로 추후 추가.
     */
    @Operation(
            summary = "순환 거래처 임베딩 백필",
            description = "embedding이 없는 거래처를 지정한 배치 크기로 반복 처리한다. limit은 1~200, maxBatches는 1~10000 범위로 보정된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "임베딩 백필 실행 결과 반환"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/embeddings/backfill")
    public BaseResponse<Map<String, Object>> backfillEmbeddings(
            @Parameter(description = "1회 처리할 거래처 수. 최대 200", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            @Parameter(description = "최대 반복 배치 수. 최대 10000", example = "1000")
            @RequestParam(defaultValue = "1000") int maxBatches) {
        CircularBuyerEmbeddingService.BackfillRunResult result = service.backfillEmbeddings(limit, maxBatches);
        return BaseResponse.success(Map.of(
                "batches", (long) result.batches(),
                "processed", (long) result.processed(),
                "succeeded", (long) result.succeeded(),
                "failed", (long) result.failed(),
                "remaining", result.remaining(),
                "completed", result.remaining() == 0,
                "stopReason", result.stopReason()
        ));
    }

    /**
     * ADR-021 AI 거래처 추천 — 3층 RAG (SQL 룰 → 임베딩 코사인 → LLM 사유).
     * 판매 등록 페이지 Step 1 → Step 2 [다음] 클릭 1회 호출 (사용자 결정 2026-04-30).
     * LLM 호출 실패 시 200 OK + rationale fallback 텍스트 — 등록 흐름이 LLM 가용성에 묶이지 않음.
     */
    @Operation(
            summary = "순환 거래처 추천",
            description = "소재 적합도, 상품/설명 텍스트, 창고 위치를 바탕으로 순환재고 거래처를 추천한다. materialFit 허용값: natural-single, synthetic, blended."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래처 추천 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/recommend")
    public BaseResponse<CircularBuyerDto.RecommendRes> recommend(
            @Valid @RequestBody CircularBuyerDto.RecommendReq req) {
        return BaseResponse.success(recommendService.recommend(req));
    }
}
