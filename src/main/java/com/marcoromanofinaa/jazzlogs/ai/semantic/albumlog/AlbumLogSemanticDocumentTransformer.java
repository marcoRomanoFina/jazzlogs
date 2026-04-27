package com.marcoromanofinaa.jazzlogs.ai.semantic.albumlog;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticTextBuilder;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogPersonnel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AlbumLogSemanticDocumentTransformer
        implements SemanticDocumentTransformer<AlbumLog, AlbumLogSemanticDocument> {

    /*
     * Los album logs se transforman con templates determinísticos.
     */
    @Override
    public AlbumLogSemanticDocument transform(AlbumLog source) {
        var title = "%s by %s".formatted(source.getAlbum(), source.getArtist());
        return new AlbumLogSemanticDocument(
                sourceId(source),
                title,
                embeddingText(source),
                source.getLogNumber(),
                source.getAlbum(),
                source.getArtist()
        );
    }

    private String sourceId(AlbumLog source) {
        return Objects.requireNonNull(source.getId(), "AlbumLog id must be present before semantic indexing").toString();
    }

    private String embeddingText(AlbumLog source) {
       
        return new SemanticTextBuilder()
                .addSection("Identidad", identity(source))
                .addSection("Carácter musical", musicalCharacter(source))
                .addSection("Perfil emocional", moodProfile(source))
                .addSection("Contexto de escucha", listeningFit(source))
                .addSection("Importancia", source.getWhyItMatters())
                .addSection("Mirada JazzLogs", jazzLogsTake(source))
                .addSection("Mejor momento", source.getBestMoment())
                .addSection("Recomendado si", source.getRecommendedIf())
                .addSection("Evitar si", source.getAvoidIf())
                .addSection("Personal", personnel(source.getPersonnel()))
                .build();
    }

    private String identity(AlbumLog source) {
        var details = SemanticTextBuilder.clean(Arrays.asList(
                source.getReleaseYear(),
                source.getStyle(),
                source.getTier()
        ));

        if (details.isEmpty()) {
            return "%s de %s es un perfil de recomendación de álbum de JazzLogs incluido en el log #%s."
                    .formatted(source.getAlbum(), source.getArtist(), source.getLogNumber());
        }

        return "%s de %s es un álbum %s incluido en el log #%s de JazzLogs."
                .formatted(
                        source.getAlbum(),
                        source.getArtist(),
                        SemanticTextBuilder.naturalJoin(details),
                        source.getLogNumber()
                );
    }

    private String musicalCharacter(AlbumLog source) {
        var details = SemanticTextBuilder.clean(Arrays.asList(
                SemanticTextBuilder.phrase(source.getEnergy(), "%s de energía"),
                SemanticTextBuilder.phrase(source.getMoodIntensity(), "%s de intensidad emocional"),
                SemanticTextBuilder.phrase(source.getAccessibility(), "un nivel de accesibilidad %s")
        ));

        if (details.isEmpty()) {
            return source.getAlbumContext();
        }

        var character = "Su carácter musical tiene %s.".formatted(SemanticTextBuilder.naturalJoin(details));
        if (SemanticTextBuilder.hasText(source.getAlbumContext())) {
            return "%s %s".formatted(character, source.getAlbumContext());
        }

        return character;
    }

    private Optional<String> moodProfile(AlbumLog source) {
        var vibe = SemanticTextBuilder.naturalJoin(source.getVibe());
        var moods = SemanticTextBuilder.naturalJoin(source.getMoods());

        if (SemanticTextBuilder.hasText(vibe) && SemanticTextBuilder.hasText(moods)) {
            return Optional.of("Crea una atmósfera %s, con moods como %s.".formatted(vibe, moods));
        }

        if (SemanticTextBuilder.hasText(vibe)) {
            return Optional.of("Crea una atmósfera %s.".formatted(vibe));
        }

        if (SemanticTextBuilder.hasText(moods)) {
            return Optional.of("Su perfil emocional se inclina hacia %s.".formatted(moods));
        }

        return Optional.empty();
    }

    private Optional<String> listeningFit(AlbumLog source) {
        var contexts = SemanticTextBuilder.naturalJoin(source.getListeningContext());
        return SemanticTextBuilder.formatIfHasText(contexts, "Funciona bien para %s.");
    }

    private String jazzLogsTake(AlbumLog source) {
        // Preferimos la editorial note curada, pero mantenemos notes como fallback para data legacy.
        return SemanticTextBuilder.firstText(source.getEditorialNote(), source.getNotes());
    }

    private Optional<String> personnel(List<AlbumLogPersonnel> personnel) {
        if (personnel == null || personnel.isEmpty()) {
            return Optional.empty();
        }

        var members = personnel.stream()
                .map(member -> "%s en %s".formatted(member.name(), member.role()))
                .toList();
        return Optional.of("El personal acreditado incluye a %s.".formatted(SemanticTextBuilder.naturalJoin(members)));
    }
}
