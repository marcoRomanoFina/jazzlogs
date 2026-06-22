package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist;

import com.marcoromanofinaa.jazzlogs.editorial.graph.album.AlbumGraphWriter;
import com.marcoromanofinaa.jazzlogs.editorial.graph.artist.ArtistGraphWriter;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.AlbumNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ArtistNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.track.TrackGraphWriter;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyCatalogImportException;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyPlaylistTrackDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpotifyPlaylistImportService {

    private record ArtistAlbumLeaderLink(String artistId, String albumId) {
    }

    private record AlbumTrackLink(String albumId, String trackId, Integer trackNumber) {
    }

    private record ArtistTrackPerformanceLink(String artistId, String trackId, int position, boolean primaryCredit) {
    }

    private final ArtistGraphWriter artistGraphWriter;
    private final AlbumGraphWriter albumGraphWriter;
    private final TrackGraphWriter trackGraphWriter;
    private final Neo4jClient neo4jClient;

    public void importPlaylistTracks(List<SpotifyPlaylistTrackDTO> tracks) {
        try {
            if (tracks == null || tracks.isEmpty()) {
                return;
            }

            var importableTracks = tracks.stream()
                    .filter(this::isImportableTrack)
                    .toList();

            if (importableTracks.isEmpty()) {
                return;
            }

            var artistsBySpotifyId = upsertArtists(importableTracks);
            var albumsBySpotifyId = upsertAlbums(importableTracks, artistsBySpotifyId);
            upsertTracks(importableTracks, artistsBySpotifyId, albumsBySpotifyId);
        }
        catch (RuntimeException exception) {
            log.error("Spotify playlist catalog import failed. trackCount={}", tracks == null ? 0 : tracks.size(), exception);
            throw new SpotifyCatalogImportException(buildImportFailureMessage(exception), exception);
        }
    }

    private Map<String, ArtistNode> upsertArtists(List<SpotifyPlaylistTrackDTO> tracks) {
        var artistDtosBySpotifyId = new LinkedHashMap<String, SpotifyArtistDTO>();
        for (SpotifyPlaylistTrackDTO track : tracks) {
            collectArtists(track.artists(), artistDtosBySpotifyId);
            collectArtists(track.album().artists(), artistDtosBySpotifyId);
        }

        var artistsBySpotifyId = new LinkedHashMap<String, ArtistNode>();

        for (SpotifyArtistDTO artist : artistDtosBySpotifyId.values()) {
            try {
                var artistNode = artistGraphWriter.upsertFromSpotify(
                        artist.spotifyArtistId(),
                        defaultText(artist.name()),
                        artist.spotifyUrl()
                );
                artistsBySpotifyId.put(artist.spotifyArtistId(), artistNode);
            }
            catch (RuntimeException exception) {
                throw stageFailure(
                        "upsert artist",
                        exception,
                        contextOf(
                                "spotifyArtistId", artist.spotifyArtistId(),
                                "artistName", defaultText(artist.name())
                        )
                );
            }
        }
        return artistsBySpotifyId;
    }

    private Map<String, AlbumNode> upsertAlbums(
            List<SpotifyPlaylistTrackDTO> tracks,
            Map<String, ArtistNode> artistsBySpotifyId
    ) {
        var albumDtosBySpotifyId = new LinkedHashMap<String, com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyAlbumDTO>();
        for (SpotifyPlaylistTrackDTO track : tracks) {
            albumDtosBySpotifyId.put(track.album().spotifyAlbumId(), track.album());
        }

        var albumsBySpotifyId = new LinkedHashMap<String, AlbumNode>();
        var leaderLinks = new ArrayList<ArtistAlbumLeaderLink>();

        for (var album : albumDtosBySpotifyId.values()) {
            try {
                var albumNode = albumGraphWriter.upsertFromSpotify(
                        album.spotifyAlbumId(),
                        defaultText(album.name()),
                        album.releaseDate(),
                        album.totalTracks(),
                        album.imageUrl(),
                        album.spotifyUrl()
                );

                for (var artist : resolveArtistsInSpotifyOrder(album.artists(), artistsBySpotifyId)) {
                    leaderLinks.add(new ArtistAlbumLeaderLink(artist.getId(), albumNode.getId()));
                }
                albumsBySpotifyId.put(album.spotifyAlbumId(), albumNode);
            }
            catch (RuntimeException exception) {
                throw stageFailure(
                        "upsert album",
                        exception,
                        contextOf(
                                "spotifyAlbumId", album.spotifyAlbumId(),
                                "albumName", defaultText(album.name())
                        )
                );
            }
        }

        try {
            linkArtistsToAlbumsAsLeaders(leaderLinks);
        }
        catch (RuntimeException exception) {
            throw stageFailure(
                    "link album leaders",
                    exception,
                    contextOf("linkCount", leaderLinks.size())
            );
        }
        return albumsBySpotifyId;
    }

    private void upsertTracks(
            List<SpotifyPlaylistTrackDTO> tracks,
            Map<String, ArtistNode> artistsBySpotifyId,
            Map<String, AlbumNode> albumsBySpotifyId
    ) {
        var albumTrackLinks = new ArrayList<AlbumTrackLink>();
        var artistTrackLinks = new ArrayList<ArtistTrackPerformanceLink>();

        for (SpotifyPlaylistTrackDTO track : tracks) {
            try {
                var album = albumsBySpotifyId.get(track.album().spotifyAlbumId());
                var trackNode = trackGraphWriter.upsertFromSpotify(
                        track.spotifyTrackId(),
                        defaultText(track.name()),
                        track.durationMs(),
                        track.spotifyUrl()
                );

                if (album != null) {
                    albumTrackLinks.add(new AlbumTrackLink(album.getId(), trackNode.getId(), track.trackNumber()));
                }

                var orderedArtists = resolveArtistsInSpotifyOrder(track.artists(), artistsBySpotifyId);
                for (int index = 0; index < orderedArtists.size(); index++) {
                    var artist = orderedArtists.get(index);
                    artistTrackLinks.add(new ArtistTrackPerformanceLink(
                            artist.getId(),
                            trackNode.getId(),
                            index,
                            index == 0
                    ));
                }
            }
            catch (RuntimeException exception) {
                throw stageFailure(
                        "upsert track",
                        exception,
                        contextOf(
                                "spotifyTrackId", track.spotifyTrackId(),
                                "trackName", defaultText(track.name()),
                                "spotifyAlbumId", track.album() == null ? null : track.album().spotifyAlbumId(),
                                "albumName", track.album() == null ? null : defaultText(track.album().name())
                        )
                );
            }
        }

        try {
            linkAlbumsToTracks(albumTrackLinks);
            linkArtistsToTracks(artistTrackLinks);
        }
        catch (RuntimeException exception) {
            throw stageFailure(
                    "link track relationships",
                    exception,
                    contextOf(
                            "albumTrackLinkCount", albumTrackLinks.size(),
                            "artistTrackLinkCount", artistTrackLinks.size()
                    )
            );
        }
    }

    private void collectArtists(
            List<SpotifyArtistDTO> artists,
            Map<String, SpotifyArtistDTO> artistDtosBySpotifyId
    ) {
        if (artists == null || artists.isEmpty()) {
            return;
        }

        for (SpotifyArtistDTO artist : artists) {
            if (artist == null || isBlank(artist.spotifyArtistId())) {
                continue;
            }
            artistDtosBySpotifyId.put(artist.spotifyArtistId(), artist);
        }
    }

    private List<ArtistNode> resolveArtistsInSpotifyOrder(
            List<SpotifyArtistDTO> artists,
            Map<String, ArtistNode> artistsBySpotifyId
    ) {
        List<ArtistNode> resolvedArtists = new ArrayList<>();
        if (artists == null || artists.isEmpty()) {
            return resolvedArtists;
        }

        for (SpotifyArtistDTO artist : artists) {
            if (artist == null || isBlank(artist.spotifyArtistId())) {
                continue;
            }

            var artistNode = artistsBySpotifyId.get(artist.spotifyArtistId());
            if (artistNode != null) {
                resolvedArtists.add(artistNode);
            }
        }

        return resolvedArtists;
    }

    private boolean isImportableTrack(SpotifyPlaylistTrackDTO track) {
        return track != null
                && !isBlank(track.spotifyTrackId())
                && track.album() != null
                && !isBlank(track.album().spotifyAlbumId());
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void linkArtistsToAlbumsAsLeaders(List<ArtistAlbumLeaderLink> links) {
        if (links.isEmpty()) {
            return;
        }

        var batch = links.stream()
                .map(link -> {
                    var values = new LinkedHashMap<String, Object>();
                    values.put("artistId", link.artistId());
                    values.put("albumId", link.albumId());
                    return values;
                })
                .toList();

        neo4jClient.query("""
                UNWIND $batch AS row
                MATCH (artist:Artist {id: row.artistId})
                MATCH (album:Album {id: row.albumId})
                MERGE (artist)-[:LEADER_OF]->(album)
                """)
                .bind(batch).to("batch")
                .run();
    }

    private void linkAlbumsToTracks(List<AlbumTrackLink> links) {
        if (links.isEmpty()) {
            return;
        }

        var batch = links.stream()
                .map(link -> {
                    var values = new LinkedHashMap<String, Object>();
                    values.put("albumId", link.albumId());
                    values.put("trackId", link.trackId());
                    values.put("trackNumber", link.trackNumber());
                    return values;
                })
                .toList();

        neo4jClient.query("""
                UNWIND $batch AS row
                MATCH (album:Album {id: row.albumId})
                MATCH (track:Track {id: row.trackId})
                MERGE (album)-[relationship:CONTAINS]->(track)
                SET relationship.trackNumber = row.trackNumber
                """)
                .bind(batch).to("batch")
                .run();
    }

    private void linkArtistsToTracks(List<ArtistTrackPerformanceLink> links) {
        if (links.isEmpty()) {
            return;
        }

        var batch = links.stream()
                .map(link -> {
                    var values = new LinkedHashMap<String, Object>();
                    values.put("artistId", link.artistId());
                    values.put("trackId", link.trackId());
                    values.put("position", link.position());
                    values.put("primaryCredit", link.primaryCredit());
                    return values;
                })
                .toList();

        neo4jClient.query("""
                UNWIND $batch AS row
                MATCH (artist:Artist {id: row.artistId})
                MATCH (track:Track {id: row.trackId})
                MERGE (artist)-[relationship:PERFORMED_ON]->(track)
                SET relationship.position = row.position,
                    relationship.primaryCredit = row.primaryCredit
                """)
                .bind(batch).to("batch")
                .run();
    }

    private RuntimeException stageFailure(
            String stage,
            RuntimeException cause,
            Map<String, Object> context
    ) {
        log.error("Spotify playlist import failed during stage={} context={}", stage, context, cause);
        return new IllegalStateException(
                "Spotify playlist import failed during stage=%s context=%s rootCause=%s"
                        .formatted(stage, context, rootCauseMessage(cause)),
                cause
        );
    }

    private String buildImportFailureMessage(RuntimeException exception) {
        var message = exception.getMessage();
        if (message != null && !message.isBlank()) {
            return "Failed to import Spotify playlist tracks into catalog: " + message;
        }
        return "Failed to import Spotify playlist tracks into catalog: " + rootCauseMessage(exception);
    }

    private String rootCauseMessage(Throwable throwable) {
        var current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        var message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private Map<String, Object> contextOf(Object... keyValues) {
        var context = new LinkedHashMap<String, Object>();
        for (int index = 0; index < keyValues.length; index += 2) {
            context.put((String) keyValues[index], keyValues[index + 1]);
        }
        return context;
    }
}
