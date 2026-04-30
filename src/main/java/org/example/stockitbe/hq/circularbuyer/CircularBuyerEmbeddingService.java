package org.example.stockitbe.hq.circularbuyer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ADR-021 AI 거래처 추천 — 임베딩 단계 전용.
 * 거래처 자연어 설명을 OpenAI text-embedding-3-small 로 1536차원 벡터화하여 circular_buyer.embedding JSON 컬럼에 저장.
 * Vector DB 미사용 (CLAUDE.md / ADR-021).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CircularBuyerEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final CircularBuyerRepository circularBuyerRepository;

    /**
     * 의미 필드를 합쳐 임베딩 입력 텍스트 빌드. null 안전 (null 필드는 빈 문자열).
     */
    public String buildEmbeddingText(CircularBuyer v) {
        String productTypes = v.getProductTypes() != null
                ? String.join(",", v.getProductTypes())
                : "";
        return String.join(" ",
                safe(v.getCompanyName()),
                safe(v.getIndustryGroup()),
                productTypes,
                safe(v.getProductNote()),
                safe(v.getDescription()),
                safe(v.getPrimaryMaterialFit())
        ).trim();
    }

    /**
     * OpenAI 임베딩 1콜 + entity embedding 컬럼 갱신.
     * 영속 상태 entity 가정 — dirty checking 으로 update.
     * 호출 실패 시 로그만 남기고 예외 throw 하지 않음 (거래처 등록/수정 1차 책임 보호 — backfill 로 사후 재시도 가능).
     */
    public void embedAndApply(CircularBuyer v) {
        try {
            String text = buildEmbeddingText(v);
            float[] embedding = embeddingModel.embed(text);
            v.updateEmbedding(toDoubleList(embedding));
        } catch (Exception e) {
            log.warn("임베딩 생성 실패 — code={}, reason={}. backfill 엔드포인트로 사후 재시도 가능",
                    v.getCode(), e.getMessage());
        }
    }

    /**
     * 시드 backfill — embedding == null 인 거래처 일괄 처리. 처리 시도한 건수 반환.
     * 30건 정도라 단일 트랜잭션 OK. 개별 임베딩 실패는 스킵 (다음 거래처 계속).
     */
    @Transactional
    public int backfillNullEmbeddings() {
        List<CircularBuyer> targets = circularBuyerRepository.findAll().stream()
                .filter(v -> v.getEmbedding() == null)
                .toList();
        for (CircularBuyer v : targets) {
            embedAndApply(v);
        }
        return targets.size();
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
