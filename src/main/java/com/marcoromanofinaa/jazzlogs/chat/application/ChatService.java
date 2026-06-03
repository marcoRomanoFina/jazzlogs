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
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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

    @Transactional
    public CreateChatResponseDTO createChat(UUID userId, CreateChatRequestDTO request) {
        var userMessage = request.userMessage();
        aiModelAccessService.validateAccess(userId, userMessage.requestedModel());

        var now = Instant.now(clock);
        var chatSession = chatSessionRepository.save(ChatSession.create(userId, now));

        var recommendationResult = recommendationOrchestrator.generate(
                new RecommendationCommand(
                        userId,
                        chatSession.getId(),
                        userMessage.content(),
                        request.timeZone(),
                        userMessage.requestedModel(),
                        chatSession.getRecommendationMemory(),
                        List.of()
                )
        );

        var chatExchange = chatExchangeRepository.save(ChatExchange.create(
                chatSession.getId(),
                userMessage.content(),
                userMessage.requestedModel(),
                recommendationResult.assistantResponse(),
                recommendationResult.winners(),
                recommendationResult.modelUsed(),
                recommendationResult.timing() == null ? 0L : recommendationResult.timing().routerLatencyMs(),
                recommendationResult.timing() == null ? 0L : recommendationResult.timing().flowLatencyMs(),
                recommendationResult.timing() == null ? 0L : recommendationResult.timing().totalRecommendationLatencyMs(),
                now
        ));

        userSubscriptionService.consumeTokens(
                userId,
                chatSession.getId(),
                chatExchange.getId(),
                recommendationResult.usageEntries()
        );

        if (recommendationResult.suggestedChatTitle() != null && !recommendationResult.suggestedChatTitle().isBlank()) {
            chatSession.updateTitle(recommendationResult.suggestedChatTitle(), now);
        }
        chatSession.updateRecommendationMemory(
                chatRecommendationMemoryService.append(chatSession.getRecommendationMemory(), recommendationResult),
                now
        );
        chatSession.markInteraction(now);

        return new CreateChatResponseDTO(
                chatSession.getId(),
                chatSession.getTitle(),
                toDto(chatExchange)
        );
    }

    @Transactional
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

        var now = Instant.now(clock);
        var chatExchange = chatExchangeRepository.save(ChatExchange.create(
                chatSessionId,
                userMessage.content(),
                userMessage.requestedModel(),
                recommendationResult.assistantResponse(),
                recommendationResult.winners(),
                recommendationResult.modelUsed(),
                recommendationResult.timing() == null ? 0L : recommendationResult.timing().routerLatencyMs(),
                recommendationResult.timing() == null ? 0L : recommendationResult.timing().flowLatencyMs(),
                recommendationResult.timing() == null ? 0L : recommendationResult.timing().totalRecommendationLatencyMs(),
                now
        ));

        userSubscriptionService.consumeTokens(
                userId,
                chatSessionId,
                chatExchange.getId(),
                recommendationResult.usageEntries()
        );

        chatSession.updateRecommendationMemory(
                chatRecommendationMemoryService.append(chatSession.getRecommendationMemory(), recommendationResult),
                now
        );
        chatSession.markInteraction(now);

        return new SendChatMessageResponseDTO(
                chatSessionId,
                toDto(chatExchange)
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

        var exchanges = chatExchangeRepository.findByChatSessionIdOrderByCreatedAtAsc(chatSessionId).stream()
                .map(this::toDto)
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

    private ChatSession requireChatSession(UUID userId, UUID chatSessionId, boolean allowDeleted) {
        var chatSession = chatSessionRepository.findByIdAndUserId(chatSessionId, userId)
                .orElseThrow(() -> new ChatSessionNotFoundException(chatSessionId));
        if (!allowDeleted && chatSession.isDeleted()) {
            throw new ChatSessionInactiveException(chatSessionId);
        }
        return chatSession;
    }

    private ChatExchangeDTO toDto(ChatExchange chatExchange) {
        return new ChatExchangeDTO(
                chatExchange.getId(),
                chatExchange.getUserMessage(),
                chatExchange.getRequestedModel(),
                chatExchange.getAssistantResponse(),
                chatExchange.getWinners(),
                recommendedItemEnrichmentService.enrich(chatExchange.getWinners()),
                chatExchange.getModelUsed(),
                chatExchange.getCreatedAt()
        );
    }
}
