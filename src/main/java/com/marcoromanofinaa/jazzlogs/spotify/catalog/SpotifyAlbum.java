package com.marcoromanofinaa.jazzlogs.spotify.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "spotify_albums")
public class SpotifyAlbum {

    @Id
    @Column(name = "spotify_album_id", nullable = false, length = 64, updatable = false)
    private String spotifyAlbumId;

    @Column(name = "source_playlist_id", nullable = false, length = 64)
    private String sourcePlaylistId;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "spotify_url", length = 512)
    private String spotifyUrl;

    @Column(name = "cover_image_url", length = 1024)
    private String coverImageUrl;

    @Column(name = "album_type", length = 32)
    private String albumType;

    @Column(name = "total_tracks")
    private Integer totalTracks;

    @Column(name = "release_date", length = 32)
    private String releaseDate;

    @Column(name = "release_date_precision", length = 16)
    private String releaseDatePrecision;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private SpotifyAlbum(
            String spotifyAlbumId,
            String sourcePlaylistId,
            String name,
            String spotifyUrl,
            String coverImageUrl,
            String albumType,
            Integer totalTracks,
            String releaseDate,
            String releaseDatePrecision
    ) {
        this.spotifyAlbumId = spotifyAlbumId;
        this.sourcePlaylistId = sourcePlaylistId;
        this.name = name;
        this.spotifyUrl = spotifyUrl;
        this.coverImageUrl = coverImageUrl;
        this.albumType = albumType;
        this.totalTracks = totalTracks;
        this.releaseDate = releaseDate;
        this.releaseDatePrecision = releaseDatePrecision;
    }

    public void updateSyncData(SpotifyAlbumSyncData syncData) {
        this.sourcePlaylistId = syncData.sourcePlaylistId();
        this.name = syncData.name();
        this.spotifyUrl = syncData.spotifyUrl();
        this.coverImageUrl = syncData.coverImageUrl();
        this.albumType = syncData.albumType();
        this.totalTracks = syncData.totalTracks();
        this.releaseDate = syncData.releaseDate();
        this.releaseDatePrecision = syncData.releaseDatePrecision();
    }
}
