package com.marcoromanofinaa.jazzlogs.auth.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyInUseException extends ApplicationException {

    public EmailAlreadyInUseException() {
        super(HttpStatus.CONFLICT, "Email is already in use");
    }
}
