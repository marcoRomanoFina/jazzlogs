package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InsufficientUsageLimitException extends ApplicationException {

    public InsufficientUsageLimitException(UUID userId, AIModelType modelType) {
        super(HttpStatus.CONFLICT, "User " + userId + " has insufficient usage limit for model " + modelType);
    }
}
