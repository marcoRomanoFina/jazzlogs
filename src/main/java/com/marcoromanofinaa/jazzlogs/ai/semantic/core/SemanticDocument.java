package com.marcoromanofinaa.jazzlogs.ai.semantic.core;

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

    public abstract SemanticDocumentType type();
}
