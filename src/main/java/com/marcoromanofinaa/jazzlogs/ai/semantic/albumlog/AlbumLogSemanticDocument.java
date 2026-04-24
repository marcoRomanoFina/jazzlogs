package com.marcoromanofinaa.jazzlogs.ai.semantic.albumlog;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import java.util.Map;
import lombok.Getter;

@Getter
public class AlbumLogSemanticDocument extends SemanticDocument {

    // Los documentos de álbum responden preguntas amplias: mood, contexto, importancia y mirada curatorial.
    private final Integer logNumber;
    private final String album;
    private final String artist;

    public AlbumLogSemanticDocument(
            String sourceId,
            String title,
            String embeddingText,
            Integer logNumber,
            String album,
            String artist
    ) {
        super(sourceId, title, embeddingText);
        this.logNumber = logNumber;
        this.album = album;
        this.artist = artist;
    }

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.ALBUM_LOG;
    }

    @Override
    protected void appendMetadata(Map<String, Object> metadata) {
        metadata.put("logNumber", logNumber);
        metadata.put("album", album);
        metadata.put("artist", artist);
    }
}
