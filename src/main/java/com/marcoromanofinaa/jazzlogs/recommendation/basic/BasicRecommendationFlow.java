package com.marcoromanofinaa.jazzlogs.recommendation.basic;

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
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesContext;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesService;
import com.marcoromanofinaa.jazzlogs.recommendation.retrieval.RetrievalCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.retrieval.RetrievalService;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.ConversationRouter;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRoute;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterResponse;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BasicRecommendationFlow implements RecommendationFlow {

    private static final int BASIC_ALBUM_TOP_K = 8;
    private static final int BASIC_TRACKS_TOP_K = 12;
    private static final int NO_CANDIDATES_HISTORY_LIMIT = 3;
    private static final DateTimeFormatter NO_CANDIDATES_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private final UserPreferencesService userPreferencesService;
    private final RetrievalService retrievalService;
    private final BasicPromptBuilder basicPromptBuilder;
    private final LLMClientResolver llmClientResolver;
    private final LLMResponseValidator llmResponseValidator;
    private final ConversationRouter conversationRouter;
    private final OpenAIRecommendationProperties openAIRecommendationProperties;
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
        var topK = target == BasicRecommendationTarget.ALBUM ? BASIC_ALBUM_TOP_K : BASIC_TRACKS_TOP_K;

        var candidateDocuments = retrievalService.retrieveRelevantDocuments(
                new RetrievalCommand(
                        retrievalQuery,
                        target,
                        topK,
                        List.of(),
                        excludedWinners(routerResponse, command.recommendationMemory())
                )
        );
        var candidates = toCandidates(target, candidateDocuments);

        if (log.isDebugEnabled()) {
            log.debug("Basic flow candidates for query '{}' (target: {}):", retrievalQuery, target);
            if (candidates.isEmpty()) {
                log.debug("  No candidates found.");
            } else {
                for (int i = 0; i < candidates.size(); i++) {
                    log.debug("  Candidate {}: {}", i + 1, candidates.get(i).title());
                }
            }
        }

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

        var winners = content.winners() == null ? List.<String>of() : content.winners().stream()
                .filter(winner -> winner != null && !winner.isBlank())
                .map(String::trim)
                .toList();
        winners = normalizeWinners(target, winners, candidates);

        if (!winners.isEmpty()) {
            validateWinners(target, winners, candidates);
        }

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

    private List<String> normalizeWinners(
            BasicRecommendationTarget target,
            List<String> winners,
            List<RecommendationCandidate> candidates
    ) {
        if (target != BasicRecommendationTarget.ALBUM || winners.isEmpty()) {
            return winners;
        }

        var albumTitles = candidates.stream()
                .map(RecommendationCandidate::album)
                .map(this::normalizeKey)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        var trackToAlbum = new LinkedHashMap<String, String>();
        for (var candidate : candidates) {
            var track = candidate.track();
            var album = candidate.album();
            if (track == null || album == null) {
                continue;
            }
            trackToAlbum.putIfAbsent(normalizeKey(track), album);
        }

        return winners.stream()
                .map(String::trim)
                .map(winner -> normalizeAlbumWinner(winner, albumTitles, trackToAlbum))
                .toList();
    }

    private String normalizeAlbumWinner(String winner, Set<String> albumTitles, Map<String, String> trackToAlbum) {
        var normalizedWinner = normalizeKey(winner);
        if (albumTitles.contains(normalizedWinner)) {
            return winner;
        }
        return trackToAlbum.getOrDefault(normalizedWinner, winner);
    }

    private void validateWinners(
            BasicRecommendationTarget target,
            List<String> winners,
            List<RecommendationCandidate> candidates
    ) {
        if (target == BasicRecommendationTarget.TRACKS) {
            return;
        }

        var allowedAlbums = candidates.stream()
                .map(RecommendationCandidate::album)
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalizeKey)
                .collect(java.util.stream.Collectors.toSet());

        var invalidWinners = winners.stream()
                .filter(winner -> !allowedAlbums.contains(normalizeKey(winner)))
                .toList();

        if (!invalidWinners.isEmpty()) {
            throw new IllegalArgumentException(
                    "Basic recommendation flow returned non-album winners for album target: " + invalidWinners
            );
        }
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

    private List<String> excludedWinners(
            ConversationRouterResponse response,
            ChatRecommendationMemory recommendationMemory
    ) {
        var excluded = new LinkedHashSet<String>();
        if (response.excludedWinners() != null) {
            response.excludedWinners().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(excluded::add);
        }

        if (response.userIntent() == ConversationUserIntent.RECOMMEND_TRACK) {
            excluded.addAll(previouslyRecommendedTracksFromReferencedAlbum(response, recommendationMemory));
        }

        return List.copyOf(excluded);
    }

    private List<String> previouslyRecommendedTracksFromReferencedAlbum(
            ConversationRouterResponse response,
            ChatRecommendationMemory recommendationMemory
    ) {
        if (recommendationMemory == null
                || recommendationMemory.orderedRecommendedItems() == null
                || recommendationMemory.orderedRecommendedItems().isEmpty()
                || response.contextualizedQuery() == null
                || response.contextualizedQuery().isBlank()) {
            return List.of();
        }

        var referencedAlbum = referencedAlbumFromMemory(response.contextualizedQuery(), recommendationMemory);
        if (referencedAlbum.isEmpty()) {
            return List.of();
        }

        var normalizedAlbum = normalize(referencedAlbum.orElseThrow());
        var excluded = new ArrayList<String>();
        for (var item : recommendationMemory.orderedRecommendedItems()) {
            if (item == null || item.item() == null || item.winnerName() == null || item.winnerName().isBlank()) {
                continue;
            }
            var itemAlbum = item.item().album();
            var itemTrack = item.item().track();
            if (itemTrack == null || itemTrack.isBlank() || itemAlbum == null || itemAlbum.isBlank()) {
                continue;
            }
            if (normalize(itemAlbum).equals(normalizedAlbum)) {
                excluded.add(item.winnerName());
            }
        }
        return excluded;
    }

    private Optional<String> referencedAlbumFromMemory(
            String contextualizedQuery,
            ChatRecommendationMemory recommendationMemory
    ) {
        var normalizedQuery = normalize(contextualizedQuery);

        if (recommendationMemory.lastRecommendedItem() != null && recommendationMemory.lastRecommendedItem().items() != null) {
            for (var item : recommendationMemory.lastRecommendedItem().items()) {
                if (item == null || item.album() == null || item.album().isBlank()) {
                    continue;
                }
                if (normalizedQuery.contains(normalize(item.album()))) {
                    return Optional.of(item.album());
                }
            }
        }

        for (var orderedItem : recommendationMemory.orderedRecommendedItems()) {
            if (orderedItem == null || orderedItem.item() == null || orderedItem.item().album() == null) {
                continue;
            }
            var album = orderedItem.item().album();
            if (!album.isBlank() && normalizedQuery.contains(normalize(album))) {
                return Optional.of(album);
            }
        }

        return Optional.empty();
    }

    private List<RecommendationCandidate> toCandidates(
            BasicRecommendationTarget target,
            List<Document> candidateDocuments
    ) {
        if (candidateDocuments == null || candidateDocuments.isEmpty()) {
            return List.of();
        }

        return candidateDocuments.stream()
                .map(document -> toCandidate(target, document))
                .toList();
    }

    private RecommendationCandidate toCandidate(BasicRecommendationTarget target, Document document) {
        var metadata = document.getMetadata();
        return new RecommendationCandidate(
                target,
                stringValue(metadata == null ? null : metadata.get("sourceType")),
                stringValue(metadata == null ? null : metadata.get("sourceId")),
                candidateTitle(target, metadata),
                stringValue(metadata == null ? null : metadata.get("album")),
                stringValue(metadata == null ? null : metadata.get("track")),
                stringValue(metadata == null ? null : metadata.get("primaryArtist")),
                stringListValue(metadata == null ? null : metadata.get("secondaryArtists")),
                stringValue(metadata == null ? null : metadata.get("tier")),
                stringValue(metadata == null ? null : metadata.get("style")),
                stringValue(metadata == null ? null : metadata.get("vocalProfile")),
                stringListValue(metadata == null ? null : metadata.get("moods")),
                stringListValue(metadata == null ? null : metadata.get("vibe")),
                stringValue(metadata == null ? null : metadata.get("energy")),
                stringValue(metadata == null ? null : metadata.get("accessibility")),
                stringValue(metadata == null ? null : metadata.get("tempoFeel")),
                stringValue(metadata == null ? null : metadata.get("instrumentFocus")),
                stringListValue(metadata == null ? null : metadata.get("listeningContext")),
                stringValue(metadata == null ? null : metadata.get("standout")),
                stringValue(metadata == null ? null : metadata.get("albumRole")),
                stringValue(metadata == null ? null : metadata.get("compositionType")),
                stringValue(metadata == null ? null : metadata.get("captionEssence")),
                stringValue(metadata == null ? null : metadata.get("editorialNote")),
                document.getText()
        );
    }

    private String candidateTitle(BasicRecommendationTarget target, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "Unknown";
        }
        var preferredKey = target == BasicRecommendationTarget.ALBUM ? "album" : "track";
        var fallbackKey = target == BasicRecommendationTarget.ALBUM ? "track" : "album";
        return Optional.ofNullable(stringValue(metadata.get(preferredKey)))
                .or(() -> Optional.ofNullable(stringValue(metadata.get(fallbackKey))))
                .orElse("Unknown");
    }

    private String normalizeKey(Object value) {
        var stringValue = stringValue(value);
        if (stringValue == null) {
            return "";
        }
        return stringValue.trim().toLowerCase(Locale.ROOT);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        var rendered = value.toString().trim();
        return rendered.isBlank() ? null : rendered;
    }

    private List<String> stringListValue(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        var singleValue = stringValue(value);
        return singleValue == null ? List.of() : List.of(singleValue);
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
                However, the search returned ZERO results. The requested music is either outside our strict jazz domain, too specific, or simply not in our curated catalog yet.

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
                3. Explain with a friendly boutique-record-store vibe that this specific material is not in the current curated catalog.
                4. DO NOT hallucinate, recommend, or name any specific artist, album, or track.
                5. End by pivoting gently: offer to look for a different jazz mood, era, or style instead.
                6. Keep it concise: 2 or 3 short sentences maximum.
                7. Return ONLY the assistant message text, with no JSON and no extra formatting.
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
                UsageRecordStage.BASIC_RECOMMENDATION,
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

    private String summarizeUserPreferences(UserPreferencesContext context) {
        if (context == null) {
            return "NONE";
        }
        var parts = new ArrayList<String>();
        if (context.jazzPreferences() != null) {
            var prefs = context.jazzPreferences();
            if (prefs.preferredMoods() != null && !prefs.preferredMoods().isEmpty()) {
                parts.add("Preferred moods: " + renderValues(prefs.preferredMoods()));
            }
            if (prefs.preferredSubgenres() != null && !prefs.preferredSubgenres().isEmpty()) {
                parts.add("Preferred subgenres: " + renderValues(prefs.preferredSubgenres()));
            }
            if (prefs.favoriteArtists() != null && !prefs.favoriteArtists().isEmpty()) {
                parts.add("Favorite artists: " + renderValues(prefs.favoriteArtists()));
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
