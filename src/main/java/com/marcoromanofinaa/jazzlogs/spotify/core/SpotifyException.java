package com.marcoromanofinaa.jazzlogs.spotify.core;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class SpotifyException extends ApplicationException {

    public SpotifyException(int statusCode, String message) {
        super(HttpStatus.valueOf(statusCode), message);
    }

    public SpotifyException(String message) {
        this(500, message);
    }
}
