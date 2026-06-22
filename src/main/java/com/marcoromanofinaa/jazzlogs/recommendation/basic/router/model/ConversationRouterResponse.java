package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ConversationRouterResponse(
        @NotNull ConversationRoute route,
        @NotNull ConversationUserIntent userIntent,
        @NotNull Boolean isFollowUp,
        @NotNull Boolean needsRetrieval,
        String updatedSessionSummary,
        String suggestedChatTitle,
        String contextualizedQuery,
        String directAnswer,
        String clarificationQuestion,
        List<@Size(min = 1) String> excludedNodeIds,
        @Valid ConversationSubgraphFilters subgraphFilters
) {

    @AssertTrue(message = "must include contextualizedQuery for MUSIC_RECOMMENDATION")
    boolean hasContextualizedQueryWhenNeeded() {
        return route != ConversationRoute.MUSIC_RECOMMENDATION
                || contextualizedQuery != null && !contextualizedQuery.isBlank();
    }

    @AssertTrue(message = "must include directAnswer only for DIRECT_ANSWER")
    boolean hasDirectAnswerConsistency() {
        return route == ConversationRoute.DIRECT_ANSWER
                ? directAnswer != null && !directAnswer.isBlank()
                : directAnswer == null;
    }

    @AssertTrue(message = "must include clarificationQuestion only for CLARIFICATION_NEEDED")
    boolean hasClarificationConsistency() {
        return route == ConversationRoute.CLARIFICATION_NEEDED
                ? clarificationQuestion != null && !clarificationQuestion.isBlank()
                : clarificationQuestion == null;
    }

    @AssertTrue(message = "must not include excludedNodeIds outside MUSIC_RECOMMENDATION")
    boolean hasExcludedWinnersConsistency() {
        return route == ConversationRoute.MUSIC_RECOMMENDATION || excludedNodeIds == null;
    }

    @AssertTrue(message = "must not include subgraphFilters outside MUSIC_RECOMMENDATION")
    boolean hasSubgraphFiltersConsistency() {
        return route == ConversationRoute.MUSIC_RECOMMENDATION || subgraphFilters == null;
    }
}
