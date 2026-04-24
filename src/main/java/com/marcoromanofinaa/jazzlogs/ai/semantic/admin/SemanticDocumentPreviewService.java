package com.marcoromanofinaa.jazzlogs.ai.semantic.admin;

import com.marcoromanofinaa.jazzlogs.ai.semantic.albumlog.AlbumLogSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.artistprofile.ArtistProfileSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.ai.semantic.tracknote.TrackNoteSemanticDocumentTransformer;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogNotFoundException;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileNotFoundException;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileRepository;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteNotFoundException;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SemanticDocumentPreviewService {

    /*
     * Los endpoints de preview son una herramienta de debug para la capa RAG.
     * Permiten inspeccionar el embedding text antes de pagar embeddings o escribir en el vector store.
     */
    private final AlbumLogRepository albumLogRepository;
    private final TrackNoteRepository trackNoteRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final AlbumLogSemanticDocumentTransformer albumLogTransformer;
    private final TrackNoteSemanticDocumentTransformer trackNoteTransformer;
    private final ArtistProfileSemanticDocumentTransformer artistProfileTransformer;

    public SemanticDocumentPreview previewAlbumLog(Integer logNumber) {
        log.debug("Generating semantic preview for album log {}", logNumber);
        var albumLog = albumLogRepository.findByLogNumber(logNumber)
                .orElseThrow(() -> new AlbumLogNotFoundException(logNumber));
        return SemanticDocumentPreview.from(albumLogTransformer.transform(albumLog));
    }

    public SemanticDocumentPreview previewTrackNote(String spotifyTrackId) {
        log.debug("Generating semantic preview for track note {}", spotifyTrackId);
        var trackNote = trackNoteRepository.findBySpotifyTrackId(spotifyTrackId)
                .orElseThrow(() -> new TrackNoteNotFoundException(spotifyTrackId));
        return SemanticDocumentPreview.from(trackNoteTransformer.transform(trackNote));
    }

    public SemanticDocumentPreview previewArtistProfile(String spotifyArtistId) {
        log.debug("Generating semantic preview for artist profile {}", spotifyArtistId);
        var artistProfile = artistProfileRepository.findBySpotifyArtistId(spotifyArtistId)
                .orElseThrow(() -> new ArtistProfileNotFoundException(spotifyArtistId));
        return SemanticDocumentPreview.from(artistProfileTransformer.transform(artistProfile));
    }
}
