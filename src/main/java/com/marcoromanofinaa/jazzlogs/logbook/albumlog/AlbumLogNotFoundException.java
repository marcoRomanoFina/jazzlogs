package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class AlbumLogNotFoundException extends ApplicationException {

    public AlbumLogNotFoundException(int logNumber) {
        super(HttpStatus.NOT_FOUND, "Album log %d was not found".formatted(logNumber));
    }
}
