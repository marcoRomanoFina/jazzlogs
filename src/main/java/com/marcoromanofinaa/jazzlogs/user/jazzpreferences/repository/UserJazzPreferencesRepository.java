package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.repository;

import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.UserJazzPreferences;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJazzPreferencesRepository extends JpaRepository<UserJazzPreferences, UUID> {

    Optional<UserJazzPreferences> findByUserId(UUID userId);
}
