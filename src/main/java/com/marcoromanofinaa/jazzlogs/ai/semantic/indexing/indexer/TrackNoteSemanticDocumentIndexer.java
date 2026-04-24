package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.indexer;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.ai.semantic.tracknote.TrackNoteSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteNotFoundException;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrackNoteSemanticDocumentIndexer implements SemanticDocumentIndexer {

    private final TrackNoteRepository trackNoteRepository;
    private final TrackNoteSemanticDocumentTransformer trackNoteTransformer;

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.TRACK_NOTE;
    }

    @Override
    public SemanticDocument indexOne(String sourceIdentifier) {
        var trackNote = trackNoteRepository.findBySpotifyTrackId(sourceIdentifier)
                .orElseThrow(() -> new TrackNoteNotFoundException(sourceIdentifier));
        log.info("Indexing semantic document for track note {}", sourceIdentifier);
        return trackNoteTransformer.transform(trackNote);
    }

    @Override
    public List<SemanticDocument> indexAll() {
        return trackNoteRepository.findAll().stream()
                .map(trackNoteTransformer::transform)
                .map(SemanticDocument.class::cast)
                .toList();
    }
}
