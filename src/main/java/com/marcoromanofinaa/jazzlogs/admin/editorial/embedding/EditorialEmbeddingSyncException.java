package com.marcoromanofinaa.jazzlogs.admin.editorial.embedding;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class EditorialEmbeddingSyncException extends ApplicationException {

    public EditorialEmbeddingSyncException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}
