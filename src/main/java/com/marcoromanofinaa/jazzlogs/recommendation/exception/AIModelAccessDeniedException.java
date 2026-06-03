package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AIModelAccessDeniedException extends ApplicationException {

    public AIModelAccessDeniedException(UUID userId, AIModelType modelType, Plan currentPlan) {
        super(HttpStatus.FORBIDDEN,
                "User " + userId + " with plan " + currentPlan + " cannot access model " + modelType);
    }
}
