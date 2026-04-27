package com.marcoromanofinaa.jazzlogs.ai.semantic.tracknote;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticTextBuilder;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNote;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TrackNoteSemanticDocumentTransformer
        implements SemanticDocumentTransformer<TrackNote, TrackNoteSemanticDocument> {

    /*
     * Las track notes capturan semántica a nivel canción: rol, groove, energía, contexto y mirada personal.
     * Estos documentos permiten consultas como "música para una noche lluviosa" a nivel track.
     */
    @Override
    public TrackNoteSemanticDocument transform(TrackNote source) {
        var title = "%s from %s".formatted(source.getTrack(), source.getAlbum());
        return new TrackNoteSemanticDocument(
                sourceId(source),
                title,
                embeddingText(source),
                source.getSpotifyTrackId(),
                source.getSpotifyAlbumId(),
                source.getTrack(),
                source.getArtistId()
        );
    }

    private String sourceId(TrackNote source) {
        return Objects.requireNonNull(source.getId(), "TrackNote id must be present before semantic indexing").toString();
    }

    private String embeddingText(TrackNote source) {
        // Mantener el mismo orden entre tracks ayuda a que el modelo compare estructuras de prosa parecidas.
        return new SemanticTextBuilder()
                .addSection("Identidad", identity(source))
                .addSection("Carácter musical", musicalCharacter(source))
                .addSection("Perfil emocional", moodProfile(source))
                .addSection("Contexto de escucha", listeningFit(source))
                .addSection("Por qué pega", source.getWhyItHits())
                .addSection("Mirada JazzLogs", source.getEditorialNote())
                .addSection("Mejor momento", source.getBestMoment())
                .addSection("Recomendado si", source.getRecommendedIf())
                .addSection("Evitar si", source.getAvoidIf())
                .addSection("Tags destacados", standoutTags(source))
                .build();
    }

    private String identity(TrackNote source) {
        return "%s es %s del álbum %s, conectado al log #%s de JazzLogs."
                .formatted(
                        source.getTrack(),
                        source.isInstrumental() ? "un track instrumental" : "un track vocal",
                        source.getAlbum(),
                        source.getLogNumber()
                );
    }

    private Optional<String> musicalCharacter(TrackNote source) {
        var details = SemanticTextBuilder.clean(Arrays.asList(
                SemanticTextBuilder.phrase(source.getEnergy(), "%s de energía"),
                SemanticTextBuilder.phrase(source.getTempoFeel(), "%s como sensación de tempo"),
                SemanticTextBuilder.phrase(source.getRhythmicFeel(), "%s como sensación rítmica"),
                SemanticTextBuilder.phrase(source.getCompositionType(), "%s como tipo de composición"),
                SemanticTextBuilder.phrase(source.getInstrumentFocus(), "foco instrumental en %s"),
                SemanticTextBuilder.phrase(source.getVocalStyle(), "estilo vocal %s")
        ));

        if (details.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of("El track tiene %s.".formatted(SemanticTextBuilder.naturalJoin(details)));
    }

    private Optional<String> moodProfile(TrackNote source) {
        var vibe = SemanticTextBuilder.naturalJoin(source.getVibe());
        var details = SemanticTextBuilder.clean(Arrays.asList(
                SemanticTextBuilder.phrase(source.getMoodIntensity(), "%s de intensidad emocional"),
                SemanticTextBuilder.phrase(source.getAccessibility(), "un nivel de accesibilidad %s")
        ));

        if (SemanticTextBuilder.hasText(vibe) && !details.isEmpty()) {
            return Optional.of("Se siente %s, con %s.".formatted(vibe, SemanticTextBuilder.naturalJoin(details)));
        }

        if (SemanticTextBuilder.hasText(vibe)) {
            return Optional.of("Se siente %s.".formatted(vibe));
        }

        if (!details.isEmpty()) {
            return Optional.of("Su perfil emocional tiene %s.".formatted(SemanticTextBuilder.naturalJoin(details)));
        }

        return Optional.empty();
    }

    private Optional<String> listeningFit(TrackNote source) {
        var contexts = SemanticTextBuilder.naturalJoin(source.getListeningContext());
        var role = source.getTrackRole();

        if (SemanticTextBuilder.hasText(contexts) && SemanticTextBuilder.hasText(role)) {
            return Optional.of("Funciona bien para %s, especialmente como %s.".formatted(contexts, role));
        }

        if (SemanticTextBuilder.hasText(contexts)) {
            return Optional.of("Funciona bien para %s.".formatted(contexts));
        }

        if (SemanticTextBuilder.hasText(role)) {
            return Optional.of("Dentro del álbum, funciona como %s.".formatted(role));
        }

        return Optional.empty();
    }

    private Optional<String> standoutTags(TrackNote source) {
        var tags = SemanticTextBuilder.naturalJoin(source.getStandoutTags());
        if (!SemanticTextBuilder.hasText(tags)) {
            return Optional.empty();
        }

        var sentence = source.isStandout()
                ? "Es un track destacado asociado con %s.".formatted(tags)
                : "El track está asociado con %s.".formatted(tags);
        return Optional.of(sentence);
    }
}
