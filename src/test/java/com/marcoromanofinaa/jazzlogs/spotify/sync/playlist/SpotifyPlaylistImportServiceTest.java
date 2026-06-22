package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.editorial.graph.album.AlbumGraphWriter;
import com.marcoromanofinaa.jazzlogs.editorial.graph.artist.ArtistGraphWriter;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.AlbumNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ArtistNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.TrackNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.track.TrackGraphWriter;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyAlbumDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyPlaylistTrackDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;

@ExtendWith(MockitoExtension.class)
class SpotifyPlaylistImportServiceTest {

    @Mock
    private ArtistGraphWriter artistGraphWriter;

    @Mock
    private AlbumGraphWriter albumGraphWriter;

    @Mock
    private TrackGraphWriter trackGraphWriter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Neo4jClient neo4jClient;

    @Test
    void importPlaylistTracksUpsertsGraphNodesAndPersistsRelationships() {
        var service = new SpotifyPlaylistImportService(
                artistGraphWriter,
                albumGraphWriter,
                trackGraphWriter,
                neo4jClient
        );
        var artistOne = new SpotifyArtistDTO("artist-1", "Miles Davis", "https://spotify/artist-1");
        var artistTwo = new SpotifyArtistDTO("artist-2", "John Coltrane", "https://spotify/artist-2");
        var albumDto = new SpotifyAlbumDTO(
                "album-1",
                "Kind of Blue",
                List.of(artistTwo, artistOne),
                "1959-08-17",
                5,
                "https://img/kind-of-blue",
                "https://spotify/album-1"
        );
        var trackDto = new SpotifyPlaylistTrackDTO(
                "track-1",
                "So What",
                List.of(artistOne, artistTwo),
                albumDto,
                545000,
                1,
                "https://spotify/track-1"
        );

        var artistNodeOne = ArtistNode.builder()
                .id("artist-node-1")
                .spotifyArtistId("artist-1")
                .name("Miles Davis")
                .build();
        var artistNodeTwo = ArtistNode.builder()
                .id("artist-node-2")
                .spotifyArtistId("artist-2")
                .name("John Coltrane")
                .build();
        var albumNode = AlbumNode.builder()
                .id("album-node-1")
                .spotifyAlbumId("album-1")
                .name("Kind of Blue")
                .build();
        var trackNode = TrackNode.builder()
                .id("track-node-1")
                .spotifyTrackId("track-1")
                .name("So What")
                .build();

        when(artistGraphWriter.upsertFromSpotify("artist-1", "Miles Davis", "https://spotify/artist-1")).thenReturn(artistNodeOne);
        when(artistGraphWriter.upsertFromSpotify("artist-2", "John Coltrane", "https://spotify/artist-2")).thenReturn(artistNodeTwo);
        when(albumGraphWriter.upsertFromSpotify(
                "album-1",
                "Kind of Blue",
                "1959-08-17",
                5,
                "https://img/kind-of-blue",
                "https://spotify/album-1"
        )).thenReturn(albumNode);
        when(trackGraphWriter.upsertFromSpotify(
                "track-1",
                "So What",
                545000,
                "https://spotify/track-1"
        )).thenReturn(trackNode);

        service.importPlaylistTracks(List.of(trackDto));

        verify(artistGraphWriter).upsertFromSpotify("artist-1", "Miles Davis", "https://spotify/artist-1");
        verify(artistGraphWriter).upsertFromSpotify("artist-2", "John Coltrane", "https://spotify/artist-2");
        verify(albumGraphWriter).upsertFromSpotify(
                "album-1",
                "Kind of Blue",
                "1959-08-17",
                5,
                "https://img/kind-of-blue",
                "https://spotify/album-1"
        );
        verify(trackGraphWriter).upsertFromSpotify(
                "track-1",
                "So What",
                545000,
                "https://spotify/track-1"
        );
        verify(neo4jClient, org.mockito.Mockito.times(3)).query(anyString());
    }

    @Test
    void importPlaylistTracksIgnoresEntriesWithoutTrackOrAlbumIdentifiers() {
        var service = new SpotifyPlaylistImportService(
                artistGraphWriter,
                albumGraphWriter,
                trackGraphWriter,
                neo4jClient
        );
        var invalidTrack = new SpotifyPlaylistTrackDTO(
                null,
                "Unknown",
                List.of(),
                new SpotifyAlbumDTO(
                        null,
                        "No Album",
                        List.of(),
                        null,
                        null,
                        null,
                        null
                ),
                null,
                null,
                null
        );

        service.importPlaylistTracks(List.of(invalidTrack));

        verify(artistGraphWriter, never()).upsertFromSpotify(any(), any(), any());
        verify(albumGraphWriter, never()).upsertFromSpotify(any(), any(), any(), any(), any(), any());
        verify(trackGraphWriter, never()).upsertFromSpotify(any(), any(), any(), any());
        verify(neo4jClient, never()).query(anyString());
    }
}
