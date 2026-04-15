package com.marcoromanofinaa.jazzlogs.logbook.web;

import com.marcoromanofinaa.jazzlogs.logbook.application.AlbumLogQueryService;
import com.marcoromanofinaa.jazzlogs.logbook.application.AlbumLogResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class AlbumLogController {

    private final AlbumLogQueryService albumLogQueryService;

    @GetMapping
    public List<AlbumLogResponse> getAlbumLogs() {
        return albumLogQueryService.findAll();
    }

    @GetMapping("/{logNumber}")
    public AlbumLogResponse getAlbumLog(@PathVariable int logNumber) {
        return albumLogQueryService.findByLogNumber(logNumber);
    }
}
