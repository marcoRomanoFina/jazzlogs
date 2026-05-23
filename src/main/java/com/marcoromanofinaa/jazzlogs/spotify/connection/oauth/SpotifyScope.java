package com.marcoromanofinaa.jazzlogs.spotify.connection.oauth;

public enum SpotifyScope {
    USER_READ_PRIVATE("user-read-private"),
    USER_READ_EMAIL("user-read-email"),
    USER_TOP_READ("user-top-read"),
    PLAYLIST_READ_PRIVATE("playlist-read-private"),
    PLAYLIST_READ_COLLABORATIVE("playlist-read-collaborative");

    private final String value;

    SpotifyScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
