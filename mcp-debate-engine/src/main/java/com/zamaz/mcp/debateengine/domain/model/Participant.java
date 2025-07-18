package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.Entity;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a debate participant.
 */
public class Participant implements Entity<ParticipantId> {
    
    private final ParticipantId id;
    private final DebateId debateId;
    private final ParticipantType type;
    private final Position position;
    private final UUID userId; // Null for AI participants
    private final AIModel aiModel; // Null for human participants
    private final LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private int totalResponses;
    private long totalResponseTimeMs;
    
    /**
     * Create human participant.
     */
    public static Participant createHuman(
            ParticipantId id,
            DebateId debateId,
            UUID userId,
            Position position) {
        Objects.requireNonNull(userId, "User ID required for human participant");
        return new Participant(
            id,
            debateId,
            ParticipantType.HUMAN,
            position,
            userId,
            null,
            LocalDateTime.now()
        );
    }
    
    /**
     * Create AI participant.
     */
    public static Participant createAI(
            ParticipantId id,
            DebateId debateId,
            AIModel aiModel,
            Position position) {
        Objects.requireNonNull(aiModel, "AI model required for AI participant");
        return new Participant(
            id,
            debateId,
            ParticipantType.AI,
            position,
            null,
            aiModel,
            LocalDateTime.now()
        );
    }
    
    private Participant(
            ParticipantId id,
            DebateId debateId,
            ParticipantType type,
            Position position,
            UUID userId,
            AIModel aiModel,
            LocalDateTime joinedAt) {
        this.id = Objects.requireNonNull(id);
        this.debateId = Objects.requireNonNull(debateId);
        this.type = Objects.requireNonNull(type);
        this.position = Objects.requireNonNull(position);
        this.userId = userId;
        this.aiModel = aiModel;
        this.joinedAt = Objects.requireNonNull(joinedAt);
        this.totalResponses = 0;
        this.totalResponseTimeMs = 0;
    }
    
    /**
     * Record a response from this participant.
     */
    public void recordResponse(long responseTimeMs) {
        if (responseTimeMs < 0) {
            throw new IllegalArgumentException("Response time cannot be negative");
        }
        this.totalResponses++;
        this.totalResponseTimeMs += responseTimeMs;
    }
    
    /**
     * Mark participant as left.
     */
    public void leave() {
        if (this.leftAt != null) {
            throw new IllegalStateException("Participant has already left");
        }
        this.leftAt = LocalDateTime.now();
    }
    
    /**
     * Check if participant is active.
     */
    public boolean isActive() {
        return leftAt == null;
    }
    
    /**
     * Get average response time in milliseconds.
     */
    public long getAverageResponseTimeMs() {
        if (totalResponses == 0) {
            return 0;
        }
        return totalResponseTimeMs / totalResponses;
    }
    
    @Override
    public ParticipantId getId() {
        return id;
    }
    
    public DebateId getDebateId() {
        return debateId;
    }
    
    public ParticipantType getType() {
        return type;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public AIModel getAiModel() {
        return aiModel;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public LocalDateTime getLeftAt() {
        return leftAt;
    }
    
    public int getTotalResponses() {
        return totalResponses;
    }
    
    public long getTotalResponseTimeMs() {
        return totalResponseTimeMs;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}