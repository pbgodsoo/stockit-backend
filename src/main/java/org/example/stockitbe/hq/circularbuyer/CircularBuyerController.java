package org.example.stockitbe.hq.circularbuyer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hq/circular-buyers")
@RequiredArgsConstructor
public class CircularBuyerController {

    private final CircularBuyerService service;
    private final CircularBuyerRecommendService recommendService;

    @GetMapping
    public BaseResponse<List<CircularBuyerDto.ListRes>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String materialFit) {
        return BaseResponse.success(service.findAll(keyword, materialFit));
    }

    @GetMapping("/{code}")
    public BaseResponse<CircularBuyerDto.DetailRes> detail(@PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @PostMapping
    public BaseResponse<CircularBuyerDto.DetailRes> create(@Valid @RequestBody CircularBuyerDto.CreateReq req) {
        return BaseResponse.success(service.create(req));
    }

    @PatchMapping("/{code}")
    public BaseResponse<CircularBuyerDto.DetailRes> update(@PathVariable String code,
                                                             @Valid @RequestBody CircularBuyerDto.UpdateReq req) {
        return BaseResponse.success(service.update(code, req));
    }

    @DeleteMapping("/{code}")
    public BaseResponse<Void> delete(@PathVariable String code) {
        service.delete(code);
        return BaseResponse.success(null);
    }

    /**
     * ADR-021 시드 backfill — embedding == null 인 거래처들을 일괄 임베딩.
     * 인증 가드는 ADR-011 미정으로 추후 추가.
     */
    @PostMapping("/embeddings/backfill")
    public BaseResponse<Map<String, Integer>> backfillEmbeddings() {
        int processed = service.backfillEmbeddings();
        return BaseResponse.success(Map.of("processed", processed));
    }

    /**
     * ADR-021 AI 거래처 추천 — 3층 RAG (SQL 룰 → 임베딩 코사인 → LLM 사유).
     * 판매 등록 페이지 Step 1 → Step 2 [다음] 클릭 1회 호출 (사용자 결정 2026-04-30).
     * LLM 호출 실패 시 200 OK + rationale fallback 텍스트 — 등록 흐름이 LLM 가용성에 묶이지 않음.
     */
    @PostMapping("/recommend")
    public BaseResponse<CircularBuyerDto.RecommendRes> recommend(
            @Valid @RequestBody CircularBuyerDto.RecommendReq req) {
        return BaseResponse.success(recommendService.recommend(req));
    }
}
