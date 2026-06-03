package com.marcoromanofinaa.jazzlogs.chat.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title")
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation_state", columnDefinition = "jsonb")
    private ChatRecommendationMemory recommendationMemory;

    @Column(name = "last_interaction_at", nullable = false)
    private Instant lastInteractionAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ChatSession create(UUID userId, Instant now) {
        var session = new ChatSession();
        session.userId = userId;
        session.createdAt = now;
        session.updatedAt = now;
        session.lastInteractionAt = now;
        return session;
    }

    public void updateTitle(String title, Instant now) {
        this.title = title;
        this.updatedAt = now;
    }

    public void markInteraction(Instant now) {
        this.lastInteractionAt = now;
        this.updatedAt = now;
    }

    public void updateRecommendationMemory(ChatRecommendationMemory recommendationMemory, Instant now) {
        this.recommendationMemory = recommendationMemory;
        this.updatedAt = now;
    }

    public void delete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean belongsTo(UUID userId) {
        return this.userId.equals(userId);
    }
}
