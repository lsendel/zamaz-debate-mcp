package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.Entity;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Entity representing a round in a debate.
 */
public class Round implements Entity<RoundId> {
    
    private final RoundId id;
    private final int roundNumber;
    private final Instant startedAt;
    private final Duration timeLimit;
    private final List<Response> responses;
    private Instant completedAt;
    private RoundStatus status;
    
    private Round(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Round ID cannot be null");
        this.roundNumber = builder.roundNumber;
        this.startedAt = Objects.requireNonNull(builder.startedAt, "Started timestamp cannot be null");
        this.timeLimit = builder.timeLimit;
        this.responses = new ArrayList<>(builder.responses);
        this.completedAt = builder.completedAt;
        this.status = Objects.requireNonNull(builder.status, "Round status cannot be null");
        
        validateInvariants();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Round create(RoundId id, int roundNumber, Duration timeLimit) {
        return builder()
            .id(id)
            .roundNumber(roundNumber)
            .startedAt(Instant.now())
            .timeLimit(timeLimit)
            .status(RoundStatus.ACTIVE)
            .build();
    }
    
    @Override
    public RoundId getId() {
        return id;
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public Duration getTimeLimit() {
        return timeLimit;
    }
    
    public List<Response> getResponses() {
        return Collections.unmodifiableList(responses);
    }
    
    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }
    
    public RoundStatus getStatus() {
        return status;
    }
    
    public boolean isActive() {
        return status == RoundStatus.ACTIVE;
    }
    
    public boolean isCompleted() {
        return status == RoundStatus.COMPLETED;
    }
    
    public boolean isExpired() {
        return status == RoundStatus.EXPIRED;
    }
    
    public boolean hasTimeLimit() {
        return timeLimit != null && !timeLimit.isZero();
    }
    
    public boolean isExpiredByTime() {
        if (!hasTimeLimit()) {
            return false;
        }
        
        Instant deadline = startedAt.plus(timeLimit);
        return Instant.now().isAfter(deadline);
    }
    
    public Duration getRemainingTime() {
        if (!hasTimeLimit() || !isActive()) {
            return Duration.ZERO;
        }
        
        Instant deadline = startedAt.plus(timeLimit);
        Instant now = Instant.now();
        
        if (now.isAfter(deadline)) {
            return Duration.ZERO;
        }
        
        return Duration.between(now, deadline);
    }
    
    public Duration getElapsedTime() {
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, endTime);
    }
    
    public void addResponse(Response response) {
        Objects.requireNonNull(response, "Response cannot be null");
        
        if (!isActive()) {
            throw new IllegalStateException("Cannot add response to non-active round");
        }
        
        if (hasResponseFromParticipant(response.getParticipantId())) {
            throw new IllegalArgumentException(
                "Participant " + response.getParticipantId() + " has already responded in this round"
            );
        }
        
        responses.add(response);
    }
    
    public void complete() {
        if (!isActive()) {
            throw new IllegalStateException("Round is not active");
        }
        
        this.status = RoundStatus.COMPLETED;
        this.completedAt = Instant.now();
    }
    
    public void expire() {
        if (!isActive()) {
            throw new IllegalStateException("Round is not active");
        }
        
        this.status = RoundStatus.EXPIRED;
        this.completedAt = Instant.now();
    }
    
    public void checkExpiration() {
        if (isActive() && isExpiredByTime()) {
            expire();
        }
    }
    
    public boolean hasResponseFromParticipant(ParticipantId participantId) {
        Objects.requireNonNull(participantId, "Participant ID cannot be null");
        return responses.stream()
            .anyMatch(response -> response.getParticipantId().equals(participantId));
    }
    
    public Optional<Response> getResponseFromParticipant(ParticipantId participantId) {
        Objects.requireNonNull(participantId, "Participant ID cannot be null");
        return responses.stream()
            .filter(response -> response.getParticipantId().equals(participantId))
            .findFirst();
    }
    
    public List<Response> getResponsesByPosition(Position position) {
        Objects.requireNonNull(position, "Position cannot be null");
        return responses.stream()
            .filter(response -> response.getPosition().equals(position))
            .toList();
    }
    
    public int getResponseCount() {
        return responses.size();
    }
    
    public int getResponseCountForPosition(Position position) {
        return getResponsesByPosition(position).size();
    }
    
    public ArgumentQuality getAverageQuality() {
        if (responses.isEmpty()) {
            return ArgumentQuality.unknown();
        }
        
        double avgLogical = responses.stream()
            .mapToDouble(r -> r.getQuality().logicalStrength().doubleValue())
            .average().orElse(0.0);
        
        double avgEvidence = responses.stream()
            .mapToDouble(r -> r.getQuality().evidenceQuality().doubleValue())
            .average().orElse(0.0);
        
        double avgClarity = responses.stream()
            .mapToDouble(r -> r.getQuality().clarity().doubleValue())
            .average().orElse(0.0);
        
        double avgRelevance = responses.stream()
            .mapToDouble(r -> r.getQuality().relevance().doubleValue())
            .average().orElse(0.0);
        
        double avgOriginality = responses.stream()
            .mapToDouble(r -> r.getQuality().originality().doubleValue())
            .average().orElse(0.0);
        
        return ArgumentQuality.of(avgLogical, avgEvidence, avgClarity, avgRelevance, avgOriginality);
    }
    
    public Optional<Response> getBestResponse() {
        return responses.stream()
            .max((r1, r2) -> r1.getQuality().compareTo(r2.getQuality()));
    }
    
    public Round withStatus(RoundStatus newStatus) {
        Objects.requireNonNull(newStatus, "Status cannot be null");
        return builder()
            .id(this.id)
            .roundNumber(this.roundNumber)
            .startedAt(this.startedAt)
            .timeLimit(this.timeLimit)
            .responses(this.responses)
            .completedAt(this.completedAt)
            .status(newStatus)
            .build();
    }
    
    private void validateInvariants() {
        if (roundNumber < 1) {
            throw new IllegalArgumentException("Round number must be positive");
        }
        
        if (timeLimit != null && timeLimit.isNegative()) {
            throw new IllegalArgumentException("Time limit cannot be negative");
        }
        
        if (status == RoundStatus.COMPLETED || status == RoundStatus.EXPIRED) {
            if (completedAt == null) {
                throw new IllegalArgumentException("Completed/expired rounds must have completion timestamp");
            }
            if (completedAt.isBefore(startedAt)) {
                throw new IllegalArgumentException("Completion time cannot be before start time");
            }
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Round round = (Round) obj;
        return Objects.equals(id, round.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Round{id=%s, number=%d, status=%s, responses=%d, elapsed=%s}",
            id, roundNumber, status, responses.size(), getElapsedTime());
    }
    
    public static class Builder {
        private RoundId id;
        private int roundNumber = 1;
        private Instant startedAt;
        private Duration timeLimit;
        private List<Response> responses = new ArrayList<>();
        private Instant completedAt;
        private RoundStatus status = RoundStatus.ACTIVE;
        
        public Builder id(RoundId id) {
            this.id = id;
            return this;
        }
        
        public Builder roundNumber(int roundNumber) {
            this.roundNumber = roundNumber;
            return this;
        }
        
        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }
        
        public Builder timeLimit(Duration timeLimit) {
            this.timeLimit = timeLimit;
            return this;
        }
        
        public Builder responses(List<Response> responses) {
            this.responses = new ArrayList<>(responses);
            return this;
        }
        
        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }
        
        public Builder status(RoundStatus status) {
            this.status = status;
            return this;
        }
        
        public Round build() {
            return new Round(this);
        }
    }
}