package com.marcoromanofinaa.jazzlogs.chat.usage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    @Query("""
        select coalesce(sum(record.totalTokens), 0)
        from UsageRecord record
        where record.userId = :userId
          and record.createdAt between :from and :to
    """)
    Integer sumTotalTokensByUserIdAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
        select coalesce(sum(record.costMicrosUsd), 0)
        from UsageRecord record
        where record.userId = :userId
          and record.createdAt between :from and :to
    """)
    Long sumCostMicrosUsdByUserIdAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
