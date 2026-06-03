package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class RecommendationGenerationException extends ApplicationException {

    public RecommendationGenerationException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public RecommendationGenerationException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message);
        initCause(cause);
    }
}
