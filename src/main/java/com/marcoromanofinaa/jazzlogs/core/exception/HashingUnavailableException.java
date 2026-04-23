package com.marcoromanofinaa.jazzlogs.core.exception;

import org.springframework.http.HttpStatus;

public class HashingUnavailableException extends ApplicationException {

    public HashingUnavailableException(String algorithm, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "%s digest is not available".formatted(algorithm));
        initCause(cause);
    }
}
