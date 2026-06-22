package com.marcoromanofinaa.jazzlogs.admin.editorial.album;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AlbumEditorialBestMoment(

        @NotBlank
        String introduction,

        @Size(max = 3)
        @Valid
        List<AlbumEditorialBestMomentItem> moments,

        @NotBlank
        String conclusion
) {
}
