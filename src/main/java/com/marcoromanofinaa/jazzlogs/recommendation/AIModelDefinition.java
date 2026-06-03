package com.marcoromanofinaa.jazzlogs.recommendation;

import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.util.List;

public record AIModelDefinition(
        AIModelType type,
        AIProvider provider,
        String providerModelName,
        boolean enabled,
        List<Plan> allowedPlans,
        Integer inputTokenLimit,
        Integer outputTokenLimit,
        RecommendationFlowType recommendationFlowType
) {
}
