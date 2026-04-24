package com.marcoromanofinaa.jazzlogs.logbook.artistprofile;

public record ArtistProfileData(
        String spotifyArtistId,
        String name,
        String primaryInstrument,
        String[] mainStyles,
        String signatureSound,
        String artistContext,
        String jazzlogsTake,
        String recommendedEntryPoint,
        String[] bestFor,
        String avoidIf,
        String[] relatedArtists,
        String importance,
        Integer[] logAppearances
) {
}
