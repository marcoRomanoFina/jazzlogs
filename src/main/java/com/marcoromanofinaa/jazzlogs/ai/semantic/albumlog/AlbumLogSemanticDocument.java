package com.marcoromanofinaa.jazzlogs.ai.semantic.albumlog;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import lombok.Getter;

@Getter
public class AlbumLogSemanticDocument extends SemanticDocument {

    // Los documentos de álbum responden preguntas amplias: mood, contexto, importancia y mirada curatorial.
    private final Integer logNumber;
    private final String album;
    private final String artist;

    private AlbumLogSemanticDocument(
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

    public static AlbumLogSemanticDocument create(
            String sourceId,
            String title,
            String embeddingText,
            Integer logNumber,
            String album,
            String artist
    ) {
        return new AlbumLogSemanticDocument(sourceId, title, embeddingText, logNumber, album, artist);
    }

    @Override
    public SemanticDocumentType type() {
        return SemanticDocumentType.ALBUM_LOG;
    }
}
