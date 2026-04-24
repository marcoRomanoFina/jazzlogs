package com.marcoromanofinaa.jazzlogs.ai.semantic.artistprofile;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import java.util.Map;
import lombok.Getter;

@Getter
public class ArtistProfileSemanticDocument extends SemanticDocument {

    // Los perfiles de artista dan contexto amplio para explicar sonido, influencias y puntos de entrada.
    private final String spotifyArtistId;
    private final String name;

    public ArtistProfileSemanticDocument(
            String sourceId,
            String title,
            String embeddingText,
            String spotifyArtistId,
            String name
    ) {
        super(sourceId, title, embeddingText);
        this.spotifyArtistId = spotifyArtistId;
        this.name = name;
    }

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.ARTIST_PROFILE;
    }

    @Override
    protected void appendMetadata(Map<String, Object> metadata) {
        metadata.put("spotifyArtistId", spotifyArtistId);
        metadata.put("artist", name);
    }
}
