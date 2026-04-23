package com.marcoromanofinaa.jazzlogs.core.exception;

import org.springframework.http.HttpStatus;

public class FeatureUnavailableException extends ApplicationException {

    public FeatureUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
