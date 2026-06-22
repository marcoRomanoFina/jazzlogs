package com.marcoromanofinaa.jazzlogs.admin;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class AdminAccessRequiredException extends ApplicationException {

    public AdminAccessRequiredException() {
        super(HttpStatus.FORBIDDEN, "Admin access required");
    }
}
