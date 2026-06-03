package com.marcoromanofinaa.jazzlogs.chat.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Optional<ChatSession> findByIdAndUserId(UUID chatSessionId, UUID userId);

    List<ChatSession> findByUserIdOrderByLastInteractionAtDesc(UUID userId);
}
