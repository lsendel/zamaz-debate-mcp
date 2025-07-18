package com.zamaz.mcp.controller.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.RoundId;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a round starts.
 */
public class RoundStartedEvent implements DomainEvent {
    
    private final DebateId debateId;
    private final RoundId roundId;
    private final int roundNumber;
    private final Instant occurredAt;
    
    public RoundStartedEvent(DebateId debateId, RoundId roundId, int roundNumber) {
        this.debateId = Objects.requireNonNull(debateId, "Debate ID cannot be null");
        this.roundId = Objects.requireNonNull(roundId, "Round ID cannot be null");
        this.roundNumber = roundNumber;
        this.occurredAt = Instant.now();
    }
    
    public DebateId getDebateId() {
        return debateId;
    }
    
    public RoundId getRoundId() {
        return roundId;
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "RoundStarted";
    }
    
    @Override
    public String getAggregateId() {
        return debateId.toString();
    }
    
    public boolean isFirstRound() {
        return roundNumber == 1;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RoundStartedEvent that = (RoundStartedEvent) obj;
        return Objects.equals(debateId, that.debateId) && 
               Objects.equals(roundId, that.roundId) &&
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(debateId, roundId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("RoundStartedEvent{debateId=%s, roundId=%s, roundNumber=%d, occurredAt=%s}",
            debateId, roundId, roundNumber, occurredAt);
    }
}