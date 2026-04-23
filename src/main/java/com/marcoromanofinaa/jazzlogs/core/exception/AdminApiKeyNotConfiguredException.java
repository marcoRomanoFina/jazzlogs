package com.marcoromanofinaa.jazzlogs.core.exception;

import org.springframework.http.HttpStatus;

public class AdminApiKeyNotConfiguredException extends ApplicationException {

    public AdminApiKeyNotConfiguredException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Admin API key is not configured");
    }
}
