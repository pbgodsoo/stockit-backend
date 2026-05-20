package org.example.stockitbe.hq.inventory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.InventoryDoc;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.inventory.model.InventoryStatusPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ADR-028 전사 재고 조회 Read-side — Elasticsearch CQRS.
// Command(MariaDB)/Query(ES) 책임 분리. Logstash JDBC pipeline 으로 동기화된 `inventory` 인덱스를 조회.
// QUERY_ALLOWED_STATUSES (NORMAL/CIRCULAR_CANDIDATE) 필터는 ES query 측에서 처리.
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private static final String INDEX = "inventory";
    private static final int MAX_PRODUCT_BUCKETS = 1000;
    private static final int MAX_SKU_BUCKETS = 5000;

    private final ElasticsearchClient esClient;
    private final InfrastructureRepository infrastructureRepository;

    @Transactional(readOnly = true)
    public InventoryDto.CompanyWidePageRes findCompanyWide(LocationType locationType,
                                                            List<Long> locationIds,
                                                            String parentCategory,
                                                            String childCategory,
                                                            String category,
                                                            InventoryStatus status,
                                                            String keyword,
                                                            Pageable pageable) {
        String safeKeyword = blankToNull(keyword);
        String safeParent = blankToNull(parentCategory);
        String safeChild = blankToNull(childCategory);
        String safeCategory = blankToNull(category);

        Pageable safePageable = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.max(pageable.getPageSize(), 1)
        );

        List<Query> filters = buildCommonFilters(
                locationType, locationIds, safeParent, safeChild, safeCategory, status, safeKeyword
        );

        try {
            SearchResponse<InventoryDoc> response = esClient.search(s -> s
                    .index(INDEX)
                    .size(0)
                    .query(q -> q.bool(b -> b.filter(filters)))
                    .aggregations("by_product", a -> a
                            .terms(t -> t.field("product_code.keyword").size(MAX_PRODUCT_BUCKETS))
                            .aggregations("total_quantity", sub -> sub.sum(sm -> sm.field("quantity")))
                            .aggregations("total_available", sub -> sub.sum(sm -> sm.field("available_quantity")))
                            .aggregations("last_update", sub -> sub.max(mx -> mx.field("update_date")))
                            .aggregations("safety_warehouse", sub -> sub
                                    .filter(qf -> qf.term(t -> t.field("location_type").value("WAREHOUSE")))
                                    .aggregations("sum_w", inner -> inner.sum(sm -> sm.field("warehouse_safety_stock"))))
                            .aggregations("safety_store", sub -> sub
                                    .filter(qf -> qf.term(t -> t.field("location_type").value("STORE")))
                                    .aggregations("sum_s", inner -> inner.sum(sm -> sm.field("store_safety_stock"))))
                            .aggregations("product_meta", sub -> sub
                                    .topHits(th -> th.size(1)
                                            .source(src -> src.filter(f -> f.includes(
                                                    "product_name", "parent_category", "child_category"
                                            )))))
                    ), InventoryDoc.class);

            Aggregate byProduct = response.aggregations().get("by_product");
            List<StringTermsBucket> buckets = byProduct.sterms().buckets().array();

            List<InventoryDto.CompanyWideRes> all = buckets.stream()
                    .map(this::toCompanyWideRes)
                    .toList();

            int from = Math.min(safePageable.getPageNumber() * safePageable.getPageSize(), all.size());
            int to = Math.min(from + safePageable.getPageSize(), all.size());
            List<InventoryDto.CompanyWideRes> pageContent = all.subList(from, to);

            Page<InventoryDto.CompanyWideRes> page = new PageImpl<>(pageContent, safePageable, all.size());
            return InventoryDto.CompanyWidePageRes.from(page, buildLocationOptions(locationType));
        } catch (Exception e) {
            log.error("findCompanyWide ES query failed", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.CompanyWideSkuDetailRes> findCompanyWideSkuDetails(String itemCode,
                                                                                 LocationType locationType,
                                                                                 List<Long> locationIds,
                                                                                 String parentCategory,
                                                                                 String childCategory,
                                                                                 InventoryStatus status,
                                                                                 String keyword) {
        if (itemCode == null || itemCode.isBlank()) return List.of();
        String safeItemCode = itemCode.trim();
        String safeKeyword = blankToNull(keyword);

        List<Query> filters = new ArrayList<>();
        filters.add(termQuery("product_code.keyword", safeItemCode));
        filters.add(allowedStatusFilter());
        if (locationType != null) filters.add(termQuery("location_type", locationType.name()));
        if (locationIds != null && !locationIds.isEmpty()) filters.add(termsLongQuery("location_id", locationIds));
        if (status != null) filters.add(termQuery("inventory_status", status.name()));
        if (parentCategory != null && !parentCategory.isBlank()) filters.add(termQuery("parent_category", parentCategory.trim()));
        if (childCategory != null && !childCategory.isBlank()) filters.add(termQuery("child_category", childCategory.trim()));

        List<Query> musts = new ArrayList<>();
        if (safeKeyword != null) {
            musts.add(Query.of(q -> q.multiMatch(m -> m
                    .query(safeKeyword)
                    .fields("sku_code", "color", "size", "location_name"))));
        }

        try {
            SearchResponse<InventoryDoc> response = esClient.search(s -> s
                    .index(INDEX)
                    .size(MAX_SKU_BUCKETS)
                    .query(q -> q.bool(b -> b
                            .filter(filters)
                            .must(musts)
                    )), InventoryDoc.class);

            Map<Long, SkuDetailAggregate> bySku = new LinkedHashMap<>();
            for (Hit<InventoryDoc> hit : response.hits().hits()) {
                InventoryDoc doc = hit.source();
                if (doc == null || doc.getSkuId() == null) continue;
                SkuDetailAggregate agg = bySku.computeIfAbsent(doc.getSkuId(), k -> new SkuDetailAggregate(doc));
                agg.actualStock += nz(doc.getQuantity());
                agg.availableStock += nz(doc.getAvailableQuantity());
                String safetyKey = doc.getSkuId() + ":" + doc.getLocationId();
                if (agg.safetyKeys.add(safetyKey)) {
                    boolean isWarehouse = "WAREHOUSE".equals(doc.getLocationType());
                    agg.safetyStock += nz(isWarehouse ? doc.getWarehouseSafetyStock() : doc.getStoreSafetyStock());
                }
                Date docUpdated = doc.getUpdateDate();
                if (docUpdated != null && (agg.updatedAt == null || docUpdated.after(agg.updatedAt))) {
                    agg.updatedAt = docUpdated;
                }
            }

            return bySku.values().stream()
                    .filter(agg -> agg.actualStock > 0 || agg.availableStock > 0)
                    .map(agg -> InventoryDto.CompanyWideSkuDetailRes.builder()
                            .skuCode(agg.skuCode)
                            .color(agg.color)
                            .size(agg.size)
                            .unitPrice(agg.unitPrice)
                            .actualStock(agg.actualStock)
                            .availableStock(agg.availableStock)
                            .safetyStock(agg.safetyStock)
                            .status(toUiStatus(agg.availableStock, agg.safetyStock))
                            .updatedAt(agg.updatedAt)
                            .build())
                    .sorted(Comparator.comparing(InventoryDto.CompanyWideSkuDetailRes::getSkuCode))
                    .toList();
        } catch (Exception e) {
            log.error("findCompanyWideSkuDetails ES query failed", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public InventoryDto.CompanyWideSkuPageRes findCompanyWideSkus(LocationType locationType,
                                                                    List<Long> locationIds,
                                                                    String parentCategory,
                                                                    String childCategory,
                                                                    String status,
                                                                    String color,
                                                                    String skuSize,
                                                                    String keyword,
                                                                    Pageable pageable) {
        String safeParent = blankToNull(parentCategory);
        String safeChild = blankToNull(childCategory);
        String safeStatus = blankToNull(status);
        String safeColor = blankToNull(color);
        String safeSkuSize = blankToNull(skuSize);
        String safeKeyword = blankToNull(keyword);

        Pageable safePageable = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.max(pageable.getPageSize(), 1)
        );

        List<Query> filters = new ArrayList<>();
        filters.add(allowedStatusFilter());
        if (locationType != null) filters.add(termQuery("location_type", locationType.name()));
        if (locationIds != null && !locationIds.isEmpty()) filters.add(termsLongQuery("location_id", locationIds));
        if (safeParent != null) filters.add(termQuery("parent_category", safeParent));
        if (safeChild != null) filters.add(termQuery("child_category", safeChild));
        if (safeColor != null) filters.add(termQuery("color", safeColor));
        if (safeSkuSize != null) filters.add(termQuery("size", safeSkuSize));

        List<Query> musts = new ArrayList<>();
        if (safeKeyword != null) {
            musts.add(Query.of(q -> q.multiMatch(m -> m
                    .query(safeKeyword)
                    .fields("sku_code", "product_code", "product_name"))));
        }

        try {
            SearchResponse<InventoryDoc> response = esClient.search(s -> s
                    .index(INDEX)
                    .size(0)
                    .query(q -> q.bool(b -> b.filter(filters).must(musts)))
                    .aggregations("by_sku", a -> a
                            .terms(t -> t.field("sku_id").size(MAX_SKU_BUCKETS))
                            .aggregations("total_quantity", sub -> sub.sum(sm -> sm.field("quantity")))
                            .aggregations("total_available", sub -> sub.sum(sm -> sm.field("available_quantity")))
                            .aggregations("safety_warehouse", sub -> sub
                                    .filter(qf -> qf.term(t -> t.field("location_type").value("WAREHOUSE")))
                                    .aggregations("sum_w", inner -> inner.sum(sm -> sm.field("warehouse_safety_stock"))))
                            .aggregations("safety_store", sub -> sub
                                    .filter(qf -> qf.term(t -> t.field("location_type").value("STORE")))
                                    .aggregations("sum_s", inner -> inner.sum(sm -> sm.field("store_safety_stock"))))
                            .aggregations("sku_meta", sub -> sub
                                    .topHits(th -> th.size(1)
                                            .source(src -> src.filter(f -> f.includes(
                                                    "sku_code", "color", "size",
                                                    "product_code", "product_name",
                                                    "parent_category", "child_category"
                                            )))))
                    ), InventoryDoc.class);

            Aggregate bySku = response.aggregations().get("by_sku");
            List<LongTermsBucket> buckets = bySku.lterms().buckets().array();

            List<InventoryDto.CompanyWideSkuRowRes> all = buckets.stream()
                    .map(this::toSkuRowRes)
                    .sorted(Comparator.comparing(r -> r.getSkuCode() == null ? "" : r.getSkuCode()))
                    .toList();

            int from = Math.min(safePageable.getPageNumber() * safePageable.getPageSize(), all.size());
            int to = Math.min(from + safePageable.getPageSize(), all.size());
            List<InventoryDto.CompanyWideSkuRowRes> pageContent = all.subList(from, to);
            Page<InventoryDto.CompanyWideSkuRowRes> page = new PageImpl<>(pageContent, safePageable, all.size());

            if (safeStatus != null) {
                List<InventoryDto.CompanyWideSkuRowRes> filtered = page.getContent().stream()
                        .filter(r -> safeStatus.equals(r.getStatus()))
                        .toList();
                page = new PageImpl<>(filtered, safePageable, page.getTotalElements());
            }

            return InventoryDto.CompanyWideSkuPageRes.from(page, buildLocationOptions(locationType));
        } catch (Exception e) {
            log.error("findCompanyWideSkus ES query failed", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public InventoryDto.CompanyWideSkuFacetsRes findCompanyWideSkuFacets(LocationType locationType,
                                                                          List<Long> locationIds,
                                                                          String parentCategory,
                                                                          String childCategory,
                                                                          String keyword) {
        String safeParent = blankToNull(parentCategory);
        String safeChild = blankToNull(childCategory);
        String safeKeyword = blankToNull(keyword);

        List<Query> filters = new ArrayList<>();
        filters.add(allowedStatusFilter());
        if (locationType != null) filters.add(termQuery("location_type", locationType.name()));
        if (locationIds != null && !locationIds.isEmpty()) filters.add(termsLongQuery("location_id", locationIds));
        if (safeParent != null) filters.add(termQuery("parent_category", safeParent));
        if (safeChild != null) filters.add(termQuery("child_category", safeChild));

        List<Query> musts = new ArrayList<>();
        if (safeKeyword != null) {
            musts.add(Query.of(q -> q.multiMatch(m -> m
                    .query(safeKeyword)
                    .fields("sku_code", "product_code", "product_name"))));
        }

        try {
            SearchResponse<Void> response = esClient.search(s -> s
                    .index(INDEX)
                    .size(0)
                    .query(q -> q.bool(b -> b.filter(filters).must(musts)))
                    .aggregations("colors", a -> a.terms(t -> t.field("color").size(100)))
                    .aggregations("sizes", a -> a.terms(t -> t.field("size").size(100))),
                    Void.class);

            List<String> colors = response.aggregations().get("colors").sterms().buckets().array().stream()
                    .map(b -> b.key().stringValue())
                    .filter(s -> s != null && !s.isBlank())
                    .sorted()
                    .toList();
            List<String> sizes = response.aggregations().get("sizes").sterms().buckets().array().stream()
                    .map(b -> b.key().stringValue())
                    .filter(s -> s != null && !s.isBlank())
                    .sorted()
                    .toList();

            return InventoryDto.CompanyWideSkuFacetsRes.builder()
                    .colors(colors)
                    .sizes(sizes)
                    .build();
        } catch (Exception e) {
            log.error("findCompanyWideSkuFacets ES query failed", e);
            throw new RuntimeException(e);
        }
    }

    private List<Query> buildCommonFilters(LocationType locationType,
                                            List<Long> locationIds,
                                            String parentCategory,
                                            String childCategory,
                                            String category,
                                            InventoryStatus status,
                                            String keyword) {
        List<Query> filters = new ArrayList<>();
        filters.add(allowedStatusFilter());
        if (locationType != null) filters.add(termQuery("location_type", locationType.name()));
        if (locationIds != null && !locationIds.isEmpty()) filters.add(termsLongQuery("location_id", locationIds));
        if (status != null) filters.add(termQuery("inventory_status", status.name()));
        if (parentCategory != null) filters.add(termQuery("parent_category", parentCategory));
        if (childCategory != null) filters.add(termQuery("child_category", childCategory));
        if (category != null) {
            filters.add(Query.of(q -> q.bool(b -> b
                    .should(termQuery("parent_category", category))
                    .should(termQuery("child_category", category))
                    .minimumShouldMatch("1"))));
        }
        if (keyword != null) {
            filters.add(Query.of(q -> q.multiMatch(m -> m
                    .query(keyword)
                    .fields("product_code", "product_name", "sku_code"))));
        }
        return filters;
    }

    private Query allowedStatusFilter() {
        List<FieldValue> values = InventoryStatusPolicy.QUERY_ALLOWED_STATUSES.stream()
                .map(s -> FieldValue.of(s.name()))
                .toList();
        return Query.of(q -> q.terms(t -> t
                .field("inventory_status")
                .terms(TermsQueryField.of(tf -> tf.value(values)))));
    }

    private Query termQuery(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    private Query termsLongQuery(String field, List<Long> ids) {
        List<FieldValue> values = ids.stream().map(FieldValue::of).toList();
        return Query.of(q -> q.terms(t -> t.field(field).terms(TermsQueryField.of(tf -> tf.value(values)))));
    }

    private InventoryDto.CompanyWideRes toCompanyWideRes(StringTermsBucket bucket) {
        String itemCode = bucket.key().stringValue();
        int actual = (int) Math.round(bucket.aggregations().get("total_quantity").sum().value());
        int available = (int) Math.round(bucket.aggregations().get("total_available").sum().value());
        int safety = readSafetySum(bucket.aggregations());

        InventoryDoc metaDoc = readTopHit(bucket.aggregations().get("product_meta").topHits());
        String itemName = metaDoc == null ? "" : nullToEmpty(metaDoc.getProductName());
        String parent = metaDoc == null ? "" : nullToEmpty(metaDoc.getParentCategory());
        String child = metaDoc == null ? "" : nullToEmpty(metaDoc.getChildCategory());

        Double lastUpdate = bucket.aggregations().get("last_update").max().value();
        Date updatedAt = lastUpdate == null || lastUpdate == 0d || Double.isNaN(lastUpdate)
                ? null : new Date(lastUpdate.longValue());

        return InventoryDto.CompanyWideRes.builder()
                .itemCode(itemCode)
                .parentCategory(parent)
                .childCategory(child)
                .itemName(itemName)
                .actualStock(actual)
                .availableStock(available)
                .safetyStock(safety)
                .status(toUiStatus(available, safety))
                .updatedAt(updatedAt)
                .build();
    }

    private InventoryDto.CompanyWideSkuRowRes toSkuRowRes(LongTermsBucket bucket) {
        int actual = (int) Math.round(bucket.aggregations().get("total_quantity").sum().value());
        int available = (int) Math.round(bucket.aggregations().get("total_available").sum().value());
        int safety = readSafetySum(bucket.aggregations());

        InventoryDoc m = readTopHit(bucket.aggregations().get("sku_meta").topHits());

        return InventoryDto.CompanyWideSkuRowRes.builder()
                .skuCode(m == null ? "" : nullToEmpty(m.getSkuCode()))
                .itemCode(m == null ? "" : nullToEmpty(m.getProductCode()))
                .itemName(m == null ? "" : nullToEmpty(m.getProductName()))
                .parentCategory(m == null ? "" : nullToEmpty(m.getParentCategory()))
                .childCategory(m == null ? "" : nullToEmpty(m.getChildCategory()))
                .color(m == null ? "" : nullToEmpty(m.getColor()))
                .size(m == null ? "" : nullToEmpty(m.getSize()))
                .actualStock(actual)
                .availableStock(available)
                .safetyStock(safety)
                .status(toUiStatus(available, safety))
                .build();
    }

    private int readSafetySum(Map<String, Aggregate> aggs) {
        double w = aggs.get("safety_warehouse").filter().aggregations().get("sum_w").sum().value();
        double s = aggs.get("safety_store").filter().aggregations().get("sum_s").sum().value();
        return (int) Math.round(w + s);
    }

    private InventoryDoc readTopHit(TopHitsAggregate topHits) {
        if (topHits == null) return null;
        List<Hit<JsonData>> hits = topHits.hits().hits();
        if (hits.isEmpty()) return null;
        JsonData src = hits.get(0).source();
        return src == null ? null : src.to(InventoryDoc.class);
    }

    private List<InventoryDto.LocationOptionRes> buildLocationOptions(LocationType locationType) {
        return infrastructureRepository.findAll().stream()
                .filter(i -> locationType == null || i.getLocationType() == locationType)
                .sorted(Comparator.comparing(Infrastructure::getName))
                .map(i -> InventoryDto.LocationOptionRes.builder()
                        .id(i.getId())
                        .code(i.getCode())
                        .name(i.getName())
                        .region(i.getRegion())
                        .build())
                .toList();
    }

    private int nz(Integer v) { return v == null ? 0 : v; }
    private String nullToEmpty(String s) { return s == null ? "" : s; }
    private String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private String toUiStatus(int available, int safety) {
        if (available <= 0) return "품절";
        if (available < safety) return "부족";
        return "정상";
    }

    private static class SkuDetailAggregate {
        private final String skuCode;
        private final String color;
        private final String size;
        private final Long unitPrice;
        private int actualStock = 0;
        private int availableStock = 0;
        private int safetyStock = 0;
        private Date updatedAt;
        private final Set<String> safetyKeys = new HashSet<>();

        private SkuDetailAggregate(InventoryDoc doc) {
            this.skuCode = doc.getSkuCode();
            this.color = doc.getColor();
            this.size = doc.getSize();
            this.unitPrice = doc.getUnitPrice();
        }
    }
}
