package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;

public record AlbumLogBestMoment(
        @NotBlank
        @Size(max = 2000)
        String introduccion,
        @Valid
        @Size(max = 10)
        List<AlbumLogBestMomentItem> momentos,
        @NotBlank
        @Size(max = 2000)
        String conclusion
) {
    public AlbumLogBestMoment {
        momentos = momentos == null ? List.of() : List.copyOf(momentos);
    }

    public String flattened() {
        var parts = new java.util.ArrayList<String>();
        parts.add(introduccion);
        if (!momentos.isEmpty()) {
            parts.add(momentos.stream()
                    .map(momento -> "%s: %s".formatted(momento.momento(), momento.descripcion()))
                    .collect(Collectors.joining(" ")));
        }
        parts.add(conclusion);
        return parts.stream()
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }
}
