package com.marcoromanofinaa.jazzlogs.spotify.catalog;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "spotify_artists",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_spotify_artists_spotify_artist_id",
                        columnNames = "spotify_artist_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Access(AccessType.FIELD)
public class SpotifyArtist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "spotify_artist_id", nullable = false, unique = true)
    private String spotifyArtistId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "spotify_url")
    private String spotifyUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SpotifyArtist create(
            String spotifyArtistId,
            String name,
            String spotifyUrl,
            Instant now
    ) {
        SpotifyArtist artist = new SpotifyArtist();
        artist.spotifyArtistId = spotifyArtistId;
        artist.name = name;
        artist.spotifyUrl = spotifyUrl;
        artist.createdAt = now;
        artist.updatedAt = now;
        return artist;
    }

    public void updateMetadata(
            String name,
            String spotifyUrl,
            Instant now
    ) {
        this.name = name;
        this.spotifyUrl = spotifyUrl;
        this.updatedAt = now;
    }
}
