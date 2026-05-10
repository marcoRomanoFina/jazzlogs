package com.marcoromanofinaa.jazzlogs.ai.recommend.basic.strategy;

import com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate.TrackRecommendCandidate;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate.TrackRecommendCandidateAssembler;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.logging.RecommendRequestAuditLogger;
import com.marcoromanofinaa.jazzlogs.ai.recommend.basic.llm.RecommendResponseJsonParser;
import com.marcoromanofinaa.jazzlogs.ai.recommend.basic.llm.TrackRecommendPromptBuilder;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendItem;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendItemKind;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendMode;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendRequest;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendResponse;
import com.marcoromanofinaa.jazzlogs.ai.recommend.openai.RecommendResponsesClient;
import com.marcoromanofinaa.jazzlogs.ai.recommend.openai.RecommendResponsesResult;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.retrieval.RecommendRetrievalService;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.support.RecommendQuestionSanitizer;
import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrackRecommendStrategy implements RecommendStrategy {

    private static final int TRACK_CANDIDATE_LIMIT = 8;

    private final RecommendRetrievalService retrievalService;
    private final TrackRecommendCandidateAssembler candidateAssembler;
    private final TrackRecommendPromptBuilder promptBuilder;
    private final RecommendResponseJsonParser responseJsonParser;
    private final RecommendRequestAuditLogger auditLogger;
    private final RecommendResponsesClient responsesClient;

    @Override
    public AiRecommendMode supports() {
        return AiRecommendMode.TRACKS;
    }

    @Override
    public AiRecommendResponse recommend(AiRecommendRequest request) {
        var sanitizedQuestion = RecommendQuestionSanitizer.sanitize(request.question());
        var candidates = List.<TrackRecommendCandidate>of();
        candidates = candidateAssembler.assembleAll(
                retrievalService.topTrackDocuments(sanitizedQuestion, TRACK_CANDIDATE_LIMIT)
        );
        if (candidates.isEmpty()) {
            return new AiRecommendResponse(
                    request.question(),
                    supports(),
                    "Todavía no tengo suficientes tracks curados para responder bien esa búsqueda, pero voy a seguir armando buenas opciones. Ojalá tengas un gran día y vuelvas a probar en un rato.",
                    List.of(),
                    List.of()
            );
        }

        var prompt = promptBuilder.build(request.question(), sanitizedQuestion, candidates);
        RecommendResponsesResult responsesResult = responsesClient.call(prompt);
        String rawResponse = responsesResult.outputText();
        log.info(
                "AI recommend track raw response length={}, finishReason={}, usage={}",
                rawResponse == null ? null : rawResponse.length(),
                responsesResult.finishReason(),
                responsesResult.nativeUsage()
        );

        var decision = responseJsonParser.parseTrackDecision(rawResponse);
        var validSelections = decision.selections() == null
                ? List.<com.marcoromanofinaa.jazzlogs.ai.recommend.basic.llm.TrackRecommendSelection>of()
                : decision.selections().stream()
                        .filter(Objects::nonNull)
                        .filter(selection -> selection.semanticDocumentId() != null && !selection.semanticDocumentId().isBlank())
                        .toList();

        if (validSelections.isEmpty()) {
            auditLogger.logRequest(
                    supports(),
                    request.question(),
                    candidates.size(),
                    summarizeCandidates(candidates),
                    responsesResult,
                    rawResponse,
                    decision.answer(),
                    List.of(),
                    List.of()
            );
            return new AiRecommendResponse(
                    request.question(),
                    supports(),
                    decision.answer(),
                    List.of(),
                    List.of()
            );
        }

        var candidatesBySemanticId = candidates.stream()
                .collect(Collectors.toMap(TrackRecommendCandidate::semanticDocumentId, Function.identity()));

        var recommendationItems = validSelections.stream()
                .map(selection -> {
                    var candidate = candidatesBySemanticId.get(selection.semanticDocumentId());
                    if (candidate == null) {
                        throw new FeatureUnavailableException("AI recommend selected an unknown track candidate");
                    }
                    return toRecommendationItem(candidate);
                })
                .toList();

        var sources = validSelections.stream()
                .map(selection -> selection.semanticDocumentId())
                .toList();

        auditLogger.logRequest(
                supports(),
                request.question(),
                candidates.size(),
                summarizeCandidates(candidates),
                responsesResult,
                rawResponse,
                decision.answer(),
                sources,
                recommendationItems
        );

        return new AiRecommendResponse(
                request.question(),
                supports(),
                decision.answer(),
                recommendationItems,
                sources
        );
    }

    private AiRecommendItem toRecommendationItem(TrackRecommendCandidate candidate) {
        var deliveryMetadata = candidate.deliveryMetadata();
        return new AiRecommendItem(
                AiRecommendItemKind.TRACK,
                candidate.track(),
                candidate.artist(),
                candidate.logNumber(),
                deliveryMetadata.spotifyUrl(),
                deliveryMetadata.coverImageUrl(),
                null
        );
    }

    private List<Map<String, Object>> summarizeCandidates(List<TrackRecommendCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("semanticDocumentId", candidate.semanticDocumentId());
                    summary.put("similarityScore", candidate.similarityScore());
                    summary.put("logNumber", candidate.logNumber());
                    summary.put("spotifyTrackId", candidate.spotifyTrackId());
                    summary.put("track", candidate.track());
                    summary.put("album", candidate.album());
                    summary.put("artist", candidate.artist());
                    summary.put("instrumental", candidate.decisionContext().instrumental());
                    summary.put("tier", candidate.decisionContext().tier());
                    summary.put("vibe", candidate.decisionContext().vibe());
                    summary.put("energy", candidate.decisionContext().energy());
                    summary.put("moodIntensity", candidate.decisionContext().moodIntensity());
                    summary.put("accessibility", candidate.decisionContext().accessibility());
                    summary.put("tempoFeel", candidate.decisionContext().tempoFeel());
                    summary.put("rhythmicFeel", candidate.decisionContext().rhythmicFeel());
                    summary.put("trackRole", candidate.decisionContext().trackRole());
                    summary.put("compositionType", candidate.decisionContext().compositionType());
                    summary.put("listeningContext", candidate.decisionContext().listeningContext());
                    summary.put("instrumentFocus", candidate.decisionContext().instrumentFocus());
                    summary.put("vocalStyle", candidate.decisionContext().vocalStyle());
                    summary.put("standoutTags", candidate.decisionContext().standoutTags());
                    summary.put("albumPersonnel", candidate.decisionContext().albumPersonnel());
                    return summary;
                })
                .toList();
    }
}
