package com.marcoromanofinaa.jazzlogs.user.subscription.repository;

import com.marcoromanofinaa.jazzlogs.user.subscription.model.UserSubscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findByUser_Id(UUID userId);
}
