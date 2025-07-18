package com.zamaz.mcp.controller.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.ParticipantId;
import com.zamaz.mcp.controller.domain.model.ResponseId;
import com.zamaz.mcp.controller.domain.model.RoundId;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a response is submitted to a round.
 */
public class ResponseSubmittedEvent implements DomainEvent {
    
    private final DebateId debateId;
    private final RoundId roundId;
    private final ResponseId responseId;
    private final ParticipantId participantId;
    private final Instant occurredAt;
    
    public ResponseSubmittedEvent(DebateId debateId, RoundId roundId, ResponseId responseId, ParticipantId participantId) {
        this.debateId = Objects.requireNonNull(debateId, "Debate ID cannot be null");
        this.roundId = Objects.requireNonNull(roundId, "Round ID cannot be null");
        this.responseId = Objects.requireNonNull(responseId, "Response ID cannot be null");
        this.participantId = Objects.requireNonNull(participantId, "Participant ID cannot be null");
        this.occurredAt = Instant.now();
    }
    
    public DebateId getDebateId() {
        return debateId;
    }
    
    public RoundId getRoundId() {
        return roundId;
    }
    
    public ResponseId getResponseId() {
        return responseId;
    }
    
    public ParticipantId getParticipantId() {
        return participantId;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "ResponseSubmitted";
    }
    
    @Override
    public String getAggregateId() {
        return debateId.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ResponseSubmittedEvent that = (ResponseSubmittedEvent) obj;
        return Objects.equals(debateId, that.debateId) && 
               Objects.equals(responseId, that.responseId) &&
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(debateId, responseId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("ResponseSubmittedEvent{debateId=%s, roundId=%s, responseId=%s, participantId=%s, occurredAt=%s}",
            debateId, roundId, responseId, participantId, occurredAt);
    }
}