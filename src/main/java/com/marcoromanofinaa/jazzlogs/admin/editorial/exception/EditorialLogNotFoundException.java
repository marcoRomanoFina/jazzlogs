package com.marcoromanofinaa.jazzlogs.admin.editorial.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class EditorialLogNotFoundException extends ApplicationException {

    public EditorialLogNotFoundException(String logType, UUID logId) {
        super(HttpStatus.NOT_FOUND, logType + " not found: " + logId);
    }
}
