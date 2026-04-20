package com.marcoromanofinaa.jazzlogs.ingestion.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.logbook.domain.TrackNote;
import com.marcoromanofinaa.jazzlogs.logbook.domain.TrackNoteData;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.TrackNoteRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackNoteImportService {

    private static final TypeReference<List<TrackNoteSeed>> SEED_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final TrackNoteRepository trackNoteRepository;

    @Transactional
    public int importFromJson(Path path) {
        var seeds = readSeeds(path).stream()
                .filter(seed -> !seed.isTemplate())
                .toList();

        validate(seeds);

        var existingByTrackId = trackNoteRepository.findAllBySpotifyTrackIdIn(
                        seeds.stream().map(TrackNoteSeed::spotifyTrackId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(TrackNote::getSpotifyTrackId, trackNote -> trackNote));

        var trackNotesToSave = new LinkedHashSet<TrackNote>();
        for (var seed : seeds) {
            var existing = existingByTrackId.get(seed.spotifyTrackId());
            if (existing != null) {
                existing.update(toData(seed));
                trackNotesToSave.add(existing);
                continue;
            }

            trackNotesToSave.add(TrackNote.create(toData(seed)));
        }

        trackNoteRepository.saveAll(trackNotesToSave);
        return trackNotesToSave.size();
    }

    private List<TrackNoteSeed> readSeeds(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Track note seed file does not exist: " + path);
        }

        try {
            return objectMapper.readValue(path.toFile(), SEED_LIST_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read track note seed file: " + path, exception);
        }
    }

    private void validate(List<TrackNoteSeed> seeds) {
        for (var seed : seeds) {
            var violations = validator.validate(seed);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }

    private TrackNoteData toData(TrackNoteSeed seed) {
        return new TrackNoteData(
                seed.spotifyTrackId(),
                seed.spotifyAlbumId(),
                seed.logNumber(),
                seed.track(),
                seed.album(),
                seed.artistId(),
                seed.tier(),
                seed.isInstrumental(),
                seed.isStandout(),
                seed.vibe().toArray(String[]::new),
                seed.energy(),
                seed.moodIntensity(),
                seed.accessibility(),
                seed.tempoFeel(),
                seed.rhythmicFeel(),
                seed.trackRole(),
                seed.compositionType(),
                seed.bestMoment(),
                seed.listeningContext().toArray(String[]::new),
                seed.whyItHits(),
                seed.editorialNote(),
                seed.recommendedIf(),
                seed.avoidIf(),
                seed.instrumentFocus(),
                seed.vocalStyle(),
                seed.standoutTags().toArray(String[]::new)
        );
    }
}
