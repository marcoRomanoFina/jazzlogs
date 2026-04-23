package com.marcoromanofinaa.jazzlogs.ai.semantic.tracknote;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import lombok.Getter;

@Getter
public class TrackNoteSemanticDocument extends SemanticDocument {

    // Los documentos de track son más granulares que los album logs y sirven para recomendar canciones puntuales.
    private final String spotifyTrackId;
    private final String spotifyAlbumId;
    private final String track;
    private final String artistId;

    private TrackNoteSemanticDocument(
            String sourceId,
            String title,
            String embeddingText,
            String spotifyTrackId,
            String spotifyAlbumId,
            String track,
            String artistId
    ) {
        super(sourceId, title, embeddingText);
        this.spotifyTrackId = spotifyTrackId;
        this.spotifyAlbumId = spotifyAlbumId;
        this.track = track;
        this.artistId = artistId;
    }

    public static TrackNoteSemanticDocument create(
            String sourceId,
            String title,
            String embeddingText,
            String spotifyTrackId,
            String spotifyAlbumId,
            String track,
            String artistId
    ) {
        return new TrackNoteSemanticDocument(
                sourceId,
                title,
                embeddingText,
                spotifyTrackId,
                spotifyAlbumId,
                track,
                artistId
        );
    }

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.TRACK_NOTE;
    }
}
