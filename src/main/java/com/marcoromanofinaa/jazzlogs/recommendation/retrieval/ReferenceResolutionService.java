package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphReference;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReferenceResolutionService {

    private final Neo4jClient neo4jClient;

    public ConversationSubgraphFilters resolve(ConversationSubgraphFilters filters) {
        if (filters == null) {
            return null;
        }

        var resolvedReferences = filters.references() == null
                ? List.<ConversationSubgraphReference>of()
                : filters.references().stream()
                        .map(this::resolveReference)
                        .toList();

        return new ConversationSubgraphFilters(
                filters.styles() == null ? List.of() : filters.styles(),
                filters.instruments() == null ? List.of() : filters.instruments(),
                filters.rhythms() == null ? List.of() : filters.rhythms(),
                resolvedReferences
        );
    }

    private ConversationSubgraphReference resolveReference(ConversationSubgraphReference reference) {
        if (reference == null || reference.type() == null || reference.name() == null || reference.name().isBlank()) {
            return reference;
        }

        return switch (reference.type()) {
            case ARTIST -> resolveArtist(reference);
            case ALBUM -> resolveAlbum(reference);
            case TRACK -> resolveTrack(reference);
        };
    }

    private ConversationSubgraphReference resolveArtist(ConversationSubgraphReference reference) {
        var row = neo4jClient.query("""
                MATCH (artist:Artist)
                WHERE artist.normalizedName = $normalizedName
                RETURN artist.name AS canonicalName
                LIMIT 1
                """)
                .bind(normalize(reference.name())).to("normalizedName")
                .fetch()
                .one()
                .orElse(null);

        if (row == null) {
            return reference;
        }

        return new ConversationSubgraphReference(
                reference.type(),
                stringValue(row.get("canonicalName"), reference.name())
        );
    }

    private ConversationSubgraphReference resolveAlbum(ConversationSubgraphReference reference) {
        var row = neo4jClient.query("""
                MATCH (album:Album)
                WHERE album.normalizedName = $normalizedName
                RETURN album.name AS canonicalName
                LIMIT 1
                """)
                .bind(normalize(reference.name())).to("normalizedName")
                .fetch()
                .one()
                .orElse(null);

        if (row == null) {
            return reference;
        }

        return new ConversationSubgraphReference(
                reference.type(),
                stringValue(row.get("canonicalName"), reference.name())
        );
    }

    private ConversationSubgraphReference resolveTrack(ConversationSubgraphReference reference) {
        var row = neo4jClient.query("""
                MATCH (track:Track)
                WHERE track.normalizedName = $normalizedName
                RETURN track.name AS canonicalName
                LIMIT 1
                """)
                .bind(normalize(reference.name())).to("normalizedName")
                .fetch()
                .one()
                .orElse(null);

        if (row == null) {
            return reference;
        }

        return new ConversationSubgraphReference(
                reference.type(),
                stringValue(row.get("canonicalName"), reference.name())
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        var rendered = value.toString().trim();
        return rendered.isBlank() ? fallback : rendered;
    }
}
