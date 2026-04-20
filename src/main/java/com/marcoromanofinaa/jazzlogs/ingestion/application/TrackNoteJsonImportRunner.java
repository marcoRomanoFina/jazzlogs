package com.marcoromanofinaa.jazzlogs.ingestion.application;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class TrackNoteJsonImportRunner implements ApplicationRunner {

    private final AlbumLogIngestionProperties properties;
    private final TrackNoteImportService trackNoteImportService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled() || properties.trackNotesPath() == null || properties.trackNotesPath().isBlank()) {
            return;
        }

        var imported = trackNoteImportService.importFromJson(Path.of(properties.trackNotesPath()));
        log.info("Imported {} track notes from {}", imported, properties.trackNotesPath());
    }
}
