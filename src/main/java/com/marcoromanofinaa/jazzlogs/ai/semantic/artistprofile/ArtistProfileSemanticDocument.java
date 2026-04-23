package com.marcoromanofinaa.jazzlogs.ai.semantic.artistprofile;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import lombok.Getter;

@Getter
public class ArtistProfileSemanticDocument extends SemanticDocument {

    // Los perfiles de artista dan contexto amplio para explicar sonido, influencias y puntos de entrada.
    private final String spotifyArtistId;
    private final String name;

    private ArtistProfileSemanticDocument(
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

    public static ArtistProfileSemanticDocument create(
            String sourceId,
            String title,
            String embeddingText,
            String spotifyArtistId,
            String name
    ) {
        return new ArtistProfileSemanticDocument(sourceId, title, embeddingText, spotifyArtistId, name);
    }

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.ARTIST_PROFILE;
    }
}
