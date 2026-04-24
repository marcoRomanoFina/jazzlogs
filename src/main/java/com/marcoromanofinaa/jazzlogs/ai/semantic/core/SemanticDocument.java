package com.marcoromanofinaa.jazzlogs.ai.semantic.core;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public abstract class SemanticDocument {

    /*
     * Un SemanticDocument es la frontera estable entre el dominio de JazzLogs y la capa de IA.
     * Las entidades pueden ser ricas y relacionales, pero el vector store necesita:
     * - un source id estable para volver a la fila real de la DB,
     * - un título legible para humanos/debug,
     * - un texto listo para embeddings que capture el significado semántico.
     */
    private final String sourceId;
    private final String title;
    private final String embeddingText;

    protected SemanticDocument(String sourceId, String title, String embeddingText) {
        this.sourceId = sourceId;
        this.title = title;
        this.embeddingText = embeddingText;
    }

    public final String documentId() {
        return "%s:%s".formatted(type().name(), sourceId);
    }

    public final Map<String, Object> metadata(String transformerVersion) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("type", type().name());
        metadata.put("sourceId", sourceId);
        metadata.put("title", title);
        metadata.put("transformerVersion", transformerVersion);
        appendMetadata(metadata);
        return metadata;
    }

    protected abstract void appendMetadata(Map<String, Object> metadata);

    public abstract SemanticDocumentType type();
}
