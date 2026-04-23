package com.marcoromanofinaa.jazzlogs.logbook.tracknote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "track_notes")
public class TrackNote {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "spotify_track_id", nullable = false, unique = true, length = 64)
    private String spotifyTrackId;

    @Column(name = "spotify_album_id", length = 64)
    private String spotifyAlbumId;

    @Column(name = "log_number", nullable = false)
    private Integer logNumber;

    @Column(nullable = false, length = 512)
    private String track;

    @Column(nullable = false, length = 512)
    private String album;

    @Column(name = "artist_id", nullable = false, length = 64)
    private String artistId;

    @Column(length = 64)
    private String tier;

    @Column(name = "is_instrumental", nullable = false)
    private boolean instrumental;

    @Column(name = "is_standout", nullable = false)
    private boolean standout;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] vibe;

    @Column(length = 32)
    private String energy;

    @Column(name = "mood_intensity", length = 32)
    private String moodIntensity;

    @Column(length = 32)
    private String accessibility;

    @Column(name = "tempo_feel", length = 32)
    private String tempoFeel;

    @Column(name = "rhythmic_feel", length = 64)
    private String rhythmicFeel;

    @Column(name = "track_role", length = 64)
    private String trackRole;

    @Column(name = "composition_type", length = 64)
    private String compositionType;

    @Column(name = "best_moment", columnDefinition = "text")
    private String bestMoment;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "listening_context", nullable = false, columnDefinition = "text[]")
    private String[] listeningContext;

    @Column(name = "why_it_hits", columnDefinition = "text")
    private String whyItHits;

    @Column(name = "editorial_note", columnDefinition = "text")
    private String editorialNote;

    @Column(name = "recommended_if", columnDefinition = "text")
    private String recommendedIf;

    @Column(name = "avoid_if", columnDefinition = "text")
    private String avoidIf;

    @Column(name = "instrument_focus", length = 128)
    private String instrumentFocus;

    @Column(name = "vocal_style", length = 128)
    private String vocalStyle;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "standout_tags", nullable = false, columnDefinition = "text[]")
    private String[] standoutTags;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static TrackNote create(TrackNoteData data) {
        var trackNote = new TrackNote();
        trackNote.update(data);
        return trackNote;
    }

    public void update(TrackNoteData data) {
        this.spotifyTrackId = data.spotifyTrackId();
        this.spotifyAlbumId = data.spotifyAlbumId();
        this.logNumber = data.logNumber();
        this.track = data.track();
        this.album = data.album();
        this.artistId = data.artistId();
        this.tier = data.tier();
        this.instrumental = data.instrumental();
        this.standout = data.standout();
        this.vibe = data.vibe();
        this.energy = data.energy();
        this.moodIntensity = data.moodIntensity();
        this.accessibility = data.accessibility();
        this.tempoFeel = data.tempoFeel();
        this.rhythmicFeel = data.rhythmicFeel();
        this.trackRole = data.trackRole();
        this.compositionType = data.compositionType();
        this.bestMoment = data.bestMoment();
        this.listeningContext = data.listeningContext();
        this.whyItHits = data.whyItHits();
        this.editorialNote = data.editorialNote();
        this.recommendedIf = data.recommendedIf();
        this.avoidIf = data.avoidIf();
        this.instrumentFocus = data.instrumentFocus();
        this.vocalStyle = data.vocalStyle();
        this.standoutTags = data.standoutTags();
    }
}
