package com.marcoromanofinaa.jazzlogs.spotify.sync.taste;

public enum SpotifyTimeRange {
    SHORT_TERM("short_term"),
    MEDIUM_TERM("medium_term"),
    LONG_TERM("long_term");

    private final String apiValue;

    SpotifyTimeRange(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
