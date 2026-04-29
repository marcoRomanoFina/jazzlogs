package com.marcoromanofinaa.jazzlogs.ai.recommend.flow;

import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendMode;
import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendFlowResolver {

    private static final Set<String> TRACK_KEYWORDS = Set.of(
            "track",
            "tracks",
            "song",
            "songs",
            "tema",
            "temas",
            "tracklist",
            "cancion",
            "canciones",
            "canción"
    );

    private final List<RecommendFlow> flows;

    public RecommendFlow resolve(String question) {
        var mode = resolveMode(question);
        return flows.stream()
                .filter(flow -> flow.supports() == mode)
                .findFirst()
                .orElseThrow(() -> new FeatureUnavailableException("Unsupported recommend mode: " + mode));
    }

    private AiRecommendMode resolveMode(String question) {
        var normalizedQuestion = normalize(question);

        if (TRACK_KEYWORDS.stream().anyMatch(keyword -> normalizedQuestion.matches(".*\\b%s\\b.*".formatted(keyword)))) {
            return AiRecommendMode.TRACKS;
        }

        return AiRecommendMode.ALBUM;
    }

    private String normalize(String value) {
        var normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}
