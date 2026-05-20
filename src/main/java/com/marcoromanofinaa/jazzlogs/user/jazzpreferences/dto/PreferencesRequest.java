package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PreferencesRequest(
        @Valid @NotNull UserJazzPreferencesDto userPreferences
) {}
