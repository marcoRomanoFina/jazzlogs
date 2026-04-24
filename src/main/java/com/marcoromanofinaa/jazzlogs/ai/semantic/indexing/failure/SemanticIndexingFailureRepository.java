package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.failure;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemanticIndexingFailureRepository extends JpaRepository<SemanticIndexingFailure, UUID> {

    Optional<SemanticIndexingFailure> findByTypeAndSourceIdentifier(SemanticDocumentType type, String sourceIdentifier);

    void deleteByTypeAndSourceIdentifier(SemanticDocumentType type, String sourceIdentifier);

    java.util.List<SemanticIndexingFailure> findAllByOrderByLastFailedAtAsc(Pageable pageable);
}
