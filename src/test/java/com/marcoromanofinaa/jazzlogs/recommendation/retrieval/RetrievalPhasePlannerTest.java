package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphReference;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphReferenceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetrievalPhasePlannerTest {

    private final RetrievalPhasePlanner planner = new RetrievalPhasePlanner();

    @Test
    void planFallsBackToSemanticGlobalWhenFiltersAreNull() {
        var phases = planner.plan(new RetrievalCommand(
                "algo nocturno",
                BasicRecommendationTarget.ALBUM,
                8,
                List.of(),
                null
        ));

        assertThat(phases).containsExactly(new RetrievalPhase("semantic_global", null));
    }

    @Test
    void planWithReferencesAlwaysKeepsSemanticGlobalAsLastFallback() {
        var filters = new ConversationSubgraphFilters(
                List.of("Cool Jazz"),
                List.of("Trumpet"),
                List.of("Swing"),
                List.of(new ConversationSubgraphReference(
                        ConversationSubgraphReferenceType.ARTIST,
                        "Chet Baker"
                ))
        );

        var phases = planner.plan(new RetrievalCommand(
                "mas de chet",
                BasicRecommendationTarget.ALBUM,
                8,
                List.of(),
                filters
        ));

        assertThat(phases).containsExactly(
                new RetrievalPhase(
                        "strict_subgraph",
                        new ConversationSubgraphFilters(
                                List.of("Cool Jazz"),
                                List.of("Trumpet"),
                                List.of("Swing"),
                                List.of(new ConversationSubgraphReference(
                                        ConversationSubgraphReferenceType.ARTIST,
                                        "Chet Baker"
                                ))
                        )
                ),
                new RetrievalPhase(
                        "relaxed_keep_references_drop_styles",
                        new ConversationSubgraphFilters(
                                List.of(),
                                List.of("Trumpet"),
                                List.of("Swing"),
                                List.of(new ConversationSubgraphReference(
                                        ConversationSubgraphReferenceType.ARTIST,
                                        "Chet Baker"
                                ))
                        )
                ),
                new RetrievalPhase(
                        "relaxed_keep_references_drop_instruments",
                        new ConversationSubgraphFilters(
                                List.of("Cool Jazz"),
                                List.of(),
                                List.of("Swing"),
                                List.of(new ConversationSubgraphReference(
                                        ConversationSubgraphReferenceType.ARTIST,
                                        "Chet Baker"
                                ))
                        )
                ),
                new RetrievalPhase(
                        "relaxed_keep_references_only",
                        new ConversationSubgraphFilters(
                                List.of(),
                                List.of(),
                                List.of("Swing"),
                                List.of(new ConversationSubgraphReference(
                                        ConversationSubgraphReferenceType.ARTIST,
                                        "Chet Baker"
                                ))
                        )
                ),
                new RetrievalPhase("semantic_global", null)
        );
    }

    @Test
    void planWithoutReferencesRelaxesToStylesAndInstrumentsBeforeGlobalFallback() {
        var filters = new ConversationSubgraphFilters(
                List.of("Hard Bop"),
                List.of("Piano"),
                List.of("Shuffle"),
                List.of()
        );

        var phases = planner.plan(new RetrievalCommand(
                "algo con piano",
                BasicRecommendationTarget.TRACKS,
                12,
                List.of(),
                filters
        ));

        assertThat(phases).containsExactly(
                new RetrievalPhase("strict_subgraph", filters),
                new RetrievalPhase(
                        "relaxed_styles_only",
                        new ConversationSubgraphFilters(List.of("Hard Bop"), List.of(), List.of("Shuffle"), List.of())
                ),
                new RetrievalPhase(
                        "relaxed_instruments_only",
                        new ConversationSubgraphFilters(List.of(), List.of("Piano"), List.of("Shuffle"), List.of())
                ),
                new RetrievalPhase("semantic_global", null)
        );
    }

    @Test
    void planNormalizesAndDeduplicatesFiltersAndReferences() {
        var duplicatedReference = new ConversationSubgraphReference(
                ConversationSubgraphReferenceType.ALBUM,
                "Blue Hour"
        );
        var filters = new ConversationSubgraphFilters(
                List.of(" Hard Bop ", "Hard Bop"),
                List.of(" Piano", "Piano "),
                List.of(" Shuffle ", "Shuffle"),
                List.of(
                        duplicatedReference,
                        duplicatedReference,
                        new ConversationSubgraphReference(
                                ConversationSubgraphReferenceType.TRACK,
                                "   "
                        )
                )
        );

        var phases = planner.plan(new RetrievalCommand(
                "blue hour",
                BasicRecommendationTarget.ALBUM,
                8,
                List.of(),
                filters
        ));

        assertThat(phases).containsExactly(
                new RetrievalPhase(
                        "strict_subgraph",
                        new ConversationSubgraphFilters(
                                List.of("Hard Bop"),
                                List.of("Piano"),
                                List.of("Shuffle"),
                                List.of(duplicatedReference)
                        )
                ),
                new RetrievalPhase(
                        "relaxed_keep_references_drop_styles",
                        new ConversationSubgraphFilters(
                                List.of(),
                                List.of("Piano"),
                                List.of("Shuffle"),
                                List.of(duplicatedReference)
                        )
                ),
                new RetrievalPhase(
                        "relaxed_keep_references_drop_instruments",
                        new ConversationSubgraphFilters(
                                List.of("Hard Bop"),
                                List.of(),
                                List.of("Shuffle"),
                                List.of(duplicatedReference)
                        )
                ),
                new RetrievalPhase(
                        "relaxed_keep_references_only",
                        new ConversationSubgraphFilters(
                                List.of(),
                                List.of(),
                                List.of("Shuffle"),
                                List.of(duplicatedReference)
                        )
                ),
                new RetrievalPhase("semantic_global", null)
        );
    }
}
