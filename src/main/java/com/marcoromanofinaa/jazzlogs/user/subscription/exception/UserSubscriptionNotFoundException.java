package com.marcoromanofinaa.jazzlogs.user.subscription.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserSubscriptionNotFoundException extends ApplicationException {

    public UserSubscriptionNotFoundException(UUID userId) {
        super(HttpStatus.CONFLICT, "User subscription not found for user: " + userId);
    }
}
