package com.marcoromanofinaa.jazzlogs.ai.semantic.artistprofile;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticTextBuilder;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfile;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ArtistProfileSemanticDocumentTransformer
        implements SemanticDocumentTransformer<ArtistProfile, ArtistProfileSemanticDocument> {

    /*
     * Los documentos de artista no reemplazan la data factual de Spotify.
     * Agregan contexto curatorial: sonido, rol en JazzLogs, punto de entrada y cuándo recomendar al artista.
     */
    @Override
    public ArtistProfileSemanticDocument transform(ArtistProfile source) {
        return new ArtistProfileSemanticDocument(
                sourceId(source),
                source.getName(),
                embeddingText(source),
                source.getSpotifyArtistId(),
                source.getName()
        );
    }

    private String sourceId(ArtistProfile source) {
        return Objects.requireNonNull(source.getId(), "ArtistProfile id must be present before semantic indexing").toString();
    }

    private String embeddingText(ArtistProfile source) {
        // Los templates de artista son más biográficos/contextuales que los de álbum o track.
        return new SemanticTextBuilder()
                .addSection("Identidad", identity(source))
                .addSection("Sonido característico", source.getSignatureSound())
                .addSection("Contexto del artista", source.getArtistContext())
                .addSection("Mirada JazzLogs", source.getJazzlogsTake())
                .addSection("Mejor contexto de escucha", bestListeningFit(source))
                .addSection("Punto de entrada recomendado", entryPoint(source))
                .addSection("Evitar si", source.getAvoidIf())
                .addSection("Artistas relacionados", relatedArtists(source))
                .addSection("Importancia", source.getImportance())
                .addSection("Apariciones en logs", logAppearances(source))
                .build();
    }

    private String identity(ArtistProfile source) {
        var details = SemanticTextBuilder.clean(Arrays.asList(
                SemanticTextBuilder.phrase(source.getPrimaryInstrument(), "centrado en %s"),
                SemanticTextBuilder.phrase(
                        SemanticTextBuilder.naturalJoin(source.getMainStyles()),
                        "con estilos principales como %s"
                )
        ));

        if (details.isEmpty()) {
            return "%s es un perfil de artista de JazzLogs.".formatted(source.getName());
        }

        return "%s es un perfil de artista de JazzLogs %s."
                .formatted(source.getName(), SemanticTextBuilder.naturalJoin(details));
    }

    private Optional<String> bestListeningFit(ArtistProfile source) {
        var bestFor = SemanticTextBuilder.naturalJoin(source.getBestFor());
        return SemanticTextBuilder.formatIfHasText(
                bestFor,
                "%s es especialmente útil para %%s.".formatted(source.getName())
        );
    }

    private Optional<String> entryPoint(ArtistProfile source) {
        return SemanticTextBuilder.formatIfHasText(
                source.getRecommendedEntryPoint(),
                "Un buen punto de entrada es %s."
        );
    }

    private Optional<String> relatedArtists(ArtistProfile source) {
        var relatedArtists = SemanticTextBuilder.naturalJoin(source.getRelatedArtists());
        return SemanticTextBuilder.formatIfHasText(relatedArtists, "Artistas relacionados incluyen %s.");
    }

    private Optional<String> logAppearances(ArtistProfile source) {
        var appearances = SemanticTextBuilder.naturalJoin(source.getLogAppearances());
        return SemanticTextBuilder.formatIfHasText(
                appearances,
                "Este artista aparece en las entradas %s de JazzLogs."
        );
    }
}
