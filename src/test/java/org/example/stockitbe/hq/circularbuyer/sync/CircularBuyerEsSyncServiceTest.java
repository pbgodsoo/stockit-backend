package org.example.stockitbe.hq.circularbuyer.sync;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.example.stockitbe.hq.circularbuyer.sync.repository.EsSyncOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CircularBuyerEsSyncServiceTest {

    @Test
    void toEsDocument_includesEmbedding_whenDimsIs1536() {
        CircularBuyerEsSyncService service = newService();
        CircularBuyer buyer = sampleBuyer();

        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < 1536; i++) {
            embedding.add((double) i / 1000);
        }
        buyer.updateEmbedding(embedding);

        Map<String, Object> doc = service.toEsDocument(buyer);

        assertThat(doc.get("code")).isEqualTo("RCV-00001");
        assertThat(doc.get("company_name_normalized")).isEqualTo("홍익플러스");
        assertThat(doc.get("company_name_chosung")).isEqualTo("ㅈㅎㅇㅍㄹㅅ");
        assertThat(doc.get("manager_name_chosung")).isEqualTo("ㅇㄷㅇ");
        assertThat(doc.get("factory_product")).isInstanceOf(List.class);
        assertThat((List<String>) doc.get("factory_product")).contains("작업용 점퍼", "린넨 셔츠");
        assertThat(doc.get("embedding")).isInstanceOf(List.class);
        assertThat((List<?>) doc.get("embedding")).hasSize(1536);
        assertThat(doc.get("embedding_model")).isEqualTo("text-embedding-3-small");
        assertThat(doc.get("embedding_version")).isEqualTo(1);
    }

    @Test
    void toEsDocument_omitsEmbedding_whenDimsMismatch() {
        CircularBuyerEsSyncService service = newService();
        CircularBuyer buyer = sampleBuyer();

        buyer.updateEmbedding(List.of(0.1, 0.2, 0.3));

        Map<String, Object> doc = service.toEsDocument(buyer);

        assertThat(doc).doesNotContainKey("embedding");
        assertThat(doc.get("factory_product")).isInstanceOf(List.class);
    }

    private CircularBuyerEsSyncService newService() {
        CircularBuyerEsSyncService service = new CircularBuyerEsSyncService(
                mock(ElasticsearchClient.class),
                mock(EsSyncOutboxRepository.class),
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "embeddingModel", "text-embedding-3-small");
        ReflectionTestUtils.setField(service, "embeddingVersion", 1);
        return service;
    }

    private CircularBuyer sampleBuyer() {
        CircularBuyerDto.CreateReq req = CircularBuyerDto.CreateReq.builder()
                .companyName("(주) 홍익플러스")
                .industryGroup("의복 제조업")
                .factoryProduct(List.of("작업용 점퍼", "린넨 셔츠"))
                .description("순환 거래처")
                .primaryMaterialFit("blended")
                .managerName("윤도우")
                .phone("02-721-3811")
                .address("서울특별시 종로구")
                .partnerType("general")
                .build();

        CircularBuyer buyer = req.toEntity("RCV-00001");
        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(buyer, "createdAt", Timestamp.from(Instant.parse("2026-05-21T00:00:00Z")));
        ReflectionTestUtils.setField(buyer, "updatedAt", Timestamp.from(Instant.parse("2026-05-21T00:10:00Z")));
        return buyer;
    }
}
