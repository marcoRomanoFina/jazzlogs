package com.marcoromanofinaa.jazzlogs.chat.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ChatSessionInactiveException extends ApplicationException {

    public ChatSessionInactiveException(UUID chatSessionId) {
        super(HttpStatus.CONFLICT, "Chat session cannot receive new messages: " + chatSessionId);
    }
}
