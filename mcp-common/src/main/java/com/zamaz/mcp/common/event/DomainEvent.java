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
    
    // Defensive getter for metadata
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    // Defensive setter for metadata
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }
    
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
    
    // Standard getters and setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }
    
    public String getAggregateType() {
        return aggregateType;
    }
    
    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }
    
    public Long getAggregateVersion() {
        return aggregateVersion;
    }
    
    public void setAggregateVersion(Long aggregateVersion) {
        this.aggregateVersion = aggregateVersion;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSourceService() {
        return sourceService;
    }
    
    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }
}