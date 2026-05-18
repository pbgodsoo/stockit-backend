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
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.model.Material;
import org.example.stockitbe.hq.product.model.ProductMaterialComposition;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final double SIM_WEIGHT = 0.7;
    private static final double DIST_WEIGHT = 0.3;
    private static final double DIST_NORM_KM = 30.0;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final Pattern JSON_ARRAY = Pattern.compile("\\[.*]", Pattern.DOTALL);

    /**
     * 이슈 #218 — 소재 그룹별 자연어 일반론. 임베딩 input 에 추가하여 거래처의 처리 도메인 어휘
     * (단일 소재 회수 / 화학 재활용 / 혼방 분리 처리 등) 와 매칭 신호 강화.
     * material.description 의 단순 합으로 표현되지 않는 그룹 고유 특성(특히 혼방의 분리 어려움) 을 박는다.
     */
    private static final Map<String, String> GROUP_DESCRIPTION = Map.of(
            "natural-single",
            "천연 단일 섬유. 단일 소재로 회수 분류 깨끗. 셀룰로오스 단백질 펄프 솜 자연 원료 회수 재활용 적합. 자연 분해성 친환경 처리.",
            "synthetic",
            "합성 단일 섬유. 화학 재활용 가능 단일 소재 분류 처리. PET 페트병 원사 회수 순환 경제 핵심.",
            "blended",
            "혼방 직물. 단일 소재 한계 보완 시너지 활동복 일상복. 재활용 시 소재 분리가 어려워 혼방 전문 처리 거래처가 필요. 다중 소재 폐기물 화학 처리 분류 까다로움."
    );

    private final CircularBuyerRepository circularBuyerRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final ProductMasterRepository productMasterRepository;
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
        WarehousePoint warehousePoint = resolveWarehousePoint(req.getWarehouseCode());
        List<RankedBuyer> rankedTop = rankByScore(candidates, buildQueryText(req), warehousePoint);
        List<CircularBuyer> top = rankedTop.stream().map(RankedBuyer::buyer).toList();

        // 3층 — LLM 사유 생성 (실패 시 fallback)
        Map<String, String> rationales = generateRationales(top, req);
        Map<String, RankedBuyer> rankedByCode = rankedTop.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        r -> r.buyer().getCode(),
                        r -> r,
                        (left, right) -> left
                ));

        List<CircularBuyerDto.RecommendItem> items = top.stream()
                .map(b -> {
                    RankedBuyer rankedBuyer = rankedByCode.get(b.getCode());
                    Double distanceKm = rankedBuyer == null ? null : rankedBuyer.distanceKm();
                    return CircularBuyerDto.RecommendItem.builder()
                        .code(b.getCode())
                        .companyName(b.getCompanyName())
                        .primaryMaterialFit(b.getPrimaryMaterialFit())
                        .industryGroup(b.getIndustryGroup())
                        .partnerType(b.getPartnerType())
                        .factoryProduct(b.getFactoryProduct())
                        .managerName(b.getManagerName())
                        .phone(b.getPhone())
                        .address(b.getAddress())
                        .distanceKm(distanceKm)
                        .rationale(rationales.getOrDefault(b.getCode(), fallbackRationale()))
                        .build();
                })
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

    /**
     * 이슈 #218 — 임베딩 input 풍부화.
     * 1) 사용자 입력 4필드 (그룹 라벨 + 품목명 + 설명 + 수량힌트)
     * 2) productCode 가 박혔으면 ProductMaster → composition → Material join 으로 소재별 자연어 합침
     *    (혼방의 경우 두 번째 소재까지 정확 표현 — 면 70% 폴리 30% → 면 description + 폴리 description 둘 다)
     * 3) materialFit 그룹 일반론 — 단일/합성/혼방 도메인 어휘 (특히 혼방의 분리 어려움)
     *
     * productCode 미입력 또는 미존재 시 자유 텍스트 fallback. ReadOnly 트랜잭션 안에서 EntityGraph
     * 가 composition + material 한 번에 fetch.
     */
    private String buildQueryText(CircularBuyerDto.RecommendReq req) {
        StringBuilder sb = new StringBuilder();
        sb.append(materialFitLabel(req.getMaterialFit())).append(' ');
        sb.append(safe(req.getProductName())).append(' ');
        sb.append(safe(req.getDescription())).append(' ');
        sb.append(safe(req.getQuantityHint())).append(' ');

        appendProductMaterialContext(sb, req.getProductCode());

        String groupDesc = GROUP_DESCRIPTION.get(req.getMaterialFit());
        if (groupDesc != null) sb.append(groupDesc);

        return sb.toString().trim();
    }

    private void appendProductMaterialContext(StringBuilder sb, String productCode) {
        if (productCode == null || productCode.isBlank()) return;
        productMasterRepository.findByCode(productCode).ifPresent(pm -> {
            sb.append(safe(pm.getName())).append(' ');
            sb.append(safe(pm.getCategoryCode())).append(' ');
            for (ProductMaterialComposition comp : pm.getMaterialCompositions()) {
                Material material = comp.getMaterial();
                if (material == null) continue;
                sb.append(safe(material.getNameKo()))
                        .append(comp.getRatio() == null ? "" : (comp.getRatio() + "% "));
                if (material.getDescription() != null && !material.getDescription().isBlank()) {
                    sb.append(material.getDescription()).append(' ');
                }
            }
        });
    }

    private List<RankedBuyer> rankByScore(
            List<CircularBuyer> candidates,
            String queryText,
            WarehousePoint warehousePoint
    ) {
        float[] q;
        try {
            q = embeddingModel.embed(queryText);
        } catch (Exception e) {
            log.warn("쿼리 임베딩 생성 실패 — 코사인 정렬 없이 fallback 정렬(앞 N건). reason={}", e.getMessage());
            return candidates.stream()
                    .limit(TOP_K)
                    .map(b -> new RankedBuyer(b, null, 0.0))
                    .toList();
        }
        List<Double> qVec = toDoubleList(q);
        return candidates.stream()
                .map(buyer -> toRankedBuyer(buyer, qVec, warehousePoint))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(TOP_K)
                .toList();
    }

    private RankedBuyer toRankedBuyer(CircularBuyer buyer, List<Double> qVec, WarehousePoint warehousePoint) {
        double simScore = cosine(qVec, buyer.getEmbedding());
        boolean canUseDistance = warehousePoint != null
                && isValidLatLng(buyer.getLatitude(), buyer.getLongitude());
        if (!canUseDistance) {
            return new RankedBuyer(buyer, null, simScore);
        }
        double distanceKm = haversineKm(
                warehousePoint.latitude(), warehousePoint.longitude(),
                buyer.getLatitude(), buyer.getLongitude()
        );
        double distScore = 1.0 / (1.0 + (distanceKm / DIST_NORM_KM));
        double finalScore = (SIM_WEIGHT * simScore) + (DIST_WEIGHT * distScore);
        return new RankedBuyer(buyer, round2(distanceKm), finalScore);
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
        WarehousePoint wp = resolveWarehousePoint(req.getWarehouseCode());
        sb.append("너는 SPA 브랜드 본사 관리자에게 순환재고 처리 거래처를 추천하는 한국어 어시스턴트야.\n");
        sb.append("아래 재고 정보와 후보 거래처 ").append(top.size()).append("곳을 보고, ");
        sb.append("각 거래처가 왜 이 재고에 적합한지 충분히 자세하고 신뢰감 있는 사유를 작성해.\n\n");

        sb.append("[사유 작성 가이드]\n");
        sb.append("- 각 거래처당 5~6줄, 약 200~300자 분량으로 충분히 길게.\n");
        sb.append("- 다음 4가지 흐름을 자연스럽게 한 문단에 녹여서 작성:\n");
        sb.append("  1) 거래처의 핵심 처리 영역과 전문성 (어떤 소재를 어떤 방식으로 처리하는 회사인지)\n");
        sb.append("  2) 요청 재고와 매칭되는 구체적 이유 (소재 일치·처리 방식 적합성·산업군 부합)\n");
        sb.append("  3) 처리 후 재활용 경로 (어떤 산업·제품으로 환생되는지 — 자동차 흡음재·건설 단열재·재생 원사·펄프·매트리스 충전 등)\n");
        sb.append("  4) 친환경·순환경제 가치 (ESG 효과·탄소 감축·자원 순환)\n");
        sb.append("- 거래처 description·주소·취급 품목에 등장하는 구체 어휘를 적극 활용해 사실 기반으로 작성.\n");
        sb.append("- '적합한 거래처입니다' 같은 추상적 칭찬·불필요한 인사말·서론 금지 — 바로 본론.\n");
        sb.append("- 한국어 자연어 매끄러운 문장, 각 문장은 마침표로 종료.\n");
        sb.append("- JSON 안에 줄바꿈(\\n) 넣지 말고 한 문단으로 이어 써 (UI 가 자연스럽게 줄바꿈 처리).\n\n");

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
            if (b.getFactoryProduct() != null && !b.getFactoryProduct().isEmpty()) {
                sb.append(" / 취급=").append(String.join(",", b.getFactoryProduct()));
            }
            if (b.getAddress() != null) sb.append(" / 주소=").append(b.getAddress());
            if (isValidLatLng(b.getLatitude(), b.getLongitude()) && wp != null) {
                double d = haversineKm(wp.latitude(), wp.longitude(), b.getLatitude(), b.getLongitude());
                sb.append(" / 거리=").append(round2(d)).append("km");
            } else {
                sb.append(" / 거리=거리 정보 없음");
            }
            if (b.getDescription() != null) sb.append(" / 설명=").append(b.getDescription());
            sb.append("\n");
        }

        sb.append("\n반드시 아래 JSON 배열 형식으로만 답해 (다른 텍스트·코드블록·마크다운 절대 금지):\n");
        sb.append("[\n");
        for (int i = 0; i < top.size(); i++) {
            sb.append("  {\"code\": \"").append(top.get(i).getCode())
                    .append("\", \"rationale\": \"<5~6줄 200~300자 사유 한 문단>\"}");
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

    private WarehousePoint resolveWarehousePoint(String warehouseCode) {
        if (warehouseCode == null || warehouseCode.isBlank()) return null;
        Optional<Infrastructure> infraOpt = infrastructureRepository.findByCode(warehouseCode.trim());
        if (infraOpt.isEmpty()) return null;
        Infrastructure infra = infraOpt.get();
        if (!isValidLatLng(infra.getLatitude(), infra.getLongitude())) return null;
        return new WarehousePoint(infra.getLatitude(), infra.getLongitude());
    }

    private static boolean isValidLatLng(Double lat, Double lng) {
        if (lat == null || lng == null) return false;
        return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record WarehousePoint(double latitude, double longitude) {}
    private record RankedBuyer(CircularBuyer buyer, Double distanceKm, double score) {}
}
