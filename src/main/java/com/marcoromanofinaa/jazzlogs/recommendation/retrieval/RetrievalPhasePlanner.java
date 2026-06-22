package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RetrievalPhasePlanner {

    List<RetrievalPhase> plan(RetrievalCommand command) {
        var phases = new ArrayList<RetrievalPhase>();
        var filters = command.subgraphFilters();

        if (filters == null) {
            phases.add(new RetrievalPhase("semantic_global", null));
            return List.copyOf(phases);
        }

        var normalized = normalize(filters);
        if (isMeaningful(normalized)) {
            phases.add(new RetrievalPhase("strict_subgraph", normalized));
        }

        if (!normalized.references().isEmpty()) {
            if (!normalized.styles().isEmpty()) {
                phases.add(new RetrievalPhase(
                        "relaxed_keep_references_drop_styles",
                        new ConversationSubgraphFilters(List.of(), normalized.instruments(), normalized.rhythms(), normalized.references())
                ));
            }

            if (!normalized.instruments().isEmpty()) {
                phases.add(new RetrievalPhase(
                        "relaxed_keep_references_drop_instruments",
                        new ConversationSubgraphFilters(normalized.styles(), List.of(), normalized.rhythms(), normalized.references())
                ));
            }

            if (!normalized.styles().isEmpty() && !normalized.instruments().isEmpty()) {
                phases.add(new RetrievalPhase(
                        "relaxed_keep_references_only",
                        new ConversationSubgraphFilters(List.of(), List.of(), normalized.rhythms(), normalized.references())
                ));
            }
        }

        if (normalized.references().isEmpty() && !normalized.styles().isEmpty() && !normalized.instruments().isEmpty()) {
            phases.add(new RetrievalPhase(
                    "relaxed_styles_only",
                    new ConversationSubgraphFilters(normalized.styles(), List.of(), normalized.rhythms(), List.of())
            ));
            phases.add(new RetrievalPhase(
                    "relaxed_instruments_only",
                    new ConversationSubgraphFilters(List.of(), normalized.instruments(), normalized.rhythms(), List.of())
            ));
        }

        phases.add(new RetrievalPhase("semantic_global", null));
        return deduplicate(phases);
    }

    private ConversationSubgraphFilters normalize(ConversationSubgraphFilters filters) {
        return new ConversationSubgraphFilters(
                safeDistinct(filters.styles()),
                safeDistinct(filters.instruments()),
                safeDistinct(filters.rhythms()),
                filters.references() == null ? List.of() : filters.references().stream()
                        .filter(reference -> reference != null
                                && reference.type() != null
                                && reference.name() != null
                                && !reference.name().isBlank())
                        .distinct()
                        .toList()
        );
    }

    private List<String> safeDistinct(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private boolean isMeaningful(ConversationSubgraphFilters filters) {
        return !filters.styles().isEmpty()
                || !filters.instruments().isEmpty()
                || !filters.rhythms().isEmpty()
                || !filters.references().isEmpty();
    }

    private List<RetrievalPhase> deduplicate(List<RetrievalPhase> phases) {
        return phases.stream()
                .distinct()
                .toList();
    }
}
