package com.marcoromanofinaa.jazzlogs.core.exception;

import org.springframework.http.HttpStatus;

public class InvalidAdminApiKeyException extends ApplicationException {

    public InvalidAdminApiKeyException() {
        super(HttpStatus.FORBIDDEN, "Invalid admin key");
    }
}
