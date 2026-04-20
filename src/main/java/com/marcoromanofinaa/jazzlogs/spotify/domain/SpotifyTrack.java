package com.marcoromanofinaa.jazzlogs.spotify.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "spotify_tracks")
public class SpotifyTrack {

    @Id
    @Column(name = "spotify_track_id", nullable = false, length = 64, updatable = false)
    private String spotifyTrackId;

    @Column(name = "source_playlist_id", nullable = false, length = 64)
    private String sourcePlaylistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spotify_album_id")
    private SpotifyAlbum album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_artist_id")
    private SpotifyArtist mainArtist;

    @ManyToMany
    @JoinTable(
            name = "spotify_track_secondary_artists",
            joinColumns = @JoinColumn(name = "spotify_track_id"),
            inverseJoinColumns = @JoinColumn(name = "spotify_artist_id")
    )
    private Set<SpotifyArtist> secondaryArtists = new LinkedHashSet<>();

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "spotify_url", length = 512)
    private String spotifyUrl;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "disc_number")
    private Integer discNumber;

    @Column(name = "track_number")
    private Integer trackNumber;

    @Column(name = "added_to_playlist_at")
    private OffsetDateTime addedToPlaylistAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    private SpotifyTrack(
            String spotifyTrackId,
            String sourcePlaylistId,
            SpotifyAlbum album,
            SpotifyArtist mainArtist,
            Set<SpotifyArtist> secondaryArtists,
            String name,
            String spotifyUrl,
            Integer durationMs,
            Integer discNumber,
            Integer trackNumber,
            OffsetDateTime addedToPlaylistAt
    ) {
        this.spotifyTrackId = spotifyTrackId;
        this.sourcePlaylistId = sourcePlaylistId;
        this.album = album;
        this.mainArtist = mainArtist;
        this.secondaryArtists = secondaryArtists == null ? new LinkedHashSet<>() : new LinkedHashSet<>(secondaryArtists);
        this.name = name;
        this.spotifyUrl = spotifyUrl;
        this.durationMs = durationMs;
        this.discNumber = discNumber;
        this.trackNumber = trackNumber;
        this.addedToPlaylistAt = addedToPlaylistAt;
    }

    public void updateSyncData(SpotifyTrackSyncData syncData) {
        this.sourcePlaylistId = syncData.sourcePlaylistId();
        this.album = syncData.album();
        this.mainArtist = syncData.mainArtist();
        this.secondaryArtists.clear();
        if (syncData.secondaryArtists() != null) {
            this.secondaryArtists.addAll(syncData.secondaryArtists());
        }
        this.name = syncData.name();
        this.spotifyUrl = syncData.spotifyUrl();
        this.durationMs = syncData.durationMs();
        this.discNumber = syncData.discNumber();
        this.trackNumber = syncData.trackNumber();
        this.addedToPlaylistAt = syncData.addedToPlaylistAt();
    }
}
