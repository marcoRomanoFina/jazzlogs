package com.marcoromanofinaa.jazzlogs.core.exception;

import org.springframework.http.HttpStatus;

public class VectorStoreNotConfiguredException extends ApplicationException {

    public VectorStoreNotConfiguredException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Vector store is not configured");
    }
}
