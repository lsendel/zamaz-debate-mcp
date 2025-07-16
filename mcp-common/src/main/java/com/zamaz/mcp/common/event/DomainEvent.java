package com.zamaz.mcp.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all domain events in the MCP system
 * Uses Jackson polymorphic serialization for event type handling
 */
@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DebateCreatedEvent.class, name = "DEBATE_CREATED"),
    @JsonSubTypes.Type(value = DebateStartedEvent.class, name = "DEBATE_STARTED"),
    @JsonSubTypes.Type(value = DebateCompletedEvent.class, name = "DEBATE_COMPLETED"),
    @JsonSubTypes.Type(value = MessageAddedEvent.class, name = "MESSAGE_ADDED"),
    @JsonSubTypes.Type(value = ParticipantJoinedEvent.class, name = "PARTICIPANT_JOINED"),
    @JsonSubTypes.Type(value = ParticipantLeftEvent.class, name = "PARTICIPANT_LEFT"),
    @JsonSubTypes.Type(value = OrganizationCreatedEvent.class, name = "ORGANIZATION_CREATED"),
    @JsonSubTypes.Type(value = UserRegisteredEvent.class, name = "USER_REGISTERED")
})
public abstract class DomainEvent {
    
    // Event metadata
    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String version;
    private String correlationId;
    
    // Aggregate information
    private String aggregateId;
    private String aggregateType;
    private Long aggregateVersion;
    
    // Context information
    private String organizationId;
    private String userId;
    private String sourceService;
    
    // Additional metadata
    private Map<String, Object> metadata = new HashMap<>();
    
    protected DomainEvent(String eventType, String aggregateId, String aggregateType) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
    }
    
    /**
     * Add custom metadata to the event
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    /**
     * Get metadata value by key
     */
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
}