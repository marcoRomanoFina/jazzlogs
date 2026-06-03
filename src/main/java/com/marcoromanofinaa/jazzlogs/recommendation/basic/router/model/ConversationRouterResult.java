package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import java.util.List;

public record ConversationRouterResult(
        ConversationRouterResponse response,
        List<ModelUsage> usageEntries
) {
}
