package com.marcoromanofinaa.jazzlogs.ai.recommend.flow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendFlowResolverTest {

    private final RecommendFlowResolver resolver = new RecommendFlowResolver(
            List.of(new AlbumRecommendFlow(), new TrackRecommendFlow())
    );

    @Test
    void resolvesAlbumFlowByDefault() {
        assertThat(resolver.resolve("I want something warm and nocturnal"))
                .isInstanceOf(AlbumRecommendFlow.class);
    }

    @Test
    void resolvesTrackFlowWhenTracksAreExplicitlyRequested() {
        assertThat(resolver.resolve("Dame algunos temas para manejar de noche"))
                .isInstanceOf(TrackRecommendFlow.class);
    }
}
