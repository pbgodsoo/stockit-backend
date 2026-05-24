package org.example.stockitbe.hq.circularbuyer;

import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.InfraStatus;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CircularBuyerRecommendServiceTest {

    private final CircularBuyerRepository circularBuyerRepository = mock(CircularBuyerRepository.class);
    private final CircularBuyerRecommendSearchService recommendSearchService = mock(CircularBuyerRecommendSearchService.class);
    private final InfrastructureRepository infrastructureRepository = mock(InfrastructureRepository.class);
    private final ProductMasterRepository productMasterRepository = mock(ProductMasterRepository.class);
    private final EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
    private final ChatModel chatModel = mock(ChatModel.class);

    @Test
    void recommend_usesEsKnnWithoutRdbCandidateScan() throws Exception {
        CircularBuyerRecommendService service = newService();
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1.0f, 0.0f});
        when(recommendSearchService.searchTopKByKnn(any(), anyString(), anyInt()))
                .thenReturn(List.of(esBuyer("RCV-00001", 0.91, 37.5, 127.0)));
        when(chatModel.call(anyString())).thenReturn("""
                [{"code":"RCV-00001","rationale":"합성 섬유 재활용 역량이 요청 재고와 잘 맞습니다."}]
                """);

        CircularBuyerDto.RecommendRes result = service.recommend(req(null));

        assertThat(result.getRecommendations()).hasSize(1);
        assertThat(result.getRecommendations().get(0).getCode()).isEqualTo("RCV-00001");
        assertThat(result.getRecommendations().get(0).getRationale()).contains("합성 섬유");
        verify(recommendSearchService).searchTopKByKnn(any(), anyString(), anyInt());
        verify(circularBuyerRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void recommend_reranksEsCandidatesWithDistanceScore() throws Exception {
        CircularBuyerRecommendService service = newService();
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1.0f, 0.0f});
        when(infrastructureRepository.findByCode("WH-SL-0001")).thenReturn(Optional.of(warehouse()));
        when(recommendSearchService.searchTopKByKnn(any(), anyString(), anyInt()))
                .thenReturn(List.of(
                        esBuyer("RCV-FAR", 1.0, 0.0, 0.0),
                        esBuyer("RCV-NEAR", 0.8, 37.5, 127.0)
                ));
        when(chatModel.call(anyString())).thenReturn("""
                [
                  {"code":"RCV-NEAR","rationale":"가까운 처리 거점입니다."},
                  {"code":"RCV-FAR","rationale":"벡터 유사도가 높습니다."}
                ]
                """);

        CircularBuyerDto.RecommendRes result = service.recommend(req("WH-SL-0001"));

        assertThat(result.getRecommendations()).extracting(CircularBuyerDto.RecommendItem::getCode)
                .containsExactly("RCV-NEAR", "RCV-FAR");
        assertThat(result.getRecommendations().get(0).getDistanceKm()).isEqualTo(0.0);
    }

    @Test
    void recommend_fallsBackToRdbCosineWhenEsFails() throws Exception {
        CircularBuyerRecommendService service = newService();
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1.0f, 0.0f});
        when(recommendSearchService.searchTopKByKnn(any(), anyString(), anyInt()))
                .thenThrow(new IOException("ES unavailable"));
        when(circularBuyerRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(
                        rdbBuyer("RCV-WRONG", List.of(0.0, 1.0)),
                        rdbBuyer("RCV-MATCH", List.of(1.0, 0.0))
                ));

        CircularBuyerDto.RecommendRes result = service.recommend(req(null));

        assertThat(result.getRecommendations()).extracting(CircularBuyerDto.RecommendItem::getCode)
                .containsExactly("RCV-MATCH", "RCV-WRONG");
    }

    @Test
    void recommend_fallsBackToFirstFiveRdbRowsWhenEmbeddingFails() {
        CircularBuyerRecommendService service = newService();
        when(embeddingModel.embed(anyString())).thenThrow(new IllegalStateException("embedding down"));
        when(circularBuyerRepository.findAll(any(Specification.class))).thenReturn(rdbBuyers(6));

        CircularBuyerDto.RecommendRes result = service.recommend(req(null));

        assertThat(result.getRecommendations()).hasSize(5);
        assertThat(result.getRecommendations()).extracting(CircularBuyerDto.RecommendItem::getCode)
                .containsExactly("RCV-00001", "RCV-00002", "RCV-00003", "RCV-00004", "RCV-00005");
        assertThat(result.getRecommendations()).allSatisfy(item ->
                assertThat(item.getRationale()).isEqualTo("AI 사유 생성을 일시적으로 사용할 수 없습니다."));
        verify(chatModel, never()).call(anyString());
    }

    private CircularBuyerRecommendService newService() {
        CircularBuyerRecommendService service = new CircularBuyerRecommendService(
                circularBuyerRepository,
                recommendSearchService,
                infrastructureRepository,
                productMasterRepository,
                embeddingModel,
                chatModel
        );
        ReflectionTestUtils.setField(service, "embeddingTimeoutMs", 5000L);
        ReflectionTestUtils.setField(service, "rationaleTimeoutMs", 5000L);
        return service;
    }

    private CircularBuyerDto.RecommendReq req(String warehouseCode) {
        return CircularBuyerDto.RecommendReq.builder()
                .materialFit("synthetic")
                .productName("폴리에스터 자켓")
                .description("합성 섬유 잔재고")
                .quantityHint("120kg")
                .warehouseCode(warehouseCode)
                .build();
    }

    private CircularBuyerRecommendSearchService.RecommendedBuyer esBuyer(
            String code,
            Double score,
            Double latitude,
            Double longitude
    ) {
        return new CircularBuyerRecommendSearchService.RecommendedBuyer(
                code,
                "ES 거래처 " + code,
                "synthetic",
                "섬유 제품 제조업",
                "general",
                List.of("재생 폴리에스터 원사"),
                "김담당",
                "02-1000-2000",
                "서울특별시 강남구",
                "합성 섬유 재활용 전문 거래처",
                latitude,
                longitude,
                score
        );
    }

    private CircularBuyer rdbBuyer(String code, List<Double> embedding) {
        return CircularBuyer.builder()
                .code(code)
                .companyName("RDB 거래처 " + code)
                .industryGroup("섬유 제품 제조업")
                .factoryProduct(List.of("재생 원사"))
                .description("RDB fallback 거래처")
                .primaryMaterialFit("synthetic")
                .managerName("박담당")
                .phone("02-2000-3000")
                .address("서울특별시 중구")
                .partnerType("general")
                .embedding(embedding)
                .build();
    }

    private List<CircularBuyer> rdbBuyers(int count) {
        List<CircularBuyer> buyers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            buyers.add(rdbBuyer(String.format("RCV-%05d", i), List.of(1.0, 0.0)));
        }
        return buyers;
    }

    private Infrastructure warehouse() {
        return Infrastructure.builder()
                .code("WH-SL-0001")
                .locationType(LocationType.WAREHOUSE)
                .name("서울 창고")
                .region("서울")
                .managerName("창고담당")
                .contact("02-0000-0000")
                .address("서울특별시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .status(InfraStatus.ACTIVE)
                .build();
    }
}
