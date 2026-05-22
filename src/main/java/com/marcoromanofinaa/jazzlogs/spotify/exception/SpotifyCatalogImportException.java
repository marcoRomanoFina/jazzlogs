package com.marcoromanofinaa.jazzlogs.spotify.exception;

public class SpotifyCatalogImportException extends RuntimeException {

    public SpotifyCatalogImportException(String message) {
        super(message);
    }

    public SpotifyCatalogImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
