package com.marcoromanofinaa.jazzlogs.spotify.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "spotify_artists")
public class SpotifyArtist {

    @Id
    @Column(name = "spotify_artist_id", nullable = false, length = 64, updatable = false)
    private String spotifyArtistId;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "spotify_url", length = 512)
    private String spotifyUrl;

    @Column(name = "href", length = 512)
    private String href;

    @Column(name = "uri", length = 128)
    private String uri;

    @Column(name = "type", length = 32)
    private String type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    private SpotifyArtist(
            String spotifyArtistId,
            String name,
            String spotifyUrl,
            String href,
            String uri,
            String type
    ) {
        this.spotifyArtistId = spotifyArtistId;
        this.name = name;
        this.spotifyUrl = spotifyUrl;
        this.href = href;
        this.uri = uri;
        this.type = type;
    }

    public void updateSyncData(SpotifyArtistSyncData syncData) {
        this.name = syncData.name();
        this.spotifyUrl = syncData.spotifyUrl();
        this.href = syncData.href();
        this.uri = syncData.uri();
        this.type = syncData.type();
    }
}
