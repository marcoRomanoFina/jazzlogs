package com.marcoromanofinaa.jazzlogs.ai.recommend.basic.strategy;

import com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate.AlbumRecommendCandidate;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate.AlbumRecommendCandidateAssembler;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.logging.RecommendRequestAuditLogger;
import com.marcoromanofinaa.jazzlogs.ai.recommend.basic.llm.AlbumRecommendPromptBuilder;
import com.marcoromanofinaa.jazzlogs.ai.recommend.basic.llm.RecommendResponseJsonParser;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlbumRecommendStrategy implements RecommendStrategy {

    private static final int ALBUM_CANDIDATE_LIMIT = 8;

    private final RecommendRetrievalService retrievalService;
    private final AlbumRecommendCandidateAssembler candidateAssembler;
    private final AlbumRecommendPromptBuilder promptBuilder;
    private final RecommendResponseJsonParser responseJsonParser;
    private final RecommendRequestAuditLogger auditLogger;
    private final RecommendResponsesClient responsesClient;

    @Override
    public AiRecommendMode supports() {
        return AiRecommendMode.ALBUM;
    }

    @Override
    public AiRecommendResponse recommend(AiRecommendRequest request) {
        var sanitizedQuestion = RecommendQuestionSanitizer.sanitize(request.question());
        var candidates = List.<AlbumRecommendCandidate>of();
        candidates = candidateAssembler.assembleAll(
                retrievalService.topAlbumDocuments(sanitizedQuestion, ALBUM_CANDIDATE_LIMIT)
        );
        if (candidates.isEmpty()) {
            return new AiRecommendResponse(
                    request.question(),
                    supports(),
                    "Todavía no tengo suficientes álbumes curados para responder bien esa búsqueda, pero voy a seguir aprendiendo. Ojalá tengas un gran día y vuelvas a probar en un rato.",
                    List.of(),
                    List.of()
            );
        }

        var prompt = promptBuilder.build(request.question(), sanitizedQuestion, candidates);
        RecommendResponsesResult responsesResult = responsesClient.call(prompt);
        String rawResponse = responsesResult.outputText();
        log.info(
                "AI recommend album raw response length={}, finishReason={}, usage={}",
                rawResponse == null ? null : rawResponse.length(),
                responsesResult.finishReason(),
                responsesResult.nativeUsage()
        );
        var decision = responseJsonParser.parseAlbumDecision(rawResponse);
        if (decision.chosenSemanticDocumentId() == null || decision.chosenSemanticDocumentId().isBlank()) {
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
                .collect(Collectors.toMap(AlbumRecommendCandidate::semanticDocumentId, Function.identity()));
        var chosenCandidate = candidatesBySemanticId.get(decision.chosenSemanticDocumentId());

        if (chosenCandidate == null) {
            throw new FeatureUnavailableException("AI recommend selected an unknown album candidate");
        }

        auditLogger.logRequest(
                supports(),
                request.question(),
                candidates.size(),
                summarizeCandidates(candidates),
                responsesResult,
                rawResponse,
                decision.answer(),
                List.of(chosenCandidate.semanticDocumentId()),
                List.of(toRecommendationItem(chosenCandidate))
        );

        return new AiRecommendResponse(
                request.question(),
                supports(),
                decision.answer(),
                List.of(toRecommendationItem(chosenCandidate)),
                List.of(chosenCandidate.semanticDocumentId())
        );
    }

    private AiRecommendItem toRecommendationItem(AlbumRecommendCandidate candidate) {
        var deliveryMetadata = candidate.deliveryMetadata();
        return new AiRecommendItem(
                AiRecommendItemKind.ALBUM,
                candidate.album(),
                candidate.artist(),
                candidate.logNumber(),
                deliveryMetadata.spotifyUrl(),
                deliveryMetadata.coverImageUrl(),
                deliveryMetadata.instagramPermalink()
        );
    }

    private List<Map<String, Object>> summarizeCandidates(List<AlbumRecommendCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("semanticDocumentId", candidate.semanticDocumentId());
                    summary.put("similarityScore", candidate.similarityScore());
                    summary.put("logNumber", candidate.logNumber());
                    summary.put("album", candidate.album());
                    summary.put("artist", candidate.artist());
                    summary.put("style", candidate.decisionContext().style());
                    summary.put("vocalProfile", candidate.decisionContext().vocalProfile());
                    summary.put("moods", candidate.decisionContext().moods());
                    summary.put("vibe", candidate.decisionContext().vibe());
                    summary.put("energy", candidate.decisionContext().energy());
                    summary.put("moodIntensity", candidate.decisionContext().moodIntensity());
                    summary.put("accessibility", candidate.decisionContext().accessibility());
                    summary.put("listeningContext", candidate.decisionContext().listeningContext());
                    summary.put("recommendedIf", candidate.decisionContext().recommendedIf());
                    summary.put("avoidIf", candidate.decisionContext().avoidIf());
                    return summary;
                })
                .toList();
    }
}
