package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.config.AIModelCatalog;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationOrchestratorTest {

    @Mock
    private AIModelCatalog aiModelCatalog;

    @Mock
    private RecommendationFlow recommendationFlow;

    @Test
    void generateDelegatesToResolvedFlow() {
        var recommendationFlowResolver = new RecommendationFlowResolver(List.of(recommendationFlow));
        var orchestrator = new RecommendationOrchestrator(aiModelCatalog, recommendationFlowResolver);
        var command = new RecommendationCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "armame una playlist",
                "America/Argentina/Buenos_Aires",
                AIModelType.PLAYLIST,
                null,
                List.of()
        );
        var modelDefinition = new AIModelDefinition(
                AIModelType.PLAYLIST,
                AIProvider.OPENAI,
                "gpt-5.5",
                true,
                List.of(Plan.PLUS, Plan.PRO),
                24_000,
                6_000,
                RecommendationFlowType.PLAYLIST
        );
        var expectedResult = RecommendationResult.fromAssistantResponse(
                "respuesta",
                List.of(new ModelUsage(
                        UsageRecordStage.BASIC_RECOMMENDATION,
                        AIModelType.PLAYLIST,
                        "gpt-5.5",
                        10,
                        0,
                        20
                ))
        );

        when(aiModelCatalog.getModel(AIModelType.PLAYLIST)).thenReturn(modelDefinition);
        when(recommendationFlow.flowType()).thenReturn(RecommendationFlowType.PLAYLIST);
        when(recommendationFlow.generate(any())).thenReturn(expectedResult);

        var result = orchestrator.generate(command);

        assertThat(result).isEqualTo(expectedResult);
        verify(recommendationFlow).generate(any());
    }

    @Test
    void generatePassesRecommendationMemoryIntoFlowCommand() {
        var recommendationFlowResolver = new RecommendationFlowResolver(List.of(recommendationFlow));
        var orchestrator = new RecommendationOrchestrator(aiModelCatalog, recommendationFlowResolver);
        var recommendationMemory = new ChatRecommendationMemory(null, List.of(), "Le gusta el hard bop nocturno.");
        var command = new RecommendationCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "dame algo de esa onda",
                "America/Argentina/Buenos_Aires",
                AIModelType.BASIC,
                recommendationMemory,
                List.of()
        );

        when(aiModelCatalog.getModel(AIModelType.BASIC)).thenReturn(basicModel());
        when(recommendationFlow.flowType()).thenReturn(RecommendationFlowType.BASIC);
        when(recommendationFlow.generate(any())).thenReturn(RecommendationResult.fromAssistantResponse(
                "respuesta",
                List.of(new ModelUsage(
                        UsageRecordStage.BASIC_RECOMMENDATION,
                        AIModelType.BASIC,
                        "gpt-5.4-mini",
                        10,
                        0,
                        20
                ))
        ));

        orchestrator.generate(command);

        var commandCaptor = ArgumentCaptor.forClass(RecommendationFlowCommand.class);
        verify(recommendationFlow).generate(commandCaptor.capture());
        assertThat(commandCaptor.getValue().recommendationMemory()).isEqualTo(recommendationMemory);
        assertThat(commandCaptor.getValue().sessionSummary()).isEqualTo("Le gusta el hard bop nocturno.");
        assertThat(commandCaptor.getValue().contextualizedUserMessage()).isNull();
        assertThat(commandCaptor.getValue().recommendationTarget()).isNull();
    }

    private AIModelDefinition basicModel() {
        return new AIModelDefinition(
                AIModelType.BASIC,
                AIProvider.OPENAI,
                "gpt-5.4-mini",
                true,
                List.of(Plan.FREE, Plan.TRIAL, Plan.PLUS, Plan.PRO),
                8_000,
                2_000,
                RecommendationFlowType.BASIC
        );
    }
}
