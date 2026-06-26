package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractCatalogContextLookupStrategy implements CatalogContextLookupStrategy {

    protected JazzToolExecutionResult foundAlbumResult(
            String id,
            Integer logNumber,
            String album,
            List<String> mainArtists,
            String captionEssence,
            String albumContext,
            String whyItMatters,
            String editorialNote,
            String instagramPermalink
    ) {
        var metadata = new LinkedHashMap<String, Object>();
        put(metadata, "found", true);
        put(metadata, "resolvedCatalogItem", true);
        put(metadata, "recommendationType", BasicRecommendationTarget.ALBUM.name());
        put(metadata, "id", blankToEmpty(id));
        put(metadata, "winnerName", blankToEmpty(album));
        put(metadata, "artistFullName", String.join(", ", safeList(mainArtists)));
        put(metadata, "logNumber", logNumber);
        put(metadata, "album", blankToEmpty(album));
        put(metadata, "mainArtists", safeList(mainArtists));
        put(metadata, "captionEssence", captionEssence);
        put(metadata, "albumContext", albumContext);
        put(metadata, "whyItMatters", whyItMatters);
        put(metadata, "editorialNote", editorialNote);
        put(metadata, "instagramPermalink", instagramPermalink);
        return success(metadata);
    }

    protected JazzToolExecutionResult foundTrackResult(
            String id,
            Integer logNumber,
            String album,
            String track,
            List<String> mainArtists,
            String editorialNote,
            String whyItHits,
            String bestMoment,
            String instagramPermalink
    ) {
        var metadata = new LinkedHashMap<String, Object>();
        put(metadata, "found", true);
        put(metadata, "resolvedCatalogItem", true);
        put(metadata, "recommendationType", BasicRecommendationTarget.TRACKS.name());
        put(metadata, "id", blankToEmpty(id));
        put(metadata, "winnerName", blankToEmpty(track));
        put(metadata, "artistFullName", String.join(", ", safeList(mainArtists)));
        put(metadata, "logNumber", logNumber);
        put(metadata, "album", album);
        put(metadata, "track", blankToEmpty(track));
        put(metadata, "mainArtists", safeList(mainArtists));
        put(metadata, "editorialNote", editorialNote);
        put(metadata, "whyItHits", whyItHits);
        put(metadata, "bestMoment", bestMoment);
        put(metadata, "instagramPermalink", instagramPermalink);
        return success(metadata);
    }

    protected JazzToolExecutionResult notFoundResult(String query, Map<String, Object> extraMetadata) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("found", false);
        metadata.put("lookupMode", lookupMode());
        metadata.put("query", query);
        if (extraMetadata != null) {
            metadata.putAll(extraMetadata);
        }
        return new JazzToolExecutionResult(
                JazzToolName.CATALOG_CONTEXT,
                "No catalog context found for lookup mode " + lookupMode() + ".",
                Map.copyOf(metadata)
        );
    }

    private JazzToolExecutionResult success(Map<String, Object> metadata) {
        return new JazzToolExecutionResult(
                JazzToolName.CATALOG_CONTEXT,
                "Catalog context resolved successfully. Use metadata as the source of truth for factual and curatorial details.",
                Map.copyOf(metadata)
        );
    }

    private void put(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    protected String requireNonBlankId(String query, String messagePrefix) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(messagePrefix + " query must be non-blank");
        }
        return query.trim();
    }

    protected String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        var rendered = value.toString().trim();
        return rendered.isBlank() ? null : rendered;
    }

    protected Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    protected List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        var result = new java.util.ArrayList<String>();
        for (var item : iterable) {
            var rendered = stringValue(item);
            if (rendered != null && !rendered.isBlank()) {
                result.add(rendered);
            }
        }
        return List.copyOf(result);
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
