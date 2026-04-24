package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.indexer;

import com.marcoromanofinaa.jazzlogs.ai.semantic.artistprofile.ArtistProfileSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileNotFoundException;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArtistProfileSemanticDocumentIndexer implements SemanticDocumentIndexer {

    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistProfileSemanticDocumentTransformer artistProfileTransformer;

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.ARTIST_PROFILE;
    }

    @Override
    public SemanticDocument indexOne(String sourceIdentifier) {
        var artistProfile = artistProfileRepository.findBySpotifyArtistId(sourceIdentifier)
                .orElseThrow(() -> new ArtistProfileNotFoundException(sourceIdentifier));
        log.info("Indexing semantic document for artist profile {}", sourceIdentifier);
        return artistProfileTransformer.transform(artistProfile);
    }

    @Override
    public List<SemanticDocument> indexAll() {
        return artistProfileRepository.findAll().stream()
                .map(artistProfileTransformer::transform)
                .map(SemanticDocument.class::cast)
                .toList();
    }
}
