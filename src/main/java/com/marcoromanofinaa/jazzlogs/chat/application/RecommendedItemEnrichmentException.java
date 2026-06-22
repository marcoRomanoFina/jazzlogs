package com.marcoromanofinaa.jazzlogs.chat.application;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class RecommendedItemEnrichmentException extends ApplicationException {

    public RecommendedItemEnrichmentException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
