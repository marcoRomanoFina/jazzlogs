package com.marcoromanofinaa.jazzlogs.chat.session;

import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
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

    private final RecommendedItemMetadataResolver recommendedItemMetadataResolver;

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

        var history = new ArrayList<>(currentHistory(current));

        if (winners.isEmpty()) {
            return new ChatRecommendationMemory(
                    currentLastRecommendationBatch(current).orElse(null),
                    List.copyOf(history),
                    updatedSessionSummary
            );
        }

        var recommendationTarget = Optional.ofNullable(recommendationResult.recommendationType())
                .orElseThrow(() -> new IllegalArgumentException("recommendationType is required when winners are present"))
                ;
        var resolvedMetadata = resolveWinnerMetadata(recommendationTarget, winners);

        int nextOrder = history.size() + 1;
        for (int index = 0; index < winners.size(); index++) {
            var metadata = resolvedMetadata.get(index);
            history.add(new ChatRecommendationMemory.RecommendationHistoryEntry(
                    nextOrder++,
                    winners.get(index),
                    metadata == null ? null : metadata.primaryArtist(),
                    metadata == null ? null : metadata.album(),
                    metadata == null ? null : metadata.track()
            ));
        }

        return new ChatRecommendationMemory(
                new ChatRecommendationMemory.LastRecommendationBatch(
                        winners,
                        List.copyOf(resolvedMetadata)
                ),
                List.copyOf(history),
                updatedSessionSummary
        );
    }

    private Optional<String> currentSessionSummary(ChatRecommendationMemory current) {
        return Optional.ofNullable(current).map(ChatRecommendationMemory::sessionSummary);
    }

    private Optional<ChatRecommendationMemory.LastRecommendationBatch> currentLastRecommendationBatch(
            ChatRecommendationMemory current
    ) {
        return Optional.ofNullable(current).map(ChatRecommendationMemory::lastRecommendationBatch);
    }

    private List<ChatRecommendationMemory.RecommendationHistoryEntry> currentHistory(
            ChatRecommendationMemory current
    ) {
        return Optional.ofNullable(current)
                .map(ChatRecommendationMemory::recommendationHistory)
                .orElse(List.of());
    }

    private List<WinnerReference> sanitizeWinners(RecommendationResult recommendationResult) {
        return Optional.ofNullable(recommendationResult.winners())
                .orElse(List.of())
                .stream()
                .filter(winner -> winner != null
                        && winner.id() != null && !winner.id().isBlank()
                        && winner.name() != null && !winner.name().isBlank()
                        && winner.artistFullName() != null && !winner.artistFullName().isBlank()
                        && winner.type() != null)
                .toList();
    }

    private List<ResolvedRecommendationMemoryItem> resolveWinnerMetadata(
            com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget recommendationType,
            List<WinnerReference> winners
    ) {
        return recommendedItemMetadataResolver.resolveAll(
                recommendationType,
                winners.stream()
                        .map(WinnerReference::id)
                        .toList()
                );
    }

    private Optional<String> blankToOptional(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(trimmed -> !trimmed.isBlank());
    }
}
