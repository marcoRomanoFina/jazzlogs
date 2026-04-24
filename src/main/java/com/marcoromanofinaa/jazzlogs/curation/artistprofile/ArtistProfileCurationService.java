package com.marcoromanofinaa.jazzlogs.curation.artistprofile;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event.SemanticIndexingRequestPublisher;
import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertArtistProfileRequest;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfile;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileData;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArtistProfileCurationService {

    private final ArtistProfileRepository artistProfileRepository;
    private final SemanticIndexingRequestPublisher indexingRequestPublisher;

    @Transactional
    public boolean upsert(UpsertArtistProfileRequest request) {
        var data = new ArtistProfileData(
                request.spotifyArtistId(),
                request.name(),
                request.primaryInstrument(),
                request.mainStyles().toArray(String[]::new),
                request.signatureSound(),
                request.artistContext(),
                request.jazzlogsTake(),
                request.recommendedEntryPoint(),
                request.bestFor().toArray(String[]::new),
                request.avoidIf(),
                request.relatedArtists().toArray(String[]::new),
                request.importance(),
                request.logAppearances().toArray(Integer[]::new)
        );

        artistProfileRepository.findBySpotifyArtistId(request.spotifyArtistId())
                .ifPresentOrElse(existingArtistProfile -> {
                    log.info("Updating artist profile curation for spotifyArtistId={}", request.spotifyArtistId());
                    existingArtistProfile.update(data);
                    artistProfileRepository.save(existingArtistProfile);
                }, () -> {
                    log.info("Creating artist profile curation for spotifyArtistId={}", request.spotifyArtistId());
                    artistProfileRepository.save(ArtistProfile.create(data));
                });
        indexingRequestPublisher.requestArtistProfileReindex(request.spotifyArtistId());
        return true;
    }
}
