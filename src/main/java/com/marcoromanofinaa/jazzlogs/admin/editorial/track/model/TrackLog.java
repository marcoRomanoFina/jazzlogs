package com.marcoromanofinaa.jazzlogs.admin.editorial.track.model;

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
        name = "track_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_track_logs_spotify_track_id", columnNames = "spotify_track_id")
        }
)
public class TrackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "spotify_track_id", nullable = false, unique = true)
    private String spotifyTrackId;

    @Column(name = "spotify_album_id")
    private String spotifyAlbumId;

    @Column(name = "log_number")
    private Integer logNumber;

    @Column(name = "track_name", nullable = false)
    private String trackName;

    @Column(name = "album_name", nullable = false)
    private String albumName;

    @Column(name = "primary_artist")
    private String primaryArtist;

    @Column(name = "main_artist_spotify_id")
    private String mainArtistSpotifyId;

    @Column(name = "tier")
    private String tier;

    @Column(name = "vocal_profile")
    private String vocalProfile;

    @Column(name = "standout")
    private Boolean standout;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vibe", nullable = false, columnDefinition = "jsonb")
    private List<String> vibe;

    @Column(name = "energy")
    private String energy;

    @Column(name = "mood_intensity")
    private String moodIntensity;

    @Column(name = "accessibility")
    private String accessibility;

    @Column(name = "tempo_feel")
    private String tempoFeel;

    @Column(name = "rhythm_feel")
    private String rhythmFeel;

    @Column(name = "album_role")
    private String albumRole;

    @Column(name = "composition_type")
    private String compositionType;

    @Column(name = "best_moment")
    private String bestMoment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "listening_context", nullable = false, columnDefinition = "jsonb")
    private List<String> listeningContext;

    @Column(name = "why_it_hits")
    private String whyItHits;

    @Column(name = "editorial_note")
    private String editorialNote;

    @Column(name = "recommended_if")
    private String recommendedIf;

    @Column(name = "avoid_if")
    private String avoidIf;

    @Column(name = "instrument_focus")
    private String instrumentFocus;

    @Column(name = "vocal_style")
    private String vocalStyle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "standout_tags", nullable = false, columnDefinition = "jsonb")
    private List<String> standoutTags;

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

    public static TrackLog create(
            String spotifyTrackId,
            String spotifyAlbumId,
            Integer logNumber,
            String trackName,
            String albumName,
            String primaryArtist,
            String mainArtistSpotifyId,
            String tier,
            String vocalProfile,
            Boolean standout,
            List<String> vibe,
            String energy,
            String moodIntensity,
            String accessibility,
            String tempoFeel,
            String rhythmFeel,
            String albumRole,
            String compositionType,
            String bestMoment,
            List<String> listeningContext,
            String whyItHits,
            String editorialNote,
            String recommendedIf,
            String avoidIf,
            String instrumentFocus,
            String vocalStyle,
            List<String> standoutTags
    ) {
        var trackLog = new TrackLog();
        trackLog.apply(
                spotifyTrackId,
                spotifyAlbumId,
                logNumber,
                trackName,
                albumName,
                primaryArtist,
                mainArtistSpotifyId,
                tier,
                vocalProfile,
                standout,
                vibe,
                energy,
                moodIntensity,
                accessibility,
                tempoFeel,
                rhythmFeel,
                albumRole,
                compositionType,
                bestMoment,
                listeningContext,
                whyItHits,
                editorialNote,
                recommendedIf,
                avoidIf,
                instrumentFocus,
                vocalStyle,
                standoutTags
        );
        return trackLog;
    }

    public void apply(
            String spotifyTrackId,
            String spotifyAlbumId,
            Integer logNumber,
            String trackName,
            String albumName,
            String primaryArtist,
            String mainArtistSpotifyId,
            String tier,
            String vocalProfile,
            Boolean standout,
            List<String> vibe,
            String energy,
            String moodIntensity,
            String accessibility,
            String tempoFeel,
            String rhythmFeel,
            String albumRole,
            String compositionType,
            String bestMoment,
            List<String> listeningContext,
            String whyItHits,
            String editorialNote,
            String recommendedIf,
            String avoidIf,
            String instrumentFocus,
            String vocalStyle,
            List<String> standoutTags
    ) {
        this.spotifyTrackId = spotifyTrackId;
        this.spotifyAlbumId = spotifyAlbumId;
        this.logNumber = logNumber;
        this.trackName = trackName;
        this.albumName = albumName;
        this.primaryArtist = primaryArtist;
        this.mainArtistSpotifyId = mainArtistSpotifyId;
        this.tier = tier;
        this.vocalProfile = vocalProfile;
        this.standout = standout;
        this.vibe = vibe;
        this.energy = energy;
        this.moodIntensity = moodIntensity;
        this.accessibility = accessibility;
        this.tempoFeel = tempoFeel;
        this.rhythmFeel = rhythmFeel;
        this.albumRole = albumRole;
        this.compositionType = compositionType;
        this.bestMoment = bestMoment;
        this.listeningContext = listeningContext;
        this.whyItHits = whyItHits;
        this.editorialNote = editorialNote;
        this.recommendedIf = recommendedIf;
        this.avoidIf = avoidIf;
        this.instrumentFocus = instrumentFocus;
        this.vocalStyle = vocalStyle;
        this.standoutTags = standoutTags;
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
