package org.example.stockitbe.hq.inventory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.inventory.model.ProductInventoryDoc;
import org.example.stockitbe.hq.inventory.model.SkuInventoryDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// ADR-028 전사 재고 조회 Read-side — Elasticsearch CQRS.
// Command(MariaDB)/Query(ES) 책임 분리. Logstash JDBC pipeline 으로 동기화.
// - 마스터 화면 (findCompanyWide): `inventory-master` 인덱스 (product 단위 doc, GROUP BY product_code 미리 합산).
// - SKU 화면 (findCompanyWideSkus / Details / Facets): `inventory-sku` 인덱스 (sku 단위 doc, GROUP BY sku_id 미리 합산).
// terms agg + sub-aggs 제거 → match + from/size + nested by_location filter 로 ms 단위 latency.
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private static final String MASTER_INDEX = "inventory-master";
    private static final String SKU_INDEX = "inventory-sku";

    private final ElasticsearchClient esClient;
    private final InfrastructureRepository infrastructureRepository;

    // 전사 재고(품목 단위) 페이지 조회 — inventory-master 인덱스 + match + from/size + nested filter.
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
        int from = safePageable.getPageNumber() * safePageable.getPageSize();
        int pageSize = safePageable.getPageSize();

        List<Query> filters = new ArrayList<>();
        if (safeParent != null) filters.add(termQuery("parent_category", safeParent));
        if (safeChild != null) filters.add(termQuery("child_category", safeChild));
        if (safeCategory != null) {
            filters.add(Query.of(q -> q.bool(b -> b
                    .should(termQuery("parent_category", safeCategory))
                    .should(termQuery("child_category", safeCategory))
                    .minimumShouldMatch("1"))));
        }
        if (locationType != null || (locationIds != null && !locationIds.isEmpty())) {
            filters.add(buildNestedLocationFilter(locationType, locationIds));
        }

        List<Query> musts = new ArrayList<>();
        if (safeKeyword != null) {
            musts.add(Query.of(q -> q.multiMatch(m -> m
                    .query(safeKeyword)
                    .fields("product_code", "product_name"))));
        }

        boolean excludeByLocation = locationIds == null || locationIds.isEmpty();
        try {
            SearchResponse<ProductInventoryDoc> response = esClient.search(s -> {
                s.index(MASTER_INDEX)
                        .from(from)
                        .size(pageSize)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(q -> q.bool(b -> b.filter(filters).must(musts)))
                        .sort(so -> so.field(f -> f.field("product_code").order(SortOrder.Asc)));
                if (excludeByLocation) {
                    s.source(src -> src.filter(f -> f.excludes("by_location")));
                }
                return s;
            }, ProductInventoryDoc.class);

            long total = response.hits().total() == null ? 0L : response.hits().total().value();
            final List<Long> safeLocationIds = locationIds;
            List<InventoryDto.CompanyWideRes> items = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(d -> d != null)
                    .map(d -> toCompanyWideResFromDoc(d, locationType, safeLocationIds))
                    .toList();

            Page<InventoryDto.CompanyWideRes> page = new PageImpl<>(items, safePageable, total);
            return InventoryDto.CompanyWidePageRes.from(page, buildLocationOptions(locationType));
        } catch (Exception e) {
            log.error("findCompanyWide ES query failed", e);
            throw new RuntimeException(e);
        }
    }

    // 전사 재고(SKU 단위) 페이지 조회 — inventory-sku 인덱스 + match + from/size + nested filter.
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
        int from = safePageable.getPageNumber() * safePageable.getPageSize();
        int pageSize = safePageable.getPageSize();

        List<Query> filters = new ArrayList<>();
        if (safeParent != null) filters.add(termQuery("parent_category", safeParent));
        if (safeChild != null) filters.add(termQuery("child_category", safeChild));
        if (safeColor != null) filters.add(termQuery("color", safeColor));
        if (safeSkuSize != null) filters.add(termQuery("size", safeSkuSize));
        if (locationType != null || (locationIds != null && !locationIds.isEmpty())) {
            filters.add(buildNestedLocationFilter(locationType, locationIds));
        }

        List<Query> musts = new ArrayList<>();
        if (safeKeyword != null) {
            musts.add(Query.of(q -> q.multiMatch(m -> m
                    .query(safeKeyword)
                    .fields("sku_code", "product_code", "product_name"))));
        }

        boolean excludeByLocation = locationIds == null || locationIds.isEmpty();
        try {
            SearchResponse<SkuInventoryDoc> response = esClient.search(s -> {
                s.index(SKU_INDEX)
                        .from(from)
                        .size(pageSize)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(q -> q.bool(b -> b.filter(filters).must(musts)))
                        .sort(so -> so.field(f -> f.field("sku_code").order(SortOrder.Asc)));
                if (excludeByLocation) {
                    s.source(src -> src.filter(f -> f.excludes("by_location")));
                }
                return s;
            }, SkuInventoryDoc.class);

            long total = response.hits().total() == null ? 0L : response.hits().total().value();
            final List<Long> safeLocationIds = locationIds;
            List<InventoryDto.CompanyWideSkuRowRes> items = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(d -> d != null)
                    .map(d -> toSkuRowResFromDoc(d, locationType, safeLocationIds))
                    .toList();

            Page<InventoryDto.CompanyWideSkuRowRes> page = new PageImpl<>(items, safePageable, total);

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

    // 전사 재고 SKU 상세 조회 (특정 product 의 SKU 들) — inventory-sku 인덱스 + term: product_code.
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
        String safeParent = blankToNull(parentCategory);
        String safeChild = blankToNull(childCategory);

        List<Query> filters = new ArrayList<>();
        filters.add(termQuery("product_code", safeItemCode));
        if (safeParent != null) filters.add(termQuery("parent_category", safeParent));
        if (safeChild != null) filters.add(termQuery("child_category", safeChild));
        if (locationType != null || (locationIds != null && !locationIds.isEmpty())) {
            filters.add(buildNestedLocationFilter(locationType, locationIds));
        }

        List<Query> musts = new ArrayList<>();
        if (safeKeyword != null) {
            musts.add(Query.of(q -> q.multiMatch(m -> m
                    .query(safeKeyword)
                    .fields("sku_code", "color", "size"))));
        }

        boolean excludeByLocation = locationIds == null || locationIds.isEmpty();
        try {
            SearchResponse<SkuInventoryDoc> response = esClient.search(s -> {
                s.index(SKU_INDEX)
                        .size(1000)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(q -> q.bool(b -> b.filter(filters).must(musts)))
                        .sort(so -> so.field(f -> f.field("sku_code").order(SortOrder.Asc)));
                if (excludeByLocation) {
                    s.source(src -> src.filter(f -> f.excludes("by_location")));
                }
                return s;
            }, SkuInventoryDoc.class);

            final LocationType lt = locationType;
            final List<Long> safeLocationIds = locationIds;
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(d -> d != null)
                    .filter(d -> nz(d.getTotalQuantity()) > 0 || nz(d.getTotalAvailable()) > 0)
                    .map(d -> toSkuDetailResFromDoc(d, lt, safeLocationIds))
                    .toList();
        } catch (Exception e) {
            log.error("findCompanyWideSkuDetails ES query failed", e);
            throw new RuntimeException(e);
        }
    }

    // SKU facets (color/size distinct) — inventory-sku 인덱스 + 단순 terms agg.
    public InventoryDto.CompanyWideSkuFacetsRes findCompanyWideSkuFacets(LocationType locationType,
                                                                          List<Long> locationIds,
                                                                          String parentCategory,
                                                                          String childCategory,
                                                                          String keyword) {
        String safeParent = blankToNull(parentCategory);
        String safeChild = blankToNull(childCategory);
        String safeKeyword = blankToNull(keyword);

        List<Query> filters = new ArrayList<>();
        if (safeParent != null) filters.add(termQuery("parent_category", safeParent));
        if (safeChild != null) filters.add(termQuery("child_category", safeChild));
        if (locationType != null || (locationIds != null && !locationIds.isEmpty())) {
            filters.add(buildNestedLocationFilter(locationType, locationIds));
        }

        List<Query> musts = new ArrayList<>();
        if (safeKeyword != null) {
            musts.add(Query.of(q -> q.multiMatch(m -> m
                    .query(safeKeyword)
                    .fields("sku_code", "product_code", "product_name"))));
        }

        try {
            SearchResponse<Void> response = esClient.search(s -> s
                    .index(SKU_INDEX)
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

    // ---------- Helpers ----------

    private Query buildNestedLocationFilter(LocationType locationType, List<Long> locationIds) {
        List<Query> inner = new ArrayList<>();
        if (locationType != null) {
            inner.add(Query.of(q -> q.term(t -> t
                    .field("by_location.location_type")
                    .value(locationType.name()))));
        }
        if (locationIds != null && !locationIds.isEmpty()) {
            List<FieldValue> ids = locationIds.stream().map(FieldValue::of).toList();
            inner.add(Query.of(q -> q.terms(t -> t
                    .field("by_location.location_id")
                    .terms(TermsQueryField.of(tf -> tf.value(ids))))));
        }
        return Query.of(q -> q.nested(n -> n
                .path("by_location")
                .query(qi -> qi.bool(b -> b.filter(inner)))));
    }

    private Query termQuery(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    // locationIds (특정 거점) > locationType (매장/창고) > 전체 우선순위로 재고 합산.
    // locationIds 있으면 doc.by_location 의 matching element 만 합산 (sub-group 색인이 거점 단위 element 보장).
    private static class StockTriple {
        final int actual, available, safety;
        StockTriple(int a, int av, int s) { this.actual = a; this.available = av; this.safety = s; }
    }

    private StockTriple sumByLocations(List<ProductInventoryDoc.LocationStock> byLocation, List<Long> locationIds) {
        if (byLocation == null) return new StockTriple(0, 0, 0);
        int q = 0, a = 0, s = 0;
        for (ProductInventoryDoc.LocationStock loc : byLocation) {
            if (loc.getLocationId() != null && locationIds.contains(loc.getLocationId())) {
                q += nz(loc.getQuantity());
                a += nz(loc.getAvailableQuantity());
                s += nz(loc.getSafetyStock());
            }
        }
        return new StockTriple(q, a, s);
    }

    private StockTriple pickProductStocks(ProductInventoryDoc d, LocationType lt, List<Long> locationIds) {
        if (locationIds != null && !locationIds.isEmpty()) {
            return sumByLocations(d.getByLocation(), locationIds);
        }
        if (lt == LocationType.STORE) {
            return new StockTriple(nz(d.getStoreQuantity()), nz(d.getStoreAvailable()), nz(d.getStoreSafety()));
        } else if (lt == LocationType.WAREHOUSE) {
            return new StockTriple(nz(d.getWarehouseQuantity()), nz(d.getWarehouseAvailable()), nz(d.getWarehouseSafety()));
        }
        return new StockTriple(nz(d.getTotalQuantity()), nz(d.getTotalAvailable()), nz(d.getTotalSafety()));
    }

    private StockTriple pickSkuStocks(SkuInventoryDoc d, LocationType lt, List<Long> locationIds) {
        if (locationIds != null && !locationIds.isEmpty()) {
            return sumByLocations(d.getByLocation(), locationIds);
        }
        if (lt == LocationType.STORE) {
            return new StockTriple(nz(d.getStoreQuantity()), nz(d.getStoreAvailable()), nz(d.getStoreSafety()));
        } else if (lt == LocationType.WAREHOUSE) {
            return new StockTriple(nz(d.getWarehouseQuantity()), nz(d.getWarehouseAvailable()), nz(d.getWarehouseSafety()));
        }
        return new StockTriple(nz(d.getTotalQuantity()), nz(d.getTotalAvailable()), nz(d.getTotalSafety()));
    }

    private InventoryDto.CompanyWideRes toCompanyWideResFromDoc(ProductInventoryDoc doc, LocationType locationType, List<Long> locationIds) {
        StockTriple s = pickProductStocks(doc, locationType, locationIds);
        return InventoryDto.CompanyWideRes.builder()
                .itemCode(doc.getProductCode())
                .parentCategory(nullToEmpty(doc.getParentCategory()))
                .childCategory(nullToEmpty(doc.getChildCategory()))
                .itemName(nullToEmpty(doc.getProductName()))
                .actualStock(s.actual)
                .availableStock(s.available)
                .safetyStock(s.safety)
                .status(toUiStatus(s.available, s.safety))
                .updatedAt(doc.getLastUpdate())
                .build();
    }

    private InventoryDto.CompanyWideSkuRowRes toSkuRowResFromDoc(SkuInventoryDoc doc, LocationType locationType, List<Long> locationIds) {
        StockTriple s = pickSkuStocks(doc, locationType, locationIds);
        return InventoryDto.CompanyWideSkuRowRes.builder()
                .skuCode(nullToEmpty(doc.getSkuCode()))
                .itemCode(nullToEmpty(doc.getProductCode()))
                .itemName(nullToEmpty(doc.getProductName()))
                .parentCategory(nullToEmpty(doc.getParentCategory()))
                .childCategory(nullToEmpty(doc.getChildCategory()))
                .color(nullToEmpty(doc.getColor()))
                .size(nullToEmpty(doc.getSize()))
                .actualStock(s.actual)
                .availableStock(s.available)
                .safetyStock(s.safety)
                .status(toUiStatus(s.available, s.safety))
                .build();
    }

    private InventoryDto.CompanyWideSkuDetailRes toSkuDetailResFromDoc(SkuInventoryDoc doc, LocationType locationType, List<Long> locationIds) {
        StockTriple s = pickSkuStocks(doc, locationType, locationIds);
        return InventoryDto.CompanyWideSkuDetailRes.builder()
                .skuCode(nullToEmpty(doc.getSkuCode()))
                .color(nullToEmpty(doc.getColor()))
                .size(nullToEmpty(doc.getSize()))
                .unitPrice(null)
                .actualStock(s.actual)
                .availableStock(s.available)
                .safetyStock(s.safety)
                .status(toUiStatus(s.available, s.safety))
                .updatedAt(doc.getLastUpdate())
                .build();
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
}
