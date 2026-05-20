package com.marcoromanofinaa.jazzlogs.user.jazzpreferences;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PreferencesRequest(
        @Valid @NotNull UserJazzPreferencesDto userPreferences
) {}
