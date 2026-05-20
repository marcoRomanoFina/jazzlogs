package com.marcoromanofinaa.jazzlogs.auth.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class UserDisabledException extends ApplicationException {

    public UserDisabledException() {
        super(HttpStatus.FORBIDDEN, "User is disabled");
    }
}
