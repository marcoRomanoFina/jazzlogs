package com.marcoromanofinaa.jazzlogs.logbook.tracknote;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class TrackNoteNotFoundException extends ApplicationException {

    public TrackNoteNotFoundException(String spotifyTrackId) {
        super(HttpStatus.NOT_FOUND, "Track note not found: " + spotifyTrackId);
    }
}
