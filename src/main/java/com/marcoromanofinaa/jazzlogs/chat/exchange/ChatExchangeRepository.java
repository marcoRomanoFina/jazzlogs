package com.marcoromanofinaa.jazzlogs.chat.exchange;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatExchangeRepository extends JpaRepository<ChatExchange, UUID> {

    List<ChatExchange> findByChatSessionIdOrderByCreatedAtAsc(UUID chatSessionId);

    List<ChatExchange> findByChatSessionIdOrderByCreatedAtDesc(UUID chatSessionId, Pageable pageable);

    void deleteByChatSessionId(UUID chatSessionId);
}
