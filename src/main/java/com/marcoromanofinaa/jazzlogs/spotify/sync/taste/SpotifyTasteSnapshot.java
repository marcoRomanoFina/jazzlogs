package com.marcoromanofinaa.jazzlogs.spotify.sync.taste;

import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyTopUserArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyUserTopTrackDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "spotify_taste_snapshots")
public class SpotifyTasteSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "spotify_connection_id", nullable = false)
    private UUID spotifyConnectionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_artists", nullable = false, columnDefinition = "jsonb")
    private Map<SpotifyTimeRange, java.util.List<SpotifyTopUserArtistDTO>> topArtists;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_tracks", nullable = false, columnDefinition = "jsonb")
    private Map<SpotifyTimeRange, java.util.List<SpotifyUserTopTrackDTO>> topTracks;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    private SpotifyTasteSnapshot(
            UUID userId,
            UUID spotifyConnectionId,
            Map<SpotifyTimeRange, java.util.List<SpotifyTopUserArtistDTO>> topArtists,
            Map<SpotifyTimeRange, java.util.List<SpotifyUserTopTrackDTO>> topTracks,
            Instant generatedAt
    ) {
        this.userId = userId;
        this.spotifyConnectionId = spotifyConnectionId;
        this.topArtists = topArtists;
        this.topTracks = topTracks;
        this.generatedAt = generatedAt;
    }

    public static SpotifyTasteSnapshot create(
            UUID userId,
            UUID spotifyConnectionId,
            Map<SpotifyTimeRange, java.util.List<SpotifyTopUserArtistDTO>> topArtists,
            Map<SpotifyTimeRange, java.util.List<SpotifyUserTopTrackDTO>> topTracks,
            Instant generatedAt
    ) {
        return new SpotifyTasteSnapshot(userId, spotifyConnectionId, topArtists, topTracks, generatedAt);
    }

    public void replaceSnapshot(
            UUID spotifyConnectionId,
            Map<SpotifyTimeRange, java.util.List<SpotifyTopUserArtistDTO>> topArtists,
            Map<SpotifyTimeRange, java.util.List<SpotifyUserTopTrackDTO>> topTracks,
            Instant generatedAt
    ) {
        this.spotifyConnectionId = spotifyConnectionId;
        this.topArtists = topArtists;
        this.topTracks = topTracks;
        this.generatedAt = generatedAt;
    }
}
