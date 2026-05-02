package org.example.stockitbe.hq.circularbuyer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ADR-021 AI 거래처 추천 — 3층 RAG 파이프라인.
 *  1층: primary_material_fit 단일 SQL 룰 (Phase 1 선제 추상화 차단 — status/minOrderQty 등 추가 룰 X).
 *  2층: 쿼리 임베딩 + 후보 embedding 인메모리 코사인 유사도 → top 5.
 *  3층: 단일 ChatModel 호출로 5개 사유 한꺼번에 생성. 실패 시 fallback 텍스트.
 *
 * 비용 — 추천 1회 = 임베딩 1콜 + chat 1콜 ≈ 1~2원, 1.5~2.5초 (CLAUDE.md 명시 budget).
 * Vector DB / VectorStore 추상화 미사용 (Phase 1 제약 — Phase 2 ES 전환 시점에 도입).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CircularBuyerRecommendService {

    private static final Set<String> ALLOWED_MATERIAL_FITS = Set.of(
            "natural-single", "synthetic", "blended"
    );
    private static final int TOP_K = 5;
    private static final Pattern JSON_ARRAY = Pattern.compile("\\[.*]", Pattern.DOTALL);

    private final CircularBuyerRepository circularBuyerRepository;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public CircularBuyerDto.RecommendRes recommend(CircularBuyerDto.RecommendReq req) {
        // 1층 — SQL 룰 필터
        validateMaterialFit(req.getMaterialFit());
        List<CircularBuyer> candidates = circularBuyerRepository.findAll(
                materialFitEqualSpec(req.getMaterialFit())
        );
        if (candidates.isEmpty()) {
            return CircularBuyerDto.RecommendRes.builder()
                    .recommendations(List.of())
                    .build();
        }

        // 2층 — 쿼리 임베딩 + 코사인 top 5
        List<CircularBuyer> top = rankByCosine(candidates, buildQueryText(req));

        // 3층 — LLM 사유 생성 (실패 시 fallback)
        Map<String, String> rationales = generateRationales(top, req);

        List<CircularBuyerDto.RecommendItem> items = top.stream()
                .map(b -> CircularBuyerDto.RecommendItem.builder()
                        .code(b.getCode())
                        .companyName(b.getCompanyName())
                        .primaryMaterialFit(b.getPrimaryMaterialFit())
                        .industryGroup(b.getIndustryGroup())
                        .rationale(rationales.getOrDefault(b.getCode(), fallbackRationale()))
                        .build())
                .toList();

        return CircularBuyerDto.RecommendRes.builder()
                .recommendations(items)
                .build();
    }

    // ─── 1층 ─────────────────────────────────────────────────────────────────

    private void validateMaterialFit(String materialFit) {
        if (materialFit == null || !ALLOWED_MATERIAL_FITS.contains(materialFit)) {
            throw BaseException.from(BaseResponseStatus.INVALID_MATERIAL_FIT);
        }
    }

    private Specification<CircularBuyer> materialFitEqualSpec(String materialFit) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get("primaryMaterialFit"), materialFit);
            return cb.and(p);
        };
    }

    // ─── 2층 ─────────────────────────────────────────────────────────────────

    private String buildQueryText(CircularBuyerDto.RecommendReq req) {
        return String.join(" ",
                materialFitLabel(req.getMaterialFit()),
                safe(req.getProductName()),
                safe(req.getDescription()),
                safe(req.getQuantityHint())
        ).trim();
    }

    private List<CircularBuyer> rankByCosine(List<CircularBuyer> candidates, String queryText) {
        float[] q;
        try {
            q = embeddingModel.embed(queryText);
        } catch (Exception e) {
            log.warn("쿼리 임베딩 생성 실패 — 코사인 정렬 없이 fallback 정렬(앞 N건). reason={}", e.getMessage());
            return candidates.stream().limit(TOP_K).toList();
        }
        List<Double> qVec = toDoubleList(q);
        return candidates.stream()
                .map(b -> Map.entry(b, cosine(qVec, b.getEmbedding())))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(TOP_K)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
            return -1.0;
        }
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double av = a.get(i);
            double bv = b.get(i);
            dot += av * bv;
            na += av * av;
            nb += bv * bv;
        }
        if (na == 0.0 || nb == 0.0) return -1.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // ─── 3층 ─────────────────────────────────────────────────────────────────

    /**
     * 단일 ChatModel 호출로 5개 사유 한꺼번에 받기. 실패 시 빈 Map 반환 → 호출자가 fallback 텍스트 박음.
     * 응답 형식 강제 — JSON 배열 [{"code":..., "rationale":...}, ...] 만 허용.
     */
    private Map<String, String> generateRationales(List<CircularBuyer> top, CircularBuyerDto.RecommendReq req) {
        if (top.isEmpty()) return Map.of();
        try {
            String prompt = buildPrompt(top, req);
            String response = chatModel.call(prompt);
            String json = extractJsonArray(response);
            if (json == null) {
                log.warn("LLM 응답에서 JSON 배열 추출 실패. raw={}", response);
                return Map.of();
            }
            List<Map<String, String>> parsed = objectMapper.readValue(
                    json, new TypeReference<List<Map<String, String>>>() {}
            );
            Map<String, String> result = new HashMap<>();
            for (Map<String, String> entry : parsed) {
                String code = entry.get("code");
                String rationale = entry.get("rationale");
                if (code != null && rationale != null) {
                    result.put(code, rationale);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("LLM rationale generation failed — fallback 텍스트 사용. reason={}", e.getMessage());
            return Map.of();
        }
    }

    private String buildPrompt(List<CircularBuyer> top, CircularBuyerDto.RecommendReq req) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 SPA 브랜드의 순환재고를 처리할 거래처를 추천하는 한국어 어시스턴트야.\n");
        sb.append("아래 재고 정보와 후보 거래처를 보고, 각 후보가 왜 이 재고에 적합한지 1~2문장 사유를 만들어.\n\n");
        sb.append("[요청 재고]\n");
        sb.append("- 소재 구분: ").append(materialFitLabel(req.getMaterialFit())).append("\n");
        if (req.getProductName() != null) sb.append("- 품목: ").append(req.getProductName()).append("\n");
        if (req.getDescription() != null) sb.append("- 설명: ").append(req.getDescription()).append("\n");
        if (req.getQuantityHint() != null) sb.append("- 수량 힌트: ").append(req.getQuantityHint()).append("\n");
        sb.append("\n[후보 거래처 ").append(top.size()).append("곳]\n");
        for (int i = 0; i < top.size(); i++) {
            CircularBuyer b = top.get(i);
            sb.append(i + 1).append(". code=").append(b.getCode())
                    .append(" / 회사=").append(b.getCompanyName())
                    .append(" / 산업군=").append(safe(b.getIndustryGroup()));
            if (b.getProductTypes() != null && !b.getProductTypes().isEmpty()) {
                sb.append(" / 취급=").append(String.join(",", b.getProductTypes()));
            }
            if (b.getProductNote() != null) sb.append(" / 메모=").append(b.getProductNote());
            if (b.getDescription() != null) sb.append(" / 설명=").append(b.getDescription());
            sb.append("\n");
        }
        sb.append("\n반드시 아래 JSON 배열 형식으로만 답해 (다른 텍스트 절대 금지):\n");
        sb.append("[\n");
        for (int i = 0; i < top.size(); i++) {
            sb.append("  {\"code\": \"").append(top.get(i).getCode()).append("\", \"rationale\": \"...\"}");
            if (i < top.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String extractJsonArray(String response) {
        if (response == null) return null;
        Matcher m = JSON_ARRAY.matcher(response);
        return m.find() ? m.group() : null;
    }

    private static String fallbackRationale() {
        return "AI 사유 생성을 일시적으로 사용할 수 없습니다.";
    }

    // ─── 공통 헬퍼 ──────────────────────────────────────────────────────────

    private static String materialFitLabel(String fit) {
        if (fit == null) return "";
        return switch (fit) {
            case "natural-single" -> "천연 단일 섬유";
            case "synthetic" -> "합성 섬유";
            case "blended" -> "혼방";
            default -> fit;
        };
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static List<Double> toDoubleList(float[] arr) {
        List<Double> out = new ArrayList<>(arr.length);
        for (float f : arr) {
            out.add((double) f);
        }
        return out;
    }
}
