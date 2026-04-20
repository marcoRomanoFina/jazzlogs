package com.marcoromanofinaa.jazzlogs.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marcoromanofinaa.jazzlogs.logbook.domain.ArtistProfile;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.ArtistProfileRepository;
import jakarta.validation.Validation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistProfileImportServiceTest {

    @Mock
    private ArtistProfileRepository artistProfileRepository;

    @TempDir
    Path tempDir;

    @Test
    void importsArtistProfilesAndSkipsTemplateEntry() throws Exception {
        var service = new ArtistProfileImportService(
                JsonMapper.builder().findAndAddModules().build(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                artistProfileRepository
        );
        var json = """
                [
                  {
                    "spotifyArtistId": "artist-1",
                    "name": "Test Artist",
                    "primaryInstrument": "piano",
                    "mainStyles": ["hard_bop"],
                    "signatureSound": "Bright piano.",
                    "artistContext": "Context.",
                    "jazzlogsTake": "Take.",
                    "recommendedEntryPoint": "Test Album",
                    "bestFor": ["focus"],
                    "avoidIf": "Avoid.",
                    "relatedArtists": ["Related Artist"],
                    "importance": "Important.",
                    "logAppearances": [1, 2]
                  },
                  {
                    "spotifyArtistId": "",
                    "name": "",
                    "mainStyles": [],
                    "bestFor": [],
                    "relatedArtists": [],
                    "logAppearances": []
                  }
                ]
                """;
        var path = writeSeedFile(json);
        when(artistProfileRepository.findAllBySpotifyArtistIdIn(anyCollection())).thenReturn(List.of());

        var imported = service.importFromJson(path);

        assertThat(imported).isEqualTo(1);
        var captor = ArgumentCaptor.forClass(Iterable.class);
        verify(artistProfileRepository).saveAll(captor.capture());
        var saved = toList(captor.getValue());
        assertThat(saved).singleElement().satisfies(profile -> {
            assertThat(profile.getSpotifyArtistId()).isEqualTo("artist-1");
            assertThat(profile.getName()).isEqualTo("Test Artist");
            assertThat(profile.getMainStyles()).containsExactly("hard_bop");
            assertThat(profile.getBestFor()).containsExactly("focus");
            assertThat(profile.getRelatedArtists()).containsExactly("Related Artist");
            assertThat(profile.getLogAppearances()).containsExactly(1, 2);
        });
    }

    private Path writeSeedFile(String json) throws Exception {
        var path = tempDir.resolve("artists.json");
        Files.writeString(path, json);
        return path;
    }

    private List<ArtistProfile> toList(Object iterable) {
        var profiles = new ArrayList<ArtistProfile>();
        for (var profile : (Iterable<?>) iterable) {
            profiles.add((ArtistProfile) profile);
        }
        return profiles;
    }
}
