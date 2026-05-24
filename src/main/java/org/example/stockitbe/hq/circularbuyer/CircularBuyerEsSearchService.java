package org.example.stockitbe.hq.circularbuyer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CircularBuyerEsSearchService {

    private final ElasticsearchClient esClient;

    @Value("${stockit.elasticsearch.circular-buyer-index:circular-buyer-search}")
    private String indexName;

    public CircularBuyerDto.PageRes findPage(String keyword,
                                             String materialFit,
                                             String partnerType,
                                             Pageable pageable) throws IOException {
        String safeKeyword = blankToNull(keyword);
        String safeMaterialFit = blankToNull(materialFit);
        String safePartnerType = blankToNull(partnerType);
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.max(pageable.getPageSize(), 1);

        List<Query> filters = new ArrayList<>();
        if (safeMaterialFit != null) {
            filters.add(termQuery("primary_material_fit", safeMaterialFit));
        }
        if (safePartnerType != null) {
            filters.add(termQuery("partner_type", safePartnerType));
        }

        List<Query> musts = new ArrayList<>();
        if (safeKeyword != null) {
            String chosungKeyword = ChosungUtils.toChosung(safeKeyword);
            String normalizedKeyword = CorporateNameNormalizer.stripLeadingMarker(safeKeyword);
            List<Query> shoulds = new ArrayList<>();
            shoulds.add(Query.of(q -> q.multiMatch(m -> m
                    .query(safeKeyword)
                    .fields(
                            "code^4",
                            "company_name^3",
                            "company_name.ngram^2",
                            "manager_name^2",
                            "manager_name.ngram",
                            "factory_product",
                            "industry_group.text"
                    ))));
            if (!normalizedKeyword.isBlank() && !normalizedKeyword.equals(safeKeyword)) {
                shoulds.add(Query.of(q -> q.multiMatch(m -> m
                        .query(normalizedKeyword)
                        .fields(
                                "company_name_normalized^5",
                                "company_name.ngram^2",
                                "company_name^2"
                        ))));
            }
            if (!chosungKeyword.isBlank()) {
                shoulds.add(prefixQuery("company_name_chosung", chosungKeyword));
                shoulds.add(prefixQuery("manager_name_chosung", chosungKeyword));
            }
            musts.add(Query.of(q -> q.bool(b -> b
                    .should(shoulds)
                    .minimumShouldMatch("1")
            )));
        }

        SearchResponse<Map> response = esClient.search(s -> {
            s.index(indexName)
                    .from(page * size)
                    .size(size)
                    .trackTotalHits(t -> t.enabled(true))
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
                            "partner_type"
                    )))
                    .query(q -> q.bool(b -> b.filter(filters).must(musts)));

            if (safeKeyword == null) {
                s.sort(so -> so.field(f -> f.field("company_name.keyword").order(SortOrder.Asc)));
                s.sort(so -> so.field(f -> f.field("code").order(SortOrder.Asc)));
            }
            return s;
        }, Map.class);

        long total = response.hits().total() == null ? 0L : response.hits().total().value();
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        List<CircularBuyerDto.ListRes> content = response.hits().hits().stream()
                .map(Hit::source)
                .map(this::toListRes)
                .toList();

        return CircularBuyerDto.PageRes.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .totalElements(total)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();
    }

    private CircularBuyerDto.ListRes toListRes(Map<String, Object> source) {
        return CircularBuyerDto.ListRes.builder()
                .code(str(source.get("code")))
                .companyName(str(source.get("company_name")))
                .industryGroup(str(source.get("industry_group")))
                .factoryProduct(strList(source.get("factory_product")))
                .description(str(source.get("description")))
                .primaryMaterialFit(str(source.get("primary_material_fit")))
                .managerName(str(source.get("manager_name")))
                .phone(str(source.get("phone")))
                .address(str(source.get("address")))
                .partnerType(str(source.get("partner_type")))
                .build();
    }

    private Query termQuery(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    private Query prefixQuery(String field, String value) {
        return Query.of(q -> q.prefix(p -> p.field(field).value(value)));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
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
}
