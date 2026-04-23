package com.marcoromanofinaa.jazzlogs.ai.semantic.core;


public interface SemanticDocumentTransformer<S, D extends SemanticDocument> {

    D transform(S source);
}
