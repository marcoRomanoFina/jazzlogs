package com.marcoromanofinaa.jazzlogs.core.exception;

import java.nio.file.Path;
import org.springframework.http.HttpStatus;

public class SeedFileReadException extends ApplicationException {

    public SeedFileReadException(String label, Path path, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read %s seed file: %s".formatted(label, path));
        initCause(cause);
    }
}
