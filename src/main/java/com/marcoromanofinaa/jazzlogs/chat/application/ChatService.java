package com.marcoromanofinaa.jazzlogs.chat.application;

import com.marcoromanofinaa.jazzlogs.chat.api.dto.ChatExchangeDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.ChatSessionDetailDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.ChatSessionSummaryDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.CreateChatRequestDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.CreateChatResponseDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.SendChatMessageRequestDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.SendChatMessageResponseDTO;
import com.marcoromanofinaa.jazzlogs.chat.config.ChatProperties;
import com.marcoromanofinaa.jazzlogs.chat.exception.ChatSessionInactiveException;
import com.marcoromanofinaa.jazzlogs.chat.exception.ChatSessionNotFoundException;
import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchangeRepository;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemoryService;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatSession;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatSessionRepository;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationOrchestrator;
import com.marcoromanofinaa.jazzlogs.recommendation.service.AIModelAccessService;
import com.marcoromanofinaa.jazzlogs.user.subscription.service.UserSubscriptionService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatExchangeRepository chatExchangeRepository;
    private final ChatRecommendationMemoryService chatRecommendationMemoryService;
    private final RecommendedItemEnrichmentService recommendedItemEnrichmentService;
    private final RecommendationOrchestrator recommendationOrchestrator;
    private final AIModelAccessService aiModelAccessService;
    private final UserSubscriptionService userSubscriptionService;
    private final ChatProperties chatProperties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public CreateChatResponseDTO createChat(UUID userId, CreateChatRequestDTO request) {
        var userMessage = request.userMessage();
        aiModelAccessService.validateAccess(userId, userMessage.requestedModel());

        var chatSessionId = UUID.randomUUID();

        var recommendationResult = recommendationOrchestrator.generate(
                new RecommendationCommand(
                        userId,
                        chatSessionId,
                        userMessage.content(),
                        request.timeZone(),
                        userMessage.requestedModel(),
                        null,
                        List.of()
                )
        );

        var persistedInteraction = persistInteraction(
                userId,
                chatSessionId,
                userMessage.content(),
                userMessage.requestedModel(),
                recommendationResult,
                true
        );

        return new CreateChatResponseDTO(
                persistedInteraction.chatSessionId(),
                persistedInteraction.chatTitle(),
                toDto(persistedInteraction.chatExchange())
        );
    }

    public SendChatMessageResponseDTO sendMessage(UUID userId, UUID chatSessionId, SendChatMessageRequestDTO request) {
        var chatSession = requireChatSession(userId, chatSessionId, false);
        var userMessage = request.userMessage();
        aiModelAccessService.validateAccess(userId, userMessage.requestedModel());

        var recentHistoryDesc = chatExchangeRepository.findByChatSessionIdOrderByCreatedAtDesc(
                chatSessionId,
                PageRequest.of(0, chatProperties.history().recentExchangesLimit())
        );
        var recentHistory = new ArrayList<>(recentHistoryDesc);
        java.util.Collections.reverse(recentHistory);

        var recommendationResult = recommendationOrchestrator.generate(
                new RecommendationCommand(
                        userId,
                        chatSessionId,
                        userMessage.content(),
                        request.timeZone(),
                        userMessage.requestedModel(),
                        chatSession.getRecommendationMemory(),
                        recentHistory
                )
        );

        var persistedInteraction = persistInteraction(
                userId,
                chatSessionId,
                userMessage.content(),
                userMessage.requestedModel(),
                recommendationResult,
                false
        );

        return new SendChatMessageResponseDTO(
                chatSessionId,
                toDto(persistedInteraction.chatExchange())
        );
    }

    public List<ChatSessionSummaryDTO> getUserChats(UUID userId) {
        return chatSessionRepository.findByUserIdOrderByLastInteractionAtDesc(userId).stream()
                .filter(session -> !session.isDeleted())
                .map(session -> new ChatSessionSummaryDTO(
                        session.getId(),
                        session.getTitle(),
                        session.getLastInteractionAt(),
                        session.getCreatedAt()
                ))
                .toList();
    }

    public ChatSessionDetailDTO getChat(UUID userId, UUID chatSessionId) {
        var chatSession = requireChatSession(userId, chatSessionId, false);
        var chatExchanges = chatExchangeRepository.findByChatSessionIdOrderByCreatedAtAsc(chatSessionId);
        var recommendedItemsByExchangeId = recommendedItemEnrichmentService.enrichByExchangeId(chatExchanges);

        var exchanges = chatExchanges.stream()
                .map(chatExchange -> toDto(
                        chatExchange,
                        recommendedItemsByExchangeId.getOrDefault(chatExchange.getId(), List.of())
                ))
                .toList();

        return new ChatSessionDetailDTO(
                chatSession.getId(),
                chatSession.getTitle(),
                exchanges,
                chatSession.getLastInteractionAt(),
                chatSession.getCreatedAt()
        );
    }

    @Transactional
    public void deleteChat(UUID userId, UUID chatSessionId) {
        var chatSession = requireChatSession(userId, chatSessionId, true);
        if (!chatSession.isDeleted()) {
            chatSession.delete(Instant.now(clock));
        }
    }

    private PersistedInteraction persistInteraction(
            UUID userId,
            UUID chatSessionId,
            String userMessage,
            com.marcoromanofinaa.jazzlogs.recommendation.AIModelType requestedModel,
            com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult recommendationResult,
            boolean updateTitleFromSuggestion
    ) {
        var winners = requireValidWinners(recommendationResult);
        var usageEntries = requireUsageEntries(recommendationResult);
        var modelUsed = recommendationResult.modelUsed();

        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            var now = Instant.now(clock);
            var chatSession = chatSessionRepository.findByIdAndUserId(chatSessionId, userId)
                    .orElseGet(() -> chatSessionRepository.save(ChatSession.create(chatSessionId, userId, now)));

            if (chatSession.isDeleted()) {
                throw new ChatSessionInactiveException(chatSessionId);
            }

            var chatExchange = chatExchangeRepository.save(ChatExchange.create(
                    chatSessionId,
                    userMessage,
                    requestedModel,
                    recommendationResult.assistantResponse(),
                    winners,
                    recommendationResult.recommendationType(),
                    modelUsed,
                    recommendationResult.timing() == null ? 0L : recommendationResult.timing().routerLatencyMs(),
                    recommendationResult.timing() == null ? 0L : recommendationResult.timing().flowLatencyMs(),
                    recommendationResult.timing() == null ? 0L : recommendationResult.timing().totalRecommendationLatencyMs(),
                    now
            ));

            userSubscriptionService.consumeCredits(
                    userId,
                    chatSessionId,
                    chatExchange.getId(),
                    usageEntries
            );

            if (updateTitleFromSuggestion
                    && recommendationResult.suggestedChatTitle() != null
                    && !recommendationResult.suggestedChatTitle().isBlank()) {
                chatSession.updateTitle(recommendationResult.suggestedChatTitle(), now);
            }
            chatSession.updateRecommendationMemory(
                    chatRecommendationMemoryService.updateMemory(chatSession.getRecommendationMemory(), recommendationResult),
                    now
            );
            chatSession.markInteraction(now);

            return new PersistedInteraction(
                    chatSession.getId(),
                    chatSession.getTitle(),
                    chatExchange
            );
        }));
    }

    private ChatSession requireChatSession(UUID userId, UUID chatSessionId, boolean allowDeleted) {
        var chatSession = chatSessionRepository.findByIdAndUserId(chatSessionId, userId)
                .orElseThrow(() -> new ChatSessionNotFoundException(chatSessionId));
        if (!allowDeleted && chatSession.isDeleted()) {
            throw new ChatSessionInactiveException(chatSessionId);
        }
        return chatSession;
    }

    private ChatExchangeDTO toDto(ChatExchange chatExchange) {
        return toDto(
                chatExchange,
                recommendedItemEnrichmentService.enrich(chatExchange.getRecommendationType(), chatExchange.getWinners())
        );
    }

    private ChatExchangeDTO toDto(
            ChatExchange chatExchange,
            List<com.marcoromanofinaa.jazzlogs.chat.api.dto.RecommendedItemDTO> recommendedItems
    ) {
        return new ChatExchangeDTO(
                chatExchange.getId(),
                chatExchange.getUserMessage(),
                chatExchange.getRequestedModel(),
                chatExchange.getAssistantResponse(),
                chatExchange.getRecommendationType(),
                recommendedItems,
                chatExchange.getModelUsed(),
                chatExchange.getCreatedAt()
        );
    }

    private List<com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference> safeWinners(
            com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult recommendationResult
    ) {
        return Optional.ofNullable(recommendationResult.winners()).orElse(List.of());
    }

    private List<com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference> requireValidWinners(
            com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult recommendationResult
    ) {
        var winners = safeWinners(recommendationResult);
        if (winners.isEmpty()) {
            return winners;
        }

        var recommendationType = recommendationResult.recommendationType();
        if (recommendationType == null) {
            throw new IllegalArgumentException("recommendationType is required when winners are present");
        }
        if (winners.stream().anyMatch(winner -> winner == null || winner.type() != recommendationType)) {
            throw new IllegalArgumentException("winner types must match recommendationType");
        }
        return winners;
    }

    private List<com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage> requireUsageEntries(
            com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult recommendationResult
    ) {
        var usageEntries = Optional.ofNullable(recommendationResult.usageEntries()).orElse(List.of());
        if (usageEntries.isEmpty()) {
            throw new IllegalArgumentException("usageEntries must not be empty");
        }
        return usageEntries;
    }

    private record PersistedInteraction(
            UUID chatSessionId,
            String chatTitle,
            ChatExchange chatExchange
    ) {
    }
}
