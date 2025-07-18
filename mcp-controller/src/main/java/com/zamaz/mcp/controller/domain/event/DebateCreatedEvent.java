package com.zamaz.mcp.controller.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.controller.domain.model.DebateConfig;
import com.zamaz.mcp.controller.domain.model.DebateId;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a debate is created.
 */
public class DebateCreatedEvent implements DomainEvent {
    
    private final DebateId debateId;
    private final String topic;
    private final DebateConfig config;
    private final Instant occurredAt;
    
    public DebateCreatedEvent(DebateId debateId, String topic, DebateConfig config) {
        this.debateId = Objects.requireNonNull(debateId, "Debate ID cannot be null");
        this.topic = Objects.requireNonNull(topic, "Topic cannot be null");
        this.config = Objects.requireNonNull(config, "Config cannot be null");
        this.occurredAt = Instant.now();
    }
    
    public DebateId getDebateId() {
        return debateId;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public DebateConfig getConfig() {
        return config;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "DebateCreated";
    }
    
    @Override
    public String getAggregateId() {
        return debateId.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DebateCreatedEvent that = (DebateCreatedEvent) obj;
        return Objects.equals(debateId, that.debateId) && 
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(debateId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("DebateCreatedEvent{debateId=%s, topic='%.50s%s', occurredAt=%s}",
            debateId, topic, topic.length() > 50 ? "..." : "", occurredAt);
    }
}