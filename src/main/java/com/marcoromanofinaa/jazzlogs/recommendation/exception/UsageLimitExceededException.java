package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UsageLimitExceededException extends ApplicationException {

    public UsageLimitExceededException(UUID userId, AIModelType modelType) {
        super(HttpStatus.CONFLICT, "Usage limit exceeded for user %s and model %s".formatted(userId, modelType));
    }

    public UsageLimitExceededException(UUID userId, AIModelType modelType, String message) {
        super(HttpStatus.CONFLICT, "%s [user=%s, model=%s]".formatted(message, userId, modelType));
    }
}
