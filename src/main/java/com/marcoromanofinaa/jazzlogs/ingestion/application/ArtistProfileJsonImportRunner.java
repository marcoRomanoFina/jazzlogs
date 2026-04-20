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
@Order(3)
@RequiredArgsConstructor
public class ArtistProfileJsonImportRunner implements ApplicationRunner {

    private final AlbumLogIngestionProperties properties;
    private final ArtistProfileImportService artistProfileImportService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled() || properties.artistProfilesPath() == null || properties.artistProfilesPath().isBlank()) {
            return;
        }

        var imported = artistProfileImportService.importFromJson(Path.of(properties.artistProfilesPath()));
        log.info("Imported {} artist profiles from {}", imported, properties.artistProfilesPath());
    }
}
