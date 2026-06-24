package com.marcoromanofinaa.jazzlogs.editorial.graph.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Neo4jGraphSchemaInitializer implements ApplicationRunner {

    private final Neo4jClient neo4jClient;

    @Value("${spring.ai.openai.embedding.options.dimensions}")
    private Integer embeddingDimensions;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        ensureUniqueConstraint("album_id_unique", "Album", "id");
        ensureUniqueConstraint("track_id_unique", "Track", "id");
        ensureUniqueConstraint("artist_id_unique", "Artist", "id");
        ensureUniqueConstraint("user_id_unique", "User", "id");

        ensureUniqueConstraint("album_spotify_id_unique", "Album", "spotifyAlbumId");
        ensureUniqueConstraint("track_spotify_id_unique", "Track", "spotify_track_id");
        ensureUniqueConstraint("artist_spotify_id_unique", "Artist", "spotifyArtistId");
        ensureUniqueConstraint("user_user_id_unique", "User", "userId");
        ensureUniqueConstraint("album_log_number_unique", "Album", "logNumber");

        ensurePropertyIndex("track_log_number_index", "Track", "logNumber");

        ensurePropertyIndex("album_normalized_name_index", "Album", "normalizedName");
        ensurePropertyIndex("track_normalized_name_index", "Track", "normalizedName");
        ensurePropertyIndex("artist_normalized_name_index", "Artist", "normalizedName");

        ensurePropertyIndex("album_tier_index", "Album", "tier");
        ensurePropertyIndex("track_tier_index", "Track", "tier");
        ensurePropertyIndex("album_energy_index", "Album", "energy");
        ensurePropertyIndex("track_energy_index", "Track", "energy");
        ensurePropertyIndex("album_accessibility_index", "Album", "accessibility");
        ensurePropertyIndex("track_accessibility_index", "Track", "accessibility");

        createVectorIndex("album_embeddings", "Album");
        createVectorIndex("track_embeddings", "Track");
        createVectorIndex("artist_embeddings", "Artist");
    }

    private void ensureUniqueConstraint(String constraintName, String label, String property) {
        var query = """
                CREATE CONSTRAINT %s IF NOT EXISTS
                FOR (node:%s)
                REQUIRE node.%s IS UNIQUE
                """.formatted(constraintName, label, property);

        neo4jClient.query(query).run();
        log.info("Ensured Neo4j unique constraint '{}' for :{}({})", constraintName, label, property);
    }

    private void ensurePropertyIndex(String indexName, String label, String property) {
        var query = """
                CREATE INDEX %s IF NOT EXISTS
                FOR (node:%s)
                ON (node.%s)
                """.formatted(indexName, label, property);

        neo4jClient.query(query).run();
        log.info("Ensured Neo4j property index '{}' for :{}({})", indexName, label, property);
    }

    private void createVectorIndex(String indexName, String label) {
        var query = """
                CREATE VECTOR INDEX %s IF NOT EXISTS
                FOR (node:%s)
                ON (node.embedding)
                OPTIONS {
                  indexConfig: {
                    `vector.dimensions`: %d,
                    `vector.similarity_function`: 'cosine'
                  }
                }
                """.formatted(indexName, label, embeddingDimensions);

        neo4jClient.query(query).run();
        log.info("Ensured Neo4j vector index '{}' for :{}(embedding)", indexName, label);
    }
}
