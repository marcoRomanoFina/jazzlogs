package com.marcoromanofinaa.jazzlogs.logbook.infrastructure;

import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumLogRepository extends JpaRepository<AlbumLog, UUID> {

    List<AlbumLog> findAllByLogNumberIn(Collection<Integer> logNumbers);
}
