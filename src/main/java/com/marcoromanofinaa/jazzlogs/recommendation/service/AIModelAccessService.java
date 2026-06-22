package com.marcoromanofinaa.jazzlogs.recommendation.service;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.config.AIModelCatalog;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.AIModelAccessDeniedException;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.AIModelDisabledException;
import com.marcoromanofinaa.jazzlogs.user.subscription.service.UserSubscriptionService;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIModelAccessService {

    private final AIModelCatalog aiModelCatalog;
    private final UserSubscriptionService userSubscriptionService;
    private final UsageLimitService usageLimitService;

    public void validateAccess(UUID userId, AIModelType requestedModel) {
        var modelDefinition = aiModelCatalog.getModel(requestedModel);

        if (!modelDefinition.enabled()) {
            throw new AIModelDisabledException(requestedModel);
        }

        Plan currentPlan = userSubscriptionService.getCurrentPlan(userId);
        if (!modelDefinition.allowedPlans().contains(currentPlan)) {
            throw new AIModelAccessDeniedException(userId, requestedModel, currentPlan);
        }

        usageLimitService.validateCreditBalance(userId, requestedModel);
    }
}
