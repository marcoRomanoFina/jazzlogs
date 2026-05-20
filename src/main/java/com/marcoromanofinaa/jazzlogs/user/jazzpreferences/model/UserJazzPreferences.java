package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
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
@Table(name = "user_jazz_preferences")
public class UserJazzPreferences {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "jazz_experience_level", nullable = false, length = 40)
    private JazzExperienceLevel jazzExperienceLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_artists", nullable = false, columnDefinition = "jsonb")
    private List<Artist> favoriteArtists;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_subgenres", nullable = false, columnDefinition = "jsonb")
    private List<Subgenre> preferredSubgenres;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_moods", nullable = false, columnDefinition = "jsonb")
    private List<Mood> preferredMoods;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_instruments", nullable = false, columnDefinition = "jsonb")
    private List<Instrument> favoriteInstruments;

    @Enumerated(EnumType.STRING)
    @Column(name = "tempo_feel", nullable = false, length = 20)
    private TempoFeel tempoFeel;

    @Column(name = "likes_vocals", nullable = false)
    private boolean likesVocals;

    @Enumerated(EnumType.STRING)
    @Column(name = "discovery_mode", nullable = false, length = 40)
    private DiscoveryMode discoveryMode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserJazzPreferences(UUID userId) {
        this.userId = userId;
    }

    public void apply(
            JazzExperienceLevel jazzExperienceLevel,
            List<Artist> favoriteArtists,
            List<Subgenre> preferredSubgenres,
            List<Mood> preferredMoods,
            List<Instrument> favoriteInstruments,
            TempoFeel tempoFeel,
            boolean likesVocals,
            DiscoveryMode discoveryMode
    ) {
        this.jazzExperienceLevel = jazzExperienceLevel;
        this.favoriteArtists = favoriteArtists;
        this.preferredSubgenres = preferredSubgenres;
        this.preferredMoods = preferredMoods;
        this.favoriteInstruments = favoriteInstruments;
        this.tempoFeel = tempoFeel;
        this.likesVocals = likesVocals;
        this.discoveryMode = discoveryMode;
    }
}
