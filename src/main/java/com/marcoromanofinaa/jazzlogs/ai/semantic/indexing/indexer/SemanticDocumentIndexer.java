package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.indexer;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import java.util.List;

public interface SemanticDocumentIndexer {

    SemanticDocumentType type();

    SemanticDocument indexOne(String sourceIdentifier);

    List<SemanticDocument> indexAll();
}
