package com.marcoromanofinaa.jazzlogs.admin.editorial.embedding;

import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.AlbumNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ArtistNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.TrackNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship.AlbumSidemanRelationship;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship.ContainsTrackRelationship;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship.TrackPerformedByArtistRelationship;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class EditorialEmbeddingTextBuilder {

    public String buildAlbumEmbeddingText(AlbumNode album) {
        return buildParagraph(
                buildParagraph(
                        sentence("Album overview for %s", album.getName()),
                        sentence("The record is led by %s", extractAndJoinNames(album.getLeaderArtists(), ArtistNode::getName)),
                        sentence("It is framed around the release date %s", album.getReleaseDate()),
                        sentence("%s", album.getAlbumContext()),
                        sentence("%s", album.getCaptionEssence()),
                        sentence("%s", album.getWhyItMatters()),
                        sentence("%s", album.getEditorialNote())
                ),
                buildParagraph(
                        sentence("Mood guide for %s", album.getName()),
                        sentence("The styles include %s", extractAndJoinNames(album.getStyles(), com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.StyleNode::getName)),
                        sentence("The moods include %s", extractAndJoinNames(album.getMoods(), com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.MoodNode::getName)),
                        sentence("The energy is %s with accessibility %s", album.getEnergy(), album.getAccessibility()),
                        sentence("It works especially well in contexts like %s", extractAndJoinNames(album.getContexts(), com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ContextNode::getName)),
                        sentence("The vocal profile is %s", album.getVocalProfile())
                ),
                buildParagraph(
                        sentence("Recommendation guide for %s", album.getName()),
                        sentence("This album sits in tier %s", album.getTier()),
                        sentence("%s", album.getRecommendedIf()),
                        sentence("%s", album.getAvoidIf())
                ),
                buildParagraph(
                        sentence("Track map for %s", album.getName()),
                        sentence("%s", formatTrackMap(album.getTracks())),
                        sentence("Relevant sidemen include %s", formatSidemen(album.getSidemen()))
                )
        );
    }

    public String buildTrackEmbeddingText(TrackNode track) {
        return buildParagraph(
                buildParagraph(
                        sentence("Track overview for %s", track.getName()),
                        sentence("It belongs to the album %s", track.getAlbum() == null ? null : track.getAlbum().getName()),
                        sentence("The main artists are %s", joinTrackArtists(track)),
                        sentence("Within the album, the track functions as %s", trackRole(track)),
                        sentence("Its composition type is %s", track.getCompositionType()),
                        sentence("A key moment in the track is %s", track.getBestMoment()),
                        sentence("%s", track.getWhyItHits()),
                        sentence("%s", track.getEditorialNote())
                ),
                buildParagraph(
                        sentence("Feel profile for %s", track.getName()),
                        sentence("The moods come through as %s", extractAndJoinNames(track.getMoods(), com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.MoodNode::getName)),
                        sentence("Its energy is %s with accessibility %s", track.getEnergy(), track.getAccessibility()),
                        sentence("The tempo feel is %s", track.getTempoFeel()),
                        sentence("The rhythm focus is %s", track.getRhythm() == null ? null : track.getRhythm().getName()),
                        sentence("The instrumental focus is %s",
                                extractAndJoinNames(track.getInstrumentFocus(), com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.InstrumentNode::getName)),
                        sentence("The vocal style is %s", track.getVocalStyle()),
                        sentence("The vocal profile is %s", track.getVocalProfile()),
                        sentence("Standout status is %s", track.getIsStandout())
                ),
                buildParagraph(
                        sentence("Recommendation guide for the track %s", track.getName()),
                        sentence("It belongs to tier %s", track.getTier()),
                        sentence("%s", track.getRecommendedIf()),
                        sentence("%s", track.getAvoidIf()),
                        sentence("Suggested listening contexts include %s",
                                extractAndJoinNames(track.getContexts(), com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ContextNode::getName))
                )
        );
    }

    public String buildArtistEmbeddingText(ArtistNode artist) {
        return buildParagraph(
                buildParagraph(
                        sentence("Artist overview for %s", artist.getName()),
                        sentence("The artist is primarily associated with %s",
                                artist.getPrimaryInstrument() == null ? null : artist.getPrimaryInstrument().getName()),
                        sentence("Main styles include %s", extractAndJoinNames(artist.getMainStyles(), com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.StyleNode::getName)),
                        sentence("%s", artist.getArtistContext()),
                        sentence("%s", artist.getEditorialImportance()),
                        sentence("%s", artist.getJazzlogsTake()),
                        sentence("This artist appears in logs such as %s", joinIntegers(artist.getLogAppearances()))
                ),
                buildParagraph(
                        sentence("Sound profile for %s", artist.getName()),
                        sentence("%s", artist.getSignatureSound()),
                        sentence("Best listening contexts include %s",
                                extractAndJoinNames(artist.getBestForContexts(), com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ContextNode::getName)),
                        sentence("Related artists include %s", extractAndJoinNames(artist.getRelatedArtists(), ArtistNode::getName)),
                        sentence("Leader of albums such as %s", extractAndJoinNames(artist.getLeaderOfAlbums(), AlbumNode::getName))
                ),
                buildParagraph(
                        sentence("Recommendation guide for %s", artist.getName()),
                        sentence("A good entry point in the catalog is %s", entryPointDescription(artist)),
                        sentence("%s", artist.getAvoidIf())
                )
        );
    }

    private String trackRole(TrackNode track) {
        if (track.getAlbum() == null || track.getAlbum().getTracks() == null) {
            return null;
        }
        return track.getAlbum().getTracks().stream()
                .filter(relationship -> relationship.getTrack() != null)
                .filter(relationship -> Objects.equals(relationship.getTrack().getId(), track.getId()))
                .map(ContainsTrackRelationship::getTrackRole)
                .filter(Objects::nonNull)
                .filter(role -> !role.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String formatTrackMap(java.util.Set<ContainsTrackRelationship> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return null;
        }
        return tracks.stream()
                .sorted(Comparator.comparing(
                        ContainsTrackRelationship::getTrackNumber,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .map(relationship -> {
                    var track = relationship.getTrack();
                    if (track == null || track.getName() == null || track.getName().isBlank()) {
                        return null;
                    }
                    var parts = new ArrayList<String>();
                    parts.add(track.getName());
                    if (relationship.getTrackRole() != null && !relationship.getTrackRole().isBlank()) {
                        parts.add("role " + relationship.getTrackRole());
                    }
                    if (relationship.getTrackNumber() != null) {
                        parts.add("track number " + relationship.getTrackNumber());
                    }
                    return String.join(", ", parts);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));
    }

    private String formatSidemen(java.util.Set<AlbumSidemanRelationship> sidemen) {
        if (sidemen == null || sidemen.isEmpty()) {
            return null;
        }
        return sidemen.stream()
                .map(relationship -> {
                    var parts = new ArrayList<String>();
                    if (relationship.getArtist() != null
                            && relationship.getArtist().getName() != null
                            && !relationship.getArtist().getName().isBlank()) {
                        parts.add(relationship.getArtist().getName());
                    }
                    if (relationship.getInstruments() != null && !relationship.getInstruments().isEmpty()) {
                        parts.add(String.join(", ", relationship.getInstruments()));
                    }
                    return String.join(" - ", parts);
                })
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("; "));
    }

    private String joinTrackArtists(TrackNode track) {
        if (track.getPerformedByArtists() == null || track.getPerformedByArtists().isEmpty()) {
            return track.getEntryPointToArtist() == null ? null : track.getEntryPointToArtist().getName();
        }
        return track.getPerformedByArtists().stream()
                .sorted(Comparator.comparing(
                        TrackPerformedByArtistRelationship::getPosition,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .map(relationship -> relationship.getArtist() == null ? null : relationship.getArtist().getName())
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
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

    private String entryPointDescription(ArtistNode artist) {
        if (artist.getLeaderOfAlbums() == null || artist.getLeaderOfAlbums().isEmpty()) {
            return "this artist's graph profile";
        }
        var albumEntryPoint = artist.getLeaderOfAlbums().stream()
                .map(AlbumNode::getEntryPointToArtist)
                .filter(entryArtist -> entryArtist != null && Objects.equals(entryArtist.getId(), artist.getId()))
                .findFirst();
        if (albumEntryPoint.isPresent()) {
            return "albums in this artist's catalog";
        }
        return "this artist's graph profile";
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
        return value instanceof String stringValue && stringValue.isBlank();
    }

    private String buildParagraph(String... sentences) {
        return java.util.Arrays.stream(sentences)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private <T> String extractAndJoinNames(Collection<T> nodes, Function<T, String> nameExtractor) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return nodes.stream()
                .filter(Objects::nonNull)
                .map(nameExtractor)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));
    }
}
