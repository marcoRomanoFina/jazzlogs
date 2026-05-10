package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "main_artists", columnDefinition = "jsonb")
    private List<AlbumLogMainArtist> mainArtists;

    @Column(name = "artist_id", length = 64)
    private String artistId;

    @Column(name = "artist", nullable = false)
    private String artistName;

    @Column(nullable = false, columnDefinition = "text")
    private String caption;

    @Column(name = "posted_at", nullable = false)
    private LocalDate postedAt;

    @Column(name = "instagram_permalink", nullable = false, unique = true, length = 512)
    private String instagramPermalink;

    @Column
    private String style;

    @Column(name = "vocal_profile", length = 32)
    private String vocalProfile;

    @Column(name = "release_year", length = 16)
    private String releaseYear;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] moods;

    @Column(length = 64)
    private String tier;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] vibe;

    @Column(length = 32)
    private String energy;

    @Column(name = "mood_intensity", length = 32)
    private String moodIntensity;

    @Column(length = 32)
    private String accessibility;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "best_moment", columnDefinition = "jsonb")
    private AlbumLogBestMoment bestMoment;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "listening_context", nullable = false, columnDefinition = "text[]")
    private String[] listeningContext;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "why_it_matters", columnDefinition = "text")
    private String whyItMatters;

    @Column(name = "editorial_note", columnDefinition = "text")
    private String editorialNote;

    @Column(name = "recommended_if", columnDefinition = "text")
    private String recommendedIf;

    @Column(name = "avoid_if", columnDefinition = "text")
    private String avoidIf;

    @Column(name = "album_context", columnDefinition = "text")
    private String albumContext;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<AlbumLogPersonnel> personnel;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spotify_album_id", unique = true)
    private SpotifyAlbum spotifyAlbum;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AlbumLog create(AlbumLogData data) {
        var albumLog = new AlbumLog();
        albumLog.update(data);
        return albumLog;
    }

    public void update(AlbumLog source) {
        update(new AlbumLogData(
                source.getLogNumber(),
                source.getAlbum(),
                source.getMainArtists(),
                source.getCaption(),
                source.getPostedAt(),
                source.getInstagramPermalink(),
                source.getStyle(),
                source.getVocalProfile(),
                source.getReleaseYear(),
                source.getMoods(),
                source.getTier(),
                source.getVibe(),
                source.getEnergy(),
                source.getMoodIntensity(),
                source.getAccessibility(),
                source.getBestMoment(),
                source.getListeningContext(),
                source.getNotes(),
                source.getWhyItMatters(),
                source.getEditorialNote(),
                source.getRecommendedIf(),
                source.getAvoidIf(),
                source.getAlbumContext(),
                source.getPersonnel(),
                source.getSpotifyAlbum() != null ? source.getSpotifyAlbum().getSpotifyAlbumId() : null
        ));
    }

    public void update(AlbumLogData data) {
        this.logNumber = data.logNumber();
        this.album = data.album();
        this.mainArtists = data.mainArtists();
        this.artistId = data.mainArtists() == null || data.mainArtists().isEmpty()
                ? null
                : data.mainArtists().getFirst().spotifyArtistId();
        this.artistName = data.mainArtists() == null || data.mainArtists().isEmpty()
                ? null
                : data.mainArtists().getFirst().artistName();
        this.caption = data.caption();
        this.postedAt = data.postedAt();
        this.instagramPermalink = data.instagramPermalink();
        this.style = data.style();
        this.vocalProfile = data.vocalProfile();
        this.releaseYear = data.releaseYear();
        this.moods = data.moods();
        this.tier = data.tier();
        this.vibe = data.vibe();
        this.energy = data.energy();
        this.moodIntensity = data.moodIntensity();
        this.accessibility = data.accessibility();
        this.bestMoment = data.bestMoment();
        this.listeningContext = data.listeningContext();
        this.notes = data.notes();
        this.whyItMatters = data.whyItMatters();
        this.editorialNote = data.editorialNote();
        this.recommendedIf = data.recommendedIf();
        this.avoidIf = data.avoidIf();
        this.albumContext = data.albumContext();
        this.personnel = data.personnel();
    }

    public void linkSpotifyAlbum(SpotifyAlbum spotifyAlbum) {
        this.spotifyAlbum = spotifyAlbum;
    }

    public String getArtist() {
        return Optional.ofNullable(mainArtists)
                .filter(artists -> !artists.isEmpty())
                .map(artists -> artists.stream()
                        .map(AlbumLogMainArtist::artistName)
                        .collect(Collectors.joining(" & ")))
                .orElse(artistName);
    }
}
