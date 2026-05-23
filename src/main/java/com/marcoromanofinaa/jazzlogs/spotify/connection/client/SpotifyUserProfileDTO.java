package com.marcoromanofinaa.jazzlogs.spotify.connection.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpotifyUserProfileDTO(
        @JsonProperty("id") String spotifyUserId,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("country") String country,
        @JsonProperty("product") String product
) {
}
