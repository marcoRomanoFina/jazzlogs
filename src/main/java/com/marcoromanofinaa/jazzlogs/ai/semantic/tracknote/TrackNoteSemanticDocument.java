package com.marcoromanofinaa.jazzlogs.ai.semantic.tracknote;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import java.util.Map;
import lombok.Getter;

@Getter
public class TrackNoteSemanticDocument extends SemanticDocument {

    // Los documentos de track son más granulares que los album logs y sirven para recomendar canciones puntuales.
    private final String spotifyTrackId;
    private final String spotifyAlbumId;
    private final String track;
    private final String artistId;

    public TrackNoteSemanticDocument(
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

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.TRACK_NOTE;
    }

    @Override
    protected void appendMetadata(Map<String, Object> metadata) {
        metadata.put("spotifyTrackId", spotifyTrackId);
        metadata.put("spotifyAlbumId", spotifyAlbumId);
        metadata.put("track", track);
        metadata.put("artistId", artistId);
    }
}
