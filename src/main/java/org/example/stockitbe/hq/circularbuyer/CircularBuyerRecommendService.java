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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ADR-021 AI 거래처 추천 — 3층 RAG 파이프라인.
 *  1층: primary_material_fit 단일 룰 필터.
 *  2층: 쿼리 임베딩 + Elasticsearch dense_vector kNN → 거리 가중치 재정렬 → top 5.
 *  3층: 단일 ChatModel 호출로 5개 사유 한꺼번에 생성. 실패 시 fallback 텍스트.
 *
 * ES/임베딩 장애 시 기존 RDB cosine 또는 앞 N건 fallback 으로 등록 흐름을 보호한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CircularBuyerRecommendService {

    private static final Set<String> ALLOWED_MATERIAL_FITS = Set.of(
            "natural-single", "synthetic", "blended"
    );
    private static final int TOP_K = 5;
    private static final int ES_CANDIDATE_K = 50;
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
    private final CircularBuyerRecommendSearchService recommendSearchService;
    private final InfrastructureRepository infrastructureRepository;
    private final ProductMasterRepository productMasterRepository;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${stockit.circular-buyer.ai.embedding-timeout-ms:15000}")
    private long embeddingTimeoutMs;

    @Value("${stockit.circular-buyer.ai.rationale-timeout-ms:60000}")
    private long rationaleTimeoutMs;

    public CircularBuyerDto.RecommendRes recommend(CircularBuyerDto.RecommendReq req) {
        long totalStart = System.nanoTime();
        validateMaterialFit(req.getMaterialFit());
        log.info("CircularBuyer recommend start materialFit={} warehouseCode={}",
                req.getMaterialFit(), req.getWarehouseCode());

        WarehousePoint warehousePoint = resolveWarehousePoint(req.getWarehouseCode());
        long queryStart = System.nanoTime();
        String queryText = buildQueryText(req);
        log.info("CircularBuyer recommend queryText completed elapsedMs={} queryLength={}",
                elapsedMs(queryStart), queryText.length());

        RankingResult rankingResult = rankByScore(req, queryText, warehousePoint);
        List<RankedBuyer> rankedTop = rankingResult.rankedTop();
        if (rankedTop.isEmpty()) {
            log.info("CircularBuyer recommend completed totalMs={} materialFit={} recommendations=0 rationaleGenerated=false",
                    elapsedMs(totalStart), req.getMaterialFit());
            return CircularBuyerDto.RecommendRes.builder()
                    .recommendations(List.of())
                    .build();
        }

        List<RecommendedCandidate> top = rankedTop.stream().map(RankedBuyer::buyer).toList();

        // 3층 — LLM 사유 생성 (실패 시 fallback)
        Map<String, RationaleSections> rationales = rankingResult.rationaleGenerationAllowed()
                ? generateRationales(top, req)
                : Map.of();
        boolean rationaleGenerated = !rationales.isEmpty();
        Map<String, RankedBuyer> rankedByCode = rankedTop.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        r -> r.buyer().code(),
                        r -> r,
                        (left, right) -> left
                ));

        List<CircularBuyerDto.RecommendItem> items = top.stream()
                .map(b -> {
                    RankedBuyer rankedBuyer = rankedByCode.get(b.code());
                    Double distanceKm = rankedBuyer == null ? null : rankedBuyer.distanceKm();
                    RationaleSections rationale = rationales
                            .getOrDefault(b.code(), fallbackRationaleSections(distanceKm))
                            .withFallbacks(distanceKm);
                    return CircularBuyerDto.RecommendItem.builder()
                        .code(b.code())
                        .companyName(b.companyName())
                        .primaryMaterialFit(b.primaryMaterialFit())
                        .industryGroup(b.industryGroup())
                        .partnerType(b.partnerType())
                        .factoryProduct(b.factoryProduct())
                        .managerName(b.managerName())
                        .phone(b.phone())
                        .address(b.address())
                        .distanceKm(distanceKm)
                        .companyRationale(rationale.companyRationale())
                        .materialRationale(rationale.materialRationale())
                        .distanceRationale(rationale.distanceRationale())
                        .rationale(rationale.joined())
                        .build();
                })
                .toList();

        log.info("CircularBuyer recommend completed totalMs={} materialFit={} recommendations={} rationaleGenerationAllowed={} rationaleGenerated={}",
                elapsedMs(totalStart), req.getMaterialFit(), items.size(),
                rankingResult.rationaleGenerationAllowed(), rationaleGenerated);
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

    private RankingResult rankByScore(CircularBuyerDto.RecommendReq req, String queryText, WarehousePoint warehousePoint) {
        float[] q;
        long embeddingStart = System.nanoTime();
        try {
            q = callAiWithTimeout(
                    () -> embeddingModel.embed(queryText),
                    embeddingTimeoutMs,
                    "OpenAI embedding"
            );
            log.info("CircularBuyer recommend embedding completed elapsedMs={} queryLength={}",
                    elapsedMs(embeddingStart), queryText.length());
        } catch (Exception e) {
            log.warn("쿼리 임베딩 생성 실패 — 코사인 정렬 없이 fallback 정렬(앞 N건). elapsedMs={} reason={}",
                    elapsedMs(embeddingStart), e.getMessage());
            long fallbackStart = System.nanoTime();
            List<RankedBuyer> fallback = fallbackWithoutEmbedding(req.getMaterialFit());
            log.info("CircularBuyer recommend fallbackWithoutEmbedding completed elapsedMs={} results={}",
                    elapsedMs(fallbackStart), fallback.size());
            return new RankingResult(fallback, false);
        }

        long esStart = System.nanoTime();
        try {
            List<CircularBuyerRecommendSearchService.RecommendedBuyer> esCandidates =
                    recommendSearchService.searchTopKByKnn(q, req.getMaterialFit(), ES_CANDIDATE_K);
            long esElapsedMs = elapsedMs(esStart);
            log.info("CircularBuyer recommend esKnn completed elapsedMs={} candidates={}",
                    esElapsedMs, esCandidates.size());
            if (!esCandidates.isEmpty()) {
                List<RankedBuyer> rankedTop = esCandidates.stream()
                        .map(candidate -> toRankedBuyer(candidate, warehousePoint))
                        .sorted((a, b) -> Double.compare(b.score(), a.score()))
                        .limit(TOP_K)
                        .toList();
                return new RankingResult(rankedTop, true);
            }
            log.warn("순환재고 거래처 ES kNN 추천 결과 없음 — RDB cosine fallback 수행. elapsedMs={} materialFit={}",
                    esElapsedMs, req.getMaterialFit());
        } catch (Exception e) {
            log.warn("순환재고 거래처 ES kNN 추천 실패 — RDB cosine fallback 수행. elapsedMs={} reason={}",
                    elapsedMs(esStart), e.getMessage());
        }

        List<Double> qVec = toDoubleList(q);
        long fallbackStart = System.nanoTime();
        List<RankedBuyer> fallback = fallbackByCosine(req.getMaterialFit(), qVec, warehousePoint);
        log.info("CircularBuyer recommend rdbFallback completed elapsedMs={} results={}",
                elapsedMs(fallbackStart), fallback.size());
        return new RankingResult(fallback, true);
    }

    private List<RankedBuyer> fallbackByCosine(String materialFit, List<Double> qVec, WarehousePoint warehousePoint) {
        List<CircularBuyer> candidates = circularBuyerRepository.findAll(materialFitEqualSpec(materialFit));
        return candidates.stream()
                .map(buyer -> toRankedBuyer(buyer, qVec, warehousePoint))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(TOP_K)
                .toList();
    }

    private List<RankedBuyer> fallbackWithoutEmbedding(String materialFit) {
        return circularBuyerRepository.findAll(materialFitEqualSpec(materialFit)).stream()
                .limit(TOP_K)
                .map(b -> new RankedBuyer(toCandidate(b), null, 0.0))
                .toList();
    }

    private RankedBuyer toRankedBuyer(
            CircularBuyerRecommendSearchService.RecommendedBuyer candidate,
            WarehousePoint warehousePoint
    ) {
        double simScore = candidate.score() == null ? 0.0 : candidate.score();
        boolean canUseDistance = warehousePoint != null
                && isValidLatLng(warehousePoint.latitude(), warehousePoint.longitude())
                && isValidLatLng(candidate.latitude(), candidate.longitude());
        RecommendedCandidate buyer = toCandidate(candidate);
        if (!canUseDistance) {
            return new RankedBuyer(buyer, null, simScore);
        }
        double distanceKm = haversineKm(
                warehousePoint.latitude(), warehousePoint.longitude(),
                candidate.latitude(), candidate.longitude()
        );
        double distScore = 1.0 / (1.0 + (distanceKm / DIST_NORM_KM));
        double finalScore = (SIM_WEIGHT * simScore) + (DIST_WEIGHT * distScore);
        return new RankedBuyer(buyer, round2(distanceKm), finalScore);
    }

    private RankedBuyer toRankedBuyer(CircularBuyer buyer, List<Double> qVec, WarehousePoint warehousePoint) {
        double simScore = cosine(qVec, buyer.getEmbedding());
        boolean canUseDistance = warehousePoint != null
                && isValidLatLng(warehousePoint.latitude(), warehousePoint.longitude())
                && isValidLatLng(buyer.getLatitude(), buyer.getLongitude());
        if (!canUseDistance) {
            return new RankedBuyer(toCandidate(buyer), null, simScore);
        }
        double distanceKm = haversineKm(
                warehousePoint.latitude(), warehousePoint.longitude(),
                buyer.getLatitude(), buyer.getLongitude()
        );
        double distScore = 1.0 / (1.0 + (distanceKm / DIST_NORM_KM));
        double finalScore = (SIM_WEIGHT * simScore) + (DIST_WEIGHT * distScore);
        return new RankedBuyer(toCandidate(buyer), round2(distanceKm), finalScore);
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
     * 단일 ChatModel 호출로 5개 사유 한꺼번에 받기. 실패 시 빈 Map 반환 → 호출자가 섹션별 fallback 텍스트 박음.
     * 응답 형식 강제 — JSON 배열 [{"code":..., "companyRationale":..., ...}, ...] 만 허용.
     */
    private Map<String, RationaleSections> generateRationales(List<RecommendedCandidate> top, CircularBuyerDto.RecommendReq req) {
        if (top.isEmpty()) return Map.of();
        long totalStart = System.nanoTime();
        try {
            String prompt = buildPrompt(top, req);
            long chatStart = System.nanoTime();
            String response = callAiWithTimeout(
                    () -> chatModel.call(prompt),
                    rationaleTimeoutMs,
                    "OpenAI chat rationale"
            );
            log.info("CircularBuyer recommend rationaleChat completed elapsedMs={} promptLength={} responseLength={}",
                    elapsedMs(chatStart), prompt.length(), response == null ? 0 : response.length());
            String json = extractJsonArray(response);
            if (json == null) {
                log.warn("LLM 응답에서 JSON 배열 추출 실패. elapsedMs={} promptLength={} responseLength={}",
                        elapsedMs(totalStart), prompt.length(), response == null ? 0 : response.length());
                return Map.of();
            }
            List<RationaleJsonItem> parsed = objectMapper.readValue(
                    json, new TypeReference<List<RationaleJsonItem>>() {}
            );
            Map<String, RationaleSections> result = new HashMap<>();
            for (RationaleJsonItem entry : parsed) {
                String code = entry.code();
                RationaleSections rationale = new RationaleSections(
                        entry.companyRationale(),
                        entry.materialRationale(),
                        entry.distanceRationale()
                );
                if (code != null) {
                    result.put(code, rationale);
                }
            }
            log.info("CircularBuyer recommend rationale completed elapsedMs={} promptLength={} parsed={}",
                    elapsedMs(totalStart), prompt.length(), result.size());
            return result;
        } catch (Exception e) {
            log.warn("LLM rationale generation failed — fallback 텍스트 사용. elapsedMs={} reason={}",
                    elapsedMs(totalStart), e.getMessage());
            return Map.of();
        }
    }

    private String buildPrompt(List<RecommendedCandidate> top, CircularBuyerDto.RecommendReq req) {
        StringBuilder sb = new StringBuilder();
        WarehousePoint wp = resolveWarehousePoint(req.getWarehouseCode());
        sb.append("너는 SPA 브랜드 본사 관리자에게 순환재고 처리 거래처를 추천하는 한국어 어시스턴트야.\n");
        sb.append("아래 재고 정보와 후보 거래처 ").append(top.size()).append("곳을 보고, ");
        sb.append("각 거래처가 왜 이 재고에 적합한지 짧고 신뢰감 있는 사유를 작성해.\n\n");

        sb.append("[사유 작성 가이드]\n");
        sb.append("- 각 거래처마다 companyRationale, materialRationale, distanceRationale 3개 필드를 모두 작성.\n");
        sb.append("- 각 필드는 약 220~260자, 2~4문장으로 작성하고 서로 내용을 중복하지 마.\n");
        sb.append("- companyRationale: 회사의 생산품·처리 영역·취급 소재 후보군·강점을 구체적으로 설명.\n");
        sb.append("- materialRationale: 요청 재고의 소재가 이 거래처와 맞는 이유와 회사가 해당 소재를 어떻게 활용하는지 설명.\n");
        sb.append("- distanceRationale: 거리(km)가 제공된 경우 수치를 직접 쓰고 물류 효율·운송 부담·처리 속도 관점의 장점을 설명.\n");
        sb.append("- 요청 재고에 여러 품목이 있으면 한 품목만 언급하지 말고, 선택된 품목명과 소재들을 함께 묶어 설명.\n");
        sb.append("- 거리 정보가 '거리 정보 없음'이면 distanceRationale에는 '창고 또는 거래처 좌표 정보가 부족해 거리 적합도는 판단할 수 없습니다.' 취지로 작성.\n");
        sb.append("- 거래처 description·주소·취급 품목에 등장하는 구체 어휘를 적극 활용해 사실 기반으로 작성.\n");
        sb.append("- '기회를 제공할 것입니다', '적합한 거래처입니다' 같은 추상적 칭찬·불필요한 인사말·서론 금지 — 바로 본론.\n");
        sb.append("- 근거가 없는 생산품·활용처를 새로 만들지 말고, 제공된 취급 품목·description 안에서만 표현.\n");
        sb.append("- 한국어 자연어 매끄러운 문장, 각 문장은 마침표로 종료.\n");
        sb.append("- JSON 안에 줄바꿈(\\n) 넣지 말고 한 문단으로 이어 써 (UI 가 자연스럽게 줄바꿈 처리).\n\n");

        sb.append("[요청 재고]\n");
        sb.append("- 소재 구분: ").append(materialFitLabel(req.getMaterialFit())).append("\n");
        if (req.getProductName() != null) sb.append("- 품목: ").append(req.getProductName()).append("\n");
        if (req.getDescription() != null) sb.append("- 설명: ").append(req.getDescription()).append("\n");
        if (req.getQuantityHint() != null) sb.append("- 수량 힌트: ").append(req.getQuantityHint()).append("\n");
        if (wp != null) {
            sb.append("- 창고: code=").append(wp.code())
                    .append(" / 이름=").append(safe(wp.name()))
                    .append(" / 지역=").append(safe(wp.region()))
                    .append(" / 주소=").append(safe(wp.address()));
            if (isValidLatLng(wp.latitude(), wp.longitude())) {
                sb.append(" / 위도=").append(wp.latitude())
                        .append(" / 경도=").append(wp.longitude());
            } else {
                sb.append(" / 좌표 정보 없음");
            }
            sb.append("\n");
        } else if (req.getWarehouseCode() != null && !req.getWarehouseCode().isBlank()) {
            sb.append("- 창고: code=").append(req.getWarehouseCode()).append(" / 상세 위치 정보 없음\n");
        }

        sb.append("\n[후보 거래처 ").append(top.size()).append("곳]\n");
        for (int i = 0; i < top.size(); i++) {
            RecommendedCandidate b = top.get(i);
            sb.append(i + 1).append(". code=").append(b.code())
                    .append(" / 회사=").append(b.companyName())
                    .append(" / 산업군=").append(safe(b.industryGroup()));
            if (b.factoryProduct() != null && !b.factoryProduct().isEmpty()) {
                sb.append(" / 취급=").append(String.join(",", b.factoryProduct()));
            }
            if (b.address() != null) sb.append(" / 주소=").append(b.address());
            if (wp != null && isValidLatLng(wp.latitude(), wp.longitude()) && isValidLatLng(b.latitude(), b.longitude())) {
                double d = haversineKm(wp.latitude(), wp.longitude(), b.latitude(), b.longitude());
                sb.append(" / 거리=").append(round2(d)).append("km");
            } else {
                sb.append(" / 거리=거리 정보 없음");
            }
            if (b.description() != null) sb.append(" / 설명=").append(b.description());
            sb.append("\n");
        }

        sb.append("\n[응답 JSON 계약]\n");
        sb.append("- 최상위는 반드시 JSON 배열만 사용.\n");
        sb.append("- 각 객체는 code, companyRationale, materialRationale, distanceRationale 4개 필드만 포함.\n");
        sb.append("- 아래 후보 code 를 빠짐없이 모두 포함하고, code 값은 후보 code 와 정확히 일치.\n");
        sb.append("- 모든 rationale 값은 빈 문자열/null 금지.\n");
        sb.append("- 다른 텍스트·코드블록·마크다운 절대 금지.\n");
        sb.append("반드시 아래 JSON 배열 형식으로만 답해:\n");
        sb.append("[\n");
        for (int i = 0; i < top.size(); i++) {
            sb.append("  {\"code\": \"").append(top.get(i).code())
                    .append("\", \"companyRationale\": \"<거래처 특장점 약 220~260자>\", ")
                    .append("\"materialRationale\": \"<소재 활용 적합성 약 220~260자>\", ")
                    .append("\"distanceRationale\": \"<물류 접근성 약 220~260자>\"}");
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

    private static RationaleSections fallbackRationaleSections(Double distanceKm) {
        return new RationaleSections(
                fallbackCompanyRationale(),
                fallbackMaterialRationale(),
                fallbackDistanceRationale(distanceKm)
        );
    }

    private static String fallbackCompanyRationale() {
        return "AI 거래처 설명 생성을 일시적으로 사용할 수 없습니다.";
    }

    private static String fallbackMaterialRationale() {
        return "AI 소재 적합도 분석을 일시적으로 사용할 수 없습니다.";
    }

    private static String fallbackDistanceRationale(Double distanceKm) {
        if (distanceKm == null) {
            return "창고 또는 거래처 좌표 정보가 부족해 거리 적합도는 판단할 수 없습니다.";
        }
        return "AI 거리 적합도 분석을 일시적으로 사용할 수 없습니다.";
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static long elapsedMs(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    private <T> T callAiWithTimeout(Supplier<T> supplier, long timeoutMs, String label) throws Exception {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException(label + " timeout after " + timeoutMs + "ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            throw unwrapExecutionException(e);
        }
    }

    private static Exception unwrapExecutionException(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (cause instanceof Exception exception) {
            return exception;
        }
        return new IllegalStateException(cause);
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
        return new WarehousePoint(
                infra.getCode(),
                infra.getName(),
                infra.getRegion(),
                infra.getAddress(),
                infra.getLatitude(),
                infra.getLongitude()
        );
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

    private static RecommendedCandidate toCandidate(CircularBuyer buyer) {
        return new RecommendedCandidate(
                buyer.getCode(),
                buyer.getCompanyName(),
                buyer.getPrimaryMaterialFit(),
                buyer.getIndustryGroup(),
                buyer.getPartnerType(),
                buyer.getFactoryProduct(),
                buyer.getManagerName(),
                buyer.getPhone(),
                buyer.getAddress(),
                buyer.getDescription(),
                buyer.getLatitude(),
                buyer.getLongitude()
        );
    }

    private static RecommendedCandidate toCandidate(CircularBuyerRecommendSearchService.RecommendedBuyer buyer) {
        return new RecommendedCandidate(
                buyer.code(),
                buyer.companyName(),
                buyer.primaryMaterialFit(),
                buyer.industryGroup(),
                buyer.partnerType(),
                buyer.factoryProduct(),
                buyer.managerName(),
                buyer.phone(),
                buyer.address(),
                buyer.description(),
                buyer.latitude(),
                buyer.longitude()
        );
    }

    private record WarehousePoint(
            String code,
            String name,
            String region,
            String address,
            Double latitude,
            Double longitude
    ) {}
    private record RankingResult(List<RankedBuyer> rankedTop, boolean rationaleGenerationAllowed) {}
    private record RankedBuyer(RecommendedCandidate buyer, Double distanceKm, double score) {}
    private record RationaleJsonItem(
            String code,
            String companyRationale,
            String materialRationale,
            String distanceRationale
    ) {}
    private record RationaleSections(
            String companyRationale,
            String materialRationale,
            String distanceRationale
    ) {
        private RationaleSections withFallbacks(Double distanceKm) {
            return new RationaleSections(
                    firstNonBlank(companyRationale, fallbackCompanyRationale()),
                    firstNonBlank(materialRationale, fallbackMaterialRationale()),
                    firstNonBlank(distanceRationale, fallbackDistanceRationale(distanceKm))
            );
        }

        private String joined() {
            return String.join(" ", companyRationale, materialRationale, distanceRationale);
        }
    }
    private record RecommendedCandidate(
            String code,
            String companyName,
            String primaryMaterialFit,
            String industryGroup,
            String partnerType,
            List<String> factoryProduct,
            String managerName,
            String phone,
            String address,
            String description,
            Double latitude,
            Double longitude
    ) {}
}
