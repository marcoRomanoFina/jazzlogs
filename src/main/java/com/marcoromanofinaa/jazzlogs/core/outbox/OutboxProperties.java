package com.marcoromanofinaa.jazzlogs.core.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.outbox")
public record OutboxProperties(
        Processing processing
) {

    public record Processing(
            int batchSize,
            int maxRetries,
            Duration retryDelay,
            int retryBackoffMultiplier,
            String cron,
            String zone
    ) {
    }
}
