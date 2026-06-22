package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.context;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesContext;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesService;
import com.marcoromanofinaa.jazzlogs.user.exception.UserNotFoundException;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationRouterContextBuilder {

    private static final int RECENT_EXCHANGES_LIMIT = 5;

    private final UserRepository userRepository;
    private final UserPreferencesService userPreferencesService;

    public ConversationRouterContext build(
            UUID userId,
            ChatRecommendationMemory recommendationMemory,
            List<ChatExchange> recentHistory
    ) {
        var user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        var preferences = userPreferencesService.getPreferencesContext(userId);
        var safeHistory = safeHistory(recentHistory);

        return new ConversationRouterContext(
                user.getDisplayName(),
                summarizePreferences(preferences).orElse(null),
                lastAssistantMessage(safeHistory).orElse(null),
                summarizeRecentExchanges(safeHistory).orElse(null),
                currentSessionSummary(recommendationMemory).orElse(null),
                currentLastRecommendationBatch(recommendationMemory).orElse(null),
                recommendationHistory(recommendationMemory)
        );
    }

    private List<ChatExchange> safeHistory(List<ChatExchange> recentHistory) {
        return recentHistory == null ? List.of() : recentHistory;
    }

    private Optional<String> currentSessionSummary(ChatRecommendationMemory recommendationMemory) {
        return Optional.ofNullable(recommendationMemory)
                .map(ChatRecommendationMemory::sessionSummary)
                .filter(summary -> !summary.isBlank());
    }

    private Optional<ChatRecommendationMemory.LastRecommendationBatch> currentLastRecommendationBatch(
            ChatRecommendationMemory recommendationMemory
    ) {
        return Optional.ofNullable(recommendationMemory)
                .map(ChatRecommendationMemory::lastRecommendationBatch);
    }

    private List<ChatRecommendationMemory.RecommendationHistoryEntry> recommendationHistory(
            ChatRecommendationMemory recommendationMemory
    ) {
        return Optional.ofNullable(recommendationMemory)
                .map(ChatRecommendationMemory::recommendationHistory)
                .filter(items -> !items.isEmpty())
                .orElse(List.of());
    }

    private Optional<String> lastAssistantMessage(List<ChatExchange> recentHistory) {
        return recentHistory.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(recentHistory.getLast().getAssistantResponse())
                        .filter(response -> !response.isBlank());
    }

    private Optional<String> summarizePreferences(UserPreferencesContext context) {
        if (context == null) {
            return Optional.empty();
        }

        var parts = new ArrayList<String>();
        if (context.jazzPreferences() != null) {
            var prefs = context.jazzPreferences();
            append(parts, prefs.jazzExperienceLevelLabel() == null ? null : "experience: " + prefs.jazzExperienceLevelLabel());
            append(parts, joinList("favorite artists", prefs.favoriteArtistLabels()));
            append(parts, joinList("preferred subgenres", prefs.preferredSubgenreLabels()));
            append(parts, joinList("preferred moods", prefs.preferredMoodLabels()));
            append(parts, joinList("favorite instruments", prefs.favoriteInstrumentLabels()));
            append(parts, prefs.tempoFeelLabel() == null ? null : "tempo feel: " + prefs.tempoFeelLabel());
            append(parts, "likes vocals: " + (prefs.likesVocals() ? "yes" : "no"));
            append(parts, prefs.discoveryModeLabel() == null ? null : "discovery mode: " + prefs.discoveryModeLabel());
        }
        append(parts, joinList("top spotify artists", context.topArtists().stream().map(artist -> artist.name()).toList()));
        append(parts, joinList("top spotify tracks", context.topTracks().stream().map(track -> track.name()).toList()));
        return parts.isEmpty() ? Optional.empty() : Optional.of(String.join("; ", parts));
    }

    private Optional<String> summarizeRecentExchanges(List<ChatExchange> recentHistory) {
        if (recentHistory.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(recentHistory.stream()
                .skip(Math.max(0, recentHistory.size() - RECENT_EXCHANGES_LIMIT))
                .map(exchange -> {
                    var parts = new ArrayList<String>();
                    append(parts, "User: " + exchange.getUserMessage());
                    append(parts, "Assistant: " + exchange.getAssistantResponse());
                    if (exchange.getWinners() != null && !exchange.getWinners().isEmpty()) {
                        append(parts, "Winners: " + exchange.getWinners().stream()
                                .map(this::renderWinner)
                                .collect(Collectors.joining(", ")));
                    }
                    return String.join(" | ", parts);
                })
                .collect(Collectors.joining("\n")));
    }

    private Optional<String> joinList(String label, List<?> values) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        var rendered = values.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
        return rendered.isBlank() ? Optional.empty() : Optional.of(label + ": " + rendered);
    }

    private void append(List<String> parts, Optional<String> value) {
        value.map(String::trim)
                .filter(trimmed -> !trimmed.isBlank())
                .ifPresent(parts::add);
    }

    private void append(List<String> parts, String value) {
        append(parts, Optional.ofNullable(value));
    }

    private String renderWinner(WinnerReference winner) {
        if (winner == null || winner.name() == null || winner.name().isBlank()) {
            return "";
        }
        if (winner.artistFullName() == null || winner.artistFullName().isBlank()) {
            return winner.name();
        }
        return winner.name() + " — " + winner.artistFullName();
    }

}
