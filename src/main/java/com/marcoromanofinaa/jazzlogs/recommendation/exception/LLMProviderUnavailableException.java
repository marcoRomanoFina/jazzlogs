package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class LLMProviderUnavailableException extends ApplicationException {

    public LLMProviderUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
        initCause(cause);
    }
}
