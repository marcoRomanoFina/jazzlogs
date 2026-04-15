package com.marcoromanofinaa.jazzlogs.ingestion.application;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumLogJsonImportRunner implements ApplicationRunner {

    private final AlbumLogIngestionProperties properties;
    private final AlbumLogImportService albumLogImportService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        var imported = albumLogImportService.importFromJson(Path.of(properties.path()));
        log.info("Imported {} album logs from {}", imported, properties.path());
    }
}
