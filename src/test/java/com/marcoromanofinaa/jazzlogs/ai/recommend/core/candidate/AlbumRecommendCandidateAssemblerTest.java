package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogBestMoment;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogBestMomentItem;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogMainArtist;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

@ExtendWith(MockitoExtension.class)
class AlbumRecommendCandidateAssemblerTest {

    @Mock
    private AlbumLogRepository albumLogRepository;

    @Test
    void assemblesAlbumCandidateFromDocumentAndAlbumLog() throws Exception {
        var albumLogId = UUID.randomUUID();
        var spotifyAlbum = SpotifyAlbum.builder()
                .spotifyAlbumId("spotify-album-1")
                .name("Spunky")
                .spotifyUrl("https://open.spotify.com/album/spotify-album-1")
                .coverImageUrl("https://i.scdn.co/image/spunky")
                .totalTracks(10)
                .releaseDate("1965-01-01")
                .build();
        var albumLog = buildAlbumLog(albumLogId, spotifyAlbum);
        var document = Document.builder()
                .id("doc-1")
                .text("album semantic text")
                .metadata(Map.of(
                        "sourceId", albumLogId.toString(),
                        "semanticDocumentId", "ALBUM_LOG:" + albumLogId
                ))
                .score(0.91)
                .build();

        when(albumLogRepository.findWithSpotifyAlbumById(albumLogId)).thenReturn(Optional.of(albumLog));

        var candidate = new AlbumRecommendCandidateAssembler(albumLogRepository).assemble(document);

        assertThat(candidate.similarityScore()).isEqualTo(0.91);
        assertThat(candidate.semanticDocumentId()).isEqualTo("ALBUM_LOG:" + albumLogId);
        assertThat(candidate.albumLogId()).isEqualTo(albumLogId);
        assertThat(candidate.logNumber()).isEqualTo(1);
        assertThat(candidate.album()).isEqualTo("Spunky");
        assertThat(candidate.artist()).isEqualTo("Monty Alexander");
        assertThat(candidate.decisionContext().totalTracks()).isEqualTo(10);
        assertThat(candidate.decisionContext().releaseDate()).isEqualTo("1965-01-01");
        assertThat(candidate.decisionContext().moods()).containsExactly("playful", "warm");
        assertThat(candidate.decisionContext().vibe()).containsExactly("buoyant", "night-drive");
        assertThat(candidate.decisionContext().captionEssence()).isEqualTo("Original caption essence");
        assertThat(candidate.deliveryMetadata().instagramPermalink()).isEqualTo("https://instagram.com/p/spunky");
        assertThat(candidate.deliveryMetadata().spotifyUrl()).isEqualTo("https://open.spotify.com/album/spotify-album-1");
        assertThat(candidate.deliveryMetadata().coverImageUrl()).isEqualTo("https://i.scdn.co/image/spunky");
    }

    private AlbumLog buildAlbumLog(UUID id, SpotifyAlbum spotifyAlbum) throws Exception {
        var albumLog = AlbumLog.create(new com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogData(
                1,
                "Spunky",
                List.of(new AlbumLogMainArtist("artist-1", "Monty Alexander")),
                "Original caption essence",
                LocalDate.of(2026, 4, 1),
                "https://instagram.com/p/spunky",
                "post-bop",
                "instrumental",
                "1965",
                new String[]{"playful", "warm"},
                "esencial",
                new String[]{"buoyant", "night-drive"},
                "medium",
                "medium",
                "high",
                new AlbumLogBestMoment(
                        "Late set with motion.",
                        List.of(new AlbumLogBestMomentItem("Groove", "The trio locks in immediately.")),
                        "It works best when you want forward motion."
                ),
                new String[]{"night", "drive"},
                "Extra notes",
                "Why it matters",
                "Editorial note",
                "Recommended if",
                "Avoid if",
                "Album context",
                List.of(),
                "spotify-seed-id"
        ));
        setField(albumLog, "id", id);
        setField(albumLog, "spotifyAlbum", spotifyAlbum);
        return albumLog;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
