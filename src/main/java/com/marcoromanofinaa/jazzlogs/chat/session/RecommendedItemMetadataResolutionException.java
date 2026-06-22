package com.marcoromanofinaa.jazzlogs.chat.session;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class RecommendedItemMetadataResolutionException extends ApplicationException {

    public RecommendedItemMetadataResolutionException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
