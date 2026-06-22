package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters;
import java.util.List;

public record RetrievalCommand(
        String userMessage,
        BasicRecommendationTarget target,
        Integer topK,
        List<String> excludedNodeIds,
        ConversationSubgraphFilters subgraphFilters
) {
}
