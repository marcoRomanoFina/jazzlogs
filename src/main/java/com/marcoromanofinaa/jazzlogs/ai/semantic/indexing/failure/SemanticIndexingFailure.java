package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.failure;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "semantic_indexing_failures",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_semantic_indexing_failures_type_source",
                columnNames = {"type", "source_identifier"}
        )
)
public class SemanticIndexingFailure {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SemanticDocumentType type;

    @Column(name = "source_identifier", nullable = false, length = 128)
    private String sourceIdentifier;

    @Column(name = "failure_type", nullable = false, length = 256)
    private String failureType;

    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "first_failed_at", nullable = false)
    private Instant firstFailedAt;

    @Column(name = "last_failed_at", nullable = false)
    private Instant lastFailedAt;

    public static SemanticIndexingFailure create(SemanticDocumentType type, String sourceIdentifier) {
        var failure = new SemanticIndexingFailure();
        failure.type = type;
        failure.sourceIdentifier = sourceIdentifier;
        return failure;
    }

    public void recordFailure(String failureType, String failureMessage, int failedAttempts, Instant failedAt) {
        this.failureType = failureType;
        this.failureMessage = failureMessage;
        this.failedAttempts = failedAttempts;
        if (this.firstFailedAt == null) {
            this.firstFailedAt = failedAt;
        }
        this.lastFailedAt = failedAt;
    }
}
