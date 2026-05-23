package com.marcoromanofinaa.jazzlogs.spotify.catalog;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "spotify_albums",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_spotify_albums_spotify_album_id",
                        columnNames = "spotify_album_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Access(AccessType.FIELD)
public class SpotifyAlbum {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "spotify_album_id", nullable = false, unique = true)
    private String spotifyAlbumId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "release_date")
    private String releaseDate;

    @Column(name = "total_tracks")
    private Integer totalTracks;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "spotify_url")
    private String spotifyUrl;

    @ManyToMany
    @JoinTable(
            name = "spotify_album_artists",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    @OrderColumn(name = "position")
    private List<SpotifyArtist> artists = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SpotifyAlbum create(
            String spotifyAlbumId,
            String name,
            String releaseDate,
            Integer totalTracks,
            String imageUrl,
            String spotifyUrl,
            Instant now
    ) {
        SpotifyAlbum album = new SpotifyAlbum();
        album.spotifyAlbumId = spotifyAlbumId;
        album.name = name;
        album.releaseDate = releaseDate;
        album.totalTracks = totalTracks;
        album.imageUrl = imageUrl;
        album.spotifyUrl = spotifyUrl;
        album.createdAt = now;
        album.updatedAt = now;
        return album;
    }

    public void updateMetadata(
            String name,
            String releaseDate,
            Integer totalTracks,
            String imageUrl,
            String spotifyUrl,
            Instant now
    ) {
        this.name = name;
        this.releaseDate = releaseDate;
        this.totalTracks = totalTracks;
        this.imageUrl = imageUrl;
        this.spotifyUrl = spotifyUrl;
        this.updatedAt = now;
    }

    public void replaceArtistsInSpotifyOrder(List<SpotifyArtist> orderedArtists) {
        this.artists.clear();
        if (orderedArtists != null) {
            this.artists.addAll(orderedArtists);
        }
    }
}
