package com.marcoromanofinaa.jazzlogs.logbook.web;

import com.marcoromanofinaa.jazzlogs.logbook.application.AlbumLogNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AlbumLogExceptionHandler {

    @ExceptionHandler(AlbumLogNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAlbumLogNotFound(AlbumLogNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    public record ErrorResponse(String message) {
    }
}
