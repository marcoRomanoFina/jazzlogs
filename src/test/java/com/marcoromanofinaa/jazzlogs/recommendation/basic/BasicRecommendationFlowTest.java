package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesContext;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesService;
import com.marcoromanofinaa.jazzlogs.recommendation.retrieval.ReferenceResolutionService;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.prompt.Prompt;

class BasicRecommendationFlowTest {

    @Test
    void generateReturnsRouterDirectAnswerWithoutCallingRetrieval() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var referenceResolutionService = passThroughReferenceResolutionService();
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                referenceResolutionService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                new ObjectMapper(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                routerResponse(
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
    void generateFailsWhenModelReturnsNamesInsteadOfCandidateNodeIds() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var referenceResolutionService = passThroughReferenceResolutionService();
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var candidate = trackCandidate("Waltz for Debby", "My Foolish Heart");
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                referenceResolutionService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                new ObjectMapper(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                routerResponse(
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
        Mockito.when(retrievalService.retrieveCandidates(Mockito.any())).thenReturn(List.of(candidate));
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

        assertThatThrownBy(() -> flow.generate(baseCommand(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidate node ids");
    }

    @Test
    void generateFailsWhenTrackRecommendationReturnsMoreThanThreeWinners() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var referenceResolutionService = passThroughReferenceResolutionService();
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var candidates = List.of(
                trackCandidate("Kind Of Blue", "So What"),
                trackCandidate("Kind Of Blue", "Freddie Freeloader"),
                trackCandidate("Kind Of Blue", "Blue in Green"),
                trackCandidate("Kind Of Blue", "All Blues")
        );
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                referenceResolutionService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                new ObjectMapper(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                routerResponse(
                        ConversationRoute.MUSIC_RECOMMENDATION,
                        ConversationUserIntent.RECOMMEND_TRACK,
                        false,
                        true,
                        null,
                        null,
                        "pasame temas",
                        null,
                        null,
                        List.of()
                ),
                List.of(routerUsage())
        ));
        Mockito.when(preferencesService.getPreferencesContext(Mockito.any())).thenReturn(
                new UserPreferencesContext(null, List.of(), List.of())
        );
        Mockito.when(retrievalService.retrieveCandidates(Mockito.any())).thenReturn(candidates);
        Mockito.when(promptBuilder.build(Mockito.any())).thenReturn(new Prompt("prompt"));
        Mockito.when(llmClientResolver.resolve(AIProvider.OPENAI)).thenReturn(llmClient);
        Mockito.when(llmResponseValidator.validate(Mockito.any(), Mockito.eq(BasicRecommendationFlow.BasicRecommendationResponse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(llmClient.generateStructured(Mockito.any())).thenReturn(
                new StructuredLLMResult<>(
                        new BasicRecommendationFlow.BasicRecommendationResponse(
                                "Aca van cuatro.",
                                BasicRecommendationTarget.TRACKS,
                                List.of("track-node-1", "track-node-2", "track-node-3", "track-node-4"),
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
                .hasMessageContaining("more than 3 track winners");
    }


    @Test
    void generateUsesContextualizedQueryFromRouterForRetrieval() {
        var preferencesService = Mockito.mock(UserPreferencesService.class);
        var retrievalService = Mockito.mock(RetrievalService.class);
        var referenceResolutionService = passThroughReferenceResolutionService();
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                referenceResolutionService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                new ObjectMapper(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                routerResponse(
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
        Mockito.when(retrievalService.retrieveCandidates(Mockito.any())).thenReturn(List.of());
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
                .filter(invocation -> invocation.getMethod().getName().equals("retrieveCandidates"))
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
        var referenceResolutionService = passThroughReferenceResolutionService();
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                referenceResolutionService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                new ObjectMapper(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                routerResponse(
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
        Mockito.when(retrievalService.retrieveCandidates(Mockito.any())).thenReturn(List.of());
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
        var referenceResolutionService = passThroughReferenceResolutionService();
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var candidate = albumCandidate("Waltz for Debby");
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                referenceResolutionService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                new ObjectMapper(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                routerResponse(
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
        Mockito.when(retrievalService.retrieveCandidates(Mockito.any())).thenReturn(List.of(candidate));
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
        var referenceResolutionService = passThroughReferenceResolutionService();
        var promptBuilder = Mockito.mock(BasicPromptBuilder.class);
        var llmClientResolver = Mockito.mock(LLMClientResolver.class);
        var llmResponseValidator = Mockito.mock(LLMResponseValidator.class);
        var llmClient = Mockito.mock(LLMClient.class);
        var conversationRouter = Mockito.mock(ConversationRouter.class);
        var recommendationMemory = new ChatRecommendationMemory(
                new ChatRecommendationMemory.LastRecommendationBatch(
                        List.of(
                                winner("track-node-4", "Under a Blanket of Blue"),
                                winner("track-node-5", "Tenderly"),
                                winner("track-node-6", "The Nearness of You")
                        ),
                        List.of(
                                trackMetadata("track-node-4", "Ella and Louis"),
                                trackMetadata("track-node-5", "Ella and Louis"),
                                trackMetadata("track-node-6", "Ella and Louis")
                        )
                ),
                List.of(
                        new ChatRecommendationMemory.RecommendationHistoryEntry(1, winner("track-node-1", "Can't We Be Friends?"), "Ella Fitzgerald", "Ella and Louis", "Can't We Be Friends?"),
                        new ChatRecommendationMemory.RecommendationHistoryEntry(2, winner("track-node-2", "They Can't Take That Away from Me"), "Ella Fitzgerald", "Ella and Louis", "They Can't Take That Away from Me"),
                        new ChatRecommendationMemory.RecommendationHistoryEntry(3, winner("track-node-3", "Moonlight in Vermont"), "Ella Fitzgerald", "Ella and Louis", "Moonlight in Vermont"),
                        new ChatRecommendationMemory.RecommendationHistoryEntry(4, winner("track-node-4", "Under a Blanket of Blue"), "Ella Fitzgerald", "Ella and Louis", "Under a Blanket of Blue"),
                        new ChatRecommendationMemory.RecommendationHistoryEntry(5, winner("track-node-5", "Tenderly"), "Ella Fitzgerald", "Ella and Louis", "Tenderly"),
                        new ChatRecommendationMemory.RecommendationHistoryEntry(6, winner("track-node-6", "The Nearness of You"), "Ella Fitzgerald", "Ella and Louis", "The Nearness of You")
                ),
                null
        );
        var flow = new BasicRecommendationFlow(
                preferencesService,
                retrievalService,
                referenceResolutionService,
                promptBuilder,
                llmClientResolver,
                llmResponseValidator,
                conversationRouter,
                openAIProperties(),
                new ObjectMapper(),
                fixedClock()
        );

        Mockito.when(conversationRouter.route(any())).thenReturn(new ConversationRouterResult(
                routerResponse(
                        ConversationRoute.MUSIC_RECOMMENDATION,
                        ConversationUserIntent.RECOMMEND_TRACK,
                        true,
                        true,
                        null,
                        null,
                        "Más temas de Ella and Louis para seguir con un clima vocal tranqui de duetos Ella Fitzgerald y Louis Armstrong, sin salir del mismo disco.",
                        null,
                        null,
                        null
                ),
                List.of(routerUsage())
        ));
        Mockito.when(preferencesService.getPreferencesContext(Mockito.any())).thenReturn(
                new UserPreferencesContext(null, List.of(), List.of())
        );
        Mockito.when(retrievalService.retrieveCandidates(Mockito.any())).thenReturn(List.of());
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
                .filter(invocation -> invocation.getMethod().getName().equals("retrieveCandidates"))
                .map(invocation -> (RetrievalCommand) invocation.getArgument(0))
                .findFirst()
                .orElseThrow();

        assertThat(retrievalCommand.excludedNodeIds()).containsExactlyInAnyOrder(
                "track-node-1",
                "track-node-2",
                "track-node-3",
                "track-node-4",
                "track-node-5",
                "track-node-6"
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
                AIModelType.BASIC,
                basicModel(),
                List.of(),
                null
        );
    }

    private ReferenceResolutionService passThroughReferenceResolutionService() {
        var service = Mockito.mock(ReferenceResolutionService.class);
        Mockito.when(service.resolve(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        return service;
    }

    private ConversationRouterResponse routerResponse(
            ConversationRoute route,
            ConversationUserIntent userIntent,
            boolean isFollowUp,
            boolean needsRetrieval,
            String updatedSessionSummary,
            String suggestedChatTitle,
            String contextualizedQuery,
            String directAnswer,
            String clarificationQuestion,
            List<String> excludedWinners
    ) {
        return new ConversationRouterResponse(
                route,
                userIntent,
                isFollowUp,
                needsRetrieval,
                updatedSessionSummary,
                suggestedChatTitle,
                contextualizedQuery,
                directAnswer,
                clarificationQuestion,
                excludedWinners,
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



    private com.marcoromanofinaa.jazzlogs.chat.session.ResolvedRecommendationMemoryItem trackMetadata(String nodeId, String album) {
        return new com.marcoromanofinaa.jazzlogs.chat.session.ResolvedRecommendationMemoryItem(
                album,
                "track",
                "Ella Fitzgerald",
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null
        );
    }

    private WinnerReference winner(String nodeId, String name) {
        return new WinnerReference(
                BasicRecommendationTarget.TRACKS,
                nodeId,
                name,
                "Ella Fitzgerald y Louis Armstrong"
        );
    }

    private RecommendationCandidate albumCandidate(String album) {
        return new RecommendationCandidate(
                "album-node-1",
                BasicRecommendationTarget.ALBUM,
                1,
                "album-1",
                null,
                album,
                album,
                null,
                "Bill Evans",
                List.of(),
                "A",
                "Cool Jazz",
                "Instrumental",
                List.of("Nocturnal"),
                "Medium",
                "High",
                null,
                "Piano",
                List.of("Night"),
                null,
                null,
                null,
                "caption",
                "note",
                "editorial"
        );
    }

    private RecommendationCandidate trackCandidate(String album, String track) {
        return new RecommendationCandidate(
                "track-node-1",
                BasicRecommendationTarget.TRACKS,
                1,
                "album-1",
                "track-1",
                track,
                album,
                track,
                "Bill Evans",
                List.of(),
                "A",
                "Cool Jazz",
                "Instrumental",
                List.of("Nocturnal"),
                "Medium",
                "High",
                null,
                "Piano",
                List.of("Night"),
                null,
                null,
                null,
                "caption",
                "note",
                "editorial"
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
                null,
                null,
                null
        );
    }
}
