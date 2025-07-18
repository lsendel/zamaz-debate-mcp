package com.zamaz.mcp.controller.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.ParticipantId;
import com.zamaz.mcp.controller.domain.model.Position;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a participant joins a debate.
 */
public class ParticipantJoinedEvent implements DomainEvent {
    
    private final DebateId debateId;
    private final ParticipantId participantId;
    private final Position position;
    private final Instant occurredAt;
    
    public ParticipantJoinedEvent(DebateId debateId, ParticipantId participantId, Position position) {
        this.debateId = Objects.requireNonNull(debateId, "Debate ID cannot be null");
        this.participantId = Objects.requireNonNull(participantId, "Participant ID cannot be null");
        this.position = Objects.requireNonNull(position, "Position cannot be null");
        this.occurredAt = Instant.now();
    }
    
    public DebateId getDebateId() {
        return debateId;
    }
    
    public ParticipantId getParticipantId() {
        return participantId;
    }
    
    public Position getPosition() {
        return position;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "ParticipantJoined";
    }
    
    @Override
    public String getAggregateId() {
        return debateId.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ParticipantJoinedEvent that = (ParticipantJoinedEvent) obj;
        return Objects.equals(debateId, that.debateId) && 
               Objects.equals(participantId, that.participantId) &&
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(debateId, participantId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("ParticipantJoinedEvent{debateId=%s, participantId=%s, position=%s, occurredAt=%s}",
            debateId, participantId, position, occurredAt);
    }
}