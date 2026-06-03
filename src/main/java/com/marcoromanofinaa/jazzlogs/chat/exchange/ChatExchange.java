package com.marcoromanofinaa.jazzlogs.chat.exchange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;

@Entity
@Table(name = "chat_exchanges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_session_id", nullable = false)
    private UUID chatSessionId;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_model", nullable = false)
    private AIModelType requestedModel;

    @Column(name = "assistant_response", nullable = false, columnDefinition = "TEXT")
    private String assistantResponse;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "winners", nullable = false, columnDefinition = "jsonb")
    private List<String> winners;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type")
    private BasicRecommendationTarget recommendationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_used", nullable = false)
    private AIModelType modelUsed;

    @Column(name = "router_latency_ms", nullable = false)
    private Long routerLatencyMs;

    @Column(name = "flow_latency_ms", nullable = false)
    private Long flowLatencyMs;

    @Column(name = "total_recommendation_latency_ms", nullable = false)
    private Long totalRecommendationLatencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ChatExchange create(
            UUID chatSessionId,
            String userMessage,
            AIModelType requestedModel,
            String assistantResponse,
            List<String> winners,
            BasicRecommendationTarget recommendationType,
            AIModelType modelUsed,
            Long routerLatencyMs,
            Long flowLatencyMs,
            Long totalRecommendationLatencyMs,
            Instant now
    ) {
        var exchange = new ChatExchange();
        exchange.chatSessionId = chatSessionId;
        exchange.userMessage = userMessage;
        exchange.requestedModel = requestedModel;
        exchange.assistantResponse = assistantResponse;
        exchange.winners = winners == null ? List.of() : List.copyOf(winners);
        exchange.recommendationType = recommendationType;
        exchange.modelUsed = modelUsed;
        exchange.routerLatencyMs = routerLatencyMs == null ? 0L : Math.max(routerLatencyMs, 0L);
        exchange.flowLatencyMs = flowLatencyMs == null ? 0L : Math.max(flowLatencyMs, 0L);
        exchange.totalRecommendationLatencyMs = totalRecommendationLatencyMs == null
                ? 0L
                : Math.max(totalRecommendationLatencyMs, 0L);
        exchange.createdAt = now;
        return exchange;
    }
}
