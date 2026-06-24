package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeepRecommendationToolTest {

    @Test
    void exposesDeclaredNameAndDescription() {
        var tool = new DeepRecommendationTool();

        assertThat(tool.name()).isEqualTo(JazzToolName.DEEP_RECOMMENDATION);
        assertThat(tool.description()).contains("deeper album or track curation");
        assertThat(tool.parametersSchema()).containsKey("properties");
    }

    @Test
    void executeFailsUntilImplemented() {
        var tool = new DeepRecommendationTool();

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.DEEP_RECOMMENDATION, null),
                new JazzAgentContext(null, null, "msg", "America/Argentina/Buenos_Aires", null, List.of(), null)
        ))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not implemented");
    }
}
