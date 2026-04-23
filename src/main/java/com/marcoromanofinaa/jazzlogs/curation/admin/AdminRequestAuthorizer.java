package com.marcoromanofinaa.jazzlogs.curation.admin;

import com.marcoromanofinaa.jazzlogs.core.exception.AdminApiKeyNotConfiguredException;
import com.marcoromanofinaa.jazzlogs.core.exception.InvalidAdminApiKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminRequestAuthorizer {

    private final AdminApiProperties adminProperties;

    public void authorize(String adminKey) {
        if (adminProperties.apiKey() == null || adminProperties.apiKey().isBlank()) {
            throw new AdminApiKeyNotConfiguredException();
        }

        if (!adminProperties.apiKey().equals(adminKey)) {
            log.warn("Rejected admin request due to invalid admin API key");
            throw new InvalidAdminApiKeyException();
        }
    }
}
