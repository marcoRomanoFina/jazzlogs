package com.marcoromanofinaa.jazzlogs.spotify.exception;

public class InvalidOfficialSpotifyOwnerException extends RuntimeException {

    public InvalidOfficialSpotifyOwnerException(
            String actualSpotifyUserId,
            String expectedSpotifyUserId
    ) {
        super("Connected Spotify user " + actualSpotifyUserId
                + " does not match expected official owner "
                + expectedSpotifyUserId);
    }
}
