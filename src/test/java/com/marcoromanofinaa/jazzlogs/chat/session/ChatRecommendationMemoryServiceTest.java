package com.marcoromanofinaa.jazzlogs.chat.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationTiming;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChatRecommendationMemoryServiceTest {

    @Test
    void updateMemoryUpdatesSessionSummaryEvenWithoutNewWinners() {
        var resolver = Mockito.mock(RecommendedItemMetadataResolver.class);
        var service = new ChatRecommendationMemoryService(resolver);
        var current = new ChatRecommendationMemory(
                null,
                List.of(),
                "Le gusto una linea nocturna para cerrar el dia."
        );
        var result = recommendationResult(
                "Que bueno.",
                List.of(),
                null,
                "Le gusto una linea nocturna para cerrar el dia y ahora busca algo un poco mas oscuro."
        );

        var updated = service.updateMemory(current, result);

        assertThat(updated.sessionSummary())
                .isEqualTo("Le gusto una linea nocturna para cerrar el dia y ahora busca algo un poco mas oscuro.");
        assertThat(updated.recommendationHistory()).isEmpty();
    }

    @Test
    void updateMemoryFailsWhenAnyWinnerMetadataCannotBeResolved() {
        var resolver = Mockito.mock(RecommendedItemMetadataResolver.class);
        var service = new ChatRecommendationMemoryService(resolver);
        var unknownWinner = winner("album-unknown", "Unknown Winner", "Unknown Artist");
        var blueHourWinner = winner("album-1", "Blue Hour", "Stanley Turrentine");

        Mockito.when(resolver.resolveAll(BasicRecommendationTarget.ALBUM, List.of("album-unknown", "album-1")))
                .thenThrow(new RecommendedItemMetadataResolutionException(
                        "Failed to resolve recommended item metadata for winner ids: album-unknown"
                ));

        assertThatThrownBy(() -> service.updateMemory(
                null,
                recommendationResult(
                        "Aca va.",
                        List.of(unknownWinner, blueHourWinner),
                        BasicRecommendationTarget.ALBUM,
                        null
                )
        ))
                .isInstanceOf(RecommendedItemMetadataResolutionException.class)
                .hasMessageContaining("album-unknown");
    }

    @Test
    void updateMemoryPrefersTheFreshSummaryInsteadOfKeepingTheOldOne() {
        var resolver = Mockito.mock(RecommendedItemMetadataResolver.class);
        var service = new ChatRecommendationMemoryService(resolver);
        var current = new ChatRecommendationMemory(
                null,
                List.of(),
                "A Marco le encantó Rattlesnake y busca canciones parecidas con energía alta."
        );
        var result = recommendationResult(
                "Seguimos.",
                List.of(),
                null,
                "Marco disfruta mucho el jazz y quiere seguir escuchando con ganas."
        );

        var updated = service.updateMemory(current, result);

        assertThat(updated.sessionSummary()).isEqualTo(
                "Marco disfruta mucho el jazz y quiere seguir escuchando con ganas."
        );
    }

    @Test
    void updateMemoryUsesRefreshedSummaryWhenRouterSendsCorrection() {
        var resolver = Mockito.mock(RecommendedItemMetadataResolver.class);
        var service = new ChatRecommendationMemoryService(resolver);
        var current = new ChatRecommendationMemory(
                null,
                List.of(),
                "Le encantó la vibra vocal tranquila para la noche. Busca discos suaves."
        );
        var result = recommendationResult(
                "Seguimos.",
                List.of(),
                null,
                "Pero ahora no quiere vocales; prefiere algo más instrumental."
        );

        var updated = service.updateMemory(current, result);

        assertThat(updated.sessionSummary())
                .isEqualTo("Pero ahora no quiere vocales; prefiere algo más instrumental.");
    }

    private RecommendationResult recommendationResult(
            String assistantResponse,
            List<WinnerReference> winners,
            BasicRecommendationTarget recommendationType,
            String updatedSessionSummary
    ) {
        return new RecommendationResult(
                assistantResponse,
                winners,
                recommendationType,
                null,
                updatedSessionSummary,
                new RecommendationTiming(0L, 0L, 0L),
                List.of(new ModelUsage(
                        UsageRecordStage.BASIC_RECOMMENDATION,
                        AIModelType.BASIC,
                        "test-model",
                        10,
                        0,
                        5
                ))
        );
    }

    private WinnerReference winner(String id, String name, String artistFullName) {
        return new WinnerReference(BasicRecommendationTarget.ALBUM, id, name, artistFullName);
    }
}
