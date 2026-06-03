package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.context;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
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
                currentLastRecommendedItem(recommendationMemory).orElse(null),
                orderedMemoryItems(recommendationMemory)
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

    private Optional<ChatRecommendationMemory.LastRecommendedItem> currentLastRecommendedItem(
            ChatRecommendationMemory recommendationMemory
    ) {
        return Optional.ofNullable(recommendationMemory)
                .map(ChatRecommendationMemory::lastRecommendedItem);
    }

    private List<ChatRecommendationMemory.OrderedRecommendedItem> orderedMemoryItems(
            ChatRecommendationMemory recommendationMemory
    ) {
        return Optional.ofNullable(recommendationMemory)
                .map(ChatRecommendationMemory::orderedRecommendedItems)
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
            append(parts, "experience: " + prefs.jazzExperienceLevel());
            append(parts, joinList("favorite artists", prefs.favoriteArtists()));
            append(parts, joinList("preferred subgenres", prefs.preferredSubgenres()));
            append(parts, joinList("preferred moods", prefs.preferredMoods()));
            append(parts, joinList("favorite instruments", prefs.favoriteInstruments()));
            append(parts, prefs.tempoFeel() == null ? null : "tempo feel: " + prefs.tempoFeel());
            append(parts, "likes vocals: " + (prefs.likesVocals() ? "yes" : "no"));
            append(parts, prefs.discoveryMode() == null ? null : "discovery mode: " + prefs.discoveryMode());
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
                        append(parts, "Winners: " + String.join(", ", exchange.getWinners()));
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

}
