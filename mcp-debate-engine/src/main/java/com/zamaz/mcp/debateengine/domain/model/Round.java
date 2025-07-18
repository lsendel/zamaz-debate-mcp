package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.Entity;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Entity representing a debate round.
 */
public class Round implements Entity<RoundId> {
    
    private final RoundId id;
    private final DebateId debateId;
    private final int roundNumber;
    private RoundStatus status;
    private final Duration timeout;
    private final String promptTemplate;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private final List<Response> responses;
    private final LocalDateTime createdAt;
    
    /**
     * Create a new round.
     */
    public static Round create(
            RoundId id,
            DebateId debateId,
            int roundNumber,
            Duration timeout,
            String promptTemplate) {
        return new Round(
            id,
            debateId,
            roundNumber,
            RoundStatus.PENDING,
            timeout,
            promptTemplate,
            null,
            null,
            new ArrayList<>(),
            LocalDateTime.now()
        );
    }
    
    private Round(
            RoundId id,
            DebateId debateId,
            int roundNumber,
            RoundStatus status,
            Duration timeout,
            String promptTemplate,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            List<Response> responses,
            LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id);
        this.debateId = Objects.requireNonNull(debateId);
        this.roundNumber = roundNumber;
        this.status = Objects.requireNonNull(status);
        this.timeout = Objects.requireNonNull(timeout);
        this.promptTemplate = promptTemplate;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.responses = new ArrayList<>(responses);
        this.createdAt = Objects.requireNonNull(createdAt);
        
        if (roundNumber < 1) {
            throw new IllegalArgumentException("Round number must be positive");
        }
    }
    
    /**
     * Start the round.
     */
    public void start() {
        if (!status.canStart()) {
            throw new IllegalStateException("Round cannot be started in status: " + status);
        }
        this.status = RoundStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
    }
    
    /**
     * Complete the round.
     */
    public void complete() {
        if (!status.isActive()) {
            throw new IllegalStateException("Only active rounds can be completed");
        }
        this.status = RoundStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Timeout the round.
     */
    public void timeout() {
        if (!status.isActive()) {
            throw new IllegalStateException("Only active rounds can timeout");
        }
        this.status = RoundStatus.TIMEOUT;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Add a response to the round.
     */
    public Response addResponse(
            ParticipantId participantId,
            ResponseContent content,
            long responseTimeMs,
            int tokenCount) {
        if (!status.isActive()) {
            throw new IllegalStateException("Cannot add response to inactive round");
        }
        
        // Check if participant already responded
        boolean alreadyResponded = responses.stream()
            .anyMatch(r -> r.getParticipantId().equals(participantId));
        if (alreadyResponded) {
            throw new IllegalStateException("Participant has already responded in this round");
        }
        
        Response response = Response.create(
            ResponseId.generate(),
            id,
            participantId,
            content,
            responses.size() + 1,
            responseTimeMs,
            tokenCount
        );
        
        responses.add(response);
        return response;
    }
    
    /**
     * Check if round has timed out.
     */
    public boolean hasTimedOut() {
        if (!status.isActive() || startedAt == null) {
            return false;
        }
        Duration elapsed = Duration.between(startedAt, LocalDateTime.now());
        return elapsed.compareTo(timeout) > 0;
    }
    
    /**
     * Get remaining time.
     */
    public Duration getRemainingTime() {
        if (!status.isActive() || startedAt == null) {
            return Duration.ZERO;
        }
        Duration elapsed = Duration.between(startedAt, LocalDateTime.now());
        Duration remaining = timeout.minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
    
    @Override
    public RoundId getId() {
        return id;
    }
    
    public DebateId getDebateId() {
        return debateId;
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public RoundStatus getStatus() {
        return status;
    }
    
    public Duration getTimeout() {
        return timeout;
    }
    
    public String getPromptTemplate() {
        return promptTemplate;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public List<Response> getResponses() {
        return Collections.unmodifiableList(responses);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Round round = (Round) o;
        return Objects.equals(id, round.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}