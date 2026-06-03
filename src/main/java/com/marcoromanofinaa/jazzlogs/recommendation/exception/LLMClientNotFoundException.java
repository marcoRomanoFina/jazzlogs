package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class LLMClientNotFoundException extends ApplicationException {

    public LLMClientNotFoundException(AIProvider provider) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "No LLM client found for provider: " + provider);
    }
}
