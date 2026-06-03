package com.marcoromanofinaa.jazzlogs.chat.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ChatSessionNotFoundException extends ApplicationException {

    public ChatSessionNotFoundException(UUID chatSessionId) {
        super(HttpStatus.NOT_FOUND, "Chat session not found: " + chatSessionId);
    }
}
