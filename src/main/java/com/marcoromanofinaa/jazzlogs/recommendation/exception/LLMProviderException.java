package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class LLMProviderException extends ApplicationException {

    public LLMProviderException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public LLMProviderException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message);
        initCause(cause);
    }
}
