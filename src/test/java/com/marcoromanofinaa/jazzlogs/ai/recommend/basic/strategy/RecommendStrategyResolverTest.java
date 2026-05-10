package com.marcoromanofinaa.jazzlogs.ai.recommend.basic.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendMode;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendRequest;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendStrategyResolverTest {

    private final RecommendStrategyResolver resolver = new RecommendStrategyResolver(
            List.of(
                    new StubStrategy(AiRecommendMode.ALBUM),
                    new StubStrategy(AiRecommendMode.TRACKS)
            )
    );

    @Test
    void resolvesAlbumStrategyByDefault() {
        var strategy = resolver.resolve("I want something warm and nocturnal");
        assertThat(strategy).isInstanceOf(StubStrategy.class);
        assertThat(((StubStrategy) strategy).mode()).isEqualTo(AiRecommendMode.ALBUM);
    }

    @Test
    void resolvesTrackStrategyWhenTracksAreExplicitlyRequested() {
        var strategy = resolver.resolve("Dame algunos temas para manejar de noche");
        assertThat(strategy).isInstanceOf(StubStrategy.class);
        assertThat(((StubStrategy) strategy).mode()).isEqualTo(AiRecommendMode.TRACKS);
    }

    private record StubStrategy(AiRecommendMode mode) implements RecommendStrategy {
        @Override
        public AiRecommendMode supports() {
            return mode;
        }

        @Override
        public AiRecommendResponse recommend(AiRecommendRequest request) {
            throw new UnsupportedOperationException("Not needed for resolver test");
        }
    }
}
