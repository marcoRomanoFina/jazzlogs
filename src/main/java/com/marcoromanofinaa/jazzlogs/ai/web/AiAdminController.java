package com.marcoromanofinaa.jazzlogs.ai.web;

import com.marcoromanofinaa.jazzlogs.ai.ask.AiAskRequest;
import com.marcoromanofinaa.jazzlogs.ai.ask.AiAskResponse;
import com.marcoromanofinaa.jazzlogs.ai.ask.AiAskService;
import com.marcoromanofinaa.jazzlogs.core.exception.AdminApiKeyNotConfiguredException;
import com.marcoromanofinaa.jazzlogs.core.exception.InvalidAdminApiKeyException;
import com.marcoromanofinaa.jazzlogs.core.exception.VectorStoreNotConfiguredException;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticDocumentIndexingResult;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticDocumentIndexingService;
import com.marcoromanofinaa.jazzlogs.ai.semantic.preview.SemanticDocumentPreview;
import com.marcoromanofinaa.jazzlogs.ai.semantic.preview.SemanticDocumentPreviewService;
import com.marcoromanofinaa.jazzlogs.ai.semantic.search.SemanticSearchRequest;
import com.marcoromanofinaa.jazzlogs.ai.semantic.search.SemanticSearchResponse;
import com.marcoromanofinaa.jazzlogs.ai.semantic.search.SemanticSearchService;
import com.marcoromanofinaa.jazzlogs.curation.admin.AdminApiProperties;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai")
@Slf4j
@RequiredArgsConstructor
public class AiAdminController {

    // Los endpoints de IA son solo para admin mientras el pipeline RAG se diseña y valida.
    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final AdminApiProperties adminProperties;
    private final SemanticDocumentPreviewService previewService;
    private final SemanticSearchService semanticSearchService;
    private final AiAskService aiAskService;
    private final Optional<SemanticDocumentIndexingService> indexingService;

    @GetMapping("/semantic-documents/album-logs/{logNumber}/preview")
    public SemanticDocumentPreview previewAlbumLog(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @PathVariable Integer logNumber
    ) {
        authorize(adminKey);
        log.debug("Admin requested semantic preview for album log {}", logNumber);
        return previewService.previewAlbumLog(logNumber);
    }

    @GetMapping("/semantic-documents/track-notes/{spotifyTrackId}/preview")
    public SemanticDocumentPreview previewTrackNote(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @PathVariable String spotifyTrackId
    ) {
        authorize(adminKey);
        log.debug("Admin requested semantic preview for track note {}", spotifyTrackId);
        return previewService.previewTrackNote(spotifyTrackId);
    }

    @GetMapping("/semantic-documents/artist-profiles/{spotifyArtistId}/preview")
    public SemanticDocumentPreview previewArtistProfile(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @PathVariable String spotifyArtistId
    ) {
        authorize(adminKey);
        log.debug("Admin requested semantic preview for artist profile {}", spotifyArtistId);
        return previewService.previewArtistProfile(spotifyArtistId);
    }

    @PostMapping("/semantic-documents/index")
    public SemanticDocumentIndexingResult indexSemanticDocuments(@RequestHeader(ADMIN_HEADER) String adminKey) {
        authorize(adminKey);
        log.info("Admin requested semantic document indexing");
        return indexingService
                .map(SemanticDocumentIndexingService::indexAll)
                .orElseThrow(VectorStoreNotConfiguredException::new);
    }

    @PostMapping("/semantic-documents/search")
    public SemanticSearchResponse searchSemanticDocuments(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @Valid @RequestBody SemanticSearchRequest request
    ) {
        authorize(adminKey);
        log.info("Admin requested semantic search query='{}' topK={}", request.query(), request.resolvedTopK());
        return semanticSearchService.search(request);
    }

    @PostMapping("/ask")
    public AiAskResponse ask(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @Valid @RequestBody AiAskRequest request
    ) {
        authorize(adminKey);
        log.info("Admin requested AI ask query='{}'", request.question());
        return aiAskService.ask(request);
    }

    private void authorize(String adminKey) {
        if (adminProperties.apiKey() == null || adminProperties.apiKey().isBlank()) {
            throw new AdminApiKeyNotConfiguredException();
        }

        if (!adminProperties.apiKey().equals(adminKey)) {
            log.warn("Rejected admin AI request due to invalid admin API key");
            throw new InvalidAdminApiKeyException();
        }
    }
}
