package com.marcoromanofinaa.jazzlogs.ingestion.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.logbook.domain.ArtistProfile;
import com.marcoromanofinaa.jazzlogs.logbook.domain.ArtistProfileData;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.ArtistProfileRepository;
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
public class ArtistProfileImportService {

    private static final TypeReference<List<ArtistProfileSeed>> SEED_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ArtistProfileRepository artistProfileRepository;

    @Transactional
    public int importFromJson(Path path) {
        var seeds = readSeeds(path).stream()
                .filter(seed -> !seed.isTemplate())
                .toList();

        validate(seeds);

        var existingByArtistId = artistProfileRepository.findAllBySpotifyArtistIdIn(
                        seeds.stream().map(ArtistProfileSeed::spotifyArtistId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(ArtistProfile::getSpotifyArtistId, artistProfile -> artistProfile));

        var profilesToSave = new LinkedHashSet<ArtistProfile>();
        for (var seed : seeds) {
            var existing = existingByArtistId.get(seed.spotifyArtistId());
            if (existing != null) {
                existing.update(toData(seed));
                profilesToSave.add(existing);
                continue;
            }

            profilesToSave.add(ArtistProfile.create(toData(seed)));
        }

        artistProfileRepository.saveAll(profilesToSave);
        return profilesToSave.size();
    }

    private List<ArtistProfileSeed> readSeeds(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Artist profile seed file does not exist: " + path);
        }

        try {
            return objectMapper.readValue(path.toFile(), SEED_LIST_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read artist profile seed file: " + path, exception);
        }
    }

    private void validate(List<ArtistProfileSeed> seeds) {
        for (var seed : seeds) {
            var violations = validator.validate(seed);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }

    private ArtistProfileData toData(ArtistProfileSeed seed) {
        return new ArtistProfileData(
                seed.spotifyArtistId(),
                seed.name(),
                seed.primaryInstrument(),
                seed.mainStyles().toArray(String[]::new),
                seed.signatureSound(),
                seed.artistContext(),
                seed.jazzlogsTake(),
                seed.recommendedEntryPoint(),
                seed.bestFor().toArray(String[]::new),
                seed.avoidIf(),
                seed.relatedArtists().toArray(String[]::new),
                seed.importance(),
                seed.logAppearances().toArray(Integer[]::new)
        );
    }
}
