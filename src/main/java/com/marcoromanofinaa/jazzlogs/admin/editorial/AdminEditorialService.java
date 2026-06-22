package com.marcoromanofinaa.jazzlogs.admin.editorial;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.UpsertAlbumEditorialRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.UpsertArtistEditorialRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.UpsertTrackEditorialRequestDTO;
import com.marcoromanofinaa.jazzlogs.core.outbox.editorial.EditorialEmbeddingOutboxPublisher;
import com.marcoromanofinaa.jazzlogs.editorial.graph.album.AlbumGraphWriter;
import com.marcoromanofinaa.jazzlogs.editorial.graph.artist.ArtistGraphWriter;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.AlbumNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ArtistNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.TrackNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.track.TrackGraphWriter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminEditorialService {

    private final EditorialEmbeddingOutboxPublisher outboxPublisher;
    private final AlbumGraphWriter albumGraphWriter;
    private final ArtistGraphWriter artistGraphWriter;
    private final TrackGraphWriter trackGraphWriter;

    @Transactional
    public void upsertArtistEditorial(UUID authenticatedUserId, UpsertArtistEditorialRequestDTO request) {
        ArtistNode savedArtist = artistGraphWriter.upsertEditorial(request.artistData());
        outboxPublisher.publishArtistEmbeddingSyncRequested(savedArtist, authenticatedUserId);
    }

    @Transactional
    public void upsertTrackEditorial(UUID authenticatedUserId, UpsertTrackEditorialRequestDTO request) {
        TrackNode savedTrack = trackGraphWriter.upsertEditorial(request.trackData());
        outboxPublisher.publishTrackEmbeddingSyncRequested(savedTrack, authenticatedUserId);
    }

    @Transactional
    public void upsertAlbumEditorial(UUID authenticatedUserId, UpsertAlbumEditorialRequestDTO request) {
        AlbumNode savedAlbum = albumGraphWriter.upsertEditorial(request.albumData());
        outboxPublisher.publishAlbumEmbeddingSyncRequested(savedAlbum, authenticatedUserId);
    }
}
