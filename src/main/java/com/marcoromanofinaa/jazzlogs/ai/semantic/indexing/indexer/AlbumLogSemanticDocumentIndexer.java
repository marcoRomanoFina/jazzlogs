package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.indexer;

import com.marcoromanofinaa.jazzlogs.ai.semantic.albumlog.AlbumLogSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogNotFoundException;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlbumLogSemanticDocumentIndexer implements SemanticDocumentIndexer {

    private final AlbumLogRepository albumLogRepository;
    private final AlbumLogSemanticDocumentTransformer albumLogTransformer;

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.ALBUM_LOG;
    }

    @Override
    public SemanticDocument indexOne(String sourceIdentifier) {
        var logNumber = Integer.parseInt(sourceIdentifier);
        var albumLog = albumLogRepository.findByLogNumber(logNumber)
                .orElseThrow(() -> new AlbumLogNotFoundException(logNumber));
        log.info("Indexing semantic document for album log {}", logNumber);
        return albumLogTransformer.transform(albumLog);
    }

    @Override
    public List<SemanticDocument> indexAll() {
        return albumLogRepository.findAllByOrderByLogNumberAsc().stream()
                .map(albumLogTransformer::transform)
                .map(SemanticDocument.class::cast)
                .toList();
    }
}
