package com.marcoromanofinaa.jazzlogs.auth.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jazzlogs.auth.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration expiration
) {}
