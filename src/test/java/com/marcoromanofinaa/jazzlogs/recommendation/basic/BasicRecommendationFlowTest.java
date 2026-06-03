package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.config.OpenAIRecommendationProperties;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMClient;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMClientResolver;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMResult;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMResponseValidator;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.StructuredLLMResult;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationFlowCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesContext;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesService;
import com.marcoromanofinaa.jazzlogs.recommendation.retrieval.RetrievalCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.retrieval.RetrievalService;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.ConversationRouter;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRoute;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterResponse;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterResult;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationUserIntent;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

class BasicRecommendationFlowTest {

    @Test
    void generateReturnsRouterDirectAnswerWithoutCallingRetrieval() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                new ConversationRouterResponse(
                        ConversationRoute.DIRECT_ANSWER,
                        ConversationUserIntent.SMALLTALK,
                        false,
                        false,
                        "Le interesa arrancar suave.",
                        "Charla jazz",
                        null,
                        "Hola!",
                        null,
                        null
                ),
                List.of(routerUsage())
        ));

        var result = flow.generate(baseCommand(null));

        assertThat(result.assistantResponse()).isEqualTo("Hola!");
        assertThat(result.winners()).isEmpty();
        assertThat(result.usageEntries()).containsExactly(routerUsage());
        Mockito.verifyNoInteractions(retrievalService, promptBuilder, llmClientResolver, preferencesService);
    }

    @Test
    void generateNormalizesAlbumWinnerWhenModelReturnsTrackName() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var candidateDocument = new Document(
                "Album overview",
                Map.of(
                        "sourceType", "TRACK_LOG",
                        "album", "Waltz for Debby",
                        "track", "My Foolish Heart"
                )
        );
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                new ConversationRouterResponse(
                        ConversationRoute.MUSIC_RECOMMENDATION,
                        ConversationUserIntent.RECOMMEND_ALBUM,
                        false,
                        true,
                        "Le viene gustando una linea mas nocturna.",
                        null,
                        "hoy ta para un album",
                        null,
                        null,
                        List.of()
                ),
                List.of(routerUsage())
        ));
        Mockito.when(preferencesService.getPreferencesContext(Mockito.any())).thenReturn(
                new UserPreferencesContext(null, List.of(), List.of())
        );
        Mockito.when(retrievalService.retrieveRelevantDocuments(Mockito.any())).thenReturn(List.of(candidateDocument));
        Mockito.when(promptBuilder.build(Mockito.any())).thenReturn(new Prompt("prompt"));
        Mockito.when(llmClientResolver.resolve(AIProvider.OPENAI)).thenReturn(llmClient);
        Mockito.when(llmResponseValidator.validate(Mockito.any(), Mockito.eq(BasicRecommendationFlow.BasicRecommendationResponse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(llmClient.generateStructured(Mockito.any())).thenReturn(
                new StructuredLLMResult<>(
                        new BasicRecommendationFlow.BasicRecommendationResponse(
                                "Hoy va hermoso My Foolish Heart.",
                                BasicRecommendationTarget.ALBUM,
                                List.of("My Foolish Heart"),
                                null
                        ),
                        AIModelType.BASIC,
                        "gpt-5.4-mini",
                        100,
                        0,
                        40
                )
        );

        var result = flow.generate(baseCommand(null));

        assertThat(result.winners()).containsExactly("Waltz for Debby");
        assertThat(result.usageEntries()).containsExactly(routerUsage(), basicUsage());
    }

    @Test
    void generateUsesContextualizedQueryFromRouterForRetrieval() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                new ConversationRouterResponse(
                        ConversationRoute.MUSIC_RECOMMENDATION,
                        ConversationUserIntent.RECOMMEND_TRACK,
                        true,
                        true,
                        null,
                        null,
                        "Recommend tracks from the last recommended album Blue Hour.",
                        null,
                        null,
                        List.of()
                ),
                List.of(routerUsage())
        ));
        Mockito.when(preferencesService.getPreferencesContext(Mockito.any())).thenReturn(
                new UserPreferencesContext(null, List.of(), List.of())
        );
        Mockito.when(retrievalService.retrieveRelevantDocuments(Mockito.any())).thenReturn(List.of());
        Mockito.when(promptBuilder.build(Mockito.any())).thenReturn(new Prompt("prompt"));
        Mockito.when(llmClientResolver.resolve(AIProvider.OPENAI)).thenReturn(llmClient);
        Mockito.when(llmClient.generate(Mockito.any())).thenReturn(new LLMResult(
                "No tengo un match sólido ahora mismo.",
                AIModelType.BASIC,
                "gpt-5.4-nano",
                20,
                0,
                15
        ));
        Mockito.when(llmResponseValidator.validate(Mockito.any(), Mockito.eq(BasicRecommendationFlow.BasicRecommendationResponse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(llmClient.generateStructured(Mockito.any())).thenReturn(
                new StructuredLLMResult<>(
                        new BasicRecommendationFlow.BasicRecommendationResponse(
                                "Aca van tres.",
                                BasicRecommendationTarget.TRACKS,
                                List.of("Track 1", "Track 2", "Track 3"),
                                null
                        ),
                        AIModelType.BASIC,
                        "gpt-5.4-mini",
                        100,
                        0,
                        40
                )
        );

        flow.generate(baseCommand(null));

        var retrievalCommand = Mockito.mockingDetails(retrievalService)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("retrieveRelevantDocuments"))
                .map(invocation -> (RetrievalCommand) invocation.getArgument(0))
                .findFirst()
                .orElseThrow();

        assertThat(retrievalCommand.userMessage())
                .isEqualTo("Recommend tracks from the last recommended album Blue Hour.");
    }

    @Test
    void generateReturnsFallbackWithoutCallingMiniWhenNoCandidatesExist() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                new ConversationRouterResponse(
                        ConversationRoute.MUSIC_RECOMMENDATION,
                        ConversationUserIntent.RECOMMEND_ALBUM,
                        false,
                        true,
                        null,
                        "Jazz para leer",
                        "algo para leer",
                        null,
                        null,
                        List.of()
                ),
                List.of(routerUsage())
        ));
        Mockito.when(preferencesService.getPreferencesContext(Mockito.any())).thenReturn(
                new UserPreferencesContext(null, List.of(), List.of())
        );
        Mockito.when(retrievalService.retrieveRelevantDocuments(Mockito.any())).thenReturn(List.of());
        Mockito.when(llmClientResolver.resolve(AIProvider.OPENAI)).thenReturn(llmClient);
        Mockito.when(llmClient.generateStructured(Mockito.any())).thenReturn(null);
        Mockito.when(llmClient.generate(Mockito.any())).thenReturn(new LLMResult(
                "No tengo un match sólido ahora mismo.",
                AIModelType.BASIC,
                "gpt-5.4-nano",
                20,
                0,
                15
        ));

        var result = flow.generate(baseCommand(null));

        assertThat(result.winners()).isEmpty();
        assertThat(result.recommendationType()).isNull();
        assertThat(result.assistantResponse()).contains("No tengo un match sólido");
        assertThat(result.suggestedChatTitle()).isEqualTo("Jazz para leer");
        Mockito.verifyNoInteractions(promptBuilder, llmResponseValidator);
    }

    @Test
    void generateFailsWhenMiniReturnsRecommendationTypeThatConflictsWithRouterTarget() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var candidateDocument = new Document(
                "Album overview",
                Map.of("sourceType", "ALBUM_LOG", "album", "Waltz for Debby")
        );
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                new ConversationRouterResponse(
                        ConversationRoute.MUSIC_RECOMMENDATION,
                        ConversationUserIntent.RECOMMEND_ALBUM,
                        false,
                        true,
                        null,
                        null,
                        "quiero un disco",
                        null,
                        null,
                        List.of()
                ),
                List.of(routerUsage())
        ));
        Mockito.when(preferencesService.getPreferencesContext(Mockito.any())).thenReturn(
                new UserPreferencesContext(null, List.of(), List.of())
        );
        Mockito.when(retrievalService.retrieveRelevantDocuments(Mockito.any())).thenReturn(List.of(candidateDocument));
        Mockito.when(promptBuilder.build(Mockito.any())).thenReturn(new Prompt("prompt"));
        Mockito.when(llmClientResolver.resolve(AIProvider.OPENAI)).thenReturn(llmClient);
        Mockito.when(llmResponseValidator.validate(Mockito.any(), Mockito.eq(BasicRecommendationFlow.BasicRecommendationResponse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(llmClient.generateStructured(Mockito.any())).thenReturn(
                new StructuredLLMResult<>(
                        new BasicRecommendationFlow.BasicRecommendationResponse(
                                "Aca van tres.",
                                BasicRecommendationTarget.TRACKS,
                                List.of("Track 1", "Track 2", "Track 3"),
                                null
                        ),
                        AIModelType.BASIC,
                        "gpt-5.4-mini",
                        100,
                        0,
                        40
                )
        );

        assertThatThrownBy(() -> flow.generate(baseCommand(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationType");
    }

    @Test
    void generateExcludesAllPreviouslyRecommendedTracksFromReferencedAlbum() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var recommendationMemory = new ChatRecommendationMemory(
                new ChatRecommendationMemory.LastRecommendedItem(
                        "TRACKS",
                        "respuesta",
                        List.of("Under a Blanket of Blue", "Tenderly", "The Nearness of You"),
                        List.of(
                                trackMetadata("Ella and Louis"),
                                trackMetadata("Ella and Louis"),
                                trackMetadata("Ella and Louis")
                        )
                ),
                List.of(
                        new ChatRecommendationMemory.OrderedRecommendedItem(1, "TRACKS", "Can't We Be Friends?", trackMetadata("Ella and Louis")),
                        new ChatRecommendationMemory.OrderedRecommendedItem(2, "TRACKS", "They Can't Take That Away from Me", trackMetadata("Ella and Louis")),
                        new ChatRecommendationMemory.OrderedRecommendedItem(3, "TRACKS", "Moonlight in Vermont", trackMetadata("Ella and Louis")),
                        new ChatRecommendationMemory.OrderedRecommendedItem(4, "TRACKS", "Under a Blanket of Blue", trackMetadata("Ella and Louis")),
                        new ChatRecommendationMemory.OrderedRecommendedItem(5, "TRACKS", "Tenderly", trackMetadata("Ella and Louis")),
                        new ChatRecommendationMemory.OrderedRecommendedItem(6, "TRACKS", "The Nearness of You", trackMetadata("Ella and Louis"))
                ),
                null
        );
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                new ConversationRouterResponse(
                        ConversationRoute.MUSIC_RECOMMENDATION,
                        ConversationUserIntent.RECOMMEND_TRACK,
                        true,
                        true,
                        null,
                        null,
                        "Más temas de Ella and Louis para seguir con un clima vocal tranqui de duetos Ella Fitzgerald y Louis Armstrong, sin salir del mismo disco.",
                        null,
                        null,
                        List.of("Under a Blanket of Blue", "Tenderly", "The Nearness of You")
                ),
                List.of(routerUsage())
        ));
        Mockito.when(preferencesService.getPreferencesContext(Mockito.any())).thenReturn(
                new UserPreferencesContext(null, List.of(), List.of())
        );
        Mockito.when(retrievalService.retrieveRelevantDocuments(Mockito.any())).thenReturn(List.of());
        Mockito.when(promptBuilder.build(Mockito.any())).thenReturn(new Prompt("prompt"));
        Mockito.when(llmClientResolver.resolve(AIProvider.OPENAI)).thenReturn(llmClient);
        Mockito.when(llmClient.generate(Mockito.any())).thenReturn(new LLMResult(
                "No tengo un match sólido ahora mismo.",
                AIModelType.BASIC,
                "gpt-5.4-nano",
                20,
                0,
                15
        ));
        Mockito.when(llmResponseValidator.validate(Mockito.any(), Mockito.eq(BasicRecommendationFlow.BasicRecommendationResponse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(llmClient.generateStructured(Mockito.any())).thenReturn(
                new StructuredLLMResult<>(
                        new BasicRecommendationFlow.BasicRecommendationResponse(
                                "Aca van tres.",
                                BasicRecommendationTarget.TRACKS,
                                List.of("A Foggy Day", "April in Paris", "Stars Fell on Alabama"),
                                null
                        ),
                        AIModelType.BASIC,
                        "gpt-5.4-mini",
                        100,
                        0,
                        40
                )
        );

        flow.generate(baseCommand(recommendationMemory));

        var retrievalCommand = Mockito.mockingDetails(retrievalService)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("retrieveRelevantDocuments"))
                .map(invocation -> (RetrievalCommand) invocation.getArgument(0))
                .findFirst()
                .orElseThrow();

        assertThat(retrievalCommand.excludedWinners()).containsExactly(
                "Under a Blanket of Blue",
                "Tenderly",
                "The Nearness of You",
                "Can't We Be Friends?",
                "They Can't Take That Away from Me",
                "Moonlight in Vermont"
        );
    }

    private RecommendationFlowCommand baseCommand(ChatRecommendationMemory recommendationMemory) {
        return new RecommendationFlowCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "hoy ta para un album",
                "America/Argentina/Buenos_Aires",
                recommendationMemory,
                null,
                recommendationMemory == null ? null : recommendationMemory.sessionSummary(),
                List.of(),
                List.of(),
                AIModelType.BASIC,
                basicModel(),
                List.of(),
                null
        );
    }

    private ModelUsage routerUsage() {
        return new ModelUsage(
                UsageRecordStage.ROUTER,
                AIModelType.BASIC,
                "gpt-5.4-nano",
                10,
                0,
                20
        );
    }

    private ModelUsage basicUsage() {
        return new ModelUsage(
                UsageRecordStage.BASIC_RECOMMENDATION,
                AIModelType.BASIC,
                "gpt-5.4-mini",
                100,
                0,
                40
        );
    }

    private ChatRecommendationMemory.RecommendedItemMetadata trackMetadata(String album) {
        return new ChatRecommendationMemory.RecommendedItemMetadata(
                null,
                null,
                album,
                "track",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
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

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-06-03T03:35:00Z"), ZoneOffset.UTC);
    }

    private OpenAIRecommendationProperties openAIProperties() {
        return new OpenAIRecommendationProperties(
                null,
                "gpt-5.4-mini",
                "gpt-5.4-nano",
                null,
                2_000,
                300,
                false,
                null,
                null
        );
    }
}
