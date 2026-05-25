package org.example.stockitbe.hq.circularbuyer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ADR-021 AI 거래처 추천 — 임베딩 단계 전용.
 * 거래처 임베딩용 설명을 OpenAI text-embedding-3-small 로 1536차원 벡터화하여 circular_buyer.embedding JSON 컬럼에 저장.
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
        String factoryProduct = v.getFactoryProduct() != null
                ? String.join(",", v.getFactoryProduct())
                : "";
        String embeddingDescription = hasText(v.getEmbeddingDescription())
                ? v.getEmbeddingDescription()
                : v.getDescription();
        return String.join(" ",
                safe(v.getCompanyName()),
                safe(v.getIndustryGroup()),
                factoryProduct,
                safe(embeddingDescription),
                safe(v.getPrimaryMaterialFit()),
                safe(v.getAddress())
        ).trim();
    }

    /**
     * OpenAI 임베딩 1콜 + entity embedding 컬럼 갱신.
     * 영속 상태 entity 가정 — dirty checking 으로 update.
     * 호출 실패 시 로그만 남기고 예외 throw 하지 않음 (거래처 등록/수정 1차 책임 보호 — backfill 로 사후 재시도 가능).
     */
    public boolean embedAndApply(CircularBuyer v) {
        try {
            String text = buildEmbeddingText(v);
            float[] embedding = embeddingModel.embed(text);
            v.updateEmbedding(toDoubleList(embedding));
            return true;
        } catch (Exception e) {
            log.warn("임베딩 생성 실패 — code={}, reason={}. backfill 엔드포인트로 사후 재시도 가능",
                    v.getCode(), e.getMessage());
            return false;
        }
    }

    /**
     * embedding == null 인 거래처를 작은 배치로 처리한다.
     * 외부 API 호출을 DB 트랜잭션 밖에서 수행하고, 각 행은 save() 의 짧은 트랜잭션으로 즉시 적재한다.
     * 대량 데이터를 한 트랜잭션에 올리면 커넥션 장기 점유와 JSON flush OOM 이 발생할 수 있다.
     */
    public BackfillResult backfillNullEmbeddings(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<CircularBuyer> targets = circularBuyerRepository.findNullEmbeddingBatch(PageRequest.of(0, safeLimit));
        int succeeded = 0;
        int failed = 0;
        for (CircularBuyer v : targets) {
            if (!embedAndApply(v)) {
                failed++;
                continue;
            }
            try {
                circularBuyerRepository.save(v);
                succeeded++;
            } catch (Exception e) {
                log.warn("임베딩 저장 실패 — code={}, reason={}", v.getCode(), e.getMessage());
                failed++;
            }
        }
        long remaining = circularBuyerRepository.countNullEmbeddings();
        return new BackfillResult(targets.size(), succeeded, failed, remaining);
    }

    /**
     * limit 크기의 배치를 remaining == 0 이 될 때까지 반복한다.
     * 단, 외부 API 장애 등으로 한 배치도 성공하지 못하면 무한 재시도를 피하기 위해 중단한다.
     */
    public BackfillRunResult backfillNullEmbeddingsUntilDone(int limit, int maxBatches) {
        int safeMaxBatches = Math.min(Math.max(maxBatches, 1), 10_000);
        int batches = 0;
        int processed = 0;
        int succeeded = 0;
        int failed = 0;
        long remaining = circularBuyerRepository.countNullEmbeddings();
        String stopReason = remaining == 0 ? "completed" : "not_started";

        while (remaining > 0 && batches < safeMaxBatches) {
            BackfillResult batch = backfillNullEmbeddings(limit);
            batches++;
            processed += batch.processed();
            succeeded += batch.succeeded();
            failed += batch.failed();
            remaining = batch.remaining();

            if (remaining == 0) {
                stopReason = "completed";
                break;
            }
            if (batch.processed() == 0) {
                stopReason = "no_targets";
                break;
            }
            if (batch.succeeded() == 0) {
                stopReason = "no_progress";
                break;
            }
            stopReason = "max_batches";
        }

        return new BackfillRunResult(batches, processed, succeeded, failed, remaining, stopReason);
    }

    public record BackfillResult(int processed, int succeeded, int failed, long remaining) {
    }

    public record BackfillRunResult(int batches, int processed, int succeeded, int failed,
                                    long remaining, String stopReason) {
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static List<Double> toDoubleList(float[] arr) {
        List<Double> out = new ArrayList<>(arr.length);
        for (float f : arr) {
            out.add((double) f);
        }
        return out;
    }
}
