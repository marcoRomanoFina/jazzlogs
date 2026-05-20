package com.marcoromanofinaa.jazzlogs.auth.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidJwtException extends ApplicationException {

    public InvalidJwtException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid access token");
    }
}
