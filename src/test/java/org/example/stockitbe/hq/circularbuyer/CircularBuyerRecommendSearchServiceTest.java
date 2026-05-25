package org.example.stockitbe.hq.circularbuyer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CircularBuyerRecommendSearchServiceTest {

    @Test
    void toRecommendedBuyer_convertsSourceMap() {
        CircularBuyerRecommendSearchService service = newService();
        Map<String, Object> source = Map.ofEntries(
                entry("code", "RCV-00001"),
                entry("company_name", "순환소재파트너"),
                entry("primary_material_fit", "synthetic"),
                entry("industry_group", "섬유 제품 제조업"),
                entry("partner_type", "social_enterprise"),
                entry("factory_product", List.of("재생 폴리에스터 원사", "PET 칩")),
                entry("manager_name", "김담당"),
                entry("phone", "02-1000-2000"),
                entry("address", "서울특별시 강남구"),
                entry("description", "합성 섬유 재활용 전문 거래처"),
                entry("latitude", 37.5),
                entry("longitude", "127.1")
        );

        CircularBuyerRecommendSearchService.RecommendedBuyer result =
                service.toRecommendedBuyer(source, 0.87);

        assertThat(result.code()).isEqualTo("RCV-00001");
        assertThat(result.companyName()).isEqualTo("순환소재파트너");
        assertThat(result.primaryMaterialFit()).isEqualTo("synthetic");
        assertThat(result.factoryProduct()).containsExactly("재생 폴리에스터 원사", "PET 칩");
        assertThat(result.latitude()).isEqualTo(37.5);
        assertThat(result.longitude()).isEqualTo(127.1);
        assertThat(result.score()).isEqualTo(0.87);
    }

    @Test
    void toRecommendedBuyer_handlesNullsAndNullScoreSafely() {
        CircularBuyerRecommendSearchService service = newService();

        CircularBuyerRecommendSearchService.RecommendedBuyer result =
                service.toRecommendedBuyer(Map.of("code", "RCV-00002"), null);

        assertThat(result.code()).isEqualTo("RCV-00002");
        assertThat(result.factoryProduct()).isEmpty();
        assertThat(result.latitude()).isNull();
        assertThat(result.longitude()).isNull();
        assertThat(result.score()).isNull();
    }

    private CircularBuyerRecommendSearchService newService() {
        return new CircularBuyerRecommendSearchService(mock(ElasticsearchClient.class));
    }
}
