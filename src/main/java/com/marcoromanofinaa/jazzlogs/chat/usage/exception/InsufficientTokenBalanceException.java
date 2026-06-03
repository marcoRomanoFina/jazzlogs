package com.marcoromanofinaa.jazzlogs.chat.usage.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InsufficientTokenBalanceException extends ApplicationException {

    public InsufficientTokenBalanceException(UUID userId) {
        super(HttpStatus.CONFLICT, "Insufficient token balance for user: " + userId);
    }
}
