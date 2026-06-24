package com.marcoromanofinaa.jazzlogs.recommendation.pro.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.chat.session.ResolvedRecommendationMemoryItem;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.DeepRecommendationTool;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JazzAgentPromptBuilderTest {

    @Test
    void buildSystemPromptIncludesToolingAndSessionContext() {
        var builder = new JazzAgentPromptBuilder(
                Clock.fixed(Instant.parse("2026-06-23T03:15:00Z"), ZoneOffset.UTC)
        );

        var memory = new ChatRecommendationMemory(
                new ChatRecommendationMemory.LastRecommendationBatch(
                        List.of(new WinnerReference(
                                BasicRecommendationTarget.ALBUM,
                                "node-1",
                                "Kind of Blue",
                                "Miles Davis"
                        )),
                        List.of(new ResolvedRecommendationMemoryItem(
                                "Kind of Blue",
                                null,
                                "Miles Davis",
                                "1959",
                                "Modal Jazz",
                                "instrumental",
                                List.of("cool", "hypnotic"),
                                "low",
                                "easy",
                                "mid",
                                "trumpet"
                        ))
                ),
                List.of(new ChatRecommendationMemory.RecommendationHistoryEntry(
                        1,
                        new WinnerReference(
                                BasicRecommendationTarget.ALBUM,
                                "node-1",
                                "Kind of Blue",
                                "Miles Davis"
                        ),
                        "Miles Davis",
                        "Kind of Blue",
                        null
                )),
                "El usuario viene explorando discos canónicos pero accesibles."
        );

        var recentExchange = ChatExchange.create(
                UUID.randomUUID(),
                "quiero algo canónico pero llevadero",
                AIModelType.PRO,
                "Probá Kind of Blue de Miles Davis.",
                List.of(),
                BasicRecommendationTarget.ALBUM,
                AIModelType.PRO,
                10L,
                20L,
                30L,
                Instant.parse("2026-06-23T03:10:00Z")
        );

        var prompt = builder.buildSystemPrompt(
                new JazzAgentContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "quiero ir más hondo por esa línea",
                        "America/Argentina/Buenos_Aires",
                        memory,
                        List.of(recentExchange),
                        new AIModelDefinition(
                                AIModelType.PRO,
                                AIProvider.OPENAI,
                                "gpt-5.4-mini",
                                true,
                                List.of(Plan.PLUS, Plan.PRO),
                                16_000,
                                4_000,
                                RecommendationFlowType.PRO
                        )
                ),
                List.of(new DeepRecommendationTool())
        );

        assertThat(prompt).contains("You are Jazzlogs PRO, the main jazz expert and companion inside JazzLogs.");
        assertThat(prompt).contains("DEEP_RECOMMENDATION");
        assertThat(prompt).contains("Kind of Blue");
        assertThat(prompt).contains("Miles Davis");
        assertThat(prompt).contains("America/Argentina/Buenos_Aires");
        assertThat(prompt).contains("quiero ir más hondo por esa línea");
    }
}
