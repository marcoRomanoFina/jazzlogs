package com.marcoromanofinaa.jazzlogs.spotify.connection.oauth;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class OAuthStateGenerator {

    private static final int STATE_BYTES = 24;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        var randomBytes = new byte[STATE_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
