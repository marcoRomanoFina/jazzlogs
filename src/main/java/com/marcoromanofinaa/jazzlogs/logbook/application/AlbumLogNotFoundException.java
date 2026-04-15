package com.marcoromanofinaa.jazzlogs.logbook.application;

public class AlbumLogNotFoundException extends RuntimeException {

    public AlbumLogNotFoundException(int logNumber) {
        super("Album log %d was not found".formatted(logNumber));
    }
}
