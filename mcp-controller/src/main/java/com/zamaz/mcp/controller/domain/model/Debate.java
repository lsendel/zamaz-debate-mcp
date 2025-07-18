package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.controller.domain.event.DebateCreatedEvent;
import com.zamaz.mcp.controller.domain.event.DebateStatusChangedEvent;
import com.zamaz.mcp.controller.domain.event.ParticipantJoinedEvent;
import com.zamaz.mcp.controller.domain.event.RoundCompletedEvent;
import com.zamaz.mcp.controller.domain.event.RoundStartedEvent;
import com.zamaz.mcp.controller.domain.event.ResponseSubmittedEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Aggregate root representing a debate.
 */
public class Debate extends AggregateRoot<DebateId> {
    
    private final DebateId id;
    private final String topic;
    private final DebateConfig config;
    private final Instant createdAt;
    private final Map<ParticipantId, Participant> participants;
    private final List<Round> rounds;
    private DebateStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String result;
    
    private Debate(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Debate ID cannot be null");
        this.topic = Objects.requireNonNull(builder.topic, "Topic cannot be null");
        this.config = Objects.requireNonNull(builder.config, "Config cannot be null");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "Created timestamp cannot be null");
        this.participants = new HashMap<>(builder.participants);
        this.rounds = new ArrayList<>(builder.rounds);
        this.status = Objects.requireNonNull(builder.status, "Status cannot be null");
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
        this.result = builder.result;
        
        validateInvariants();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Debate create(DebateId id, String topic, DebateConfig config) {
        Debate debate = builder()
            .id(id)
            .topic(topic.trim())
            .config(config)
            .createdAt(Instant.now())
            .status(DebateStatus.CREATED)
            .build();
        
        debate.addDomainEvent(new DebateCreatedEvent(id, topic, config));
        return debate;
    }
    
    @Override
    public DebateId getId() {
        return id;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public DebateConfig getConfig() {
        return config;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Map<ParticipantId, Participant> getParticipants() {
        return Collections.unmodifiableMap(participants);
    }
    
    public List<Round> getRounds() {
        return Collections.unmodifiableList(rounds);
    }
    
    public DebateStatus getStatus() {
        return status;
    }
    
    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }
    
    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }
    
    public Optional<String> getResult() {
        return Optional.ofNullable(result);
    }
    
    public void addParticipant(Participant participant) {
        Objects.requireNonNull(participant, "Participant cannot be null");
        
        if (!status.canAcceptParticipants()) {
            throw new IllegalStateException("Cannot add participants to debate in status: " + status);
        }
        
        if (participants.containsKey(participant.getId())) {
            throw new IllegalArgumentException("Participant already exists in debate");
        }
        
        if (participants.size() >= config.getMaxParticipants()) {
            throw new IllegalArgumentException("Debate is full (max " + config.getMaxParticipants() + " participants)");
        }
        
        // Validate position constraints
        if (config.requiresBalancedPositions()) {
            validatePositionBalance(participant.getPosition());
        }
        
        participants.put(participant.getId(), participant);
        addDomainEvent(new ParticipantJoinedEvent(id, participant.getId(), participant.getPosition()));
        
        // Auto-initialize if we have minimum participants
        if (participants.size() >= config.getMinParticipants() && status == DebateStatus.CREATED) {
            initialize();
        }
    }
    
    public void removeParticipant(ParticipantId participantId) {
        Objects.requireNonNull(participantId, "Participant ID cannot be null");
        
        if (!status.canAcceptParticipants()) {
            throw new IllegalStateException("Cannot remove participants from debate in status: " + status);
        }
        
        if (!participants.containsKey(participantId)) {
            throw new IllegalArgumentException("Participant not found in debate");
        }
        
        participants.remove(participantId);
    }
    
    public void initialize() {
        if (status != DebateStatus.CREATED) {
            throw new IllegalStateException("Debate must be in CREATED status to initialize");
        }
        
        if (participants.size() < config.getMinParticipants()) {
            throw new IllegalStateException("Not enough participants to initialize debate");
        }
        
        validateDebateConstraints();
        changeStatus(DebateStatus.INITIALIZED);
    }
    
    public void start() {
        if (status != DebateStatus.INITIALIZED) {
            throw new IllegalStateException("Debate must be initialized to start");
        }
        
        this.startedAt = Instant.now();
        changeStatus(DebateStatus.IN_PROGRESS);
        
        // Start first round
        startNextRound();
    }
    
    public void complete(String result) {
        if (status != DebateStatus.IN_PROGRESS) {
            throw new IllegalStateException("Debate must be in progress to complete");
        }
        
        // Complete current round if active
        getCurrentRound().ifPresent(round -> {
            if (round.isActive()) {
                round.complete();
                addDomainEvent(new RoundCompletedEvent(id, round.getId(), round.getRoundNumber()));
            }
        });
        
        this.completedAt = Instant.now();
        this.result = result;
        changeStatus(DebateStatus.COMPLETED);
    }
    
    public void cancel() {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel debate in terminal status: " + status);
        }
        
        this.completedAt = Instant.now();
        changeStatus(DebateStatus.CANCELLED);
    }
    
    public void archive() {
        if (status != DebateStatus.COMPLETED) {
            throw new IllegalStateException("Only completed debates can be archived");
        }
        
        changeStatus(DebateStatus.ARCHIVED);
    }
    
    public void submitResponse(ParticipantId participantId, ArgumentContent content, Duration responseTime) {
        Objects.requireNonNull(participantId, "Participant ID cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        
        if (!status.canAcceptResponses()) {
            throw new IllegalStateException("Debate is not accepting responses in status: " + status);
        }
        
        Participant participant = participants.get(participantId);
        if (participant == null) {
            throw new IllegalArgumentException("Participant not found in debate");
        }
        
        if (!participant.canParticipate()) {
            throw new IllegalArgumentException("Participant cannot participate");
        }
        
        Round currentRound = getCurrentRound()
            .orElseThrow(() -> new IllegalStateException("No active round found"));
        
        if (!currentRound.isActive()) {
            throw new IllegalStateException("Current round is not active");
        }
        
        // Check round expiration
        currentRound.checkExpiration();
        if (!currentRound.isActive()) {
            throw new IllegalStateException("Round has expired");
        }
        
        Response response = Response.create(
            ResponseId.generate(),
            participantId,
            participant.getPosition(),
            content,
            responseTime
        );
        
        currentRound.addResponse(response);
        participant.recordResponse(ArgumentQuality.unknown()); // Will be assessed later
        
        addDomainEvent(new ResponseSubmittedEvent(id, currentRound.getId(), response.getId(), participantId));
        
        // Check if round should be completed
        checkRoundCompletion(currentRound);
    }
    
    public Optional<Round> getCurrentRound() {
        return rounds.stream()
            .filter(Round::isActive)
            .findFirst();
    }
    
    public Optional<Round> getLastRound() {
        if (rounds.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rounds.get(rounds.size() - 1));
    }
    
    public Round getRound(int roundNumber) {
        return rounds.stream()
            .filter(round -> round.getRoundNumber() == roundNumber)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Round " + roundNumber + " not found"));
    }
    
    public int getCurrentRoundNumber() {
        return getCurrentRound()
            .map(Round::getRoundNumber)
            .orElse(0);
    }
    
    public int getTotalRounds() {
        return rounds.size();
    }
    
    public boolean canStartNextRound() {
        if (status != DebateStatus.IN_PROGRESS) {
            return false;
        }
        
        if (getCurrentRoundNumber() >= config.getMaxRounds()) {
            return false;
        }
        
        return getCurrentRound().map(round -> !round.isActive()).orElse(true);
    }
    
    public void startNextRound() {
        if (!canStartNextRound()) {
            throw new IllegalStateException("Cannot start next round");
        }
        
        int nextRoundNumber = getCurrentRoundNumber() + 1;
        if (nextRoundNumber > config.getMaxRounds()) {
            throw new IllegalStateException("Maximum rounds reached");
        }
        
        Round newRound = Round.create(
            RoundId.generate(),
            nextRoundNumber,
            config.getRoundTimeLimit()
        );
        
        rounds.add(newRound);
        addDomainEvent(new RoundStartedEvent(id, newRound.getId(), nextRoundNumber));
    }
    
    public Duration getTotalDuration() {
        if (startedAt == null) {
            return Duration.ZERO;
        }
        
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, endTime);
    }
    
    public List<Response> getAllResponses() {
        return rounds.stream()
            .flatMap(round -> round.getResponses().stream())
            .toList();
    }
    
    public List<Response> getResponsesFromParticipant(ParticipantId participantId) {
        return getAllResponses().stream()
            .filter(response -> response.getParticipantId().equals(participantId))
            .toList();
    }
    
    public Map<Position, List<Response>> getResponsesByPosition() {
        return getAllResponses().stream()
            .collect(java.util.stream.Collectors.groupingBy(Response::getPosition));
    }
    
    public ArgumentQuality getAverageQuality() {
        List<Response> allResponses = getAllResponses();
        if (allResponses.isEmpty()) {
            return ArgumentQuality.unknown();
        }
        
        double avgLogical = allResponses.stream()
            .mapToDouble(r -> r.getQuality().logicalStrength().doubleValue())
            .average().orElse(0.0);
        
        double avgEvidence = allResponses.stream()
            .mapToDouble(r -> r.getQuality().evidenceQuality().doubleValue())
            .average().orElse(0.0);
        
        double avgClarity = allResponses.stream()
            .mapToDouble(r -> r.getQuality().clarity().doubleValue())
            .average().orElse(0.0);
        
        double avgRelevance = allResponses.stream()
            .mapToDouble(r -> r.getQuality().relevance().doubleValue())
            .average().orElse(0.0);
        
        double avgOriginality = allResponses.stream()
            .mapToDouble(r -> r.getQuality().originality().doubleValue())
            .average().orElse(0.0);
        
        return ArgumentQuality.of(avgLogical, avgEvidence, avgClarity, avgRelevance, avgOriginality);
    }
    
    private void changeStatus(DebateStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Invalid status transition from " + status + " to " + newStatus
            );
        }
        
        DebateStatus oldStatus = this.status;
        this.status = newStatus;
        addDomainEvent(new DebateStatusChangedEvent(id, oldStatus, newStatus));
    }
    
    private void checkRoundCompletion(Round round) {
        // Complete round if all participants have responded
        boolean allParticipantsResponded = participants.values().stream()
            .filter(Participant::canParticipate)
            .allMatch(participant -> round.hasResponseFromParticipant(participant.getId()));
        
        if (allParticipantsResponded) {
            round.complete();
            addDomainEvent(new RoundCompletedEvent(id, round.getId(), round.getRoundNumber()));
            
            // Check if debate should be completed
            if (round.getRoundNumber() >= config.getMaxRounds()) {
                complete("Debate completed after " + config.getMaxRounds() + " rounds");
            } else if (config.shouldAutoAdvanceRounds()) {
                // Auto-start next round after a delay
                startNextRound();
            }
        }
    }
    
    private void validatePositionBalance(Position newPosition) {
        Map<String, Long> positionCounts = participants.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                p -> p.getPosition().value(),
                java.util.stream.Collectors.counting()
            ));
        
        long currentCount = positionCounts.getOrDefault(newPosition.value(), 0L);
        long maxAllowed = participants.size() / 2 + 1; // Allow slight imbalance
        
        if (currentCount >= maxAllowed) {
            throw new IllegalArgumentException(
                "Too many participants with position: " + newPosition + 
                " (current: " + currentCount + ", max: " + maxAllowed + ")"
            );
        }
    }
    
    private void validateDebateConstraints() {
        if (config.requiresBalancedPositions()) {
            Map<String, Long> positionCounts = participants.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    p -> p.getPosition().value(),
                    java.util.stream.Collectors.counting()
                ));
            
            if (positionCounts.size() < 2) {
                throw new IllegalStateException("Debate requires at least 2 different positions");
            }
        }
        
        // Validate AI participants have proper configuration
        participants.values().stream()
            .filter(Participant::isAI)
            .forEach(participant -> {
                if (participant.getProvider() == null || participant.getConfig() == null) {
                    throw new IllegalStateException(
                        "AI participant " + participant.getId() + " missing provider configuration"
                    );
                }
            });
    }
    
    private void validateInvariants() {
        if (topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty");
        }
        
        if (topic.length() > 1000) {
            throw new IllegalArgumentException("Topic cannot exceed 1000 characters");
        }
        
        if (status.isTerminal() && completedAt == null) {
            throw new IllegalArgumentException("Terminal debates must have completion timestamp");
        }
        
        if (startedAt != null && completedAt != null && completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Completion time cannot be before start time");
        }
        
        if (participants.size() > config.getMaxParticipants()) {
            throw new IllegalArgumentException("Too many participants");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Debate debate = (Debate) obj;
        return Objects.equals(id, debate.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Debate{id=%s, topic='%.50s%s', status=%s, participants=%d, rounds=%d}",
            id, topic, topic.length() > 50 ? "..." : "", status, participants.size(), rounds.size());
    }
    
    public static class Builder {
        private DebateId id;
        private String topic;
        private DebateConfig config;
        private Instant createdAt;
        private Map<ParticipantId, Participant> participants = new HashMap<>();
        private List<Round> rounds = new ArrayList<>();
        private DebateStatus status = DebateStatus.CREATED;
        private Instant startedAt;
        private Instant completedAt;
        private String result;
        
        public Builder id(DebateId id) {
            this.id = id;
            return this;
        }
        
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }
        
        public Builder config(DebateConfig config) {
            this.config = config;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder participants(Map<ParticipantId, Participant> participants) {
            this.participants = new HashMap<>(participants);
            return this;
        }
        
        public Builder rounds(List<Round> rounds) {
            this.rounds = new ArrayList<>(rounds);
            return this;
        }
        
        public Builder status(DebateStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }
        
        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }
        
        public Builder result(String result) {
            this.result = result;
            return this;
        }
        
        public Debate build() {
            return new Debate(this);
        }
    }
}