package com.marcoromanofinaa.jazzlogs.admin.editorial.artist.model;

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
        name = "artist_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_artist_logs_spotify_artist_id", columnNames = "spotify_artist_id")
        }
)
public class ArtistLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "spotify_artist_id", nullable = false, unique = true)
    private String spotifyArtistId;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    @Column(name = "primary_instrument")
    private String primaryInstrument;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "main_styles", nullable = false, columnDefinition = "jsonb")
    private List<String> mainStyles;

    @Column(name = "sound_profile")
    private String soundProfile;

    @Column(name = "artist_context")
    private String artistContext;

    @Column(name = "editorial_note")
    private String editorialNote;

    @Column(name = "entry_point_log_id")
    private String entryPointLogId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "best_listening_moments", nullable = false, columnDefinition = "jsonb")
    private List<String> bestListeningMoments;

    @Column(name = "avoid_if")
    private String avoidIf;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_artists", nullable = false, columnDefinition = "jsonb")
    private List<String> relatedArtists;

    @Column(name = "why_it_matters")
    private String whyItMatters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "appears_in_logs", nullable = false, columnDefinition = "jsonb")
    private List<Integer> appearsInLogs;

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

    public static ArtistLog create(
            String spotifyArtistId,
            String artistName,
            String primaryInstrument,
            List<String> mainStyles,
            String soundProfile,
            String artistContext,
            String editorialNote,
            String entryPointLogId,
            List<String> bestListeningMoments,
            String avoidIf,
            List<String> relatedArtists,
            String whyItMatters,
            List<Integer> appearsInLogs
    ) {
        var artistLog = new ArtistLog();
        artistLog.apply(
                spotifyArtistId,
                artistName,
                primaryInstrument,
                mainStyles,
                soundProfile,
                artistContext,
                editorialNote,
                entryPointLogId,
                bestListeningMoments,
                avoidIf,
                relatedArtists,
                whyItMatters,
                appearsInLogs
        );
        return artistLog;
    }

    public void apply(
            String spotifyArtistId,
            String artistName,
            String primaryInstrument,
            List<String> mainStyles,
            String soundProfile,
            String artistContext,
            String editorialNote,
            String entryPointLogId,
            List<String> bestListeningMoments,
            String avoidIf,
            List<String> relatedArtists,
            String whyItMatters,
            List<Integer> appearsInLogs
    ) {
        this.spotifyArtistId = spotifyArtistId;
        this.artistName = artistName;
        this.primaryInstrument = primaryInstrument;
        this.mainStyles = mainStyles;
        this.soundProfile = soundProfile;
        this.artistContext = artistContext;
        this.editorialNote = editorialNote;
        this.entryPointLogId = entryPointLogId;
        this.bestListeningMoments = bestListeningMoments;
        this.avoidIf = avoidIf;
        this.relatedArtists = relatedArtists;
        this.whyItMatters = whyItMatters;
        this.appearsInLogs = appearsInLogs;
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
