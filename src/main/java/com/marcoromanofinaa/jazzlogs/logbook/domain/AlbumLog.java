package com.marcoromanofinaa.jazzlogs.logbook.domain;

import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyAlbum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "album_logs")
public class AlbumLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "log_number", nullable = false, unique = true)
    private Integer logNumber;

    @Column(nullable = false)
    private String album;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false, columnDefinition = "text")
    private String caption;

    @Column(name = "posted_at", nullable = false)
    private LocalDate postedAt;

    @Column(name = "instagram_permalink", nullable = false, unique = true, length = 512)
    private String instagramPermalink;

    @Column
    private String style;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] moods;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "spotify_album_seed_id", length = 64)
    private String spotifyAlbumSeedId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spotify_album_id", unique = true)
    private SpotifyAlbum spotifyAlbum;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static AlbumLog create(
            Integer logNumber,
            String album,
            String artist,
            String caption,
            LocalDate postedAt,
            String instagramPermalink,
            String style,
            String[] moods,
            String notes,
            String spotifyAlbumSeedId
    ) {
        var albumLog = new AlbumLog();
        albumLog.updateEditorialData(logNumber, album, artist, caption, postedAt, instagramPermalink, style, moods, notes, spotifyAlbumSeedId);
        return albumLog;
    }

    public void updateEditorialData(
            Integer logNumber,
            String album,
            String artist,
            String caption,
            LocalDate postedAt,
            String instagramPermalink,
            String style,
            String[] moods,
            String notes,
            String spotifyAlbumSeedId
    ) {
        this.logNumber = logNumber;
        this.album = album;
        this.artist = artist;
        this.caption = caption;
        this.postedAt = postedAt;
        this.instagramPermalink = instagramPermalink;
        this.style = style;
        this.moods = moods;
        this.notes = notes;
        this.spotifyAlbumSeedId = spotifyAlbumSeedId;
    }

    public void linkSpotifyAlbum(SpotifyAlbum spotifyAlbum) {
        this.spotifyAlbum = spotifyAlbum;
    }
}
