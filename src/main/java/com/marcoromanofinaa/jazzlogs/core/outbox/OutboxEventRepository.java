package com.marcoromanofinaa.jazzlogs.core.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            select *
            from outbox_events
            where status = 'PENDING'
              and (next_retry_at is null or next_retry_at <= :now)
            order by created_at asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> findPendingEventsForProcessing(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteAllByStatusIn(List<OutboxEventStatus> statuses);
}
