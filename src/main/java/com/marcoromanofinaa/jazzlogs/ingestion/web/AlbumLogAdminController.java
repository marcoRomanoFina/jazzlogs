package com.marcoromanofinaa.jazzlogs.ingestion.web;

import com.marcoromanofinaa.jazzlogs.ingestion.application.AlbumLogAdminProperties;
import com.marcoromanofinaa.jazzlogs.ingestion.application.AlbumLogImportService;
import com.marcoromanofinaa.jazzlogs.ingestion.application.AlbumLogSeed;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/ingestion")
@RequiredArgsConstructor
public class AlbumLogAdminController {

    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final AlbumLogImportService albumLogImportService;
    private final AlbumLogRepository albumLogRepository;
    private final AlbumLogAdminProperties adminProperties;

    @PostMapping("/album-logs")
    public AlbumLogIngestionResponse importAlbumLogs(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @RequestBody AlbumLogSeed seed
    ) {
        authorize(adminKey);

        var importedCount = albumLogImportService.importSeed(seed) ? 1 : 0;
        return new AlbumLogIngestionResponse(importedCount, "request-body");
    }

    @DeleteMapping("/album-logs/{logNumber}")
    public AlbumLogDeleteResponse deleteAlbumLog(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @PathVariable int logNumber
    ) {
        authorize(adminKey);

        var deletedCount = albumLogRepository.deleteByLogNumber(logNumber);
        return new AlbumLogDeleteResponse((int) deletedCount, logNumber);
    }

    private void authorize(String adminKey) {
        if (adminProperties.apiKey() == null || adminProperties.apiKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Admin API key is not configured"
            );
        }

        if (!adminProperties.apiKey().equals(adminKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin key");
        }
    }
}
