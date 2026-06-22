package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ConversationSubgraphFilters(
        List<@Size(min = 1) String> styles,
        List<@Size(min = 1) String> instruments,
        List<@Size(min = 1) String> rhythms,
        List<@Valid ConversationSubgraphReference> references
) {
}
