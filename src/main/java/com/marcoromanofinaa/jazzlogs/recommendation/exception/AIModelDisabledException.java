package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class AIModelDisabledException extends ApplicationException {

    public AIModelDisabledException(AIModelType modelType) {
        super(HttpStatus.FORBIDDEN, "AI model is disabled: " + modelType);
    }
}
