package com.marcoromanofinaa.jazzlogs.admin.editorial.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class EditorialIndexingException extends ApplicationException {

    public EditorialIndexingException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}
