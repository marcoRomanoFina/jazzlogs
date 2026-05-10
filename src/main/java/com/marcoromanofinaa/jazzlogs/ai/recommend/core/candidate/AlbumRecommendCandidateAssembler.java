package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogBestMoment;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlbumRecommendCandidateAssembler implements RecommendCandidateAssembler<AlbumRecommendCandidate> {

    private static final String SOURCE_ID_METADATA = "sourceId";
    private static final String SEMANTIC_DOCUMENT_ID_METADATA = "semanticDocumentId";

    private final AlbumLogRepository albumLogRepository;

    @Override
    public AlbumRecommendCandidate assemble(Document document) {
        var sourceId = UUID.fromString(requiredMetadata(document, SOURCE_ID_METADATA));
        var albumLog = albumLogRepository.findWithSpotifyAlbumById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("AlbumLog not found for sourceId=" + sourceId));
        var spotifyAlbum = Optional.ofNullable(albumLog.getSpotifyAlbum());

        return new AlbumRecommendCandidate(
                document.getScore(),
                requiredMetadata(document, SEMANTIC_DOCUMENT_ID_METADATA),
                albumLog.getId(),
                albumLog.getLogNumber(),
                albumLog.getAlbum(),
                albumLog.getArtist(),
                new AlbumRecommendDecisionContext(
                        albumLog.getStyle(),
                        albumLog.getVocalProfile(),
                        albumLog.getReleaseYear(),
                        spotifyAlbum.map(album -> album.getTotalTracks()).orElse(null),
                        spotifyAlbum.map(album -> album.getReleaseDate()).orElse(null),
                        toList(albumLog.getMoods()),
                        albumLog.getTier(),
                        toList(albumLog.getVibe()),
                        albumLog.getEnergy(),
                        albumLog.getMoodIntensity(),
                        albumLog.getAccessibility(),
                        toBestMoment(albumLog.getBestMoment()),
                        toList(albumLog.getListeningContext()),
                        albumLog.getNotes(),
                        albumLog.getWhyItMatters(),
                        albumLog.getEditorialNote(),
                        albumLog.getRecommendedIf(),
                        albumLog.getAvoidIf(),
                        albumLog.getAlbumContext(),
                        albumLog.getCaption()
                ),
                new AlbumRecommendDeliveryMetadata(
                        albumLog.getInstagramPermalink(),
                        spotifyAlbum.map(album -> album.getSpotifyUrl()).orElse(null),
                        spotifyAlbum.map(album -> album.getCoverImageUrl()).orElse(null)
                )
        );
    }

    private String requiredMetadata(Document document, String key) {
        return Optional.ofNullable(document.getMetadata().get(key))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing metadata key '%s' in retrieved document".formatted(key)
                ));
    }

    private List<String> toList(String[] values) {
        return Optional.ofNullable(values)
                .map(Arrays::asList)
                .orElseGet(List::of);
    }

    private AlbumRecommendBestMoment toBestMoment(AlbumLogBestMoment bestMoment) {
        return Optional.ofNullable(bestMoment)
                .map(value -> new AlbumRecommendBestMoment(
                        value.introduccion(),
                        value.momentos().stream()
                                .map(item -> new AlbumRecommendBestMomentItem(item.momento(), item.descripcion()))
                                .toList(),
                        value.conclusion()
                ))
                .orElse(null);
    }
}
