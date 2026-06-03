package com.marcoromanofinaa.jazzlogs.chat.session;

import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ChatRecommendationMemoryService {

    private final RecommendedItemMetadataLookupService recommendedItemMetadataLookupService;

    public ChatRecommendationMemory updateMemory(
            ChatRecommendationMemory current,
            @Valid @NotNull RecommendationResult recommendationResult
    ) {
        var updatedSessionSummary = blankToOptional(recommendationResult.updatedSessionSummary())
                .or(() -> currentSessionSummary(current))
                .orElse(null);

        var winners = sanitizeWinners(recommendationResult);
        if (winners.isEmpty() && updatedSessionSummary == null) {
            return current;
        }

        var ordered = new ArrayList<>(currentOrderedItems(current));

        if (winners.isEmpty()) {
            return new ChatRecommendationMemory(
                    currentLastRecommendedItem(current).orElse(null),
                    List.copyOf(ordered),
                    updatedSessionSummary
            );
        }

        var recommendationType = Optional.ofNullable(recommendationResult.recommendationType())
                .orElseThrow(() -> new IllegalArgumentException("recommendationType is required when winners are present"))
                .name();
        var resolvedMetadata = resolveWinnerMetadata(recommendationResult.recommendationType(), winners);

        int nextOrder = ordered.size() + 1;
        for (int index = 0; index < winners.size(); index++) {
            ordered.add(new ChatRecommendationMemory.OrderedRecommendedItem(
                    nextOrder++,
                    recommendationType,
                    winners.get(index),
                    resolvedMetadata.get(index)
            ));
        }

        return new ChatRecommendationMemory(
                new ChatRecommendationMemory.LastRecommendedItem(
                        recommendationType,
                        recommendationResult.assistantResponse(),
                        winners,
                        java.util.Collections.unmodifiableList(new ArrayList<>(resolvedMetadata))
                ),
                List.copyOf(ordered),
                updatedSessionSummary
        );
    }

    private Optional<String> currentSessionSummary(ChatRecommendationMemory current) {
        return Optional.ofNullable(current).map(ChatRecommendationMemory::sessionSummary);
    }

    private Optional<ChatRecommendationMemory.LastRecommendedItem> currentLastRecommendedItem(
            ChatRecommendationMemory current
    ) {
        return Optional.ofNullable(current).map(ChatRecommendationMemory::lastRecommendedItem);
    }

    private List<ChatRecommendationMemory.OrderedRecommendedItem> currentOrderedItems(
            ChatRecommendationMemory current
    ) {
        return Optional.ofNullable(current)
                .map(ChatRecommendationMemory::orderedRecommendedItems)
                .orElse(List.of());
    }

    private List<String> sanitizeWinners(RecommendationResult recommendationResult) {
        return recommendationResult.winners().stream()
                .map(String::trim)
                .toList();
    }

    private List<ChatRecommendationMemory.RecommendedItemMetadata> resolveWinnerMetadata(
            BasicRecommendationTarget recommendationType,
            List<String> winners
    ) {
        return winners.stream()
                .map(winner -> recommendedItemMetadataLookupService.findByWinner(recommendationType, winner))
                .map(optional -> optional.orElse(null))
                .toList();
    }

    private Optional<String> blankToOptional(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(trimmed -> !trimmed.isBlank());
    }
}
