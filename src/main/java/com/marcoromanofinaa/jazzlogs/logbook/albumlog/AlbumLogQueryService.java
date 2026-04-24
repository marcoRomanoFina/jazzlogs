package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlbumLogQueryService {

    private final AlbumLogRepository albumLogRepository;

    public List<AlbumLogResponse> findAll() {
        var results = albumLogRepository.findAllByOrderByLogNumberAsc()
                .stream()
                .map(AlbumLogResponse::from)
                .toList();
        log.debug("Loaded {} album logs", results.size());
        return results;
    }

    public AlbumLogResponse findByLogNumber(int logNumber) {
        log.debug("Loading album log {}", logNumber);
        return albumLogRepository.findByLogNumber(logNumber)
                .map(AlbumLogResponse::from)
                .orElseThrow(() -> new AlbumLogNotFoundException(logNumber));
    }
}
