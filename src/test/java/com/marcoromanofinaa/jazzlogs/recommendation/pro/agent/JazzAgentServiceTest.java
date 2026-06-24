package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.prompt.JazzAgentPromptBuilder;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolCall;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolDispatcher;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolExecutionResult;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolName;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class JazzAgentServiceTest {

    @Test
    void runExecutesToolCallsAndReturnsStructuredRecommendationResult() {
        var toolDispatcher = Mockito.mock(JazzToolDispatcher.class);
        var promptBuilder = new JazzAgentPromptBuilder(Clock.fixed(Instant.parse("2026-06-23T03:15:00Z"), ZoneOffset.UTC));
        var modelClient = Mockito.mock(JazzAgentModelClient.class);
        var service = new JazzAgentService(toolDispatcher, promptBuilder, modelClient);
        var context = context();

        when(toolDispatcher.registeredTools()).thenReturn(List.of());
        when(modelClient.createInitialResponse(any(), anyString(), anyCollection())).thenReturn(
                new JazzAgentModelTurnResponse(
                        "resp_1",
                        List.of(new JazzAgentToolCallRequest(
                                "call_1",
                                JazzToolName.DEEP_RECOMMENDATION,
                                Map.of("request", "algo profundo", "focus", "album", "continuationOfPreviousLine", false)
                        )),
                        null,
                        new ModelUsage(UsageRecordStage.AGENT, AIModelType.PRO, "gpt-5.4-mini", 100, 0, 20)
                )
        );
        when(toolDispatcher.dispatch(any(JazzToolCall.class), any())).thenReturn(
                new JazzToolExecutionResult(JazzToolName.DEEP_RECOMMENDATION, "tool result", Map.of("kind", "dummy"))
        );
        when(modelClient.createFollowUpResponse(any(), anyString(), anyString(), anyCollection(), anyList())).thenReturn(
                new JazzAgentModelTurnResponse(
                        "resp_2",
                        List.of(),
                        new JazzAgentFinalAnswer(
                                JazzAgentResultType.CATALOG_RESPONSE,
                                "Te llevo por una línea más profunda y cinematográfica.",
                                List.of(new WinnerReference(
                                        BasicRecommendationTarget.ALBUM,
                                        "node-1",
                                        "Speak No Evil",
                                        "Wayne Shorter"
                                )),
                                BasicRecommendationTarget.ALBUM,
                                "Noche más profunda",
                                "El usuario quiere una línea más profunda y cinematográfica."
                        ),
                        new ModelUsage(UsageRecordStage.AGENT, AIModelType.PRO, "gpt-5.4-mini", 50, 10, 30)
                )
        );

        var result = service.run(context);

        assertThat(result.assistantResponse()).isEqualTo("Te llevo por una línea más profunda y cinematográfica.");
        assertThat(result.recommendationType()).isEqualTo(BasicRecommendationTarget.ALBUM);
        assertThat(result.winners()).hasSize(1);
        assertThat(result.suggestedChatTitle()).isEqualTo("Noche más profunda");
        assertThat(result.usageEntries()).hasSize(2);
        verify(modelClient).createInitialResponse(any(), anyString(), anyCollection());
        verify(modelClient).createFollowUpResponse(any(), anyString(), anyString(), anyCollection(), anyList());

        var captor = ArgumentCaptor.forClass(JazzToolCall.class);
        verify(toolDispatcher).dispatch(captor.capture(), any());
        assertThat(captor.getValue().toolName()).isEqualTo(JazzToolName.DEEP_RECOMMENDATION);
        assertThat(captor.getValue().arguments()).containsEntry("focus", "album");
    }

    private JazzAgentContext context() {
        return new JazzAgentContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "quiero algo más profundo",
                "America/Argentina/Buenos_Aires",
                null,
                List.of(),
                new AIModelDefinition(
                        AIModelType.PRO,
                        AIProvider.OPENAI,
                        "gpt-5.4-mini",
                        true,
                        List.of(Plan.PLUS, Plan.PRO),
                        16_000,
                        8_000,
                        RecommendationFlowType.PRO
                )
        );
    }
}
