package com.marcoromanofinaa.jazzlogs.admin.editorial.indexing;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLog;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLogBestMoment;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLogMainArtist;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLogPersonnel;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.model.ArtistLog;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.model.TrackLog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class EditorialEmbeddingDocumentBuilder {

    public List<Document> buildAlbumLogDocuments(AlbumLog albumLog) {
        var metadata = albumMetadata(albumLog);

        return List.of(
                buildDocument(
                        documentId("ALBUM_LOG", albumLog.getId(), "ALBUM_OVERVIEW"),
                        buildAlbumOverviewText(albumLog),
                        metadata,
                        "ALBUM_OVERVIEW"
                ),
                buildDocument(
                        documentId("ALBUM_LOG", albumLog.getId(), "ALBUM_MOOD_GUIDE"),
                        buildAlbumMoodGuideText(albumLog),
                        metadata,
                        "ALBUM_MOOD_GUIDE"
                ),
                buildDocument(
                        documentId("ALBUM_LOG", albumLog.getId(), "ALBUM_BEST_MOMENTS"),
                        buildAlbumBestMomentsText(albumLog),
                        metadata,
                        "ALBUM_BEST_MOMENTS"
                ),
                buildDocument(
                        documentId("ALBUM_LOG", albumLog.getId(), "ALBUM_RECOMMENDATION_GUIDE"),
                        buildAlbumRecommendationGuideText(albumLog),
                        metadata,
                        "ALBUM_RECOMMENDATION_GUIDE"
                ),
                buildDocument(
                        documentId("ALBUM_LOG", albumLog.getId(), "ALBUM_PERSONNEL"),
                        buildAlbumPersonnelText(albumLog),
                        metadata,
                        "ALBUM_PERSONNEL"
                )
        );
    }

    public List<Document> buildTrackLogDocuments(TrackLog trackLog) {
        var metadata = trackMetadata(trackLog);

        return List.of(
                buildDocument(
                        documentId("TRACK_LOG", trackLog.getId(), "TRACK_OVERVIEW"),
                        buildTrackOverviewText(trackLog),
                        metadata,
                        "TRACK_OVERVIEW"
                ),
                buildDocument(
                        documentId("TRACK_LOG", trackLog.getId(), "TRACK_FEEL"),
                        buildTrackFeelText(trackLog),
                        metadata,
                        "TRACK_FEEL"
                ),
                buildDocument(
                        documentId("TRACK_LOG", trackLog.getId(), "TRACK_RECOMMENDATION_GUIDE"),
                        buildTrackRecommendationGuideText(trackLog),
                        metadata,
                        "TRACK_RECOMMENDATION_GUIDE"
                )
        );
    }

    public List<Document> buildArtistLogDocuments(ArtistLog artistLog) {
        var metadata = artistMetadata(artistLog);

        return List.of(
                buildDocument(
                        documentId("ARTIST_LOG", artistLog.getId(), "ARTIST_OVERVIEW"),
                        buildArtistOverviewText(artistLog),
                        metadata,
                        "ARTIST_OVERVIEW"
                ),
                buildDocument(
                        documentId("ARTIST_LOG", artistLog.getId(), "ARTIST_SOUND_PROFILE"),
                        buildArtistSoundProfileText(artistLog),
                        metadata,
                        "ARTIST_SOUND_PROFILE"
                ),
                buildDocument(
                        documentId("ARTIST_LOG", artistLog.getId(), "ARTIST_RECOMMENDATION_GUIDE"),
                        buildArtistRecommendationGuideText(artistLog),
                        metadata,
                        "ARTIST_RECOMMENDATION_GUIDE"
                )
        );
    }

    private Document buildDocument(
            String id,
            String text,
            Map<String, Object> baseMetadata,
            String section
    ) {
        var metadata = new LinkedHashMap<>(baseMetadata);
        metadata.put("section", section);

        return Document.builder()
                .id(id)
                .text(text)
                .metadata(metadata)
                .build();
    }

    private Map<String, Object> albumMetadata(AlbumLog albumLog) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("sourceType", "ALBUM_LOG");
        metadata.put("sourceId", albumLog.getId().toString());
        putIfPresent(metadata, "spotifyAlbumId", albumLog.getSpotifyAlbumId());
        putIfPresent(metadata, "logNumber", albumLog.getLogNumber());
        putIfPresent(metadata, "album", albumLog.getAlbumName());
        putIfPresent(metadata, "mainArtists", joinMainArtists(albumLog.getMainArtists()));
        putIfPresent(metadata, "tier", albumLog.getTier());
        return metadata;
    }

    private Map<String, Object> trackMetadata(TrackLog trackLog) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("sourceType", "TRACK_LOG");
        metadata.put("sourceId", trackLog.getId().toString());
        putIfPresent(metadata, "spotifyTrackId", trackLog.getSpotifyTrackId());
        putIfPresent(metadata, "spotifyAlbumId", trackLog.getSpotifyAlbumId());
        putIfPresent(metadata, "track", trackLog.getTrackName());
        putIfPresent(metadata, "album", trackLog.getAlbumName());
        putIfPresent(metadata, "logNumber", trackLog.getLogNumber());
        putIfPresent(metadata, "tier", trackLog.getTier());
        return metadata;
    }

    private Map<String, Object> artistMetadata(ArtistLog artistLog) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("sourceType", "ARTIST_LOG");
        metadata.put("sourceId", artistLog.getId().toString());
        putIfPresent(metadata, "spotifyArtistId", artistLog.getSpotifyArtistId());
        putIfPresent(metadata, "name", artistLog.getArtistName());
        putIfPresent(metadata, "primaryInstrument", artistLog.getPrimaryInstrument());
        putIfPresent(metadata, "importance", artistLog.getWhyItMatters());
        return metadata;
    }

    private String buildAlbumOverviewText(AlbumLog albumLog) {
        return buildParagraph(
                sentence("Album overview for %s", albumLog.getAlbumName()),
                sentence("%s is presented as a %s record", joinMainArtists(albumLog.getMainArtists()), albumLog.getStyle()),
                sentence("It is framed around the release year %s", albumLog.getReleaseYear()),
                sentence("%s", albumLog.getAlbumContext()),
                sentence("%s", albumLog.getCaptionEssence()),
                sentence("%s", albumLog.getWhyItMatters()),
                sentence("%s", albumLog.getEditorialNote())
        );
    }

    private String buildAlbumMoodGuideText(AlbumLog albumLog) {
        return buildParagraph(
                sentence("Mood guide for %s", albumLog.getAlbumName()),
                sentence("The album moves through moods such as %s", joinValues(albumLog.getMoods())),
                sentence("Its vibe is described as %s", joinValues(albumLog.getVibe())),
                sentence("The energy feels %s, with mood intensity %s and accessibility %s",
                        albumLog.getEnergy(), albumLog.getMoodIntensity(), albumLog.getAccessibility()),
                sentence("It works especially well in listening contexts like %s", joinValues(albumLog.getListeningContext())),
                sentence("The vocal profile is %s", albumLog.getVocalProfile())
        );
    }

    private String buildAlbumBestMomentsText(AlbumLog albumLog) {
        var bestMoment = albumLog.getBestMoment();
        return buildParagraph(
                sentence("Best moments for %s", albumLog.getAlbumName()),
                sentence("%s", bestMoment == null ? null : bestMoment.introduccion()),
                sentence("%s", formatBestMoments(bestMoment)),
                sentence("%s", bestMoment == null ? null : bestMoment.conclusion())
        );
    }

    private String buildAlbumRecommendationGuideText(AlbumLog albumLog) {
        return buildParagraph(
                sentence("Recommendation guide for %s", albumLog.getAlbumName()),
                sentence("This album sits in tier %s and its accessibility is described as %s",
                        albumLog.getTier(), albumLog.getAccessibility()),
                sentence("%s", albumLog.getRecommendedIf()),
                sentence("%s", albumLog.getAvoidIf()),
                sentence("Ideal listening contexts include %s", joinValues(albumLog.getListeningContext()))
        );
    }

    private String buildAlbumPersonnelText(AlbumLog albumLog) {
        return buildParagraph(
                sentence("Personnel notes for %s", albumLog.getAlbumName()),
                sentence("The main artists are %s", joinMainArtists(albumLog.getMainArtists())),
                sentence("Relevant personnel includes %s", formatPersonnel(albumLog.getPersonnel())),
                sentence("The record is shaped by a %s vocal profile within a %s setting",
                        albumLog.getVocalProfile(), albumLog.getStyle())
        );
    }

    private String buildTrackOverviewText(TrackLog trackLog) {
        return buildParagraph(
                sentence("Track overview for %s from the album %s", trackLog.getTrackName(), trackLog.getAlbumName()),
                sentence("Within the album, the track functions as %s", trackLog.getAlbumRole()),
                sentence("Its composition type is %s", trackLog.getCompositionType()),
                sentence("A key moment in the track is %s", trackLog.getBestMoment()),
                sentence("%s", trackLog.getWhyItHits()),
                sentence("%s", trackLog.getEditorialNote())
        );
    }

    private String buildTrackFeelText(TrackLog trackLog) {
        return buildParagraph(
                sentence("Feel profile for %s", trackLog.getTrackName()),
                sentence("The vibe comes through as %s", joinValues(trackLog.getVibe())),
                sentence("Its energy is %s, with mood intensity %s and accessibility %s",
                        trackLog.getEnergy(), trackLog.getMoodIntensity(), trackLog.getAccessibility()),
                sentence("The tempo feel is %s and the rhythm feel is %s",
                        trackLog.getTempoFeel(), trackLog.getRhythmFeel()),
                sentence("The instrumental focus is %s, while the vocal style is %s and the vocal profile is %s",
                        trackLog.getInstrumentFocus(), trackLog.getVocalStyle(), trackLog.getVocalProfile()),
                sentence("Standout status: %s", trackLog.getStandout()),
                sentence("Standout tags: %s", joinValues(trackLog.getStandoutTags()))
        );
    }

    private String buildTrackRecommendationGuideText(TrackLog trackLog) {
        return buildParagraph(
                sentence("Recommendation guide for the track %s", trackLog.getTrackName()),
                sentence("It belongs to tier %s and its accessibility is %s", trackLog.getTier(), trackLog.getAccessibility()),
                sentence("%s", trackLog.getRecommendedIf()),
                sentence("%s", trackLog.getAvoidIf()),
                sentence("Suggested listening contexts include %s", joinValues(trackLog.getListeningContext()))
        );
    }

    private String buildArtistOverviewText(ArtistLog artistLog) {
        return buildParagraph(
                sentence("Artist overview for %s", artistLog.getArtistName()),
                sentence("The artist is primarily associated with %s", artistLog.getPrimaryInstrument()),
                sentence("Main styles include %s", joinValues(artistLog.getMainStyles())),
                sentence("%s", artistLog.getArtistContext()),
                sentence("%s", artistLog.getWhyItMatters()),
                sentence("%s", artistLog.getEditorialNote()),
                sentence("This artist appears in logs such as %s", joinIntegers(artistLog.getAppearsInLogs()))
        );
    }

    private String buildArtistSoundProfileText(ArtistLog artistLog) {
        return buildParagraph(
                sentence("Sound profile for %s", artistLog.getArtistName()),
                sentence("%s", artistLog.getSoundProfile()),
                sentence("The primary instrument is %s", artistLog.getPrimaryInstrument()),
                sentence("Stylistically, the artist moves through %s", joinValues(artistLog.getMainStyles())),
                sentence("Related artists include %s", joinValues(artistLog.getRelatedArtists())),
                sentence("Best listening moments include %s", joinValues(artistLog.getBestListeningMoments()))
        );
    }

    private String buildArtistRecommendationGuideText(ArtistLog artistLog) {
        return buildParagraph(
                sentence("Recommendation guide for %s", artistLog.getArtistName()),
                sentence("A good entry point is log %s", artistLog.getEntryPointLogId()),
                sentence("Best listening moments include %s", joinValues(artistLog.getBestListeningMoments())),
                sentence("%s", artistLog.getAvoidIf()),
                sentence("Useful related artists are %s", joinValues(artistLog.getRelatedArtists())),
                sentence("%s", artistLog.getWhyItMatters())
        );
    }

    private String documentId(String sourceType, UUID sourceId, String section) {
        return sourceType + ":" + sourceId + ":" + section;
    }

    private void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof String stringValue && stringValue.isBlank()) {
            return;
        }

        metadata.put(key, value);
    }

    private String sentence(String pattern, Object... values) {
        if (java.util.Arrays.stream(values).anyMatch(this::isBlankValue)) {
            return null;
        }

        var text = pattern.formatted(values).trim();
        if (text.isBlank()) {
            return null;
        }
        if (!text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")) {
            text = text + ".";
        }
        return text;
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String stringValue && stringValue.isBlank()) {
            return true;
        }
        return false;
    }

    private String buildParagraph(String... sentences) {
        return java.util.Arrays.stream(sentences)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private String joinMainArtists(List<AlbumLogMainArtist> artists) {
        if (artists == null || artists.isEmpty()) {
            return null;
        }
        return artists.stream()
                .map(AlbumLogMainArtist::name)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String formatBestMoments(AlbumLogBestMoment bestMoment) {
        if (bestMoment == null || bestMoment.momentos() == null || bestMoment.momentos().isEmpty()) {
            return null;
        }

        return bestMoment.momentos().stream()
                .map(moment -> {
                    var parts = new ArrayList<String>();
                    if (moment.name() != null && !moment.name().isBlank()) {
                        parts.add(moment.name() + ":");
                    }
                    if (moment.description() != null && !moment.description().isBlank()) {
                        parts.add(moment.description());
                    }
                    return String.join(" ", parts);
                })
                .filter(entry -> !entry.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String formatPersonnel(List<AlbumLogPersonnel> personnel) {
        if (personnel == null || personnel.isEmpty()) {
            return null;
        }

        return personnel.stream()
                .map(member -> {
                    var parts = new ArrayList<String>();
                    if (member.name() != null && !member.name().isBlank()) {
                        parts.add(member.name());
                    }
                    if (member.role() != null && !member.role().isBlank()) {
                        parts.add(member.role());
                    }
                    return String.join(" - ", parts);
                })
                .filter(entry -> !entry.isBlank())
                .collect(Collectors.joining("; "));
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String joinIntegers(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}
