package com.marcoromanofinaa.jazzlogs.logbook.domain;

import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyAlbum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
@Table(name = "album_logs")
public class AlbumLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "log_number", nullable = false, unique = true)
    private Integer logNumber;

    @Column(nullable = false)
    private String album;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false, columnDefinition = "text")
    private String caption;

    @Column(name = "posted_at", nullable = false)
    private LocalDate postedAt;

    @Column(name = "instagram_permalink", nullable = false, unique = true, length = 512)
    private String instagramPermalink;

    @Column
    private String style;

    @Column(name = "release_year", length = 16)
    private String releaseYear;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] moods;

    @Column(length = 64)
    private String tier;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] vibe;

    @Column(length = 32)
    private String energy;

    @Column(name = "mood_intensity", length = 32)
    private String moodIntensity;

    @Column(length = 32)
    private String accessibility;

    @Column(name = "best_moment", columnDefinition = "text")
    private String bestMoment;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "listening_context", nullable = false, columnDefinition = "text[]")
    private String[] listeningContext;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "why_it_matters", columnDefinition = "text")
    private String whyItMatters;

    @Column(name = "editorial_note", columnDefinition = "text")
    private String editorialNote;

    @Column(name = "recommended_if", columnDefinition = "text")
    private String recommendedIf;

    @Column(name = "avoid_if", columnDefinition = "text")
    private String avoidIf;

    @Column(name = "album_context", columnDefinition = "text")
    private String albumContext;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<AlbumLogPersonnel> personnel;

    @Column(name = "spotify_album_seed_id", length = 64)
    private String spotifyAlbumSeedId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spotify_album_id", unique = true)
    private SpotifyAlbum spotifyAlbum;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static AlbumLog create(
            Integer logNumber,
            String album,
            String artist,
            String caption,
            LocalDate postedAt,
            String instagramPermalink,
            String style,
            String[] moods,
            String notes,
            String spotifyAlbumSeedId
    ) {
        return create(
                logNumber,
                album,
                artist,
                caption,
                postedAt,
                instagramPermalink,
                style,
                null,
                moods,
                null,
                new String[]{},
                null,
                null,
                null,
                null,
                new String[]{},
                notes,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                spotifyAlbumSeedId
        );
    }

    public static AlbumLog create(
            Integer logNumber,
            String album,
            String artist,
            String caption,
            LocalDate postedAt,
            String instagramPermalink,
            String style,
            String releaseYear,
            String[] moods,
            String tier,
            String[] vibe,
            String energy,
            String moodIntensity,
            String accessibility,
            String bestMoment,
            String[] listeningContext,
            String notes,
            String whyItMatters,
            String editorialNote,
            String recommendedIf,
            String avoidIf,
            String albumContext,
            List<AlbumLogPersonnel> personnel,
            String spotifyAlbumSeedId
    ) {
        var albumLog = new AlbumLog();
        albumLog.updateEditorialData(
                logNumber,
                album,
                artist,
                caption,
                postedAt,
                instagramPermalink,
                style,
                releaseYear,
                moods,
                tier,
                vibe,
                energy,
                moodIntensity,
                accessibility,
                bestMoment,
                listeningContext,
                notes,
                whyItMatters,
                editorialNote,
                recommendedIf,
                avoidIf,
                albumContext,
                personnel,
                spotifyAlbumSeedId
        );
        return albumLog;
    }

    public void updateEditorialData(
            Integer logNumber,
            String album,
            String artist,
            String caption,
            LocalDate postedAt,
            String instagramPermalink,
            String style,
            String releaseYear,
            String[] moods,
            String tier,
            String[] vibe,
            String energy,
            String moodIntensity,
            String accessibility,
            String bestMoment,
            String[] listeningContext,
            String notes,
            String whyItMatters,
            String editorialNote,
            String recommendedIf,
            String avoidIf,
            String albumContext,
            List<AlbumLogPersonnel> personnel,
            String spotifyAlbumSeedId
    ) {
        this.logNumber = logNumber;
        this.album = album;
        this.artist = artist;
        this.caption = caption;
        this.postedAt = postedAt;
        this.instagramPermalink = instagramPermalink;
        this.style = style;
        this.releaseYear = releaseYear;
        this.moods = moods;
        this.tier = tier;
        this.vibe = vibe;
        this.energy = energy;
        this.moodIntensity = moodIntensity;
        this.accessibility = accessibility;
        this.bestMoment = bestMoment;
        this.listeningContext = listeningContext;
        this.notes = notes;
        this.whyItMatters = whyItMatters;
        this.editorialNote = editorialNote;
        this.recommendedIf = recommendedIf;
        this.avoidIf = avoidIf;
        this.albumContext = albumContext;
        this.personnel = personnel;
        this.spotifyAlbumSeedId = spotifyAlbumSeedId;
    }

    public void linkSpotifyAlbum(SpotifyAlbum spotifyAlbum) {
        this.spotifyAlbum = spotifyAlbum;
    }
}
