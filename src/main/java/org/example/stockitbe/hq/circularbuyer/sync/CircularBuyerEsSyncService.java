package org.example.stockitbe.hq.circularbuyer.sync;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.circularbuyer.ChosungUtils;
import org.example.stockitbe.hq.circularbuyer.CorporateNameNormalizer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.sync.model.EsSyncOperationType;
import org.example.stockitbe.hq.circularbuyer.sync.model.EsSyncOutbox;
import org.example.stockitbe.hq.circularbuyer.sync.repository.EsSyncOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CircularBuyerEsSyncService {

    private static final String ENTITY_TYPE = "CIRCULAR_BUYER";
    private static final int EMBEDDING_DIMS = 1536;

    private final ElasticsearchClient esClient;
    private final EsSyncOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${stockit.elasticsearch.circular-buyer-index:circular-buyer-v1}")
    private String indexName;

    @Value("${stockit.elasticsearch.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${stockit.elasticsearch.sync.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${stockit.elasticsearch.sync.embedding-version:1}")
    private int embeddingVersion;

    @Value("${stockit.elasticsearch.sync.retry.base-delay-ms:30000}")
    private long baseDelayMs;

    public void syncUpsert(CircularBuyer buyer) {
        if (!syncEnabled || buyer == null) {
            return;
        }
        try {
            doUpsert(buyer);
        } catch (Exception e) {
            log.warn("CircularBuyer ES upsert 실패 — code={}, reason={}", buyer.getCode(), e.getMessage());
            enqueueFailure(EsSyncOperationType.UPSERT, buyer.getCode(), buildPayloadSafely(buyer), e.getMessage());
        }
    }

    public void syncDelete(String code) {
        if (!syncEnabled || code == null || code.isBlank()) {
            return;
        }
        try {
            doDelete(code);
        } catch (Exception e) {
            log.warn("CircularBuyer ES delete 실패 — code={}, reason={}", code, e.getMessage());
            enqueueFailure(EsSyncOperationType.DELETE, code, "{}", e.getMessage());
        }
    }

    void doUpsert(CircularBuyer buyer) throws IOException {
        Map<String, Object> doc = toEsDocument(buyer);
        esClient.index(i -> i.index(indexName).id(buyer.getCode()).document(doc));
    }

    void doDelete(String code) throws IOException {
        esClient.delete(d -> d.index(indexName).id(code));
    }

    Map<String, Object> toEsDocument(CircularBuyer buyer) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("buyer_id", buyer.getId());
        doc.put("code", buyer.getCode());
        doc.put("company_name", buyer.getCompanyName());
        doc.put("company_name_normalized", CorporateNameNormalizer.stripLeadingMarker(buyer.getCompanyName()));
        doc.put("company_name_chosung", ChosungUtils.toChosung(buyer.getCompanyName()));
        doc.put("industry_group", buyer.getIndustryGroup());
        doc.put("factory_product", buyer.getFactoryProduct() == null ? List.of() : buyer.getFactoryProduct());
        doc.put("description", buyer.getDescription());
        doc.put("primary_material_fit", buyer.getPrimaryMaterialFit());
        doc.put("manager_name", buyer.getManagerName());
        doc.put("manager_name_chosung", ChosungUtils.toChosung(buyer.getManagerName()));
        doc.put("phone", buyer.getPhone());
        doc.put("address", buyer.getAddress());
        doc.put("latitude", buyer.getLatitude());
        doc.put("longitude", buyer.getLongitude());
        doc.put("partner_type", buyer.getPartnerType());
        doc.put("embedding_model", embeddingModel);
        doc.put("embedding_version", embeddingVersion);
        doc.put("create_date", buyer.getCreatedAt());
        doc.put("update_date", buyer.getUpdatedAt());

        List<Double> embedding = buyer.getEmbedding();
        if (embedding != null) {
            if (embedding.size() == EMBEDDING_DIMS) {
                doc.put("embedding", embedding);
            } else {
                log.warn("CircularBuyer embedding 차원 불일치 — code={}, dims={}", buyer.getCode(), embedding.size());
            }
        }

        doc.values().removeIf(v -> v == null);
        return doc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueFailure(EsSyncOperationType opType, String entityKey, String payload, String reason) {
        EsSyncOutbox outbox = EsSyncOutbox.pending(
                ENTITY_TYPE,
                entityKey,
                opType,
                payload,
                Instant.now().plusMillis(Math.max(baseDelayMs, 1000L)),
                trimError(reason)
        );
        outboxRepository.save(outbox);
    }

    private String buildPayloadSafely(CircularBuyer buyer) {
        try {
            return objectMapper.writeValueAsString(toEsDocument(buyer));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String trimError(String reason) {
        if (reason == null) return "unknown";
        return reason.length() > 1500 ? reason.substring(0, 1500) : reason;
    }
}
