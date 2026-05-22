package com.marcoromanofinaa.jazzlogs.spotify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SpotifyClientConfiguration {

    @Bean("spotifyRestClient")
    RestClient spotifyRestClient(RestClient.Builder restClientBuilder, SpotifyProperties spotifyProperties) {
        return restClientBuilder
                .baseUrl(spotifyProperties.api().baseUrl())
                .build();
    }

    @Bean("spotifyTokenRestClient")
    RestClient spotifyTokenRestClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder.build();
    }
}
