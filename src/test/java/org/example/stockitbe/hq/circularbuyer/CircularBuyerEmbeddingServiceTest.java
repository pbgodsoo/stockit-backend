package org.example.stockitbe.hq.circularbuyer;

import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void backfillNullEmbeddings_processesOnlyRequestedBatch() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        CircularBuyerRepository repository = mock(CircularBuyerRepository.class);
        CircularBuyerEmbeddingService service = new CircularBuyerEmbeddingService(embeddingModel, repository);
        CircularBuyer buyer1 = sampleBuyer();
        CircularBuyer buyer2 = sampleBuyer();
        ReflectionTestUtils.setField(buyer2, "code", "RCV-00002");

        when(repository.findNullEmbeddingBatch(any(Pageable.class))).thenReturn(List.of(buyer1, buyer2));
        when(repository.countNullEmbeddings()).thenReturn(44L);
        when(embeddingModel.embed(anyString())).thenReturn(new float[] {0.1f, 0.2f});

        CircularBuyerEmbeddingService.BackfillResult result = service.backfillNullEmbeddings(2);

        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(result.remaining()).isEqualTo(44L);
        assertThat(buyer1.getEmbedding()).containsExactly(0.1d, 0.2d);
        assertThat(buyer2.getEmbedding()).containsExactly(0.1d, 0.2d);
        verify(repository).findNullEmbeddingBatch(Pageable.ofSize(2));
        verify(repository).save(buyer1);
        verify(repository).save(buyer2);
    }

    @Test
    void backfillNullEmbeddingsUntilDone_repeatsUntilNoRemaining() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        CircularBuyerRepository repository = mock(CircularBuyerRepository.class);
        CircularBuyerEmbeddingService service = new CircularBuyerEmbeddingService(embeddingModel, repository);
        CircularBuyer buyer1 = sampleBuyer();
        CircularBuyer buyer2 = sampleBuyer();
        ReflectionTestUtils.setField(buyer2, "code", "RCV-00002");

        when(repository.countNullEmbeddings()).thenReturn(2L, 1L, 0L);
        when(repository.findNullEmbeddingBatch(any(Pageable.class)))
                .thenReturn(List.of(buyer1))
                .thenReturn(List.of(buyer2));
        when(embeddingModel.embed(anyString())).thenReturn(new float[] {0.1f, 0.2f});

        CircularBuyerEmbeddingService.BackfillRunResult result =
                service.backfillNullEmbeddingsUntilDone(1, 10);

        assertThat(result.batches()).isEqualTo(2);
        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(result.remaining()).isZero();
        assertThat(result.stopReason()).isEqualTo("completed");
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
