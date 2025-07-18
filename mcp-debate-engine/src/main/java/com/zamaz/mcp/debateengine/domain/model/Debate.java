package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.AggregateRoot;
import com.zamaz.mcp.common.domain.DomainEvent;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregate root representing a debate.
 */
public class Debate extends AggregateRoot<DebateId> {
    
    private final DebateId id;
    private final OrganizationId organizationId;
    private final UUID createdByUserId;
    private final DebateTopic topic;
    private String description;
    private DebateStatus status;
    private final DebateConfiguration configuration;
    private int currentRound;
    private ContextId contextId;
    private final List<Participant> participants;
    private final List<Round> rounds;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Create a new debate.
     */
    public static Debate create(
            DebateId id,
            OrganizationId organizationId,
            UUID createdByUserId,
            DebateTopic topic,
            String description,
            DebateConfiguration configuration) {
        Debate debate = new Debate(
            id,
            organizationId,
            createdByUserId,
            topic,
            description,
            DebateStatus.DRAFT,
            configuration,
            0,
            null,
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        debate.raiseEvent(new DebateCreatedEvent(
            id,
            organizationId,
            createdByUserId,
            topic.toString(),
            LocalDateTime.now()
        ));
        
        return debate;
    }
    
    private Debate(
            DebateId id,
            OrganizationId organizationId,
            UUID createdByUserId,
            DebateTopic topic,
            String description,
            DebateStatus status,
            DebateConfiguration configuration,
            int currentRound,
            ContextId contextId,
            List<Participant> participants,
            List<Round> rounds,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.createdByUserId = Objects.requireNonNull(createdByUserId);
        this.topic = Objects.requireNonNull(topic);
        this.description = description;
        this.status = Objects.requireNonNull(status);
        this.configuration = Objects.requireNonNull(configuration);
        this.currentRound = currentRound;
        this.contextId = contextId;
        this.participants = new ArrayList<>(participants);
        this.rounds = new ArrayList<>(rounds);
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }
    
    /**
     * Add a participant to the debate.
     */
    public void addParticipant(Participant participant) {
        if (!status.canStart()) {
            throw new IllegalStateException("Cannot add participants to debate in status: " + status);
        }
        
        // Validate participant limit
        if (participants.size() >= configuration.maxParticipants()) {
            throw new IllegalStateException("Debate has reached maximum participants");
        }
        
        // Validate participant doesn't already exist
        boolean alreadyExists = participants.stream()
            .anyMatch(p -> p.getId().equals(participant.getId()));
        if (alreadyExists) {
            throw new IllegalStateException("Participant already exists in debate");
        }
        
        // Validate position not taken
        boolean positionTaken = participants.stream()
            .anyMatch(p -> p.getPosition() == participant.getPosition() && 
                          participant.getPosition().canParticipate());
        if (positionTaken) {
            throw new IllegalStateException("Position already taken: " + participant.getPosition());
        }
        
        participants.add(participant);
        updatedAt = LocalDateTime.now();
        
        raiseEvent(new ParticipantJoinedEvent(
            id,
            participant.getId(),
            participant.getType(),
            participant.getPosition(),
            LocalDateTime.now()
        ));
    }
    
    /**
     * Set the context for the debate.
     */
    public void setContext(ContextId contextId) {
        Objects.requireNonNull(contextId, "Context ID cannot be null");
        this.contextId = contextId;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Start the debate.
     */
    public void start() {
        if (!status.canStart()) {
            throw new IllegalStateException("Cannot start debate in status: " + status);
        }
        
        // Validate minimum participants
        long activeParticipants = participants.stream()
            .filter(p -> p.getPosition().canParticipate())
            .count();
        if (activeParticipants < 2) {
            throw new IllegalStateException("Debate requires at least 2 participants");
        }
        
        // Validate context is set
        if (contextId == null) {
            throw new IllegalStateException("Context must be set before starting debate");
        }
        
        this.status = DebateStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        raiseEvent(new DebateStartedEvent(
            id,
            LocalDateTime.now()
        ));
    }
    
    /**
     * Start a new round.
     */
    public Round startNewRound(String promptTemplate) {
        if (status != DebateStatus.ACTIVE) {
            throw new IllegalStateException("Can only start rounds in active debates");
        }
        
        // Check if current round is completed
        if (!rounds.isEmpty()) {
            Round lastRound = rounds.get(rounds.size() - 1);
            if (!lastRound.getStatus().isFinished()) {
                throw new IllegalStateException("Previous round is not finished");
            }
        }
        
        // Check round limit
        if (rounds.size() >= configuration.maxRounds()) {
            throw new IllegalStateException("Debate has reached maximum rounds");
        }
        
        currentRound++;
        Round round = Round.create(
            RoundId.generate(),
            id,
            currentRound,
            configuration.roundTimeout(),
            promptTemplate
        );
        
        rounds.add(round);
        round.start();
        updatedAt = LocalDateTime.now();
        
        raiseEvent(new RoundStartedEvent(
            id,
            round.getId(),
            currentRound,
            LocalDateTime.now()
        ));
        
        return round;
    }
    
    /**
     * Complete the current round.
     */
    public void completeCurrentRound() {
        Round round = getCurrentRound()
            .orElseThrow(() -> new IllegalStateException("No active round"));
        
        round.complete();
        updatedAt = LocalDateTime.now();
        
        raiseEvent(new RoundCompletedEvent(
            id,
            round.getId(),
            round.getRoundNumber(),
            LocalDateTime.now()
        ));
        
        // Check if debate should complete
        if (rounds.size() >= configuration.maxRounds()) {
            complete();
        }
    }
    
    /**
     * Complete the debate.
     */
    public void complete() {
        if (status != DebateStatus.ACTIVE) {
            throw new IllegalStateException("Can only complete active debates");
        }
        
        this.status = DebateStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        raiseEvent(new DebateCompletedEvent(
            id,
            LocalDateTime.now()
        ));
    }
    
    /**
     * Cancel the debate.
     */
    public void cancel(String reason) {
        if (!status.canCancel()) {
            throw new IllegalStateException("Cannot cancel debate in status: " + status);
        }
        
        this.status = DebateStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        raiseEvent(new DebateCancelledEvent(
            id,
            reason,
            LocalDateTime.now()
        ));
    }
    
    /**
     * Get the current active round.
     */
    public Optional<Round> getCurrentRound() {
        return rounds.stream()
            .filter(r -> r.getStatus().isActive())
            .findFirst();
    }
    
    /**
     * Get participant by ID.
     */
    public Optional<Participant> getParticipant(ParticipantId id) {
        return participants.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst();
    }
    
    @Override
    public DebateId getId() {
        return id;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public UUID getCreatedByUserId() {
        return createdByUserId;
    }
    
    public DebateTopic getTopic() {
        return topic;
    }
    
    public String getDescription() {
        return description;
    }
    
    public DebateStatus getStatus() {
        return status;
    }
    
    public DebateConfiguration getConfiguration() {
        return configuration;
    }
    
    public int getCurrentRoundNumber() {
        return currentRound;
    }
    
    public ContextId getContextId() {
        return contextId;
    }
    
    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }
    
    public List<Round> getRounds() {
        return Collections.unmodifiableList(rounds);
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Debate debate = (Debate) o;
        return Objects.equals(id, debate.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    // Domain Events
    
    public record DebateCreatedEvent(
        DebateId debateId,
        OrganizationId organizationId,
        UUID userId,
        String topic,
        LocalDateTime occurredAt
    ) implements DomainEvent {}
    
    public record DebateStartedEvent(
        DebateId debateId,
        LocalDateTime occurredAt
    ) implements DomainEvent {}
    
    public record DebateCompletedEvent(
        DebateId debateId,
        LocalDateTime occurredAt
    ) implements DomainEvent {}
    
    public record DebateCancelledEvent(
        DebateId debateId,
        String reason,
        LocalDateTime occurredAt
    ) implements DomainEvent {}
    
    public record ParticipantJoinedEvent(
        DebateId debateId,
        ParticipantId participantId,
        ParticipantType type,
        Position position,
        LocalDateTime occurredAt
    ) implements DomainEvent {}
    
    public record RoundStartedEvent(
        DebateId debateId,
        RoundId roundId,
        int roundNumber,
        LocalDateTime occurredAt
    ) implements DomainEvent {}
    
    public record RoundCompletedEvent(
        DebateId debateId,
        RoundId roundId,
        int roundNumber,
        LocalDateTime occurredAt
    ) implements DomainEvent {}
}