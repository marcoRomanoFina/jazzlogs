package com.marcoromanofinaa.jazzlogs.admin.editorial.album.model;

import com.marcoromanofinaa.jazzlogs.admin.editorial.indexing.EditorialIndexingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "album_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_album_logs_log_number", columnNames = "log_number"),
                @UniqueConstraint(name = "uk_album_logs_spotify_album_id", columnNames = "spotify_album_id")
        }
)
public class AlbumLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "log_number", nullable = false, unique = true)
    private Integer logNumber;

    @Column(name = "album_name", nullable = false)
    private String albumName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "main_artists", nullable = false, columnDefinition = "jsonb")
    private List<AlbumLogMainArtist> mainArtists;

    @Column(name = "caption_essence")
    private String captionEssence;

    @Column(name = "posted_at", nullable = false)
    private LocalDate postedAt;

    @Column(name = "instagram_permalink")
    private String instagramPermalink;

    @Column(name = "style")
    private String style;

    @Column(name = "vocal_profile")
    private String vocalProfile;

    @Column(name = "release_year")
    private String releaseYear;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "moods", nullable = false, columnDefinition = "jsonb")
    private List<String> moods;

    @Column(name = "tier")
    private String tier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vibe", nullable = false, columnDefinition = "jsonb")
    private List<String> vibe;

    @Column(name = "energy")
    private String energy;

    @Column(name = "mood_intensity")
    private String moodIntensity;

    @Column(name = "accessibility")
    private String accessibility;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "best_moment", columnDefinition = "jsonb")
    private AlbumLogBestMoment bestMoment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "listening_context", nullable = false, columnDefinition = "jsonb")
    private List<String> listeningContext;

    @Column(name = "why_it_matters")
    private String whyItMatters;

    @Column(name = "editorial_note")
    private String editorialNote;

    @Column(name = "recommended_if")
    private String recommendedIf;

    @Column(name = "avoid_if")
    private String avoidIf;

    @Column(name = "album_context")
    private String albumContext;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personnel", nullable = false, columnDefinition = "jsonb")
    private List<AlbumLogPersonnel> personnel;

    @Column(name = "spotify_album_id", unique = true)
    private String spotifyAlbumId;

    @Enumerated(EnumType.STRING)
    @Column(name = "indexing_status", nullable = false)
    private EditorialIndexingStatus indexingStatus;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AlbumLog create(
            Integer logNumber,
            String albumName,
            List<AlbumLogMainArtist> mainArtists,
            String captionEssence,
            LocalDate postedAt,
            String instagramPermalink,
            String style,
            String vocalProfile,
            String releaseYear,
            List<String> moods,
            String tier,
            List<String> vibe,
            String energy,
            String moodIntensity,
            String accessibility,
            AlbumLogBestMoment bestMoment,
            List<String> listeningContext,
            String whyItMatters,
            String editorialNote,
            String recommendedIf,
            String avoidIf,
            String albumContext,
            List<AlbumLogPersonnel> personnel,
            String spotifyAlbumId
    ) {
        var albumLog = new AlbumLog();
        albumLog.apply(
                logNumber,
                albumName,
                mainArtists,
                captionEssence,
                postedAt,
                instagramPermalink,
                style,
                vocalProfile,
                releaseYear,
                moods,
                tier,
                vibe,
                energy,
                moodIntensity,
                accessibility,
                bestMoment,
                listeningContext,
                whyItMatters,
                editorialNote,
                recommendedIf,
                avoidIf,
                albumContext,
                personnel,
                spotifyAlbumId
        );
        return albumLog;
    }

    public void apply(
            Integer logNumber,
            String albumName,
            List<AlbumLogMainArtist> mainArtists,
            String captionEssence,
            LocalDate postedAt,
            String instagramPermalink,
            String style,
            String vocalProfile,
            String releaseYear,
            List<String> moods,
            String tier,
            List<String> vibe,
            String energy,
            String moodIntensity,
            String accessibility,
            AlbumLogBestMoment bestMoment,
            List<String> listeningContext,
            String whyItMatters,
            String editorialNote,
            String recommendedIf,
            String avoidIf,
            String albumContext,
            List<AlbumLogPersonnel> personnel,
            String spotifyAlbumId
    ) {
        this.logNumber = logNumber;
        this.albumName = albumName;
        this.mainArtists = mainArtists;
        this.captionEssence = captionEssence;
        this.postedAt = postedAt;
        this.instagramPermalink = instagramPermalink;
        this.style = style;
        this.vocalProfile = vocalProfile;
        this.releaseYear = releaseYear;
        this.moods = moods;
        this.tier = tier;
        this.vibe = vibe;
        this.energy = energy;
        this.moodIntensity = moodIntensity;
        this.accessibility = accessibility;
        this.bestMoment = bestMoment;
        this.listeningContext = listeningContext;
        this.whyItMatters = whyItMatters;
        this.editorialNote = editorialNote;
        this.recommendedIf = recommendedIf;
        this.avoidIf = avoidIf;
        this.albumContext = albumContext;
        this.personnel = personnel;
        this.spotifyAlbumId = spotifyAlbumId;
        this.indexedAt = null;
    }

    public void markIndexingPending() {
        this.indexingStatus = EditorialIndexingStatus.PENDING;
        this.indexedAt = null;
    }

    public void markIndexingStale() {
        this.indexingStatus = EditorialIndexingStatus.STALE;
        this.indexedAt = null;
    }

    public void markIndexed(Instant now) {
        this.indexingStatus = EditorialIndexingStatus.INDEXED;
        this.indexedAt = now;
    }
}
