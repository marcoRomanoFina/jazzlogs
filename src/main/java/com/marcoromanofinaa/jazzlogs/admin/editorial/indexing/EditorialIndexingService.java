package com.marcoromanofinaa.jazzlogs.admin.editorial.indexing;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.exception.EditorialIndexingException;
import com.marcoromanofinaa.jazzlogs.admin.editorial.exception.EditorialLogNotFoundException;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.ArtistLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.TrackLogRepository;
import com.marcoromanofinaa.jazzlogs.core.exception.VectorStoreNotConfiguredException;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditorialIndexingService {

    private final AlbumLogRepository albumLogRepository;
    private final TrackLogRepository trackLogRepository;
    private final ArtistLogRepository artistLogRepository;
    private final EditorialEmbeddingDocumentBuilder editorialEmbeddingDocumentBuilder;
    private final VectorStore vectorStore;
    private final Clock clock;

    @Transactional
    public void indexAlbumLog(UUID albumLogId) {
        ensureVectorStoreConfigured();

        var albumLog = albumLogRepository.findById(albumLogId)
                .orElseThrow(() -> new EditorialLogNotFoundException("Album log", albumLogId));

        var documents = editorialEmbeddingDocumentBuilder.buildAlbumLogDocuments(albumLog);
        replaceDocuments(documents, "Failed to index album log " + albumLogId);
        albumLog.markIndexed(Instant.now(clock));
    }

    @Transactional
    public void indexTrackLog(UUID trackLogId) {
        ensureVectorStoreConfigured();

        var trackLog = trackLogRepository.findById(trackLogId)
                .orElseThrow(() -> new EditorialLogNotFoundException("Track log", trackLogId));

        var documents = editorialEmbeddingDocumentBuilder.buildTrackLogDocuments(trackLog);
        replaceDocuments(documents, "Failed to index track log " + trackLogId);
        trackLog.markIndexed(Instant.now(clock));
    }

    @Transactional
    public void indexArtistLog(UUID artistLogId) {
        ensureVectorStoreConfigured();

        var artistLog = artistLogRepository.findById(artistLogId)
                .orElseThrow(() -> new EditorialLogNotFoundException("Artist log", artistLogId));

        var documents = editorialEmbeddingDocumentBuilder.buildArtistLogDocuments(artistLog);
        replaceDocuments(documents, "Failed to index artist log " + artistLogId);
        artistLog.markIndexed(Instant.now(clock));
    }

    private void replaceDocuments(List<Document> documents, String errorMessage) {
        try {
            var documentIds = documents.stream()
                    .map(Document::getId)
                    .toList();

            vectorStore.delete(documentIds);
            vectorStore.add(documents);
        } catch (RuntimeException exception) {
            throw new EditorialIndexingException(errorMessage, exception);
        }
    }

    private void ensureVectorStoreConfigured() {
        if (vectorStore == null) {
            throw new VectorStoreNotConfiguredException();
        }
    }
}
