package com.marcoromanofinaa.jazzlogs.logbook.application;

import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlbumLogQueryService {

    private final AlbumLogRepository albumLogRepository;

    public List<AlbumLogResponse> findAll() {
        return albumLogRepository.findAllByOrderByLogNumberAsc()
                .stream()
                .map(AlbumLogResponse::from)
                .toList();
    }

    public AlbumLogResponse findByLogNumber(int logNumber) {
        return albumLogRepository.findByLogNumber(logNumber)
                .map(AlbumLogResponse::from)
                .orElseThrow(() -> new AlbumLogNotFoundException(logNumber));
    }
}
