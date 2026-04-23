package com.marcoromanofinaa.jazzlogs.logbook.artistprofile;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ArtistProfileNotFoundException extends ApplicationException {

    public ArtistProfileNotFoundException(String spotifyArtistId) {
        super(HttpStatus.NOT_FOUND, "Artist profile not found: " + spotifyArtistId);
    }
}
