package com.marcoromanofinaa.jazzlogs.chat.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.chat.api.dto.CreateChatRequestDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.UserChatMessageDTO;
import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.chat.config.ChatProperties;
import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchangeRepository;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemoryService;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatSession;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatSessionRepository;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationOrchestrator;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationTiming;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import com.marcoromanofinaa.jazzlogs.recommendation.service.AIModelAccessService;
import com.marcoromanofinaa.jazzlogs.user.subscription.service.UserSubscriptionService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;
    @Mock
    private ChatExchangeRepository chatExchangeRepository;
    @Mock
    private ChatRecommendationMemoryService chatRecommendationMemoryService;
    @Mock
    private RecommendedItemEnrichmentService recommendedItemEnrichmentService;
    @Mock
    private RecommendationOrchestrator recommendationOrchestrator;
    @Mock
    private AIModelAccessService aiModelAccessService;
    @Mock
    private UserSubscriptionService userSubscriptionService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-10T12:00:00Z"), ZoneOffset.UTC);

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                chatSessionRepository,
                chatExchangeRepository,
                chatRecommendationMemoryService,
                recommendedItemEnrichmentService,
                recommendationOrchestrator,
                aiModelAccessService,
                userSubscriptionService,
                new ChatProperties(new ChatProperties.History(10)),
                clock,
                transactionTemplate
        );
    }

    @Test
    void createChatDoesNotPersistSessionBeforeRecommendationSucceeds() {
        var userId = UUID.randomUUID();
        var request = new CreateChatRequestDTO(
                new UserChatMessageDTO("algo tranqui", AIModelType.BASIC),
                "America/Argentina/Buenos_Aires"
        );
        var failure = new RuntimeException("router exploded");

        when(recommendationOrchestrator.generate(any(RecommendationCommand.class))).thenThrow(failure);

        assertThatThrownBy(() -> chatService.createChat(userId, request))
                .isSameAs(failure);

        verify(chatSessionRepository, never()).save(any(ChatSession.class));
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void createChatPersistsSessionInsideInteractionTransaction() {
        var userId = UUID.randomUUID();
        var request = new CreateChatRequestDTO(
                new UserChatMessageDTO("algo tranqui", AIModelType.BASIC),
                "America/Argentina/Buenos_Aires"
        );
        var recommendationResult = new RecommendationResult(
                "respuesta",
                List.of(),
                null,
                "titulo",
                "summary",
                new RecommendationTiming(1L, 2L, 3L),
                List.of(new ModelUsage(
                        UsageRecordStage.BASIC_RECOMMENDATION,
                        AIModelType.BASIC,
                        "gpt-test",
                        10,
                        0,
                        5
                ))
        );

        when(recommendationOrchestrator.generate(any(RecommendationCommand.class))).thenReturn(recommendationResult);
        when(chatSessionRepository.findByIdAndUserId(any(UUID.class), eq(userId))).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatExchangeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        chatService.createChat(userId, request);

        verify(chatSessionRepository).save(any(ChatSession.class));
    }

    @Test
    void createChatFailsBeforePersistenceWhenRecommendationResultHasWinnersWithoutType() {
        var userId = UUID.randomUUID();
        var request = new CreateChatRequestDTO(
                new UserChatMessageDTO("algo tranqui", AIModelType.BASIC),
                "America/Argentina/Buenos_Aires"
        );
        var invalidResult = new RecommendationResult(
                "respuesta",
                List.of(new WinnerReference(
                        com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget.ALBUM,
                        "album-node-1",
                        "Blue Hour",
                        "Stanley Turrentine"
                )),
                null,
                null,
                "summary",
                new RecommendationTiming(1L, 2L, 3L),
                List.of(new ModelUsage(
                        UsageRecordStage.BASIC_RECOMMENDATION,
                        AIModelType.BASIC,
                        "gpt-test",
                        10,
                        0,
                        5
                ))
        );

        when(recommendationOrchestrator.generate(any(RecommendationCommand.class))).thenReturn(invalidResult);

        assertThatThrownBy(() -> chatService.createChat(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendationType is required");

        verify(chatSessionRepository, never()).save(any(ChatSession.class));
        verify(transactionTemplate, never()).execute(any());
    }
}
