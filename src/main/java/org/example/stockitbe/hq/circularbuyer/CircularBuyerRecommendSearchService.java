package org.example.stockitbe.hq.circularbuyer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CircularBuyerRecommendSearchService {

    private static final String EMBEDDING_FIELD = "embedding";
    private static final int MIN_NUM_CANDIDATES = 100;
    private static final int NUM_CANDIDATES_MULTIPLIER = 100;

    private final ElasticsearchClient esClient;

    @Value("${stockit.elasticsearch.circular-buyer-index:circular-buyer-search}")
    private String indexName;

    public List<RecommendedBuyer> searchTopKByKnn(
            float[] queryVector,
            String materialFit,
            int topK
    ) throws IOException {
        if (queryVector == null || queryVector.length == 0 || topK <= 0) {
            return List.of();
        }

        List<Float> vector = toFloatList(queryVector);
        long k = topK;
        long numCandidates = Math.max(MIN_NUM_CANDIDATES, (long) topK * NUM_CANDIDATES_MULTIPLIER);
        Query materialFitFilter = termQuery("primary_material_fit", materialFit);

        SearchResponse<Map> response = esClient.search(s -> s
                .index(indexName)
                .size(topK)
                .source(src -> src.filter(f -> f.includes(
                        "code",
                        "company_name",
                        "industry_group",
                        "factory_product",
                        "description",
                        "primary_material_fit",
                        "manager_name",
                        "phone",
                        "address",
                        "partner_type",
                        "latitude",
                        "longitude"
                )))
                .knn(knn -> knn
                        .field(EMBEDDING_FIELD)
                        .queryVector(vector)
                        .k(k)
                        .numCandidates(numCandidates)
                        .filter(materialFitFilter)
                ), Map.class);

        return response.hits().hits().stream()
                .map(hit -> toRecommendedBuyer(hit.source(), hit.score()))
                .toList();
    }

    RecommendedBuyer toRecommendedBuyer(Map<String, Object> source, Double score) {
        Map<String, Object> safeSource = source == null ? Map.of() : source;
        return new RecommendedBuyer(
                str(safeSource.get("code")),
                str(safeSource.get("company_name")),
                str(safeSource.get("primary_material_fit")),
                str(safeSource.get("industry_group")),
                str(safeSource.get("partner_type")),
                strList(safeSource.get("factory_product")),
                str(safeSource.get("manager_name")),
                str(safeSource.get("phone")),
                str(safeSource.get("address")),
                str(safeSource.get("description")),
                dbl(safeSource.get("latitude")),
                dbl(safeSource.get("longitude")),
                score
        );
    }

    private Query termQuery(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    private static List<Float> toFloatList(float[] arr) {
        List<Float> out = new ArrayList<>(arr.length);
        for (float value : arr) {
            out.add(value);
        }
        return out;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Double dbl(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> strList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(v -> v == null ? "" : String.valueOf(v))
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        if (value == null) {
            return List.of();
        }
        String single = String.valueOf(value);
        return single.isBlank() ? List.of() : List.of(single);
    }

    public record RecommendedBuyer(
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
            Double longitude,
            Double score
    ) {
    }
}
