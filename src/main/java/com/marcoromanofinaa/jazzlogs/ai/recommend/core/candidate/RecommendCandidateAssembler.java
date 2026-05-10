package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import java.util.List;
import org.springframework.ai.document.Document;

public interface RecommendCandidateAssembler<C> {

    C assemble(Document document);

    default List<C> assembleAll(List<Document> documents) {
        return documents.stream()
                .map(this::assemble)
                .toList();
    }
}
