package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversationSubgraphReference(
        @NotNull ConversationSubgraphReferenceType type,
        @NotBlank String name
) {
}
