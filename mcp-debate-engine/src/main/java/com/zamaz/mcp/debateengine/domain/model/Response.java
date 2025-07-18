package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.Entity;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a participant response in a round.
 */
public class Response implements Entity<ResponseId> {
    
    private final ResponseId id;
    private final RoundId roundId;
    private final ParticipantId participantId;
    private final ResponseContent content;
    private final int responseOrder;
    private final long responseTimeMs;
    private final int tokenCount;
    private QualityScore qualityScore;
    private final LocalDateTime createdAt;
    
    /**
     * Create a new response.
     */
    public static Response create(
            ResponseId id,
            RoundId roundId,
            ParticipantId participantId,
            ResponseContent content,
            int responseOrder,
            long responseTimeMs,
            int tokenCount) {
        return new Response(
            id,
            roundId,
            participantId,
            content,
            responseOrder,
            responseTimeMs,
            tokenCount,
            null,
            LocalDateTime.now()
        );
    }
    
    private Response(
            ResponseId id,
            RoundId roundId,
            ParticipantId participantId,
            ResponseContent content,
            int responseOrder,
            long responseTimeMs,
            int tokenCount,
            QualityScore qualityScore,
            LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id);
        this.roundId = Objects.requireNonNull(roundId);
        this.participantId = Objects.requireNonNull(participantId);
        this.content = Objects.requireNonNull(content);
        this.responseOrder = responseOrder;
        this.responseTimeMs = responseTimeMs;
        this.tokenCount = tokenCount;
        this.qualityScore = qualityScore;
        this.createdAt = Objects.requireNonNull(createdAt);
        
        if (responseOrder < 1) {
            throw new IllegalArgumentException("Response order must be positive");
        }
        if (responseTimeMs < 0) {
            throw new IllegalArgumentException("Response time cannot be negative");
        }
        if (tokenCount < 0) {
            throw new IllegalArgumentException("Token count cannot be negative");
        }
    }
    
    /**
     * Set quality score for the response.
     */
    public void setQualityScore(QualityScore score) {
        Objects.requireNonNull(score, "Quality score cannot be null");
        this.qualityScore = score;
    }
    
    /**
     * Check if quality has been scored.
     */
    public boolean hasQualityScore() {
        return qualityScore != null && qualityScore.isCalculated();
    }
    
    @Override
    public ResponseId getId() {
        return id;
    }
    
    public RoundId getRoundId() {
        return roundId;
    }
    
    public ParticipantId getParticipantId() {
        return participantId;
    }
    
    public ResponseContent getContent() {
        return content;
    }
    
    public int getResponseOrder() {
        return responseOrder;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    public QualityScore getQualityScore() {
        return qualityScore;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Response response = (Response) o;
        return Objects.equals(id, response.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}