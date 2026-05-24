package org.example.stockitbe.hq.circularbuyer;

import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CircularBuyerEmbeddingServiceTest {

    @Test
    void buildEmbeddingText_usesEmbeddingDescriptionBeforeDisplayDescription() {
        CircularBuyerEmbeddingService service = newService();
        CircularBuyer buyer = sampleBuyer();
        ReflectionTestUtils.setField(buyer, "embeddingDescription", "추천 검색용 키워드 설명");

        String text = service.buildEmbeddingText(buyer);

        assertThat(text).contains("추천 검색용 키워드 설명");
        assertThat(text).doesNotContain("화면 표시용 설명");
    }

    @Test
    void buildEmbeddingText_fallsBackToDescription_whenEmbeddingDescriptionIsBlank() {
        CircularBuyerEmbeddingService service = newService();
        CircularBuyer buyer = sampleBuyer();
        ReflectionTestUtils.setField(buyer, "embeddingDescription", " ");

        String text = service.buildEmbeddingText(buyer);

        assertThat(text).contains("화면 표시용 설명");
    }

    private CircularBuyerEmbeddingService newService() {
        return new CircularBuyerEmbeddingService(
                mock(EmbeddingModel.class),
                mock(CircularBuyerRepository.class)
        );
    }

    private CircularBuyer sampleBuyer() {
        return CircularBuyerDto.CreateReq.builder()
                .companyName("홍익플러스")
                .industryGroup("의복 제조업")
                .factoryProduct(List.of("작업용 점퍼", "린넨 셔츠"))
                .description("화면 표시용 설명")
                .primaryMaterialFit("blended")
                .managerName("윤도우")
                .phone("02-721-3811")
                .address("서울특별시 종로구")
                .partnerType("general")
                .build()
                .toEntity("RCV-00001");
    }
}
