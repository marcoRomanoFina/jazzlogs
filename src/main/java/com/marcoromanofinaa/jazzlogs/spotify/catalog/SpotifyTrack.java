package com.marcoromanofinaa.jazzlogs.spotify.catalog;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
        name = "spotify_tracks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_spotify_tracks_spotify_track_id",
                        columnNames = "spotify_track_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Access(AccessType.FIELD)
public class SpotifyTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "spotify_track_id", nullable = false, unique = true)
    private String spotifyTrackId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private SpotifyAlbum album;

    @ManyToMany
    @JoinTable(
            name = "spotify_track_artists",
            joinColumns = @JoinColumn(name = "track_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    @OrderColumn(name = "position")
    private List<SpotifyArtist> artists = new ArrayList<>();

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "track_number")
    private Integer trackNumber;

    @Column(name = "spotify_url")
    private String spotifyUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SpotifyTrack create(
            String spotifyTrackId,
            String name,
            SpotifyAlbum album,
            Integer durationMs,
            Integer trackNumber,
            String spotifyUrl,
            Instant now
    ) {
        SpotifyTrack track = new SpotifyTrack();
        track.spotifyTrackId = spotifyTrackId;
        track.name = name;
        track.album = album;
        track.durationMs = durationMs;
        track.trackNumber = trackNumber;
        track.spotifyUrl = spotifyUrl;
        track.createdAt = now;
        track.updatedAt = now;
        return track;
    }

    public void updateMetadata(
            String name,
            SpotifyAlbum album,
            Integer durationMs,
            Integer trackNumber,
            String spotifyUrl,
            Instant now
    ) {
        this.name = name;
        this.album = album;
        this.durationMs = durationMs;
        this.trackNumber = trackNumber;
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
