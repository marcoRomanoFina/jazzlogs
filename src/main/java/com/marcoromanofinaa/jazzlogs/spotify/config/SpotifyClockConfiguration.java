package com.marcoromanofinaa.jazzlogs.spotify.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpotifyClockConfiguration {

    @Bean
    Clock spotifyClock() {
        return Clock.systemUTC();
    }
}
