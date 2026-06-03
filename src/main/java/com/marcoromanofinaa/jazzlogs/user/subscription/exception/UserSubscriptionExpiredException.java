package com.marcoromanofinaa.jazzlogs.user.subscription.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserSubscriptionExpiredException extends ApplicationException {

    public UserSubscriptionExpiredException(UUID userId) {
        super(HttpStatus.CONFLICT, "User subscription period expired for user: " + userId);
    }
}
