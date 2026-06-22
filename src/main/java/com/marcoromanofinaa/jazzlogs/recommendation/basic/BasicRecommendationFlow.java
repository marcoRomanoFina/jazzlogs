package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMClientResolver;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMResponseValidator;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMResult;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.StructuredLLMCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.StructuredLLMResult;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationFlow;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationFlowCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationTiming;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesContext;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesService;
import com.marcoromanofinaa.jazzlogs.recommendation.retrieval.ReferenceResolutionService;
import com.marcoromanofinaa.jazzlogs.recommendation.retrieval.RetrievalCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.retrieval.RetrievalService;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.ConversationRouter;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRoute;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterResponse;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphReferenceType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationUserIntent;
import com.marcoromanofinaa.jazzlogs.recommendation.config.OpenAIRecommendationProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BasicRecommendationFlow implements RecommendationFlow {

    private static final int BASIC_ALBUM_TOP_K = 8;
    private static final int BASIC_TRACKS_TOP_K = 12;
    private static final int BASIC_MAX_TRACK_WINNERS = 3;
    private static final int NO_CANDIDATES_HISTORY_LIMIT = 3;
    private static final DateTimeFormatter NO_CANDIDATES_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private final UserPreferencesService userPreferencesService;
    private final RetrievalService retrievalService;
    private final ReferenceResolutionService referenceResolutionService;
    private final BasicPromptBuilder basicPromptBuilder;
    private final LLMClientResolver llmClientResolver;
    private final LLMResponseValidator llmResponseValidator;
    private final ConversationRouter conversationRouter;
    private final OpenAIRecommendationProperties openAIRecommendationProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public RecommendationFlowType flowType() {
        return RecommendationFlowType.BASIC;
    }

    @Override
    public RecommendationResult generate(RecommendationFlowCommand command) {
        var totalStartedAt = System.nanoTime();
        var routerStartedAt = System.nanoTime();
        var routingResult = conversationRouter.route(new ConversationRouterCommand(
                command.userId(),
                command.chatSessionId(),
                command.userMessage(),
                command.timeZone(),
                command.requestedModel(),
                command.modelDefinition(),
                command.recommendationMemory(),
                command.recentHistory()
        ));
        var routerLatencyMs = elapsedMillis(routerStartedAt);
        var route = routingResult.response().route();

        if (routingResult.response().userIntent() == ConversationUserIntent.FACTUAL_QUESTION) {
            return new RecommendationResult(
                    factualQuestionUnsupportedResponse(),
                    List.of(),
                    null,
                    routingResult.response().suggestedChatTitle(),
                    routingResult.response().updatedSessionSummary(),
                    new RecommendationTiming(routerLatencyMs, 0L, elapsedMillis(totalStartedAt)),
                    routingResult.usageEntries()
            );
        }

        if (route == ConversationRoute.DIRECT_ANSWER) {
            return new RecommendationResult(
                    routingResult.response().directAnswer(),
                    List.of(),
                    null,
                    routingResult.response().suggestedChatTitle(),
                    routingResult.response().updatedSessionSummary(),
                    new RecommendationTiming(routerLatencyMs, 0L, elapsedMillis(totalStartedAt)),
                    routingResult.usageEntries()
            );
        }

        if (route == ConversationRoute.CLARIFICATION_NEEDED) {
            return new RecommendationResult(
                    routingResult.response().clarificationQuestion(),
                    List.of(),
                    null,
                    routingResult.response().suggestedChatTitle(),
                    routingResult.response().updatedSessionSummary(),
                    new RecommendationTiming(routerLatencyMs, 0L, elapsedMillis(totalStartedAt)),
                    routingResult.usageEntries()
            );
        }

        return generateRecommendation(command, routingResult.response(), routingResult.usageEntries(), totalStartedAt, routerLatencyMs);
    }

    private RecommendationResult generateRecommendation(
            RecommendationFlowCommand command,
            ConversationRouterResponse routerResponse,
            List<ModelUsage> routerUsageEntries,
            long totalStartedAt,
            long routerLatencyMs
    ) {
        var target = toTarget(routerResponse);
        var retrievalQuery = routerResponse.contextualizedQuery().trim();
        var resolvedSubgraphFilters = referenceResolutionService.resolve(routerResponse.subgraphFilters());
        var topK = target == BasicRecommendationTarget.ALBUM ? BASIC_ALBUM_TOP_K : BASIC_TRACKS_TOP_K;

        var effectiveExcludedNodeIds = excludedWinnerNodeIds(
                target,
                resolvedSubgraphFilters,
                routerResponse.contextualizedQuery(),
                routerResponse.excludedNodeIds(),
                command.recommendationMemory()
        );

        var candidates = retrievalService.retrieveCandidates(
                new RetrievalCommand(
                        retrievalQuery,
                        target,
                        topK,
                        effectiveExcludedNodeIds,
                        resolvedSubgraphFilters
                )
        );

        logCandidateSnapshot(retrievalQuery, target, routerResponse, effectiveExcludedNodeIds, candidates);

        if (candidates.isEmpty()) {
            var userPreferencesContext = userPreferencesService.getPreferencesContext(command.userId());
            var noCandidatesResult = generateNoCandidatesResponse(
                    command,
                    routerResponse,
                    target,
                    userPreferencesContext
            );
            var usageEntries = new ArrayList<>(routerUsageEntries);
            usageEntries.add(noCandidatesUsage(noCandidatesResult));
            return new RecommendationResult(
                    noCandidatesResult.content(),
                    List.of(),
                    null,
                    routerResponse.suggestedChatTitle(),
                    routerResponse.updatedSessionSummary(),
                    new RecommendationTiming(routerLatencyMs, 0L, elapsedMillis(totalStartedAt)),
                    usageEntries
            );
        }

        var userPreferencesContext = userPreferencesService.getPreferencesContext(command.userId());

        var prompt = basicPromptBuilder.build(
                new BasicPromptCommand(
                        command.userMessage(),
                        target,
                        command.recentHistory(),
                        command.recommendationMemory() == null ? null : command.recommendationMemory().sessionSummary(),
                        java.time.ZonedDateTime.now(clock).withZoneSameInstant(resolveZoneId(command.timeZone())),
                        userPreferencesContext,
                        candidates
                )
        );

        var llmClient = llmClientResolver.resolve(command.modelDefinition().provider());
        var flowStartedAt = System.nanoTime();
        var result = llmClient.generateStructured(
                new StructuredLLMCommand<>(prompt, command.modelDefinition(), BasicRecommendationResponse.class)
        );
        var flowLatencyMs = elapsedMillis(flowStartedAt);
        var content = llmResponseValidator.validate(result.content(), BasicRecommendationResponse.class);
        validateRecommendationType(target, content.recommendationType());

        var winnerIds = sanitizeWinnerIds(content.winners());
        if (!winnerIds.isEmpty()) {
            validateWinners(target, winnerIds, candidates);
        }
        var winners = toWinnerReferences(target, winnerIds, candidates);

        logRecommendationSnapshot(retrievalQuery, target, candidates, content, winnerIds, result);

        var usageEntries = new ArrayList<>(routerUsageEntries);
        usageEntries.add(basicUsage(result));

        return new RecommendationResult(
                content.assistantResponse(),
                winners,
                content.recommendationType(),
                preferredChatTitle(content.suggestedChatTitle(), routerResponse.suggestedChatTitle()),
                routerResponse.updatedSessionSummary(),
                new RecommendationTiming(routerLatencyMs, flowLatencyMs, elapsedMillis(totalStartedAt)),
                usageEntries
        );
    }

    public record BasicRecommendationResponse(
            @NotBlank String assistantResponse,
            @jakarta.validation.constraints.NotNull BasicRecommendationTarget recommendationType,
            @Size(max = BASIC_MAX_TRACK_WINNERS)
            List<@Size(min = 1) String> winners,
            String suggestedChatTitle
    ) {
    }

    private ModelUsage basicUsage(StructuredLLMResult<BasicRecommendationResponse> result) {
        return new ModelUsage(
                UsageRecordStage.BASIC_RECOMMENDATION,
                result.modelUsed(),
                result.providerModelName(),
                result.inputTokens(),
                result.cachedInputTokens(),
                result.outputTokens()
        );
    }

    private String factualQuestionUnsupportedResponse() {
        return """
                En basic me quedo en recomendacion pura: te puedo recomendar albumes o temas, pero no meterme en contexto, historia o analisis de una obra.
                Si queres, te sigo por ese lado y te saco algo mas de ese disco, de ese artista, o en esa misma linea.
                """;
    }

    private void validateWinners(
            BasicRecommendationTarget target,
            List<String> winnerIds,
            List<RecommendationCandidate> candidates
    ) {
        var invalidWinners = invalidWinnerIds(target, winnerIds, candidates);
        if (!invalidWinners.isEmpty()) {
            throw new IllegalArgumentException(
                    "Basic recommendation flow returned winners outside candidate node ids: " + invalidWinners
            );
        }
    }

    private List<String> invalidWinnerIds(
            BasicRecommendationTarget target,
            List<String> winnerIds,
            List<RecommendationCandidate> candidates
    ) {
        if (target == BasicRecommendationTarget.TRACKS && winnerIds.size() > BASIC_MAX_TRACK_WINNERS) {
            throw new IllegalArgumentException(
                    "Basic recommendation flow returned more than %s track winners".formatted(BASIC_MAX_TRACK_WINNERS)
            );
        }

        var allowedNodeIds = candidates.stream()
                .map(RecommendationCandidate::nodeId)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        var invalidWinners = winnerIds.stream()
                .filter(winner -> !allowedNodeIds.contains(winner))
                .toList();
        return invalidWinners;
    }

    private List<String> sanitizeWinnerIds(List<String> winners) {
        return winners == null ? List.of() : winners.stream()
                .filter(winner -> winner != null && !winner.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<WinnerReference> toWinnerReferences(
            BasicRecommendationTarget target,
            List<String> winnerIds,
            List<RecommendationCandidate> candidates
    ) {
        if (winnerIds.isEmpty()) {
            return List.of();
        }

        var candidatesByNodeId = candidates.stream()
                .filter(candidate -> candidate.nodeId() != null && !candidate.nodeId().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        RecommendationCandidate::nodeId,
                        candidate -> candidate,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return winnerIds.stream()
                .map(candidatesByNodeId::get)
                .filter(candidate -> candidate != null)
                .map(candidate -> new WinnerReference(
                        target,
                        candidate.nodeId(),
                        candidate.title(),
                        renderArtistFullName(candidate)
                ))
                .toList();
    }

    private BasicRecommendationTarget toTarget(ConversationRouterResponse response) {
        return response.userIntent() == ConversationUserIntent.RECOMMEND_TRACK
                ? BasicRecommendationTarget.TRACKS
                : BasicRecommendationTarget.ALBUM;
    }

    private void validateRecommendationType(
            BasicRecommendationTarget target,
            BasicRecommendationTarget returnedType
    ) {
        if (returnedType != target) {
            throw new IllegalArgumentException(
                    "Basic recommendation flow returned recommendationType %s for target %s"
                            .formatted(returnedType, target)
            );
        }
    }

    private List<String> excludedWinnerNodeIds(
            BasicRecommendationTarget target,
            com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters resolvedSubgraphFilters,
            String contextualizedQuery,
            List<String> explicitExcludedNodeIds,
            ChatRecommendationMemory recommendationMemory
    ) {
        var excluded = new LinkedHashSet<String>();
        if (explicitExcludedNodeIds != null) {
            explicitExcludedNodeIds.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(excluded::add);
        }

        if (target == BasicRecommendationTarget.TRACKS) {
            excluded.addAll(previouslyRecommendedTracksFromReferencedAlbum(contextualizedQuery, recommendationMemory));
        }

        excluded.addAll(previouslyRecommendedWinnersFromReferencedArtists(resolvedSubgraphFilters, recommendationMemory));
        excluded.addAll(lastRecommendedWinnerIdsForSession(recommendationMemory));

        return List.copyOf(excluded);
    }

    private List<String> lastRecommendedWinnerIdsForSession(ChatRecommendationMemory recommendationMemory) {
        if (recommendationMemory == null
                || recommendationMemory.lastRecommendationBatch() == null
                || recommendationMemory.lastRecommendationBatch().winners() == null
                || recommendationMemory.lastRecommendationBatch().winners().isEmpty()) {
            return List.of();
        }

        return recommendationMemory.lastRecommendationBatch().winners().stream()
                .filter(winner -> winner != null && winner.id() != null && !winner.id().isBlank())
                .map(WinnerReference::id)
                .toList();
    }

    private List<String> previouslyRecommendedWinnersFromReferencedArtists(
            com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters resolvedSubgraphFilters,
            ChatRecommendationMemory recommendationMemory
    ) {
        if (recommendationMemory == null
                || recommendationMemory.recommendationHistory() == null
                || recommendationMemory.recommendationHistory().isEmpty()
                || resolvedSubgraphFilters == null
                || resolvedSubgraphFilters.references() == null
                || resolvedSubgraphFilters.references().isEmpty()) {
            return List.of();
        }

        var referencedArtists = resolvedSubgraphFilters.references().stream()
                .filter(reference -> reference != null && reference.type() == ConversationSubgraphReferenceType.ARTIST)
                .map(reference -> normalize(reference.name()))
                .filter(name -> !name.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        if (referencedArtists.isEmpty()) {
            return List.of();
        }

        var excluded = new ArrayList<String>();
        for (var item : recommendationMemory.recommendationHistory()) {
            if (item == null || item.winner() == null || item.winner().id() == null || item.winner().id().isBlank()) {
                continue;
            }
            if (matchesReferencedArtist(item, referencedArtists)) {
                excluded.add(item.winner().id());
            }
        }
        return excluded;
    }

    private List<String> previouslyRecommendedTracksFromReferencedAlbum(
            String contextualizedQuery,
            ChatRecommendationMemory recommendationMemory
    ) {
        if (recommendationMemory == null
                || recommendationMemory.recommendationHistory() == null
                || recommendationMemory.recommendationHistory().isEmpty()
                || contextualizedQuery == null
                || contextualizedQuery.isBlank()) {
            return List.of();
        }

        var referencedAlbum = referencedAlbumFromMemory(contextualizedQuery, recommendationMemory);
        if (referencedAlbum.isEmpty()) {
            return List.of();
        }

        var normalizedAlbum = normalize(referencedAlbum.orElseThrow());
        var excluded = new ArrayList<String>();
        for (var item : recommendationMemory.recommendationHistory()) {
            if (item == null || item.winner() == null || item.winner().id() == null || item.winner().id().isBlank()) {
                continue;
            }
            var itemAlbum = item.album();
            var itemTrack = item.track();
            if (itemTrack == null || itemTrack.isBlank() || itemAlbum == null || itemAlbum.isBlank()) {
                continue;
            }
            if (normalize(itemAlbum).equals(normalizedAlbum)) {
                excluded.add(item.winner().id());
            }
        }
        return excluded;
    }

    private Optional<String> referencedAlbumFromMemory(
            String contextualizedQuery,
            ChatRecommendationMemory recommendationMemory
    ) {
        var normalizedQuery = normalize(contextualizedQuery);

        for (var orderedItem : recommendationMemory.recommendationHistory()) {
            if (orderedItem == null || orderedItem.album() == null) {
                continue;
            }
            var album = orderedItem.album();
            if (!album.isBlank() && normalizedQuery.contains(normalize(album))) {
                return Optional.of(album);
            }
        }

        return Optional.empty();
    }

    private boolean matchesReferencedArtist(
            ChatRecommendationMemory.RecommendationHistoryEntry item,
            Set<String> referencedArtists
    ) {
        if (item == null) {
            return false;
        }
        if (referencedArtists.contains(normalize(item.primaryArtist()))) {
            return true;
        }
        if (item.winner() != null && referencedArtists.contains(normalize(item.winner().artistFullName()))) {
            return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String preferredChatTitle(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private LLMResult generateNoCandidatesResponse(
            RecommendationFlowCommand command,
            ConversationRouterResponse routerResponse,
            BasicRecommendationTarget target,
            UserPreferencesContext userPreferencesContext
    ) {
        var llmClient = llmClientResolver.resolve(command.modelDefinition().provider());
        return llmClient.generate(new LLMCommand(
                noCandidatesPrompt(command, routerResponse, target, userPreferencesContext),
                noCandidatesModelDefinition(command.modelDefinition())
        ));
    }

    private Prompt noCandidatesPrompt(
            RecommendationFlowCommand command,
            ConversationRouterResponse routerResponse,
            BasicRecommendationTarget target,
            UserPreferencesContext userPreferencesContext
    ) {
        var currentLocalTime = java.time.ZonedDateTime.now(clock).withZoneSameInstant(resolveZoneId(command.timeZone()));
        var promptText = """
                You are the curator for JazzLogs, an AI assistant dedicated EXCLUSIVELY to jazz music.

                SCENARIO:
                The user requested music, and the system attempted to search the database using the failed search query below.
                However, there is no solid continuation for that exact line of listening right now.

                YOUR TASK:
                Generate one short fallback response in natural Rioplatense Spanish.

                CONTEXT:
                - Recommendation target: %s
                - Current local time for the user: %s
                - Original user message: %s
                - Failed search query: %s
                - Recent conversation: %s
                - Session summary: %s
                - User context: %s

                RULES:
                1. You MUST sound natural, colloquial, and warm in Rioplatense Spanish.
                2. Acknowledge what the user specifically asked for, so it feels clear that you understood the request.
                3. NEVER mention catalog limitations, failed search, zero results, system constraints, or that you "couldn't find" something.
                4. Frame it naturally as: for that exact line, thread, or mood, no more solid picks remain right now without forcing it.
                5. DO NOT hallucinate, recommend, or name any specific artist, album, or track.
                6. End by pivoting gently: ask whether they want you to look for something parecido, in the same onda, mood, era, or style instead.
                7. Keep it concise: 2 or 3 short sentences maximum.
                8. Return ONLY the assistant message text, with no JSON and no extra formatting.
                """.formatted(
                target == BasicRecommendationTarget.TRACKS ? "tracks or songs" : "album or record",
                currentLocalTime.format(NO_CANDIDATES_TIME_FORMATTER),
                command.userMessage(),
                blankIfNull(routerResponse.contextualizedQuery()),
                summarizeRecentHistory(command.recentHistory()),
                blankIfNull(command.sessionSummary()),
                summarizeUserPreferences(userPreferencesContext)
        );
        return new Prompt(promptText);
    }

    private AIModelDefinition noCandidatesModelDefinition(AIModelDefinition requestedModelDefinition) {
        var providerModelName = openAIRecommendationProperties.routerModel() == null
                || openAIRecommendationProperties.routerModel().isBlank()
                ? requestedModelDefinition.providerModelName()
                : openAIRecommendationProperties.routerModel().trim();
        var maxTokens = openAIRecommendationProperties.routerMaxCompletionTokens() == null
                ? openAIRecommendationProperties.maxCompletionTokens()
                : openAIRecommendationProperties.routerMaxCompletionTokens();
        var outputTokenLimit = maxTokens == null
                ? requestedModelDefinition.outputTokenLimit()
                : Math.min(requestedModelDefinition.outputTokenLimit(), maxTokens);
        return new AIModelDefinition(
                requestedModelDefinition.type(),
                requestedModelDefinition.provider(),
                providerModelName,
                requestedModelDefinition.enabled(),
                requestedModelDefinition.allowedPlans(),
                requestedModelDefinition.inputTokenLimit(),
                outputTokenLimit,
                RecommendationFlowType.BASIC
        );
    }

    private ModelUsage noCandidatesUsage(LLMResult result) {
        return new ModelUsage(
                UsageRecordStage.EMPTY_FALLBACK,
                result.modelUsed(),
                result.providerModelName(),
                result.inputTokens(),
                result.cachedInputTokens(),
                result.outputTokens()
        );
    }

    private String summarizeRecentHistory(List<ChatExchange> recentHistory) {
        if (recentHistory == null || recentHistory.isEmpty()) {
            return "NONE";
        }
        return recentHistory.stream()
                .skip(Math.max(0, recentHistory.size() - NO_CANDIDATES_HISTORY_LIMIT))
                .map(this::summarizeExchange)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("NONE");
    }

    private String summarizeExchange(ChatExchange exchange) {
        var userMessage = blankIfNull(exchange.getUserMessage());
        var assistantResponse = blankIfNull(exchange.getAssistantResponse());
        return "User: %s | Assistant: %s".formatted(userMessage, assistantResponse);
    }

    private void logCandidateSnapshot(
            String retrievalQuery,
            BasicRecommendationTarget target,
            ConversationRouterResponse routerResponse,
            List<String> effectiveExcludedNodeIds,
            List<RecommendationCandidate> candidates
    ) {
        if (!log.isInfoEnabled()) {
            return;
        }

        var lines = new ArrayList<String>();
        lines.add("");
        lines.add("=== Basic Flow Candidates ===");
        lines.add("target: " + target);
        lines.add("query: " + compact(retrievalQuery));
        appendIfPresent(lines, "routerExcludedNodeIds: " + compactJson(routerResponse.excludedNodeIds()));
        appendIfPresent(lines, "effectiveExcludedNodeIds: " + compactJson(effectiveExcludedNodeIds));
        appendIfPresent(lines, "subgraphFilters: " + compactJson(routerResponse.subgraphFilters()));
        lines.add("candidateCount: " + candidates.size());
        if (candidates.isEmpty()) {
            lines.add("candidates: []");
        } else {
            for (int index = 0; index < candidates.size(); index++) {
                lines.add("  " + formatCandidate(index + 1, candidates.get(index)));
            }
        }
        lines.add("=============================");
        log.info(String.join("\n", lines));
    }

    private void logRecommendationSnapshot(
            String retrievalQuery,
            BasicRecommendationTarget target,
            List<RecommendationCandidate> candidates,
            BasicRecommendationResponse content,
            List<String> winnerIds,
            StructuredLLMResult<BasicRecommendationResponse> result
    ) {
        if (!log.isInfoEnabled()) {
            return;
        }

        var lines = new ArrayList<String>();
        lines.add("");
        lines.add("=== Basic Flow Result ===");
        lines.add("model: " + result.providerModelName());
        lines.add("target: " + target);
        lines.add("query: " + compact(retrievalQuery));
        lines.add("candidateCount: " + candidates.size());
        lines.add("returnedType: " + content.recommendationType());
        lines.add("winnerIds: " + compactJson(winnerIds));
        appendIfPresent(lines, "suggestedChatTitle: " + compact(content.suggestedChatTitle()));
        appendIfPresent(lines, "assistantResponse: " + compact(content.assistantResponse()));
        lines.add("usageTokens: in=%d cached=%d out=%d".formatted(
                result.inputTokens(),
                result.cachedInputTokens(),
                result.outputTokens()
        ));
        if (Boolean.TRUE.equals(openAIRecommendationProperties.rawResponseLoggingEnabled())) {
            lines.add("rawStructuredContent: " + compactJson(content));
        }
        lines.add("=========================");
        log.info(String.join("\n", lines));
    }

    private String summarizeUserPreferences(UserPreferencesContext context) {
        if (context == null) {
            return "NONE";
        }
        var parts = new ArrayList<String>();
        if (context.jazzPreferences() != null) {
            var prefs = context.jazzPreferences();
            if (!prefs.preferredMoodLabels().isEmpty()) {
                parts.add("Preferred moods: " + renderValues(prefs.preferredMoodLabels()));
            }
            if (!prefs.preferredSubgenreLabels().isEmpty()) {
                parts.add("Preferred subgenres: " + renderValues(prefs.preferredSubgenreLabels()));
            }
            if (!prefs.favoriteArtistLabels().isEmpty()) {
                parts.add("Favorite artists: " + renderValues(prefs.favoriteArtistLabels()));
            }
            parts.add("Likes vocals: " + (prefs.likesVocals() ? "yes" : "no"));
        }
        if (context.topArtists() != null && !context.topArtists().isEmpty()) {
            parts.add("Top Spotify artists: " + context.topArtists().stream()
                    .map(artist -> artist.name())
                    .filter(name -> name != null && !name.isBlank())
                    .limit(5)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(""));
        }
        if (context.topTracks() != null && !context.topTracks().isEmpty()) {
            parts.add("Top Spotify tracks: " + context.topTracks().stream()
                    .map(track -> track.name())
                    .filter(name -> name != null && !name.isBlank())
                    .limit(5)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(""));
        }
        return parts.isEmpty() ? "NONE" : String.join("\n", parts);
    }

    private String blankIfNull(String value) {
        return value == null || value.isBlank() ? "NONE" : value.trim();
    }

    private String renderValues(Collection<?> values) {
        return values.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String formatCandidate(int rank, RecommendationCandidate candidate) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("title", candidate.title());
        payload.put("nodeId", candidate.nodeId());
        payload.put("album", candidate.album());
        payload.put("track", candidate.track());
        payload.put("primaryArtist", candidate.primaryArtist());
        payload.put("logNumber", candidate.logNumber());
        payload.put("tier", candidate.tier());
        payload.put("style", candidate.style());
        payload.put("energy", candidate.energy());
        payload.put("instrumentFocus", candidate.instrumentFocus());
        payload.put("moods", candidate.moods() == null ? List.of() : candidate.moods());
        return "#%d %s".formatted(rank, compactJson(payload));
    }

    private String renderArtistFullName(RecommendationCandidate candidate) {
        if (candidate.primaryArtist() == null || candidate.primaryArtist().isBlank()) {
            return "Unknown Artist";
        }
        if (candidate.secondaryArtists() == null || candidate.secondaryArtists().isEmpty()) {
            return candidate.primaryArtist();
        }
        return candidate.primaryArtist() + " feat. " + String.join(", ", candidate.secondaryArtists());
    }

    private String compactJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private void appendIfPresent(List<String> lines, String line) {
        if (line == null || line.isBlank() || line.endsWith("null")) {
            return;
        }
        lines.add(line);
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var inline = value.replaceAll("\\s+", " ").trim();
        return inline.length() > 500 ? inline.substring(0, 500) + "...[truncated]" : inline;
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private ZoneId resolveZoneId(String timeZone) {
        try {
            return timeZone == null || timeZone.isBlank()
                    ? ZoneId.of("America/Argentina/Buenos_Aires")
                    : ZoneId.of(timeZone.trim());
        } catch (DateTimeException exception) {
            return ZoneId.of("America/Argentina/Buenos_Aires");
        }
    }
}
