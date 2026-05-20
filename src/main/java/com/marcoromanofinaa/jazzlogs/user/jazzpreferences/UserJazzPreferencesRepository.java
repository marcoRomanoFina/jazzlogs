package com.marcoromanofinaa.jazzlogs.user.jazzpreferences;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJazzPreferencesRepository extends JpaRepository<UserJazzPreferences, UUID> {

    Optional<UserJazzPreferences> findByUserId(UUID userId);
}
