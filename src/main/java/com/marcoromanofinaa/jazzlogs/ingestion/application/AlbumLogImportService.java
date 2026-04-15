package com.marcoromanofinaa.jazzlogs.ingestion.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlbumLogImportService {

    private static final TypeReference<List<AlbumLogSeed>> SEED_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AlbumLogRepository albumLogRepository;

    @Transactional
    public int importFromJson(Path path) {
        var seeds = readSeeds(path);
        return importSeeds(seeds);
    }

    @Transactional
    public boolean importSeed(AlbumLogSeed seed) {
        return importSeeds(List.of(seed)) == 1;
    }

    private List<AlbumLog> loadExistingAlbumLogs(List<AlbumLogSeed> seeds) {
        var logNumbers = seeds.stream()
                .map(AlbumLogSeed::logNumber)
                .distinct()
                .toList();

        return albumLogRepository.findAllByLogNumberIn(logNumbers);
    }

    private int importSeeds(List<AlbumLogSeed> seeds) {
        validate(seeds);

        var existingAlbumLogs = loadExistingAlbumLogs(seeds);
        var totalKnownLogNumbers = new LinkedHashSet<Integer>();
        existingAlbumLogs.stream()
                .map(AlbumLog::getLogNumber)
                .forEach(totalKnownLogNumbers::add);

        var albumLogsToSave = new LinkedHashSet<AlbumLog>();

        for (var seed : seeds) {
            if (totalKnownLogNumbers.add(seed.logNumber())) {
                albumLogsToSave.add(mapSeed(seed));
            }
        }

        albumLogRepository.saveAll(albumLogsToSave);
        return albumLogsToSave.size();
    }

    private List<AlbumLogSeed> readSeeds(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Seed file does not exist: " + path);
        }

        try {
            return objectMapper.readValue(path.toFile(), SEED_LIST_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read album log seed file: " + path, exception);
        }
    }

    private void validate(AlbumLogSeed seed) {
        var violations = validator.validate(seed);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validate(List<AlbumLogSeed> seeds) {
        for (var seed : seeds) {
            validate(seed);
        }
    }

    private AlbumLog mapSeed(AlbumLogSeed seed) {
        return AlbumLog.create(
                seed.logNumber(),
                seed.album(),
                seed.artist(),
                seed.caption(),
                seed.postedAt(),
                seed.instagramPermalink(),
                seed.style(),
                seed.moods().toArray(String[]::new),
                seed.notes()
        );
    }
}
