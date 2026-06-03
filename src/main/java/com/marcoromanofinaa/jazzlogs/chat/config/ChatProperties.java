package com.marcoromanofinaa.jazzlogs.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.chat")
public record ChatProperties(
        History history
) {

    public record History(
            int recentExchangesLimit
    ) {
    }
}
