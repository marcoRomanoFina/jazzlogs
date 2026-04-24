package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.ai.semantic.albumlog.AlbumLogSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.artistprofile.ArtistProfileSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event.SemanticIndexingRequest;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.indexer.AlbumLogSemanticDocumentIndexer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.indexer.ArtistProfileSemanticDocumentIndexer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.indexer.TrackNoteSemanticDocumentIndexer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.tracknote.TrackNoteSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogData;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfile;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileData;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileRepository;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNote;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteData;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class SemanticDocumentIndexingServiceTest {

    @Mock
    private AlbumLogRepository albumLogRepository;

    @Mock
    private TrackNoteRepository trackNoteRepository;

    @Mock
    private ArtistProfileRepository artistProfileRepository;

    @Mock
    private VectorStore vectorStore;

    @Captor
    private ArgumentCaptor<List<Document>> addedDocumentsCaptor;

    private final AlbumLogSemanticDocumentTransformer albumLogTransformer = new AlbumLogSemanticDocumentTransformer();
    private final TrackNoteSemanticDocumentTransformer trackNoteTransformer = new TrackNoteSemanticDocumentTransformer();
    private final ArtistProfileSemanticDocumentTransformer artistProfileTransformer = new ArtistProfileSemanticDocumentTransformer();

    @Test
    void rebuildsAllSemanticDocumentsIntoVectorStore() {
        when(albumLogRepository.findAllByOrderByLogNumberAsc()).thenReturn(List.of(albumLog()));
        when(trackNoteRepository.findAll()).thenReturn(List.of(trackNote()));
        when(artistProfileRepository.findAll()).thenReturn(List.of(artistProfile()));

        var result = service().indexAll();

        assertThat(result.requested()).isEqualTo(3);
        assertThat(result.indexed()).isEqualTo(3);
        verify(vectorStore, org.mockito.Mockito.times(3)).add(addedDocumentsCaptor.capture());
        assertThat(addedDocumentsCaptor.getAllValues()).hasSize(3);
        assertThat(addedDocumentsCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .map(Document::getId)
                .toList())
                .containsExactly(
                        "ALBUM_LOG:album-log-1",
                        "TRACK_NOTE:spotify-track-1",
                        "ARTIST_PROFILE:spotify-artist-1"
                );
    }

    @Test
    void indexesSingleTrackNoteFromRequest() {
        when(trackNoteRepository.findBySpotifyTrackId("spotify-track-1")).thenReturn(java.util.Optional.of(trackNote()));

        service().indexOne(new SemanticIndexingRequest(
                com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType.TRACK_NOTE,
                "spotify-track-1"
        ));

        verify(vectorStore).delete(List.of("TRACK_NOTE:spotify-track-1"));
        verify(vectorStore).add(addedDocumentsCaptor.capture());
        assertThat(addedDocumentsCaptor.getValue()).singleElement()
                .extracting(Document::getId)
                .isEqualTo("TRACK_NOTE:spotify-track-1");
    }

    private SemanticDocumentIndexingService service() {
        return new SemanticDocumentIndexingService(
                List.of(
                        new AlbumLogSemanticDocumentIndexer(albumLogRepository, albumLogTransformer),
                        new TrackNoteSemanticDocumentIndexer(trackNoteRepository, trackNoteTransformer),
                        new ArtistProfileSemanticDocumentIndexer(artistProfileRepository, artistProfileTransformer)
                ),
                new SemanticDocumentVectorStoreMapper(),
                vectorStore
        );
    }

    private AlbumLog albumLog() {
        return AlbumLog.create(new AlbumLogData(
                1,
                "Spunky",
                "Monty Alexander",
                "Caption",
                LocalDate.of(2026, 2, 2),
                "https://www.instagram.com/p/TEST123/",
                "Hard Bop / Soul Jazz",
                null,
                new String[]{"energetic", "groovy"},
                null,
                new String[]{},
                null,
                null,
                null,
                null,
                new String[]{},
                "Opening log.",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "spotify-album-1"
        ));
    }

    private TrackNote trackNote() {
        return TrackNote.create(new TrackNoteData(
                "spotify-track-1",
                "spotify-album-1",
                1,
                "Rattlesnake",
                "Spunky",
                "spotify-artist-1",
                "essential",
                true,
                true,
                new String[]{"groovy"},
                "high",
                "medium",
                "easy",
                "upbeat",
                "swing",
                "standout",
                "original",
                "The groove locks in.",
                new String[]{"friday-night"},
                "It drives forward.",
                "Pure trio chemistry.",
                "You want energy.",
                "You want calm.",
                "piano",
                "",
                new String[]{"groove"}
        ));
    }

    private ArtistProfile artistProfile() {
        return ArtistProfile.create(new ArtistProfileData(
                "spotify-artist-1",
                "Monty Alexander",
                "piano",
                new String[]{"hard_bop"},
                "Bright Caribbean piano.",
                "Early Monty context.",
                "Joyful and technical.",
                "Spunky",
                new String[]{"energy"},
                "Avoid if you want dark jazz.",
                new String[]{"Oscar Peterson"},
                "Important pianist.",
                new Integer[]{1}
        ));
    }
}
