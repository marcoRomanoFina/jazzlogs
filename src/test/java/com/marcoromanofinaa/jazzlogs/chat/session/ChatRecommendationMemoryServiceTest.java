package com.marcoromanofinaa.jazzlogs.chat.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChatRecommendationMemoryServiceTest {

    @Test
    void appendUpdatesSessionSummaryEvenWithoutNewWinners() {
        var resolver = Mockito.mock(RecommendedItemMetadataLookupService.class);
        var service = new ChatRecommendationMemoryService(resolver);
        var current = new ChatRecommendationMemory(
                null,
                List.of(),
                "Le gusto una linea nocturna para cerrar el dia."
        );
        var result = new RecommendationResult(
                "Que bueno.",
                List.of(),
                null,
                null,
                "Le gusto una linea nocturna para cerrar el dia y ahora busca algo un poco mas oscuro.",
                null,
                List.of()
        );

        var updated = service.updateMemory(current, result);

        assertThat(updated.sessionSummary())
                .isEqualTo("Le gusto una linea nocturna para cerrar el dia y ahora busca algo un poco mas oscuro.");
        assertThat(updated.orderedRecommendedItems()).isEmpty();
    }

    @Test
    void appendKeepsResolvedMetadataAlignedWithTheCorrectWinner() {
        var resolver = Mockito.mock(RecommendedItemMetadataLookupService.class);
        var service = new ChatRecommendationMemoryService(resolver);
        var secondWinnerMetadata = new ChatRecommendationMemory.RecommendedItemMetadata(
                "ALBUM_LOG",
                "source-1",
                "Blue Hour",
                null,
                "Stanley Turrentine",
                "Stanley Turrentine",
                List.of("The Three Sounds"),
                "album-1",
                null,
                "A",
                "1961",
                "Hard Bop",
                "Instrumental",
                List.of("Nocturnal"),
                List.of("Warm"),
                "Medium",
                "High",
                null,
                null,
                null,
                null
        );

        Mockito.when(resolver.findByWinner(BasicRecommendationTarget.ALBUM, "Unknown Winner")).thenReturn(Optional.empty());
        Mockito.when(resolver.findByWinner(BasicRecommendationTarget.ALBUM, "Blue Hour")).thenReturn(Optional.of(secondWinnerMetadata));

        var updated = service.updateMemory(
                null,
                new RecommendationResult(
                        "Aca va.",
                        List.of("Unknown Winner", "Blue Hour"),
                        BasicRecommendationTarget.ALBUM,
                        null,
                        null,
                        null,
                        List.of()
                )
        );

        assertThat(updated.orderedRecommendedItems()).hasSize(2);
        assertThat(updated.orderedRecommendedItems().get(0).winnerName()).isEqualTo("Unknown Winner");
        assertThat(updated.orderedRecommendedItems().get(0).item()).isNull();
        assertThat(updated.orderedRecommendedItems().get(1).winnerName()).isEqualTo("Blue Hour");
        assertThat(updated.orderedRecommendedItems().get(1).item()).isEqualTo(secondWinnerMetadata);
        assertThat(updated.lastRecommendedItem().winners()).containsExactly("Unknown Winner", "Blue Hour");
        assertThat(updated.lastRecommendedItem().items()).containsExactly(null, secondWinnerMetadata);
    }

    @Test
    void appendMergesSessionSummaryInsteadOfOverwritingIt() {
        var resolver = Mockito.mock(RecommendedItemMetadataLookupService.class);
        var service = new ChatRecommendationMemoryService(resolver);
        var current = new ChatRecommendationMemory(
                null,
                List.of(),
                "A Marco le encantó Rattlesnake y busca canciones parecidas con energía alta."
        );
        var result = new RecommendationResult(
                "Seguimos.",
                List.of(),
                null,
                null,
                "Marco disfruta mucho el jazz y quiere seguir escuchando con ganas.",
                null,
                List.of()
        );

        var updated = service.updateMemory(current, result);

        assertThat(updated.sessionSummary()).isEqualTo(
                "Marco disfruta mucho el jazz y quiere seguir escuchando con ganas."
        );
    }

    @Test
    void appendUsesRefreshedSummaryWhenRouterSendsCorrection() {
        var resolver = Mockito.mock(RecommendedItemMetadataLookupService.class);
        var service = new ChatRecommendationMemoryService(resolver);
        var current = new ChatRecommendationMemory(
                null,
                List.of(),
                "Le encantó la vibra vocal tranquila para la noche. Busca discos suaves."
        );
        var result = new RecommendationResult(
                "Seguimos.",
                List.of(),
                null,
                null,
                "Pero ahora no quiere vocales; prefiere algo más instrumental.",
                null,
                List.of()
        );

        var updated = service.updateMemory(current, result);

        assertThat(updated.sessionSummary())
                .isEqualTo("Pero ahora no quiere vocales; prefiere algo más instrumental.");
    }

}
