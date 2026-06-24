package com.marcoromanofinaa.jazzlogs.recommendation.pro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationFlowCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationTiming;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentService;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProRecommendationFlowTest {

    @Test
    void generateReturnsAgentRecommendationResult() {
        var jazzAgentService = Mockito.mock(JazzAgentService.class);
        var flow = new ProRecommendationFlow(jazzAgentService);
        var command = new RecommendationFlowCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "quiero algo mas profundo",
                "America/Argentina/Buenos_Aires",
                null,
                null,
                null,
                List.of(),
                AIModelType.PRO,
                new AIModelDefinition(
                        AIModelType.PRO,
                        AIProvider.OPENAI,
                        "gpt-5.4-mini",
                        true,
                        List.of(Plan.PLUS, Plan.PRO),
                        16_000,
                        4_000,
                        RecommendationFlowType.PRO
                ),
                List.of(),
                null
        );

        when(jazzAgentService.run(any())).thenReturn(new RecommendationResult(
                "Respuesta PRO",
                List.of(),
                null,
                "Titulo",
                "Resumen",
                new RecommendationTiming(1L, 2L, 3L),
                List.of(new ModelUsage(UsageRecordStage.AGENT, AIModelType.PRO, "gpt-5.4-mini", 10, 0, 5))
        ));

        var result = flow.generate(command);

        assertThat(result.assistantResponse()).isEqualTo("Respuesta PRO");
        assertThat(result.suggestedChatTitle()).isEqualTo("Titulo");
        assertThat(result.updatedSessionSummary()).isEqualTo("Resumen");
        assertThat(result.usageEntries()).hasSize(1);
    }
}
