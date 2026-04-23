package com.marcoromanofinaa.jazzlogs.core.exception;

import java.nio.file.Path;
import org.springframework.http.HttpStatus;

public class SeedFileNotFoundException extends ApplicationException {

    public SeedFileNotFoundException(String label, Path path) {
        super(HttpStatus.BAD_REQUEST, "%s seed file does not exist: %s".formatted(label, path));
    }
}
