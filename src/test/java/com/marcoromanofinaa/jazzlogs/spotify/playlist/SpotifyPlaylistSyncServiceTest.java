package com.marcoromanofinaa.jazzlogs.spotify.playlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.spotify.auth.SpotifyConnectionService;
import com.marcoromanofinaa.jazzlogs.spotify.binding.SpotifyAlbumLogBindingService;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtist;
import com.marcoromanofinaa.jazzlogs.spotify.auth.SpotifyConnection;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtistRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrack;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackRepository;
import com.marcoromanofinaa.jazzlogs.spotify.client.SpotifyApiClient;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyProperties;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpotifyPlaylistSyncServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private SpotifyProperties spotifyProperties;

    @Mock
    private SpotifyConnectionService spotifyConnectionService;

    @Mock
    private SpotifyApiClient spotifyApiClient;

    @Mock
    private SpotifyAlbumRepository spotifyAlbumRepository;

    @Mock
    private SpotifyArtistRepository spotifyArtistRepository;

    @Mock
    private SpotifyTrackRepository spotifyTrackRepository;

    @Mock
    private SpotifyAlbumLogBindingService spotifyAlbumLogBindingService;

    @InjectMocks
    private SpotifyPlaylistSyncService service;

    @Test
    void syncConfiguredPlaylistReconcilesSnapshotsAndDeletesRemovedRows() throws Exception {
        var connection = SpotifyConnection.create(
                "access-token",
                "refresh-token",
                "Bearer",
                new String[]{"playlist-read-private"},
                Instant.now().plusSeconds(3600)
        );

        var existingAlbum = SpotifyAlbum.builder()
                .spotifyAlbumId("album-1")
                .sourcePlaylistId("playlist-1")
                .name("Old Album Name")
                .spotifyUrl("https://open.spotify.com/album/album-1-old")
                .build();
        var removedAlbum = SpotifyAlbum.builder()
                .spotifyAlbumId("album-removed")
                .sourcePlaylistId("playlist-1")
                .name("Removed Album")
                .build();

        var existingArtist = SpotifyArtist.builder()
                .spotifyArtistId("artist-1")
                .name("Old Artist Name")
                .spotifyUrl("https://open.spotify.com/artist/artist-1-old")
                .build();
        var removedArtist = SpotifyArtist.builder()
                .spotifyArtistId("artist-removed")
                .name("Removed Artist")
                .build();

        var existingTrack = SpotifyTrack.builder()
                .spotifyTrackId("track-1")
                .sourcePlaylistId("playlist-1")
                .album(existingAlbum)
                .mainArtist(existingArtist)
                .secondaryArtists(new LinkedHashSet<>())
                .name("Old Track Name")
                .spotifyUrl("https://open.spotify.com/track/track-1-old")
                .durationMs(1000)
                .build();
        var removedTrack = SpotifyTrack.builder()
                .spotifyTrackId("track-removed")
                .sourcePlaylistId("playlist-1")
                .album(removedAlbum)
                .mainArtist(removedArtist)
                .secondaryArtists(new LinkedHashSet<>())
                .name("Removed Track")
                .spotifyUrl("https://open.spotify.com/track/track-removed")
                .durationMs(1000)
                .build();

        when(spotifyProperties.playlistId()).thenReturn("playlist-1");
        when(spotifyProperties.market()).thenReturn("AR");
        when(spotifyConnectionService.getValidConnection()).thenReturn(connection);
        when(spotifyApiClient.fetchPlaylistItems(eq("access-token"), eq("playlist-1"), eq("AR"), eq(50), eq(0)))
                .thenReturn(new SpotifyApiClient.PlaylistItemsPage(
                        objectMapper.readTree("""
                                {
                                  "items": [
                                    {
                                      "added_at": "2026-04-17T00:00:00Z",
                                      "is_local": false,
                                      "item": {
                                        "type": "track",
                                        "id": "track-1",
                                        "name": "Updated Track Name",
                                        "duration_ms": 180000,
                                        "disc_number": 1,
                                        "track_number": 1,
                                        "external_urls": {"spotify": "https://open.spotify.com/track/track-1"},
                                        "album": {
                                          "id": "album-1",
                                          "name": "Updated Album Name",
                                          "album_type": "album",
                                          "total_tracks": 9,
                                          "release_date": "1965-01-01",
                                          "release_date_precision": "day",
                                          "external_urls": {"spotify": "https://open.spotify.com/album/album-1"},
                                          "images": [{"url": "https://img/album-1.jpg"}]
                                        },
                                        "artists": [
                                          {
                                            "id": "artist-1",
                                            "name": "Updated Artist Name",
                                            "type": "artist",
                                            "uri": "spotify:artist:artist-1",
                                            "href": "https://api.spotify.com/v1/artists/artist-1",
                                            "external_urls": {"spotify": "https://open.spotify.com/artist/artist-1"}
                                          },
                                          {
                                            "id": "artist-2",
                                            "name": "Guest Artist",
                                            "type": "artist",
                                            "uri": "spotify:artist:artist-2",
                                            "href": "https://api.spotify.com/v1/artists/artist-2",
                                            "external_urls": {"spotify": "https://open.spotify.com/artist/artist-2"}
                                          }
                                        ]
                                      }
                                    },
                                    {
                                      "added_at": "2026-04-17T00:01:00Z",
                                      "is_local": false,
                                      "item": {
                                        "type": "track",
                                        "id": "track-2",
                                        "name": "Brand New Track",
                                        "duration_ms": 240000,
                                        "disc_number": 1,
                                        "track_number": 2,
                                        "external_urls": {"spotify": "https://open.spotify.com/track/track-2"},
                                        "album": {
                                          "id": "album-2",
                                          "name": "Brand New Album",
                                          "album_type": "album",
                                          "total_tracks": 10,
                                          "release_date": "1966-01-01",
                                          "release_date_precision": "day",
                                          "external_urls": {"spotify": "https://open.spotify.com/album/album-2"},
                                          "images": [{"url": "https://img/album-2.jpg"}]
                                        },
                                        "artists": [
                                          {
                                            "id": "artist-3",
                                            "name": "Brand New Artist",
                                            "type": "artist",
                                            "uri": "spotify:artist:artist-3",
                                            "href": "https://api.spotify.com/v1/artists/artist-3",
                                            "external_urls": {"spotify": "https://open.spotify.com/artist/artist-3"}
                                          }
                                        ]
                                      }
                                    }
                                  ]
                                }
                                """).path("items"),
                        null,
                        2
                ));

        when(spotifyAlbumRepository.findAllBySourcePlaylistIdOrderByNameAsc("playlist-1"))
                .thenReturn(List.of(existingAlbum, removedAlbum));
        when(spotifyArtistRepository.findAll())
                .thenReturn(List.of(existingArtist, removedArtist));
        when(spotifyTrackRepository.findAllBySourcePlaylistId("playlist-1"))
                .thenReturn(List.of(existingTrack, removedTrack));
        when(spotifyAlbumRepository.saveAll(any())).thenAnswer(invocation -> toAlbumList(invocation.getArgument(0)));
        when(spotifyArtistRepository.saveAll(any())).thenAnswer(invocation -> toArtistList(invocation.getArgument(0)));
        when(spotifyTrackRepository.saveAll(any())).thenAnswer(invocation -> toTrackList(invocation.getArgument(0)));
        doNothing().when(spotifyTrackRepository).flush();
        doNothing().when(spotifyAlbumRepository).flush();
        doNothing().when(spotifyArtistRepository).flush();

        var syncedCount = service.syncConfiguredPlaylist();

        assertThat(syncedCount).isEqualTo(2);
        assertThat(existingAlbum.getName()).isEqualTo("Updated Album Name");
        assertThat(existingArtist.getName()).isEqualTo("Updated Artist Name");
        assertThat(existingTrack.getName()).isEqualTo("Updated Track Name");
        assertThat(existingTrack.getMainArtist().getSpotifyArtistId()).isEqualTo("artist-1");
        assertThat(existingTrack.getSecondaryArtists())
                .extracting(SpotifyArtist::getSpotifyArtistId)
                .containsExactly("artist-2");

        var savedAlbums = this.<SpotifyAlbum>iterableCaptor();
        var savedArtists = this.<SpotifyArtist>iterableCaptor();
        var savedTracks = this.<SpotifyTrack>iterableCaptor();
        verify(spotifyAlbumRepository).saveAll(savedAlbums.capture());
        verify(spotifyArtistRepository).saveAll(savedArtists.capture());
        verify(spotifyTrackRepository).saveAll(savedTracks.capture());

        assertThat(toAlbumList(savedAlbums.getValue()))
                .extracting(SpotifyAlbum::getSpotifyAlbumId)
                .containsExactlyInAnyOrder("album-1", "album-2");
        assertThat(toArtistList(savedArtists.getValue()))
                .extracting(SpotifyArtist::getSpotifyArtistId)
                .containsExactlyInAnyOrder("artist-1", "artist-2", "artist-3");
        assertThat(toTrackList(savedTracks.getValue()))
                .extracting(SpotifyTrack::getSpotifyTrackId)
                .containsExactlyInAnyOrder("track-1", "track-2");

        verify(spotifyTrackRepository).deleteAll(argThat(tracks ->
                toTrackList(tracks).stream().map(SpotifyTrack::getSpotifyTrackId).toList().equals(List.of("track-removed"))));
        verify(spotifyAlbumRepository).deleteAll(argThat(albums ->
                toAlbumList(albums).stream().map(SpotifyAlbum::getSpotifyAlbumId).toList().equals(List.of("album-removed"))));
        verify(spotifyArtistRepository).deleteAll(argThat(artists ->
                toArtistList(artists).stream().map(SpotifyArtist::getSpotifyArtistId).toList().equals(List.of("artist-removed"))));
        verify(spotifyAlbumLogBindingService).bindConfiguredPlaylistAlbumsToLogs();
    }

    @SuppressWarnings("unchecked")
    private List<SpotifyAlbum> toAlbumList(Object iterable) {
        var albums = (Iterable<SpotifyAlbum>) iterable;
        return albums instanceof List<SpotifyAlbum> list
                ? list
                : java.util.stream.StreamSupport.stream(albums.spliterator(), false).toList();
    }

    @SuppressWarnings("unchecked")
    private List<SpotifyArtist> toArtistList(Object iterable) {
        var artists = (Iterable<SpotifyArtist>) iterable;
        return artists instanceof List<SpotifyArtist> list
                ? list
                : java.util.stream.StreamSupport.stream(artists.spliterator(), false).toList();
    }

    @SuppressWarnings("unchecked")
    private List<SpotifyTrack> toTrackList(Object iterable) {
        var tracks = (Iterable<SpotifyTrack>) iterable;
        return tracks instanceof List<SpotifyTrack> list
                ? list
                : java.util.stream.StreamSupport.stream(tracks.spliterator(), false).toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> ArgumentCaptor<Iterable<T>> iterableCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
    }
}
