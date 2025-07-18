package com.zamaz.mcp.controller.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.DebateStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a debate status changes.
 */
public class DebateStatusChangedEvent implements DomainEvent {
    
    private final DebateId debateId;
    private final DebateStatus oldStatus;
    private final DebateStatus newStatus;
    private final Instant occurredAt;
    
    public DebateStatusChangedEvent(DebateId debateId, DebateStatus oldStatus, DebateStatus newStatus) {
        this.debateId = Objects.requireNonNull(debateId, "Debate ID cannot be null");
        this.oldStatus = Objects.requireNonNull(oldStatus, "Old status cannot be null");
        this.newStatus = Objects.requireNonNull(newStatus, "New status cannot be null");
        this.occurredAt = Instant.now();
    }
    
    public DebateId getDebateId() {
        return debateId;
    }
    
    public DebateStatus getOldStatus() {
        return oldStatus;
    }
    
    public DebateStatus getNewStatus() {
        return newStatus;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "DebateStatusChanged";
    }
    
    @Override
    public String getAggregateId() {
        return debateId.toString();
    }
    
    public boolean isTransitionToActive() {
        return newStatus == DebateStatus.IN_PROGRESS;
    }
    
    public boolean isTransitionToTerminal() {
        return newStatus.isTerminal();
    }
    
    public boolean isStarting() {
        return oldStatus == DebateStatus.INITIALIZED && newStatus == DebateStatus.IN_PROGRESS;
    }
    
    public boolean isCompleting() {
        return oldStatus == DebateStatus.IN_PROGRESS && newStatus == DebateStatus.COMPLETED;
    }
    
    public boolean isCancelling() {
        return newStatus == DebateStatus.CANCELLED;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DebateStatusChangedEvent that = (DebateStatusChangedEvent) obj;
        return Objects.equals(debateId, that.debateId) && 
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(debateId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("DebateStatusChangedEvent{debateId=%s, %s -> %s, occurredAt=%s}",
            debateId, oldStatus, newStatus, occurredAt);
    }
}