package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.prompt;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.context.ConversationRouterContext;

public record ConversationRouterPromptCommand(
        String userMessage,
        String timeZone,
        ConversationRouterContext context
) {
}
