package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters;

record RetrievalPhase(
        String name,
        ConversationSubgraphFilters subgraphFilters
) {
}
