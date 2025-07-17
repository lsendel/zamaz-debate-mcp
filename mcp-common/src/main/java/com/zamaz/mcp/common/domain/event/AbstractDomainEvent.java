package com.zamaz.mcp.common.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base implementation of DomainEvent providing common functionality.
 * This is a pure domain class with no framework dependencies.
 */
public abstract class AbstractDomainEvent implements DomainEvent {
    
    private final UUID eventId;
    private final Instant occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    
    protected AbstractDomainEvent(String aggregateId, String aggregateType) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate ID cannot be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "Aggregate type cannot be null");
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public String getAggregateType() {
        return aggregateType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractDomainEvent that = (AbstractDomainEvent) o;
        return Objects.equals(eventId, that.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}