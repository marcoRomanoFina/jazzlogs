package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class UnsupportedLLMProviderException extends ApplicationException {

    public UnsupportedLLMProviderException(AIProvider provider) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported LLM provider: " + provider);
    }
}
