package com.marcoromanofinaa.jazzlogs.logbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "artist_profiles")
public class ArtistProfile {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "spotify_artist_id", nullable = false, unique = true, length = 64)
    private String spotifyArtistId;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(name = "primary_instrument", length = 128)
    private String primaryInstrument;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "main_styles", nullable = false, columnDefinition = "text[]")
    private String[] mainStyles;

    @Column(name = "signature_sound", columnDefinition = "text")
    private String signatureSound;

    @Column(name = "artist_context", columnDefinition = "text")
    private String artistContext;

    @Column(name = "jazzlogs_take", columnDefinition = "text")
    private String jazzlogsTake;

    @Column(name = "recommended_entry_point", length = 512)
    private String recommendedEntryPoint;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "best_for", nullable = false, columnDefinition = "text[]")
    private String[] bestFor;

    @Column(name = "avoid_if", columnDefinition = "text")
    private String avoidIf;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "related_artists", nullable = false, columnDefinition = "text[]")
    private String[] relatedArtists;

    @Column(columnDefinition = "text")
    private String importance;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "log_appearances", nullable = false, columnDefinition = "integer[]")
    private Integer[] logAppearances;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static ArtistProfile create(ArtistProfileData data) {
        var artistProfile = new ArtistProfile();
        artistProfile.update(data);
        return artistProfile;
    }

    public void update(ArtistProfileData data) {
        this.spotifyArtistId = data.spotifyArtistId();
        this.name = data.name();
        this.primaryInstrument = data.primaryInstrument();
        this.mainStyles = data.mainStyles();
        this.signatureSound = data.signatureSound();
        this.artistContext = data.artistContext();
        this.jazzlogsTake = data.jazzlogsTake();
        this.recommendedEntryPoint = data.recommendedEntryPoint();
        this.bestFor = data.bestFor();
        this.avoidIf = data.avoidIf();
        this.relatedArtists = data.relatedArtists();
        this.importance = data.importance();
        this.logAppearances = data.logAppearances();
    }
}
